package org.toasthub.stock.cache;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.toasthub.analysis.algorithm.AlgorithmCruncherDao;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.model.TechnicalIndicatorDetail;
import org.toasthub.stock.model.TradeSignalCache;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CacheManager {

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    private CacheDao cacheDao;

    @Autowired
    private AlgorithmCruncherDao algorithmCruncherDao;

    private final AtomicBoolean updatingTechnicalIndicator = new AtomicBoolean(false);

    public void initializeCache() {
        final Request request = new Request();
        final Response response = new Response();
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

            request.addParam(GlobalConstant.SYMBOL, symbol);

            AssetDay recentAsesetDay = null;

            try {
                cacheDao.getLatestAssetDay(request, response);
                recentAsesetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
            } catch (final NoResultException e) {
                System.out.println("No assetdays in database");
            }

            if (recentAsesetDay != null) {
                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAsesetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAsesetDay.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, recentAsesetDay.getVolume());
                tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, recentAsesetDay.getVwap());
            }

            AssetMinute recentAsesetMinute = null;

            try {
                cacheDao.getLatestAssetMinute(request, response);
                recentAsesetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);
            } catch (final NoResultException e) {
                System.out.println("No assetminutes in database");
            }

            if (recentAsesetMinute != null) {
                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, recentAsesetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                        recentAsesetMinute.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("MINUTE::" + symbol, recentAsesetMinute.getVolume());
                tradeSignalCache.getRecentVwapMap().put("MINUTE::" + symbol, recentAsesetMinute.getVwap());
            }
        });
    }

    public void updateRawData(final Request request, final Response response) {
        Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

            request.addParam(GlobalConstant.SYMBOL, symbol);

            AssetDay recentAsesetDay = null;

            try {
                cacheDao.getLatestAssetDay(request, response);
                recentAsesetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
            } catch (final NoResultException e) {
                System.out.println("No assetdays in database");
            }

            if (recentAsesetDay != null) {
                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAsesetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAsesetDay.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, recentAsesetDay.getVolume());
                tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, recentAsesetDay.getVwap());
            }

            AssetMinute recentAsesetMinute = null;

            try {
                cacheDao.getLatestAssetMinute(request, response);
                recentAsesetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);
            } catch (final NoResultException e) {
                System.out.println("No assetminutes in database");
            }

            if (recentAsesetMinute != null) {
                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, recentAsesetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                        recentAsesetMinute.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("MINUTE::" + symbol, recentAsesetMinute.getVolume());
                tradeSignalCache.getRecentVwapMap().put("MINUTE::" + symbol, recentAsesetMinute.getVwap());
            }
        });
    }

    public boolean goldenCrossIsFlashing(final Request request, final Response response) {
        boolean result = false;

        final String shortSMAType = (String) request.getParam("SHORT_SMA_TYPE");
        final String longSMAType = (String) request.getParam("LONG_SMA_TYPE");

        request.addParam(GlobalConstant.TYPE, shortSMAType);

        try {
            cacheDao.getSMAValue(request, response);
        } catch (final NoResultException e) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }

        final BigDecimal shortSMAValue = (BigDecimal) response.getParam(GlobalConstant.ITEM);

        request.addParam(GlobalConstant.TYPE, longSMAType);

        try {
            cacheDao.getSMAValue(request, response);
        } catch (final NoResultException e) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }

        final BigDecimal longSMAValue = (BigDecimal) response.getParam(GlobalConstant.ITEM);

        if (shortSMAValue.compareTo(longSMAValue) > 0) {
            result = true;
        }
        return result;
    }

    public boolean lowerBollingerBandIsFlashing(final Request request, final Response response) {
        boolean result = false;

        final String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
        final String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
        final String lbbType = (String) request.getParam("LBB_TYPE");
        final BigDecimal standardDeviations = (BigDecimal) request.getParam("STANDARD_DEVIATIONS");

        request.addParam(GlobalConstant.TYPE, lbbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            cacheDao.getLBB(request, response);
        } catch (final NoResultException e) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }

        final LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

        if (tradeSignalCache.getRecentClosingPriceMap().get(evaluationPeriod + "::" + symbol)
                .compareTo(lbb.getValue()) < 0) {
            result = true;
        }

        return result;
    }

    public boolean upperBollingerBandIsFlashing(final Request request, final Response response) {
        boolean result = false;

        final String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
        final String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
        final String ubbType = (String) request.getParam("UBB_TYPE");
        final BigDecimal standardDeviations = (BigDecimal) request.getParam("STANDARD_DEVIATIONS");

        request.addParam(GlobalConstant.TYPE, ubbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            cacheDao.getUBB(request, response);
        } catch (final NoResultException e) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }

        final UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

        if (tradeSignalCache.getRecentClosingPriceMap().get(evaluationPeriod + "::" + symbol)
                .compareTo(ubb.getValue()) > 0) {
            result = true;
        }

        return result;
    }

    public void updateTechnicalIndicatorCache(final Request request, final Response response) {

        while (updatingTechnicalIndicator.get()) {
            try {
                Thread.sleep(1000);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        updatingTechnicalIndicator.set(true);

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

                    final String symbol = technicalIndicator.getSymbol();
                    final String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

                    tradeSignalCache.getTechnicalIndicatorMap()
                            .put(technicalIndicator.getTechnicalIndicatorType() + "::"
                                    + technicalIndicator.getTechnicalIndicatorKey() + "::"
                                    + evaluationPeriod + "::"
                                    + symbol, technicalIndicator);

                    if (tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol) == null) {
                        return;
                    }

                    if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol) == null) {
                        return;
                    }

                    final long currentMinute = tradeSignalCache.getRecentEpochSecondsMap()
                            .get("MINUTE::" + symbol);

                    final long currentDay = ZonedDateTime
                            .ofInstant(Instant.ofEpochSecond(currentMinute), ZoneId.of("America/New_York"))
                            .truncatedTo(ChronoUnit.DAYS).toEpochSecond();

                    final BigDecimal currentPrice = tradeSignalCache.getRecentClosingPriceMap()
                            .get("MINUTE::" + symbol);

                    boolean flashing = false;

                    request.addParam(GlobalConstant.SYMBOL, symbol);
                    request.addParam("EVALUATION_PERIOD", evaluationPeriod);
                    response.getParams().remove("INSUFFICIENT_DATA");

                    switch (technicalIndicator.getTechnicalIndicatorType()) {
                        case TechnicalIndicator.GOLDENCROSS:
                            request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                            request.addParam("SHORT_SMA_TYPE", technicalIndicator.getShortSMAType());
                            request.addParam("LONG_SMA_TYPE", technicalIndicator.getLongSMAType());
                            flashing = goldenCrossIsFlashing(request, response);
                            break;
                        case TechnicalIndicator.LOWERBOLLINGERBAND:
                            request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                            request.addParam("LBB_TYPE", technicalIndicator.getLBBType());
                            request.addParam("STANDARD_DEVIATIONS", technicalIndicator.getStandardDeviations());
                            flashing = lowerBollingerBandIsFlashing(request, response);
                            break;
                        case TechnicalIndicator.UPPERBOLLINGERBAND:
                            request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                            request.addParam("UBB_TYPE", technicalIndicator.getUBBType());
                            request.addParam("STANDARD_DEVIATIONS", technicalIndicator.getStandardDeviations());
                            flashing = upperBollingerBandIsFlashing(request, response);
                            break;
                        default:
                            System.out.print(
                                    "Invalid technical indicator type in current testing service- update technical indicator cache method");
                    }

                    if (response.getParam("INSUFFICIENT_DATA") != null
                            && (boolean) response.getParam("INSUFFICIENT_DATA") == true) {
                        return;
                    }

                    technicalIndicator.setFlashing(flashing);

                    if (technicalIndicator.getLastCheck() < currentMinute) {

                        request.addParam(technicalIndicator.getSymbol() + "::CACHE_UPDATED", true);

                        final boolean dayBased = evaluationPeriod.equals("DAY");
                        final boolean checkedToday = technicalIndicator.getLastCheck() > currentDay;
                        final boolean flashedToday = technicalIndicator.getLastFlash() > currentDay;

                        if (!(dayBased && checkedToday)) {
                            technicalIndicator.setChecked(technicalIndicator.getChecked() + 1);
                        }

                        if (technicalIndicator.getFirstCheck() == 0) {
                            technicalIndicator.setFirstCheck(currentMinute);
                        }

                        technicalIndicator.setLastCheck(currentMinute);

                        technicalIndicator.getDetails().stream()
                                .filter(detail -> detail.getChecked() < 100)
                                .forEach(detail -> {

                                    if (!(dayBased && checkedToday)) {

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

                        if (technicalIndicator.isFlashing() && !(dayBased && flashedToday)) {

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

                            technicalIndicator.getDetails().add(technicalIndicatorDetail);
                        }
                    }

                    request.addParam(GlobalConstant.ITEM, technicalIndicator);

                    try {
                        cacheDao.save(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                });

        updatingTechnicalIndicator.set(false);
    }

    public void backloadTechnicalIndicator(final Request request, final Response response) {

        try {
            cacheDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final TechnicalIndicator currentTechnicalIndicator = (TechnicalIndicator) response
                .getParam(GlobalConstant.ITEM);

        final long endingEpochSeconds = currentTechnicalIndicator.getFirstCheck();

        final TechnicalIndicator backloadedTechnicalIndicator = new TechnicalIndicator();

        final List<AssetDay> assetDays = new ArrayList<AssetDay>();

        request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
        request.addParam(GlobalConstant.SYMBOL, currentTechnicalIndicator.getSymbol());
        request.addParam("STARTING_EPOCH_SECONDS",
                endingEpochSeconds - ((int) request.getParam("DAYS_TO_BACKLOAD") * 60 * 60 * 24));
        request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);

        try {
            algorithmCruncherDao.items(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (final Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            assetDays.add((AssetDay) o);
        }

        final StopWatch minuteTimer = new StopWatch();
        final StopWatch queryTimer = new StopWatch();

        assetDays.stream()
                .sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
                .forEach(assetDay -> {

                    final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

                    request.addParam("STARTING_EPOCH_SECONDS", assetDay.getEpochSeconds());
                    request.addParam("ENDING_EPOCH_SECONDS", assetDay.getEpochSeconds() + (60 * 60 * 24));
                    request.addParam(GlobalConstant.SYMBOL, currentTechnicalIndicator.getSymbol());

                    if (assetDay.getEpochSeconds() + (60 * 60 * 24) > endingEpochSeconds) {
                        request.addParam("ENDING_EPOCH_SECONDS", endingEpochSeconds);
                    }

                    request.addParam(GlobalConstant.IDENTIFIER, "AssetMinute");

                    try {
                        algorithmCruncherDao.items(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

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
                            .forEach(assetMinute -> {

                                minuteTimer.start();

                                final String symbol = currentTechnicalIndicator.getSymbol();

                                final long currentMinute = assetMinute.getEpochSeconds();

                                final long currentDay = ZonedDateTime
                                        .ofInstant(Instant.ofEpochSecond(currentMinute), ZoneId.of("America/New_York"))
                                        .truncatedTo(ChronoUnit.DAYS).toEpochSecond();

                                final BigDecimal currentPrice = assetMinute.getValue();

                                boolean flashing = false;

                                request.addParam(GlobalConstant.SYMBOL, symbol);
                                request.addParam("EVALUATION_PERIOD", currentTechnicalIndicator.getEvaluationPeriod());
                                response.getParams().remove("INSUFFICIENT_DATA");

                                queryTimer.start();

                                switch (currentTechnicalIndicator.getTechnicalIndicatorType()) {
                                    case TechnicalIndicator.GOLDENCROSS:
                                        request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                                        request.addParam("SHORT_SMA_TYPE", currentTechnicalIndicator.getShortSMAType());
                                        request.addParam("LONG_SMA_TYPE", currentTechnicalIndicator.getLongSMAType());
                                        flashing = goldenCrossIsFlashing(request, response);
                                        break;
                                    case TechnicalIndicator.LOWERBOLLINGERBAND:
                                        request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                                        request.addParam("LBB_TYPE", currentTechnicalIndicator.getLBBType());
                                        request.addParam("STANDARD_DEVIATIONS",
                                                currentTechnicalIndicator.getStandardDeviations());
                                        flashing = lowerBollingerBandIsFlashing(request, response);
                                        break;
                                    case TechnicalIndicator.UPPERBOLLINGERBAND:
                                        request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                                        request.addParam("UBB_TYPE", currentTechnicalIndicator.getUBBType());
                                        request.addParam("STANDARD_DEVIATIONS",
                                                currentTechnicalIndicator.getStandardDeviations());
                                        flashing = upperBollingerBandIsFlashing(request, response);
                                        break;
                                    default:
                                        System.out.print(
                                                "Invalid technical indicator type in current testing service- update technical indicator cache method");
                                }
                                queryTimer.stop();

                                if (response.getParam("INSUFFICIENT_DATA") != null
                                        && (boolean) response.getParam("INSUFFICIENT_DATA") == true) {
                                    System.out.println("Insufficient Data");
                                    minuteTimer.stop();
                                    return;
                                }

                                backloadedTechnicalIndicator.setFlashing(flashing);

                                if (backloadedTechnicalIndicator.getLastCheck() < currentMinute) {

                                    final boolean dayBased = currentTechnicalIndicator.getEvaluationPeriod()
                                            .equals("DAY");

                                    final boolean checkedToday = backloadedTechnicalIndicator
                                            .getLastCheck() > currentDay;
                                    final boolean flashedToday = backloadedTechnicalIndicator
                                            .getLastFlash() > currentDay;

                                    if (!(dayBased && checkedToday)) {
                                        backloadedTechnicalIndicator
                                                .setChecked(backloadedTechnicalIndicator.getChecked() + 1);
                                    }

                                    if (backloadedTechnicalIndicator.getFirstCheck() == 0) {
                                        backloadedTechnicalIndicator.setFirstCheck(currentMinute);
                                    }

                                    backloadedTechnicalIndicator.setLastCheck(currentMinute);

                                    backloadedTechnicalIndicator.getDetails().stream()
                                            .filter(detail -> detail.getChecked() < 100)
                                            .filter(detail -> detail.getFlashTime() < currentMinute)
                                            .forEach(detail -> {

                                                if (!(dayBased && checkedToday)) {
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
                                                                    backloadedTechnicalIndicator.getSuccesses() + 1);
                                                }
                                            });

                                    if (backloadedTechnicalIndicator.isFlashing() && !(dayBased && flashedToday)) {

                                        long volume = 0;
                                        BigDecimal vWap = BigDecimal.ZERO;

                                        // bug ! - since minute based records of assetDays are not kept , all day based
                                        // volume data will be equivalent across a day. I really dont care about volume
                                        // data tho
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

                                        technicalIndicatorDetail.setTechnicalIndicator(backloadedTechnicalIndicator);

                                        technicalIndicatorDetail.setFlashTime(currentMinute);

                                        technicalIndicatorDetail.setFlashPrice(currentPrice);

                                        technicalIndicatorDetail.setVolume(volume);

                                        technicalIndicatorDetail.setVwap(vWap);

                                        backloadedTechnicalIndicator.getDetails().add(technicalIndicatorDetail);
                                    }
                                }

                                minuteTimer.stop();

                            });
                    System.out
                            .println("Backloading day of technical indicator took roughly "
                                    + minuteTimer.getTotalTimeMillis() / minuteTimer.getTaskCount()
                                    + " milliseconds per minute");
                    System.out
                            .println("Querying data for day took roughly "
                                    + queryTimer.getTotalTimeMillis() / queryTimer.getTaskCount()
                                    + " milliseconds per minute");

                    System.out.println("This day had " + Runtime.getRuntime().freeMemory() / (1024 * 1024)
                            + " megabytes of free memory available to use");
                });

        while (updatingTechnicalIndicator.get()) {
            try {
                Thread.sleep(1000);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        updatingTechnicalIndicator.set(true);

        minuteTimer.start();

        try {
            cacheDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final TechnicalIndicator temp = (TechnicalIndicator) response.getParam(GlobalConstant.ITEM);

        temp.setFirstCheck(backloadedTechnicalIndicator.getFirstCheck());
        temp.setChecked(temp.getChecked() + backloadedTechnicalIndicator.getChecked());
        temp.setFlashed(temp.getFlashed() + backloadedTechnicalIndicator.getFlashed());
        temp.setSuccesses(temp.getSuccesses() + backloadedTechnicalIndicator.getSuccesses());

        backloadedTechnicalIndicator.getDetails().stream().forEach(detail -> {
            detail.setTechnicalIndicator(temp);
        });
        temp.getDetails().addAll(backloadedTechnicalIndicator.getDetails());

        request.addParam(GlobalConstant.ITEM, temp);
        try {
            cacheDao.save(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        updatingTechnicalIndicator.set(false);

        minuteTimer.stop();
        System.out.println(
                "Merging and saving technical indicator took" + minuteTimer.getLastTaskTimeMillis() + " milliseconds");

        response.setStatus(Response.SUCCESS);
    }

}
