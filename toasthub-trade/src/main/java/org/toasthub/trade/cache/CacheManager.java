package org.toasthub.trade.cache;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.AssetDay;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.TradeSignalCache;

@Component
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

        Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

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
        Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

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

        final List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

        technicalIndicators.addAll(cacheDao.getTechnicalIndicators());

        technicalIndicators.stream()
                .forEach(technicalIndicator -> {

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
                        return;
                    }

                    if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol) == null) {
                        return;
                    }

                    if (technicalIndicator.getLastCheck() < currentMinute) {

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
                                .getTechnicalIndicatorDetails(technicalIndicator);

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
                                    System.out.print(
                                            "Invalid technical indicator type in current testing service- update technical indicator cache method");
                            }
                        } catch (final NoResultException e) {
                            System.out.println("Insufficient Data");
                            return;
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
                    }

                    cacheDao.saveItem(technicalIndicator);

                });

        updatingTechnicalIndicator.set(false);
    }

    public void backloadTechnicalIndicator(final RestRequest request, final RestResponse response) {

        if (request.getParam(GlobalConstant.ITEMID) == null) {
            response.setStatus(RestResponse.ERROR);
            System.out.println("No item id given at backload technical indicator");
            return;
        }

        try {
            cacheDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final TechnicalIndicator currentTechnicalIndicator = (TechnicalIndicator) response
                .getParam(GlobalConstant.ITEM);

        request.addParam(TradeConstant.SYMBOL, currentTechnicalIndicator.getSymbol());

        final long endingEpochSeconds = currentTechnicalIndicator.getLastCheck();

        final long startingEpochSeconds = endingEpochSeconds
                - ((int) request.getParam(TradeConstant.DAYS_TO_BACKLOAD) * 60 * 60 * 24);

        final TechnicalIndicator backloadedTechnicalIndicator = new TechnicalIndicator();

        final List<AssetDay> assetDays = new ArrayList<AssetDay>();

        request.addParam(TradeConstant.SYMBOL, currentTechnicalIndicator.getSymbol());
        request.addParam("STARTING_EPOCH_SECONDS", startingEpochSeconds);
        request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);

        cacheDao.getAssetDays(request, response);

        for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            assetDays.add((AssetDay) o);
        }

        final ForkJoinPool customThreadPool = new ForkJoinPool(2);

        try {
            customThreadPool.submit(() -> assetDays.stream()
                    .parallel()
                    .sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
                    .forEachOrdered(assetDay -> {

                        final StopWatch minuteTimer = new StopWatch();
                        final StopWatch queryTimer = new StopWatch();

                        final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

                        request.addParam("STARTING_EPOCH_SECONDS", assetDay.getEpochSeconds());
                        request.addParam("ENDING_EPOCH_SECONDS", assetDay.getEpochSeconds() + (60 * 60 * 24));
                        request.addParam(TradeConstant.SYMBOL, currentTechnicalIndicator.getSymbol());

                        if (assetDay.getEpochSeconds() + (60 * 60 * 24) > endingEpochSeconds) {
                            request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);
                        }

                        cacheDao.getAssetMinutes(request, response);

                        for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
                            assetMinutes.add((AssetMinute) o);
                        }

                        assetMinutes.stream()
                                .filter(assetMinute -> assetDay
                                        .getEpochSeconds() == ZonedDateTime
                                                .ofInstant(Instant.ofEpochSecond(assetMinute.getEpochSeconds()),
                                                        ZoneId.of("America/New_York"))
                                                .truncatedTo(ChronoUnit.DAYS).toEpochSecond())
                                .sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
                                .forEachOrdered(assetMinute -> {

                                    minuteTimer.start();

                                    final String symbol = currentTechnicalIndicator.getSymbol();

                                    final long currentMinute = assetMinute.getEpochSeconds();

                                    final long currentDay = ZonedDateTime
                                            .ofInstant(Instant.ofEpochSecond(currentMinute),
                                                    ZoneId.of("America/New_York"))
                                            .truncatedTo(ChronoUnit.DAYS).toEpochSecond();

                                    final BigDecimal currentPrice = assetMinute.getValue();

                                    final boolean dayBased = currentTechnicalIndicator.getEvaluationPeriod()
                                            .equals("DAY");

                                    final boolean checkedToday = backloadedTechnicalIndicator
                                            .getLastCheck() >= currentDay;
                                    final boolean flashedToday = backloadedTechnicalIndicator
                                            .getLastFlash() >= currentDay;

                                    if (backloadedTechnicalIndicator.getLastCheck() >= currentMinute) {
                                        minuteTimer.stop();
                                        return;
                                    }

                                    if (backloadedTechnicalIndicator.getFirstCheck() == 0) {
                                        backloadedTechnicalIndicator.setFirstCheck(currentMinute);
                                    }

                                    backloadedTechnicalIndicator.setLastCheck(currentMinute);

                                    backloadedTechnicalIndicator.getDetails().stream()
                                            .filter(detail -> detail.getChecked() < 100)
                                            .filter(detail -> detail.getFlashTime() < currentMinute)
                                            .forEach(detail -> {

                                                if (!dayBased || !checkedToday) {
                                                    detail.setChecked(detail.getChecked() + 1);
                                                }

                                                BigDecimal tempSuccessPercent = (currentPrice
                                                        .subtract(detail.getFlashPrice()))
                                                        .divide(detail.getFlashPrice(), MathContext.DECIMAL32)
                                                        .multiply(BigDecimal.valueOf(100));

                                                if (currentTechnicalIndicator.getTechnicalIndicatorType()
                                                        .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
                                                    tempSuccessPercent = tempSuccessPercent.negate();
                                                }

                                                if (detail.getSuccessPercent() == null
                                                        || detail.getSuccessPercent()
                                                                .compareTo(tempSuccessPercent) < 0) {
                                                    detail.setSuccessPercent(tempSuccessPercent);
                                                }

                                                if (detail.isSuccess() == false
                                                        && detail.getFlashPrice().compareTo(currentPrice) < 0) {
                                                    detail.setSuccess(true);
                                                    backloadedTechnicalIndicator
                                                            .setSuccesses(
                                                                    backloadedTechnicalIndicator.getSuccesses()
                                                                            + 1);
                                                }
                                            });

                                    if (currentMinute >= currentTechnicalIndicator.getFirstCheck()) {
                                        minuteTimer.stop();
                                        return;
                                    }

                                    if (!dayBased || !checkedToday) {
                                        backloadedTechnicalIndicator
                                                .setChecked(backloadedTechnicalIndicator.getChecked() + 1);
                                    }

                                    boolean flashing = false;

                                    request.addParam(TradeConstant.SYMBOL, symbol);
                                    request.addParam("EVALUATION_PERIOD",
                                            currentTechnicalIndicator.getEvaluationPeriod());
                                    response.getParams().remove("INSUFFICIENT_DATA");

                                    queryTimer.start();

                                    try {
                                        switch (currentTechnicalIndicator.getTechnicalIndicatorType()) {
                                            case TechnicalIndicator.GOLDENCROSS:
                                                final BigDecimal shortSMAValue = cacheDao.getSMAValue(symbol,
                                                        currentTechnicalIndicator.getEvaluationPeriod(),
                                                        currentTechnicalIndicator.getShortSMAEvaluationDuration(),
                                                        currentMinute);
                                                final BigDecimal longSMAValue = cacheDao.getSMAValue(symbol,
                                                        currentTechnicalIndicator.getEvaluationPeriod(),
                                                        currentTechnicalIndicator.getLongSMAEvaluationDuration(),
                                                        currentMinute);
                                                flashing = shortSMAValue.compareTo(longSMAValue) > 0;

                                                break;
                                            case TechnicalIndicator.LOWERBOLLINGERBAND:
                                                final BigDecimal lbbValue = cacheDao.getLBBValue(symbol,
                                                        currentTechnicalIndicator.getEvaluationPeriod(),
                                                        currentTechnicalIndicator.getLbbEvaluationDuration(),
                                                        currentMinute,
                                                        currentTechnicalIndicator.getStandardDeviations());
                                                flashing = currentPrice.compareTo(lbbValue) < 0;
                                                break;
                                            case TechnicalIndicator.UPPERBOLLINGERBAND:
                                                final BigDecimal ubbValue = cacheDao.getUBBValue(symbol,
                                                        currentTechnicalIndicator.getEvaluationPeriod(),
                                                        currentTechnicalIndicator.getUbbEvaluationDuration(),
                                                        currentMinute,
                                                        currentTechnicalIndicator.getStandardDeviations());
                                                flashing = currentPrice.compareTo(ubbValue) > 0;
                                                break;
                                            default:
                                                System.out.print(
                                                        "Invalid technical indicator type in current testing service- update technical indicator cache method");
                                        }
                                    } catch (final NoResultException e) {
                                        System.out.println("Insufficient Data");
                                        queryTimer.stop();
                                        return;
                                    }
                                    queryTimer.stop();

                                    backloadedTechnicalIndicator.setFlashing(flashing);

                                    if (backloadedTechnicalIndicator.isFlashing() && (!dayBased || !flashedToday)) {

                                        long volume = 0;
                                        BigDecimal vWap = BigDecimal.ZERO;

                                        if (dayBased) {
                                            volume = assetDay.getVolume();

                                            vWap = assetDay.getVwap();
                                        }
                                        if (!dayBased) {
                                            volume = assetMinute.getVolume();

                                            vWap = assetMinute.getVwap();
                                        }

                                        backloadedTechnicalIndicator.setLastFlash(currentMinute);

                                        backloadedTechnicalIndicator
                                                .setFlashed(backloadedTechnicalIndicator.getFlashed() + 1);

                                        final TechnicalIndicatorDetail technicalIndicatorDetail = new TechnicalIndicatorDetail();

                                        technicalIndicatorDetail
                                                .setTechnicalIndicator(backloadedTechnicalIndicator);

                                        technicalIndicatorDetail.setFlashTime(currentMinute);

                                        technicalIndicatorDetail.setFlashPrice(currentPrice);

                                        technicalIndicatorDetail.setVolume(volume);

                                        technicalIndicatorDetail.setVwap(vWap);

                                        backloadedTechnicalIndicator.getDetails().add(technicalIndicatorDetail);
                                    }

                                    minuteTimer.stop();

                                });

                        if (minuteTimer.getTaskCount() == 0) {
                            System.out.println("No minutes in assetminutes");
                        } else {
                            System.out
                                    .println("Backloading day of technical indicator took roughly "
                                            + minuteTimer.getTotalTimeMillis() / minuteTimer.getTaskCount()
                                            + " milliseconds per minute");
                        }

                        if (queryTimer.getTaskCount() == 0) {
                            System.out.println("No queries were done for this day");
                        } else {
                            System.out
                                    .println("Querying data for day took roughly "
                                            + queryTimer.getTotalTimeMillis() / queryTimer.getTaskCount()
                                            + " milliseconds per minute");
                        }

                        System.out.println("Iterations in this day - " + minuteTimer.getTaskCount());
                        System.out.println("Query calls in this day - " + queryTimer.getTaskCount());
                        System.out.println("Epoch seconds for this assetday- " + assetDay.getEpochSeconds());

                    })).get();

            customThreadPool.shutdown();

        } catch (final Exception e) {
            e.printStackTrace();
        }

        while (updatingTechnicalIndicator.get()) {
            try {
                Thread.sleep(1000);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        updatingTechnicalIndicator.set(true);

        request.addParam(GlobalConstant.ITEM, currentTechnicalIndicator);

        cacheDao.refresh(request, response);

        currentTechnicalIndicator.setFirstCheck(backloadedTechnicalIndicator.getFirstCheck());

        currentTechnicalIndicator
                .setChecked(currentTechnicalIndicator.getChecked() + backloadedTechnicalIndicator.getChecked());

        currentTechnicalIndicator
                .setFlashed(currentTechnicalIndicator.getFlashed() + backloadedTechnicalIndicator.getFlashed());

        currentTechnicalIndicator
                .setSuccesses(currentTechnicalIndicator.getSuccesses() + backloadedTechnicalIndicator.getSuccesses());

        backloadedTechnicalIndicator.getDetails().stream()
                .forEach(detail -> detail.setTechnicalIndicator(currentTechnicalIndicator));

        currentTechnicalIndicator.getDetails().addAll(backloadedTechnicalIndicator.getDetails());

        currentTechnicalIndicator.setUpdating(false);

        request.addParam(GlobalConstant.ITEM, currentTechnicalIndicator);

        try {
            cacheDao.save(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        tradeSignalCache.insertTechnicalIndicator(currentTechnicalIndicator);

        updatingTechnicalIndicator.set(false);

        response.setStatus(RestResponse.SUCCESS);
    }

}
