package org.toasthub.trade.cache;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.AssetDay;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.ExpectedException;
import org.toasthub.trade.model.InsufficientDataException;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.TradeSignalCache;

@Component("TACacheManager")
public class CacheManager {

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    @Qualifier("TACacheDao")
    private CacheDao cacheDao;

    private final AtomicBoolean updatingTechnicalIndicator = new AtomicBoolean(false);

    public void initializeCache() {
        final RestRequest request = new RestRequest();
        final RestResponse response = new RestResponse();

        try {
            cacheDao.items(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

        for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            technicalIndicators.add((TechnicalIndicator) o);
        }

        technicalIndicators.stream()
                .forEach(technicalIndicator -> {
                    tradeSignalCache.getTechnicalIndicatorMap()
                            .put(technicalIndicator.getTechnicalIndicatorType() + "::"
                                    + technicalIndicator.getTechnicalIndicatorKey() + "::"
                                    + technicalIndicator.getEvaluationPeriod() + "::"
                                    + technicalIndicator.getSymbol(), technicalIndicator);
                });

        Symbol.SYMBOLS.stream().forEach(symbol -> {

            try {
                final AssetDay latestAssetDay = cacheDao.getLatestAssetDay(symbol);
                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, latestAssetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, latestAssetDay.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, latestAssetDay.getVolume());
                tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, latestAssetDay.getVwap());

            } catch (final NoResultException e) {
                System.out.println("No assetdays in database");
            }

            try {
                final AssetMinute latestAssetMinute = cacheDao.getLatestAssetMinute(symbol);
                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, latestAssetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                        latestAssetMinute.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("MINUTE::" + symbol, latestAssetMinute.getVolume());
                tradeSignalCache.getRecentVwapMap().put("MINUTE::" + symbol, latestAssetMinute.getVwap());
            } catch (final NoResultException e) {
                System.out.println("No assetminutes in database");
            }

        });
    }

    public void updateRawData(final RestRequest request, final RestResponse response) {
        Symbol.SYMBOLS.stream().forEach(symbol -> {

            try {
                final AssetDay latestAssetDay = cacheDao.getLatestAssetDay(symbol);
                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, latestAssetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, latestAssetDay.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, latestAssetDay.getVolume());
                tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, latestAssetDay.getVwap());

            } catch (final NoResultException e) {
                System.out.println("No assetdays in database");
            }

            try {
                final AssetMinute latestAssetMinute = cacheDao.getLatestAssetMinute(symbol);
                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, latestAssetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                        latestAssetMinute.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("MINUTE::" + symbol, latestAssetMinute.getVolume());
                tradeSignalCache.getRecentVwapMap().put("MINUTE::" + symbol, latestAssetMinute.getVwap());
            } catch (final NoResultException e) {
                System.out.println("No assetminutes in database");
            }

        });
    }

    public void updateTechnicalIndicatorCache(final RestRequest request, final RestResponse response) {

        while (updatingTechnicalIndicator.get()) {
            try {
                Thread.sleep(1000);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        updatingTechnicalIndicator.set(true);

        final List<TechnicalIndicator> technicalIndicators = cacheDao.getTechnicalIndicators();

        technicalIndicators.stream()
                .forEach(technicalIndicator -> {
                    try {
                        tradeSignalCache.insertTechnicalIndicator(technicalIndicator);

                        final String symbol = technicalIndicator.getSymbol();

                        final String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

                        final long currentMinute = tradeSignalCache.getRecentEpochSecondsMap()
                                .get("MINUTE::" + symbol);

                        final long currentDay = ZonedDateTime
                                .ofInstant(Instant.ofEpochSecond(currentMinute), ZoneId.of("America/New_York"))
                                .truncatedTo(ChronoUnit.DAYS).toEpochSecond();

                        final BigDecimal currentPrice = tradeSignalCache.getRecentClosingPriceMap()
                                .get("MINUTE::" + symbol);

                        if (tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol) == null) {
                            throw new ExpectedException("Epoch Seconds for asset data not in cache");
                        }

                        if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol) == null) {
                            throw new ExpectedException("Closing price for asset data not in cache");
                        }

                        // minute already checked
                        if (technicalIndicator.getLastCheck() == currentMinute) {
                            return;
                        }

                        request.addParam(technicalIndicator.getSymbol() + "::CACHE_UPDATED", true);

                        final boolean dayBased = evaluationPeriod.equals("DAY");
                        final boolean checkedToday = technicalIndicator.getLastCheck() >= currentDay;
                        final boolean flashedToday = technicalIndicator.getLastFlash() >= currentDay;

                        if (!dayBased || !checkedToday) {
                            technicalIndicator.setChecked(technicalIndicator.getChecked() + 1);
                        }

                        if (technicalIndicator.getFirstCheck() == 0) {
                            technicalIndicator.setFirstCheck(currentMinute);
                        }

                        technicalIndicator.setLastCheck(currentMinute);

                        final List<TechnicalIndicatorDetail> technicalIndicatorDetails = cacheDao
                                .getIncompleteTechnicalIndicatorDetails(technicalIndicator);

                        technicalIndicatorDetails.stream()
                                .filter(detail -> detail.getChecked() < 100)
                                .forEach(detail -> {

                                    if (!dayBased || !checkedToday) {

                                        detail.setChecked(detail.getChecked() + 1);
                                    }

                                    BigDecimal tempSuccessPercent = (currentPrice
                                            .subtract(detail.getFlashPrice()))
                                            .divide(detail.getFlashPrice(), MathContext.DECIMAL32)
                                            .multiply(BigDecimal.valueOf(100));

                                    if (technicalIndicator.getTechnicalIndicatorType()
                                            .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
                                        tempSuccessPercent = tempSuccessPercent.negate();
                                    }

                                    if (detail.getSuccessPercent() == null
                                            || detail.getSuccessPercent().compareTo(tempSuccessPercent) < 0) {
                                        detail.setSuccessPercent(tempSuccessPercent);
                                    }

                                    if (detail.isSuccess() == false
                                            && detail.getFlashPrice().compareTo(currentPrice) < 0) {
                                        detail.setSuccess(true);
                                        technicalIndicator.setSuccesses(technicalIndicator.getSuccesses() + 1);
                                    }
                                });

                        cacheDao.saveList(technicalIndicatorDetails);
                        boolean flashing = false;

                        try {
                            switch (technicalIndicator.getTechnicalIndicatorType()) {
                                case TechnicalIndicator.GOLDENCROSS:
                                    final BigDecimal shortSMAValue = cacheDao.getSMAValue(symbol, evaluationPeriod,
                                            technicalIndicator.getShortSMAEvaluationDuration(), currentMinute);
                                    final BigDecimal longSMAValue = cacheDao.getSMAValue(symbol, evaluationPeriod,
                                            technicalIndicator.getLongSMAEvaluationDuration(), currentMinute);
                                    flashing = shortSMAValue.compareTo(longSMAValue) > 0;

                                    break;
                                case TechnicalIndicator.LOWERBOLLINGERBAND:
                                    final BigDecimal lbbValue = cacheDao.getLBBValue(symbol, evaluationPeriod,
                                            technicalIndicator.getLbbEvaluationDuration(), currentMinute,
                                            technicalIndicator.getStandardDeviations());
                                    flashing = currentPrice.compareTo(lbbValue) < 0;
                                    break;
                                case TechnicalIndicator.UPPERBOLLINGERBAND:
                                    final BigDecimal ubbValue = cacheDao.getUBBValue(symbol, evaluationPeriod,
                                            technicalIndicator.getUbbEvaluationDuration(), currentMinute,
                                            technicalIndicator.getStandardDeviations());
                                    flashing = currentPrice.compareTo(ubbValue) > 0;
                                    break;
                                default:
                                    throw new ExpectedException(
                                            "Invalid technical indicator type in current testing service- update technical indicator cache method");
                            }
                        } catch (final NoResultException e) {
                            throw new InsufficientDataException();
                        }

                        technicalIndicator.setFlashing(flashing);

                        if (technicalIndicator.isFlashing() && (!dayBased || !flashedToday)) {

                            final long volume = tradeSignalCache.getRecentVolumeMap()
                                    .get(evaluationPeriod + "::" + symbol);

                            final BigDecimal vWap = tradeSignalCache.getRecentVwapMap()
                                    .get(evaluationPeriod + "::" + symbol);

                            technicalIndicator.setLastFlash(currentMinute);

                            technicalIndicator.setFlashed(technicalIndicator.getFlashed() + 1);

                            final TechnicalIndicatorDetail technicalIndicatorDetail = new TechnicalIndicatorDetail();

                            technicalIndicatorDetail.setTechnicalIndicator(technicalIndicator);

                            technicalIndicatorDetail.setFlashTime(currentMinute);

                            technicalIndicatorDetail.setFlashPrice(currentPrice);

                            technicalIndicatorDetail.setVolume(volume);

                            technicalIndicatorDetail.setVwap(vWap);

                            cacheDao.saveItem(technicalIndicatorDetail);
                        }

                        cacheDao.saveItem(technicalIndicator);

                    } catch (final ExpectedException e) {
                        e.printStackTrace();
                    } catch (final InsufficientDataException e) {
                        return;
                    }

                });

        updatingTechnicalIndicator.set(false);
    }

    public void backloadTechnicalIndicator(final long itemId, final long startTime) throws Exception {

        final TechnicalIndicator currentTechnicalIndicator = cacheDao.findTechnicalIndicatorById(itemId);

        final TechnicalIndicator backloadedTechnicalIndicator = new TechnicalIndicator();

        final String symbol = currentTechnicalIndicator.getSymbol();

        final long endingEpochSecondsDay = Instant.now().getEpochSecond();

        final long startingEpochSecondsDay;

        if (currentTechnicalIndicator.getFirstCheck() < startTime) {
            startingEpochSecondsDay = currentTechnicalIndicator.getFirstCheck();
        } else {
            startingEpochSecondsDay = startTime;
        }

        final List<AssetMinute> allFlashes = new ArrayList<AssetMinute>();

        switch (currentTechnicalIndicator.getTechnicalIndicatorType()) {
            case TechnicalIndicator.GOLDENCROSS:
                final List<AssetMinute> tempFlashesSMA = cacheDao
                        .getSMAAssetMinuteFlashes(
                                startingEpochSecondsDay, endingEpochSecondsDay,
                                symbol,
                                currentTechnicalIndicator.getEvaluationPeriod(),
                                currentTechnicalIndicator.getShortSMAEvaluationDuration(),
                                currentTechnicalIndicator.getLongSMAEvaluationDuration());
                allFlashes.addAll(tempFlashesSMA);
                break;
            case TechnicalIndicator.LOWERBOLLINGERBAND:
                final List<AssetMinute> tempFlashesLBB = cacheDao
                        .getLBBAssetMinuteFlashes(
                                startingEpochSecondsDay, endingEpochSecondsDay,
                                currentTechnicalIndicator.getStandardDeviations(),
                                symbol,
                                currentTechnicalIndicator.getEvaluationPeriod(),
                                currentTechnicalIndicator.getLbbEvaluationDuration());
                allFlashes.addAll(tempFlashesLBB);
                break;
            case TechnicalIndicator.UPPERBOLLINGERBAND:
                final List<AssetMinute> tempFlashesUBB = cacheDao
                        .getUBBAssetMinuteFlashes(
                                startingEpochSecondsDay, endingEpochSecondsDay,
                                currentTechnicalIndicator.getStandardDeviations(),
                                symbol,
                                currentTechnicalIndicator.getEvaluationPeriod(),
                                currentTechnicalIndicator.getUbbEvaluationDuration());
                allFlashes.addAll(tempFlashesUBB);
                break;
            default:
                throw new Exception(
                        "Invalid technical indicator type in current testing service- update technical indicator cache method");
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

        final long truncatedStartingEpochSeconds = Instant.ofEpochSecond(startingEpochSecondsDay)
                .atZone(ZoneId.of("America/New_York"))
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond();

        final long checks = cacheDao.getAssetDayCountWithinTimeFrame(symbol, truncatedStartingEpochSeconds,
                latestAssetMinute.getEpochSeconds());

        backloadedTechnicalIndicator.setChecked(checks);
        backloadedTechnicalIndicator.setFirstCheck(startingEpochSecondsDay);
        backloadedTechnicalIndicator.setLastCheck(endingEpochSecondsDay);

        trimmedFlashes.stream().forEachOrdered(assetMinute -> {

            final TechnicalIndicatorDetail technicalIndicatorDetail = new TechnicalIndicatorDetail();

            technicalIndicatorDetail
                    .setTechnicalIndicator(backloadedTechnicalIndicator);

            technicalIndicatorDetail.setFlashTime(assetMinute.getEpochSeconds());

            technicalIndicatorDetail.setFlashPrice(assetMinute.getValue());

            technicalIndicatorDetail.setVolume(assetMinute.getVolume());

            technicalIndicatorDetail.setVwap(assetMinute.getVwap());

            backloadedTechnicalIndicator.getDetails().add(technicalIndicatorDetail);

            backloadedTechnicalIndicator.setFlashed(backloadedTechnicalIndicator.getFlashed() + 1);

            backloadedTechnicalIndicator.setLastFlash(assetMinute.getEpochSeconds());

            final ZonedDateTime currentZonedAssetMinute = ZonedDateTime
                    .ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()), ZoneId.of("America/New_York"));

            final long timeDifference = latestAssetMinute.getEpochSeconds() - assetMinute.getEpochSeconds();

            final long timeDifferenceInDays = TimeUnit.SECONDS.toDays(timeDifference);

            final long finalCheck;

            if (timeDifferenceInDays < 100) {
                finalCheck = latestAssetMinute.getEpochSeconds();
                technicalIndicatorDetail.setChecked((int) timeDifferenceInDays);
            } else {
                finalCheck = currentZonedAssetMinute.plusDays(100).toEpochSecond();
                technicalIndicatorDetail.setChecked(100);
            }

            if (currentTechnicalIndicator.getTechnicalIndicatorType().equals(TechnicalIndicator.GOLDENCROSS)
                    || currentTechnicalIndicator.getTechnicalIndicatorType()
                            .equals(TechnicalIndicator.LOWERBOLLINGERBAND)) {
                final BigDecimal highestSuccessPrice = cacheDao.getHighestAssetMinuteValueWithinTimeFrame(symbol,
                        assetMinute.getEpochSeconds(), finalCheck);

                final BigDecimal highestSuccessPercent = (highestSuccessPrice
                        .subtract(technicalIndicatorDetail.getFlashPrice()))
                        .divide(technicalIndicatorDetail.getFlashPrice(), MathContext.DECIMAL32)
                        .multiply(BigDecimal.valueOf(100));

                technicalIndicatorDetail.setSuccessPercent(highestSuccessPercent);

                if (highestSuccessPrice.compareTo(technicalIndicatorDetail.getFlashPrice()) > 0) {
                    technicalIndicatorDetail.setSuccess(true);
                    backloadedTechnicalIndicator.setSuccesses(backloadedTechnicalIndicator.getSuccesses() + 1);
                } else {
                    technicalIndicatorDetail.setSuccess(false);
                }
            }

            if (currentTechnicalIndicator.getTechnicalIndicatorType()
                    .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
                final BigDecimal lowestSuccessPrice = cacheDao.getLowestAssetMinuteValueWithinTimeFrame(symbol,
                        assetMinute.getEpochSeconds(), finalCheck);

                final BigDecimal highestSuccessPercent = (lowestSuccessPrice
                        .subtract(technicalIndicatorDetail.getFlashPrice()))
                        .divide(technicalIndicatorDetail.getFlashPrice(), MathContext.DECIMAL32)
                        .multiply(BigDecimal.valueOf(100))
                        .negate();

                technicalIndicatorDetail.setSuccessPercent(highestSuccessPercent);

                if (lowestSuccessPrice.compareTo(technicalIndicatorDetail.getFlashPrice()) < 0) {
                    technicalIndicatorDetail.setSuccess(true);
                    backloadedTechnicalIndicator.setSuccesses(backloadedTechnicalIndicator.getSuccesses() + 1);
                } else {
                    technicalIndicatorDetail.setSuccess(false);
                }
            }

        });

        final TechnicalIndicator managedTechnicalIndicator = cacheDao
                .refreshTechnicalIndicator(currentTechnicalIndicator);

        managedTechnicalIndicator.setFirstCheck(backloadedTechnicalIndicator.getFirstCheck());

        managedTechnicalIndicator.setLastFlash(backloadedTechnicalIndicator.getLastFlash());

        managedTechnicalIndicator
                .setChecked(backloadedTechnicalIndicator.getChecked());

        managedTechnicalIndicator
                .setFlashed(backloadedTechnicalIndicator.getFlashed());

        managedTechnicalIndicator
                .setSuccesses(backloadedTechnicalIndicator.getSuccesses());

        managedTechnicalIndicator.setDetails(new HashSet<TechnicalIndicatorDetail>());

        managedTechnicalIndicator.setUpdating(false);

        cacheDao.saveItem(managedTechnicalIndicator);

        backloadedTechnicalIndicator.getDetails().stream().forEach(detail -> {
            detail.setTechnicalIndicator(managedTechnicalIndicator);
        });

        cacheDao.saveList(backloadedTechnicalIndicator.getDetails().stream().toList());

        tradeSignalCache.insertTechnicalIndicator(managedTechnicalIndicator);
    }

}
