package org.toasthub.trade.cache;

import java.util.ArrayList;
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
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TISnapshot;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.TradeSignalCache;
import org.toasthub.trade.technical_indicator.TechnicalIndicatorDao;
import org.toasthub.trade.ti_snapshot.TISnapshotDao;
import org.toasthub.trade.ti_snapshot.TISnapshotSvc;

@Service("TACacheSvc")
public class CacheSvcImpl implements ServiceProcessor, CacheSvc {

    @Autowired
    @Qualifier("TACacheDao")
    private CacheDao cacheDao;

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    @Qualifier("TARequestValidation")
    private RequestValidation validator;

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    @Qualifier("TACacheManager")
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("TAAlgorithmCruncherSvc")
    private AlgorithmCruncherSvc algorithmCruncherSvc;

    @Autowired
    @Qualifier("TATechnicalIndicatorDao")
    private TechnicalIndicatorDao technicalIndicatorDao;

    @Autowired
    @Qualifier("TATISnapshotSvc")
    private TISnapshotSvc tiSnapshotSvc;

    @Autowired
    @Qualifier("TATISnapshotDao")
    private TISnapshotDao tiSnapshotDao;

    @Override
    public void process(final RestRequest request, final RestResponse response) {
        try {
            final String action = (String) request.getParams().get("action");
            switch (action) {
                case "ITEM":
                    item(request, response);
                    break;
                case "LIST": {
                    final List<CustomTechnicalIndicator> customTechnicalIndicators = customTechnicalIndicatorDao
                            .getCustomTechnicalIndicators();

                    customTechnicalIndicators.stream().forEach(customTechnicalIndicator -> {

                        final List<String> effectiveSymbols = customTechnicalIndicator.getSymbols().stream()
                                .map(symbol -> symbol.getSymbol()).toList();

                        customTechnicalIndicator.setEffectiveSymbols(effectiveSymbols);

                    });

                    response.addParam(GlobalConstant.ITEMS, customTechnicalIndicators);
                    response.setStatus(RestResponse.SUCCESS);
                    break;
                }
                case "INITIALIZE_TECHNICAL_INDICATORS": {
                    final Object id = request.getParam(GlobalConstant.ITEMID);
                    final long validatedId = validator.validateId(id);

                    final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
                            .findById(validatedId);

                    final List<Symbol> symbols = customTechnicalIndicatorDao
                            .getCustomTechnicalIndicatorSymbols(customTechnicalIndicator);

                    final List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

                    symbols.stream()
                            .map(symbol -> symbol.getSymbol())
                            .forEach(symbol -> {

                                final TechnicalIndicator technicalIndicator = tradeSignalCache
                                        .getTechnicalIndicatorMap()
                                        .get(customTechnicalIndicator.getTechnicalIndicatorType() + "::"
                                                + customTechnicalIndicator.getTechnicalIndicatorKey()
                                                + "::"
                                                + customTechnicalIndicator.getEvaluationPeriod() + "::" + symbol);

                                if (technicalIndicator == null) {
                                    return;
                                }

                                final List<TechnicalIndicatorDetail> technicalIndicatorDetails = cacheDao
                                        .getTechnicalIndicatorDetails(technicalIndicator);

                                technicalIndicator.setEffectiveDetails(technicalIndicatorDetails);

                                technicalIndicators.add(technicalIndicator);
                            });

                    response.addParam(GlobalConstant.ITEMS, technicalIndicators);
                    response.setStatus(RestResponse.SUCCESS);
                    break;
                }
                case "SAVE":
                    save(request, response);
                    break;
                case "DELETE":
                    delete(request, response);
                    break;
                case "BACKLOAD": {
                    if (request.getParam(GlobalConstant.ITEMID) == null) {
                        throw new ExpectedException("Item Id is null");
                    }

                    if (request.getParam("startTime") == null) {
                        throw new ExpectedException("Start time is null");
                    }

                    if (request.getParam("endTime") == null) {
                        throw new ExpectedException("Start time is null");
                    }

                    final long itemId = Long.valueOf(String.valueOf(request.getParam(GlobalConstant.ITEMID)));

                    final long startTime = Long.valueOf(String.valueOf(request.getParam("startTime")));

                    final long endTime = Long.valueOf(String.valueOf(request.getParam("endTime")));

                    algorithmCruncherSvc.backloadAlgorithm(itemId, startTime, endTime);

                    System.out.println("Algorithms Backloaded !");

                    cacheManager.backloadTechnicalIndicator(itemId, startTime);

                    System.out.println("Technical Indicator Backloaded !");

                    response.setStatus(RestResponse.SUCCESS);
                    break;
                }
                case "MODIFY_SNAPSHOT": {
                    if (request.getParam(GlobalConstant.ITEMID) == null) {
                        throw new ExpectedException("Item Id is null");
                    }

                    if (request.getParam("startTime") == null) {
                        throw new ExpectedException("Start time is null");
                    }

                    if (request.getParam("endTime") == null) {
                        throw new ExpectedException("End time is null");
                    }

                    final long itemId = Long.valueOf(String.valueOf(request.getParam(GlobalConstant.ITEMID)));

                    final long startTime = Long.valueOf(String.valueOf(request.getParam("startTime")));

                    final long endTime = Long.valueOf(String.valueOf(request.getParam("endTime")));

                    final TISnapshot initSnapshot = tiSnapshotDao.findSnapshot(itemId);

                    final TechnicalIndicator technicalIndicator = technicalIndicatorDao.getTechnicalIndicator(
                            initSnapshot.getSymbol(),
                            initSnapshot.getEvaluationPeriod(),
                            initSnapshot.getTechnicalIndicatorKey(),
                            initSnapshot.getTechnicalIndicatorType());

                    // ensures ample data exists to initialize snapshot
                    algorithmCruncherSvc.backloadAlgorithm(technicalIndicator.getId(), startTime, endTime);

                    System.out.println("Algorithms Backloaded !");

                    initSnapshot.setUpdating(true);
                    initSnapshot.resetSnapshot();

                    final TISnapshot managedSnapshot = tiSnapshotDao.save(initSnapshot);

                    final TISnapshot initializedSnapshot = tiSnapshotSvc.initializeSnapshot(managedSnapshot, startTime,
                            endTime);

                    initializedSnapshot.setUpdating(false);

                    tiSnapshotDao.save(initializedSnapshot);

                    System.out.println("Initialized Snapshot saved !");

                    response.setStatus(RestResponse.SUCCESS);

                    break;
                }

                case "GET_SNAPSHOTS": {
                    final Object id = request.getParam(GlobalConstant.ITEMID);
                    final long validatedId = validator.validateId(id);

                    final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
                            .findById(validatedId);

                    final List<TISnapshot> snapshots = tiSnapshotDao.getSnapshots(customTechnicalIndicator);

                    response.addParam(GlobalConstant.ITEMS, snapshots);

                    response.setStatus(RestResponse.SUCCESS);
                    break;

                }
                default:
                    throw new Exception("Action : " + action + " is not recognized");
            }
        } catch (final Exception e) {
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

                    if (itemCount != 0) {
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

                    final TechnicalIndicator managedTechnicalIndicator = TechnicalIndicator.class
                            .cast(cacheDao.saveItem(t));

                    tradeSignalCache.insertTechnicalIndicator(managedTechnicalIndicator);

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
                                    .getTechnicalIndicatorDetails(technicalIndicator);

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
