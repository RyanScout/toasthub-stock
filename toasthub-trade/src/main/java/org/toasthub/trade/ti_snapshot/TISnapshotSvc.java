package org.toasthub.trade.ti_snapshot;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.algorithm.AlgorithmCruncherSvc;
import org.toasthub.trade.cache.CacheDao;
import org.toasthub.trade.cache.CacheManager;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.ExpectedException;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TISnapshot;
import org.toasthub.trade.model.TISnapshotDetail;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.technical_indicator.TechnicalIndicatorDao;

@Service("TATISnapshotSvc")
public class TISnapshotSvc implements ServiceProcessor {

    @Autowired
    @Qualifier("TACacheDao")
    private CacheDao cacheDao;

    @Autowired
    @Qualifier("TATISnapshotDao")
    private TISnapshotDao tiSnapshotDao;

    @Autowired
    @Qualifier("TATechnicalIndicatorDao")
    private TechnicalIndicatorDao technicalIndicatorDao;

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    @Qualifier("TARequestValidation")
    private RequestValidation validator;

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
                case "CREATE_SNAPSHOT": {
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

                    // ensures ample data exists to initialize snapshot
                    algorithmCruncherSvc.backloadAlgorithm(itemId, startTime);

                    System.out.println("Algorithms Backloaded !");

                    final TechnicalIndicator technicalIndicator = technicalIndicatorDao.findById(itemId);

                    final TISnapshot initSnapshot = new TISnapshot();

                    initSnapshot.setUpdating(true);

                    initSnapshot.copyProperties(technicalIndicator);

                    final TISnapshot managedSnapshot = tiSnapshotDao.save(initSnapshot);

                    final TISnapshot initializedSnapshot = initializeSnapshot(managedSnapshot, startTime, endTime);

                    tiSnapshotDao.save(initializedSnapshot);

                    System.out.println("Initialized Snapshot saved !");

                    response.setStatus(RestResponse.SUCCESS);

                    break;
                }

                case "INITIALIZE_SNAPSHOTS": {
                    final Object id = request.getParam(GlobalConstant.ITEMID);
                    final long validatedId = validator.validateId(id);

                    final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
                            .findById(validatedId);

                    final String evaluationPeriod = customTechnicalIndicator.getEvaluationPeriod();

                    final String technicalIndicatorType = customTechnicalIndicator.getTechnicalIndicatorType();

                    final String technicalIndicatorKey = customTechnicalIndicator.getTechnicalIndicatorKey();

                    final List<TISnapshot> snapshots = tiSnapshotDao.getSnapshotsWithProperties(evaluationPeriod,
                            technicalIndicatorKey, technicalIndicatorType);

                    response.addParam(GlobalConstant.ITEMS, snapshots);

                    response.setStatus(RestResponse.SUCCESS);
                    break;

                }
                default: {
                    throw new Exception("Action : " + action + " is not recognized");
                }
            }
        } catch (final Exception e) {
            response.setStatus("Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public TISnapshot initializeSnapshot(final TISnapshot snapshot, final long startTime, final long endTime)
            throws Exception {

        final String symbol = snapshot.getSymbol();

        final List<AssetMinute> allFlashes = new ArrayList<AssetMinute>();

        switch (snapshot.getTechnicalIndicatorType()) {
            case TechnicalIndicator.GOLDENCROSS:
                final List<AssetMinute> tempFlashesSMA = cacheDao
                        .getSMAAssetMinuteFlashes(
                                startTime,
                                endTime,
                                symbol,
                                snapshot.getEvaluationPeriod(),
                                snapshot.getShortSMAEvaluationDuration(),
                                snapshot.getLongSMAEvaluationDuration());
                allFlashes.addAll(tempFlashesSMA);
                break;
            case TechnicalIndicator.LOWERBOLLINGERBAND:
                final List<AssetMinute> tempFlashesLBB = cacheDao
                        .getLBBAssetMinuteFlashes(
                                startTime,
                                endTime,
                                snapshot.getStandardDeviations(),
                                symbol,
                                snapshot.getEvaluationPeriod(),
                                snapshot.getLbbEvaluationDuration());
                allFlashes.addAll(tempFlashesLBB);
                break;
            case TechnicalIndicator.UPPERBOLLINGERBAND:
                final List<AssetMinute> tempFlashesUBB = cacheDao
                        .getUBBAssetMinuteFlashes(
                                startTime,
                                endTime,
                                snapshot.getStandardDeviations(),
                                symbol,
                                snapshot.getEvaluationPeriod(),
                                snapshot.getUbbEvaluationDuration());
                allFlashes.addAll(tempFlashesUBB);
                break;
            default:
                throw new Exception(
                        "Invalid technical indicator type");
        }

        final List<AssetMinute> allSortedFlashes = allFlashes.stream().sorted((a, b) -> {
            return (int) (a.getEpochSeconds() - b.getEpochSeconds());
        }).toList();

        final List<AssetMinute> trimmedFlashes = new ArrayList<AssetMinute>();

        long lastDay = 0;

        // remove same day flashes
        for (final AssetMinute assetMinute : allSortedFlashes) {

            final long relativeDay = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()), ZoneId.of("America/New_York"))
                    .truncatedTo(ChronoUnit.DAYS)
                    .toEpochSecond();

            if (lastDay != relativeDay) {
                trimmedFlashes.add(assetMinute);
                lastDay = relativeDay;
            }
        }

        final AssetMinute latestAssetMinute = cacheDao.getLatestAssetMinute(symbol);

        final long truncatedStartingEpochSeconds = Instant.ofEpochSecond(startTime)
                .atZone(ZoneId.of("America/New_York"))
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond();

        final long checks = cacheDao.getAssetDayCountWithinTimeFrame(symbol, truncatedStartingEpochSeconds,
                endTime);

        snapshot.setChecked(checks);
        snapshot.setFirstCheck(startTime);
        snapshot.setLastCheck(endTime);

        System.out.println(trimmedFlashes.size());
        if (trimmedFlashes.size() == 0) {
            return snapshot;
        }

        snapshot.setFlashed(trimmedFlashes.size());
        snapshot.setLastFlash(trimmedFlashes.get(trimmedFlashes.size() - 1).getEpochSeconds());

        trimmedFlashes.stream().forEachOrdered(assetMinute -> {

            final TISnapshotDetail snapshotDetail = new TISnapshotDetail();

            snapshotDetail.setTISnapshot(snapshot);

            snapshotDetail.setFlashTime(assetMinute.getEpochSeconds());

            snapshotDetail.setFlashPrice(assetMinute.getValue());

            snapshotDetail.setVolume(assetMinute.getVolume());

            snapshotDetail.setVwap(assetMinute.getVwap());

            snapshot.getDetails().add(snapshotDetail);

            final ZonedDateTime currentZonedAssetMinute = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()), ZoneId.of("America/New_York"));

            final long timeDifference = latestAssetMinute.getEpochSeconds() - assetMinute.getEpochSeconds();

            final long timeDifferenceInDays = TimeUnit.SECONDS.toDays(timeDifference);

            final long finalCheck;

            if (timeDifferenceInDays < 100) {
                finalCheck = latestAssetMinute.getEpochSeconds();
                snapshotDetail.setChecked((int) timeDifferenceInDays);
            } else {
                finalCheck = currentZonedAssetMinute.plusDays(100).toEpochSecond();
                snapshotDetail.setChecked(100);
            }

            if (snapshot.getTechnicalIndicatorType().equals(TechnicalIndicator.GOLDENCROSS)
                    || snapshot.getTechnicalIndicatorType()
                            .equals(TechnicalIndicator.LOWERBOLLINGERBAND)) {
                final BigDecimal highestSuccessPrice = cacheDao.getHighestAssetMinuteValueWithinTimeFrame(symbol,
                        assetMinute.getEpochSeconds(), finalCheck);

                final BigDecimal highestSuccessPercent = (highestSuccessPrice
                        .subtract(snapshotDetail.getFlashPrice()))
                        .divide(snapshotDetail.getFlashPrice(), MathContext.DECIMAL32)
                        .multiply(BigDecimal.valueOf(100));

                snapshotDetail.setSuccessPercent(highestSuccessPercent);

                if (highestSuccessPrice.compareTo(snapshotDetail.getFlashPrice()) > 0) {
                    snapshotDetail.setSuccess(true);
                    snapshot.setSuccesses(snapshot.getSuccesses() + 1);
                } else {
                    snapshotDetail.setSuccess(false);
                }
            }

            if (snapshot.getTechnicalIndicatorType()
                    .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
                final BigDecimal lowestSuccessPrice = cacheDao.getLowestAssetMinuteValueWithinTimeFrame(symbol,
                        assetMinute.getEpochSeconds(), finalCheck);

                final BigDecimal highestSuccessPercent = (lowestSuccessPrice
                        .subtract(snapshotDetail.getFlashPrice()))
                        .divide(snapshotDetail.getFlashPrice(), MathContext.DECIMAL32)
                        .multiply(BigDecimal.valueOf(100))
                        .negate();

                snapshotDetail.setSuccessPercent(highestSuccessPercent);

                if (lowestSuccessPrice.compareTo(snapshotDetail.getFlashPrice()) < 0) {
                    snapshotDetail.setSuccess(true);
                    snapshot.setSuccesses(snapshot.getSuccesses() + 1);
                } else {
                    snapshotDetail.setSuccess(false);
                }
            }

        });

        return snapshot;
    }

}
