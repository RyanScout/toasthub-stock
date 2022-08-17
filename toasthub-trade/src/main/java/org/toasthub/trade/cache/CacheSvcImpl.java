package org.toasthub.trade.cache;

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
import org.toasthub.trade.model.ExpectedException;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
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
    @Qualifier("TACacheManager")
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("TAAlgorithmCruncherSvc")
    private AlgorithmCruncherSvc algorithmCruncherSvc;

    @Override
    public void process(final RestRequest request, final RestResponse response) {
        try {
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
                    if (request.getParam(GlobalConstant.ITEMID) == null) {
                        throw new ExpectedException("Item Id is null");
                    }

                    if (request.getParam("startTime") == null) {
                        throw new ExpectedException("Start time is null");
                    }
                    final long itemId = Long.valueOf(String.valueOf(request.getParam(GlobalConstant.ITEMID)));

                    final long startTime = Long.valueOf(String.valueOf(request.getParam("startTime")));

                    algorithmCruncherSvc.backloadAlgorithm(itemId, startTime);

                    System.out.println("Algorithms Backloaded !");

                    cacheManager.backloadTechnicalIndicator(itemId, startTime);

                    System.out.println("Technical Indicator Backloaded !");

                    response.setStatus(RestResponse.SUCCESS);
                    break;
                default:
                    throw new Exception("Action : " + action + "is not recognized");
            }
        } catch (Exception e) {
            response.setStatus("Exception : " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public void save(final CustomTechnicalIndicator c) {
        c.getSymbols().stream()
                .map(symbol -> symbol.getSymbol())
                .forEach(symbol -> {
                    final long itemCount = cacheDao.itemCount(c.getTechnicalIndicatorType(),
                            c.getEvaluationPeriod(),
                            c.getTechnicalIndicatorKey(), symbol);

                    if (itemCount >= 1) {
                        return;
                    }

                    final TechnicalIndicator t = new TechnicalIndicator();

                    t.setSymbol(symbol);
                    t.setEvaluationPeriod(c.getEvaluationPeriod());
                    t.setTechnicalIndicatorKey(c.getTechnicalIndicatorKey());
                    t.setTechnicalIndicatorType(c.getTechnicalIndicatorType());
                    t.setShortSMAEvaluationDuration(c.getShortSMAEvaluationDuration());
                    t.setLongSMAEvaluationDuration(c.getLongSMAEvaluationDuration());
                    t.setLbbEvaluationDuration(c.getLbbEvaluationDuration());
                    t.setUbbEvaluationDuration(c.getUbbEvaluationDuration());
                    t.setStandardDeviations(c.getStandardDeviations());

                    cacheDao.saveItem(t);

                });
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
            final List<CustomTechnicalIndicator> customTechnicalIndicators = customTechnicalIndicatorDao
                    .getCustomTechnicalIndicators();

            customTechnicalIndicators.stream().forEach(customTechnicalIndicator -> {
                customTechnicalIndicator.getSymbols().stream()
                        .map(symbol -> symbol.getSymbol())
                        .forEach(symbol -> {
                            final TechnicalIndicator technicalIndicator = tradeSignalCache.getTechnicalIndicatorMap()
                                    .get(customTechnicalIndicator.getTechnicalIndicatorType() + "::"
                                            + customTechnicalIndicator.getTechnicalIndicatorKey()
                                            + "::"
                                            + customTechnicalIndicator.getEvaluationPeriod() + "::" + symbol);
                            if (technicalIndicator == null) {
                                return;
                            }

                            final List<TechnicalIndicatorDetail> technicalIndicatorDetails = cacheDao
                                    .getCompleteTechnicalIndicatorDetails(technicalIndicator);

                            technicalIndicator.setEffectiveDetails(technicalIndicatorDetails);

                            customTechnicalIndicator.getTechnicalIndicators().add(technicalIndicator);
                            customTechnicalIndicator.getEffectiveSymbols().add(symbol);

                        });
            });

            response.addParam(GlobalConstant.ITEMS, customTechnicalIndicators);
            response.setStatus(RestResponse.SUCCESS);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(final RestRequest request, final RestResponse response) {
        // TODO Auto-generated method stub

    }
}
