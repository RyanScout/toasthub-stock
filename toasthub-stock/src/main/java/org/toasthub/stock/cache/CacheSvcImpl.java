package org.toasthub.stock.cache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.stock.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.stock.model.TradeSignalCache;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Service("CacheSvc")
public class CacheSvcImpl implements CacheSvc {

    @Autowired
    private CacheDao cacheDao;

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void process(final Request request, final Response response) {
        final String action = (String) request.getParams().get("action");
        switch (action) {
            case "ITEM":
                item(request, response);
                break;
            case "LIST":
                items(request, response);
                break;
            case "SAVE":
                save(request, response);
                break;
            case "DELETE":
                delete(request, response);
                break;
            case "BACKLOAD":
                cacheManager.backloadTechnicalIndicator(request, response);
        }

    }

    @Override
    public void save(final Request request, final Response response) {
        final Collection<String> symbols = new ArrayList<String>();

        for (final Object o : ArrayList.class.cast(request.getParam("SYMBOLS"))) {
            symbols.add(String.class.cast(o));
        }

        symbols.stream()
                .distinct()
                .filter(symbol -> Arrays.asList(Symbol.SYMBOLS).contains(symbol))
                .forEach(symbol -> {
                    request.addParam(GlobalConstant.SYMBOL, symbol);
                    try {
                        cacheDao.itemCount(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                        return;
                    }

                    final TechnicalIndicator temp = new TechnicalIndicator();

                    temp.setEvaluationPeriod((String) request.getParam("EVALUATION_PERIOD"));
                    temp.setTechnicalIndicatorType((String) request.getParam("TECHNICAL_INDICATOR_TYPE"));
                    temp.setTechnicalIndicatorKey((String) request.getParam("TECHNICAL_INDICATOR_KEY"));

                    temp.setSymbol(symbol);

                    if (request.getParam("SHORT_SMA_TYPE") != null) {
                        temp.setShortSMAType((String) request.getParam("SHORT_SMA_TYPE"));
                    }

                    if (request.getParam("LONG_SMA_TYPE") != null) {
                        temp.setLongSMAType((String) request.getParam("LONG_SMA_TYPE"));
                    }

                    if (request.getParam("LBB_TYPE") != null) {
                        temp.setLBBType((String) request.getParam("LBB_TYPE"));
                    }

                    if (request.getParam("UBB_TYPE") != null) {
                        temp.setUBBType((String) request.getParam("UBB_TYPE"));
                    }

                    if (request.getParam("STANDARD_DEVIATIONS") != null) {
                        temp.setStandardDeviations((BigDecimal) request.getParam("STANDARD_DEVIATIONS"));
                    }

                    request.addParam(GlobalConstant.ITEM, temp);

                    try {
                        cacheDao.save(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                });

        response.setStatus(Response.SUCCESS);

    }

    @Override
    public void delete(final Request request, final Response response) {

    }

    @Override
    public void item(final Request request, final Response response) {

    }

    @Override
    public void items(final Request request, final Response response) {
        try {
            customTechnicalIndicatorDao.items(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final List<CustomTechnicalIndicator> customTechnicalIndicators = new ArrayList<CustomTechnicalIndicator>();

        for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            customTechnicalIndicators.add(CustomTechnicalIndicator.class.cast(o));
        }

        customTechnicalIndicators.stream().forEach(item -> {
            item.getSymbols().stream()
                    .map(symbol -> symbol.getSymbol())
                    .forEach(symbol -> {
                        item.getTechnicalIndicators()
                                .add(tradeSignalCache.getTechnicalIndicatorMap()
                                        .get(item.getTechnicalIndicatorType() + "::" + item.getTechnicalIndicatorKey()
                                                + "::"
                                                + item.getEvaluationPeriod() + "::" + symbol));
                    });
        });

        response.addParam(GlobalConstant.ITEMS, customTechnicalIndicators);
    }
}
