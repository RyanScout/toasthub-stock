package org.toasthub.trade.cache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.algorithm.AlgorithmCruncherSvc;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.TradeSignalCache;

@Service("TACacheSvc")
public class CacheSvcImpl implements ServiceProcessor, CacheSvc {

    @Autowired
    @Qualifier("TACacheDao")
    private CacheDao cacheDao;

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("TAAlgorithmCruncherSvc")
    private AlgorithmCruncherSvc algorithmCruncherSvc;

    @Override
    public void process(final RestRequest request, final RestResponse response) {
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
                algorithmCruncherSvc.backloadAlg(request, response);
                cacheManager.backloadTechnicalIndicator(request, response);
                response.setStatus(RestResponse.SUCCESS);
                break;
            default:
                System.out.println(action + "is not recognized as an action as cachesvc");
                return;
        }

    }

    @Override
    public void save(final RestRequest request, final RestResponse response) {
        final Collection<String> symbols = new ArrayList<String>();

        for (final Object o : ArrayList.class.cast(request.getParam("SYMBOLS"))) {
            symbols.add(String.class.cast(o));
        }

        symbols.stream()
                .distinct()
                .filter(symbol -> Arrays.asList(Symbol.SYMBOLS).contains(symbol))
                .forEach(symbol -> {
                    request.addParam(TradeConstant.SYMBOL, symbol);
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

        response.setStatus(RestResponse.SUCCESS);

    }

    @Override
    public void delete(final RestRequest request, final RestResponse response) {

    }

    @Override
    public void item(final RestRequest request, final RestResponse response) {

    }

    @Override
    public void items(final RestRequest request, final RestResponse response) {
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
