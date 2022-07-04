package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.algorithm.AlgorithmCruncherDao;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.model.TechnicalIndicatorDetail;
import org.toasthub.stock.cache.CacheDao;
import org.toasthub.stock.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.stock.model.TradeSignalCache;
import org.toasthub.stock.trade.TradeDao;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;

@Service("CurrentTestingSvc")
public class CurrentTestingSvcImpl implements CurrentTestingSvc {

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected TradeDao tradeDao;

    @Autowired
    protected CurrentTestingDao currentTestingDao;

    @Autowired
    protected TradeSignalCache tradeSignalCache;

    @Autowired
    protected CacheDao cacheDao;

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    private AlgorithmCruncherDao algorithmCruncherDao;

    final ExpressionParser parser = new SpelExpressionParser();

    final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

    public void updateRawData(final Request request, final Response response) {
        Stream.of(Symbol.SYMBOLS).forEach(symbol -> {

            request.addParam(GlobalConstant.SYMBOL, symbol);

            AssetDay recentAsesetDay = null;

            try {
                currentTestingDao.getLatestAssetDay(request, response);
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
                currentTestingDao.getLatestAssetMinute(request, response);
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

        request.addParam(GlobalConstant.IDENTIFIER, "SMA");

        request.addParam(GlobalConstant.TYPE, shortSMAType);
        try {
            currentTestingDao.itemCount(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

        request.addParam(GlobalConstant.TYPE, longSMAType);
        try {
            currentTestingDao.itemCount(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

        if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0) {
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

        request.addParam(GlobalConstant.IDENTIFIER, "LBB");

        request.addParam(GlobalConstant.TYPE, lbbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            currentTestingDao.itemCount(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
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

        request.addParam(GlobalConstant.IDENTIFIER, "UBB");

        request.addParam(GlobalConstant.TYPE, ubbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            currentTestingDao.itemCount(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

        if (tradeSignalCache.getRecentClosingPriceMap().get(evaluationPeriod + "::" + symbol)
                .compareTo(ubb.getValue()) > 0) {
            result = true;
        }

        return result;
    }

    public void updateTechnicalIndicatorCache(final Request request, final Response response) {
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
    }

    // precautions in this method are taken to ensure technical indicator cannot
    // access or view data it would not have access to at the date of backloading
    public void backloadTechnicalIndicator(final Request request, final Response response) {

        try {
            cacheDao.item(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final TechnicalIndicator technicalIndicator = (TechnicalIndicator) response.getParam(GlobalConstant.ITEM);

        final List<AssetDay> assetDays = new ArrayList<AssetDay>();

        final long endingEpochSeconds = technicalIndicator.getFirstCheck();

        // backloading assumes a chronological order, so we make future history inacessible
        // and add it back at the end of the method
        final long lastCheck = technicalIndicator.getLastCheck();

        technicalIndicator.setFirstCheck(0);
        technicalIndicator.setLastCheck(0);

        request.addParam(GlobalConstant.IDENTIFIER, "AssetDay");
        request.addParam(GlobalConstant.SYMBOL, technicalIndicator.getSymbol());
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

        assetDays.stream()
                .sorted((a, b) -> (int) (a.getEpochSeconds() - b.getEpochSeconds()))
                .forEach(assetDay -> {

                    final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

                    request.addParam("STARTING_EPOCH_SECONDS", assetDay.getEpochSeconds());
                    request.addParam("ENDING_EPOCH_SECONDS", assetDay.getEpochSeconds() + (60 * 60 * 24));
                    request.addParam(GlobalConstant.SYMBOL, technicalIndicator.getSymbol());

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

                                final String symbol = technicalIndicator.getSymbol();

                                final long currentMinute = assetMinute.getEpochSeconds();

                                final long currentDay = ZonedDateTime
                                        .ofInstant(Instant.ofEpochSecond(currentMinute), ZoneId.of("America/New_York"))
                                        .truncatedTo(ChronoUnit.DAYS).toEpochSecond();

                                final BigDecimal currentPrice = assetMinute.getValue();

                                boolean flashing = false;

                                request.addParam(GlobalConstant.SYMBOL, symbol);
                                request.addParam("EVALUATION_PERIOD", technicalIndicator.getEvaluationPeriod());
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
                                        request.addParam("STANDARD_DEVIATIONS",
                                                technicalIndicator.getStandardDeviations());
                                        flashing = lowerBollingerBandIsFlashing(request, response);
                                        break;
                                    case TechnicalIndicator.UPPERBOLLINGERBAND:
                                        request.addParam(GlobalConstant.EPOCHSECONDS, currentMinute);
                                        request.addParam("UBB_TYPE", technicalIndicator.getUBBType());
                                        request.addParam("STANDARD_DEVIATIONS",
                                                technicalIndicator.getStandardDeviations());
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

                                    final boolean dayBased = technicalIndicator.getEvaluationPeriod().equals("DAY");
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
                                            .filter(detail -> detail.getFlashTime() < currentMinute)
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
                                                        || detail.getSuccessPercent()
                                                                .compareTo(tempSuccessPercent) < 0) {
                                                    detail.setSuccessPercent(tempSuccessPercent);
                                                }

                                                if (detail.isSuccess() == false
                                                        && detail.getFlashPrice().compareTo(currentPrice) < 0) {
                                                    detail.setSuccess(true);
                                                    technicalIndicator
                                                            .setSuccesses(technicalIndicator.getSuccesses() + 1);
                                                }
                                            });

                                    if (technicalIndicator.isFlashing() && !(dayBased && flashedToday)) {

                                        long volume = 0;
                                        BigDecimal vWap = BigDecimal.ZERO;

                                        // bug ! - since minute based records of assetDays are not kept , all day based
                                        // volume data will be equivalent across a day.
                                        if (dayBased) {
                                            volume = assetDay.getVolume();

                                            vWap = assetDay.getVwap();
                                        }
                                        if (!dayBased) {
                                            volume = assetMinute.getVolume();

                                            vWap = assetMinute.getVwap();
                                        }

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
                            });
                });

        technicalIndicator.setLastCheck(lastCheck);
        request.addParam(GlobalConstant.ITEM, technicalIndicator);

        try {
            cacheDao.save(request, response);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTrades(final Request request, final Response response) {
        final List<Trade> trades = tradeDao.getAllRunningTrades();

        if (trades == null || trades.size() == 0) {
            return;
        }

        trades.stream().forEach(trade -> {
            trade.getTradeDetails().stream()
                    .forEach(t -> {
                        Order order = null;
                        try {
                            order = alpacaAPI.orders().getByClientID(t.getOrderID());
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        if (!order.getStatus().name().equals("FILLED")) {
                            return;
                        }
                        if (t.getOrderSide().equals("BUY")) {
                            switch (trade.getOrderSide()) {
                                case "Buy":
                                    if (!trade.getIterations().equals("unlimited")) {
                                        trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
                                        if (trade.getIterationsExecuted() >= Integer
                                                .parseInt(trade.getIterations()))
                                            trade.setStatus("Not Running");
                                    }
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    break;
                                case "Sell":
                                    System.out.println("Unknown case");
                                    break;
                                case "Bot":
                                    final BigDecimal orderQuantity = new BigDecimal(order.getFilledQuantity());
                                    final BigDecimal fillPrice = new BigDecimal(order.getAverageFillPrice()).setScale(2,
                                            RoundingMode.HALF_UP);

                                    trade.setAvailableBudget(
                                            trade.getAvailableBudget()
                                                    .subtract((orderQuantity.multiply(fillPrice)),
                                                            MathContext.DECIMAL32)
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    trade.setSharesHeld(trade.getSharesHeld().add(orderQuantity));

                                    trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

                                    trade.setTotalValue(
                                            trade.getAvailableBudget().add(
                                                    trade.getSharesHeld()
                                                            .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                                    .get("MINUTE::" + trade.getSymbol()))));

                                    t.setSharesHeld(trade.getSharesHeld());

                                    t.setAvailableBudget(trade.getAvailableBudget());

                                    t.setDollarAmount(
                                            (orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    t.setShareAmount(orderQuantity);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());

                                    t.setAssetPrice(fillPrice);

                                    t.setTotalValue(trade.getTotalValue());

                                    t.setStatus("FILLED");

                                    t.setTrade(trade);
                                    break;
                                default:
                                    System.out.println("Invalid orderside error");
                                    break;
                            }
                        }
                        if (t.getOrderSide().equals("SELL")) {
                            switch (trade.getOrderSide()) {
                                case "Buy":
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    break;
                                case "Sell":
                                    if (!trade.getIterations().equals("unlimited")) {
                                        trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
                                        if (trade.getIterationsExecuted() >= Integer
                                                .parseInt(trade.getIterations()))
                                            trade.setStatus("Not Running");
                                    }
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    break;
                                case "Bot":
                                    final BigDecimal orderQuantity = new BigDecimal(order.getFilledQuantity());
                                    final BigDecimal fillPrice = new BigDecimal(order.getAverageFillPrice()).setScale(2,
                                            RoundingMode.HALF_UP);

                                    trade.setAvailableBudget(
                                            trade.getAvailableBudget()
                                                    .add((orderQuantity.multiply(fillPrice)),
                                                            MathContext.DECIMAL32)
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    trade.setSharesHeld(trade.getSharesHeld().subtract(orderQuantity));

                                    trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

                                    trade.setTotalValue(
                                            trade.getAvailableBudget().add(
                                                    trade.getSharesHeld()
                                                            .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                                    .get("MINUTE::" + trade.getSymbol()))));

                                    t.setSharesHeld(trade.getSharesHeld());

                                    t.setAvailableBudget(trade.getAvailableBudget());

                                    t.setDollarAmount(
                                            (orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    t.setShareAmount(orderQuantity);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());

                                    t.setAssetPrice(fillPrice);

                                    t.setTotalValue(trade.getTotalValue());

                                    t.setStatus("FILLED");

                                    t.setTrade(trade);
                                    break;
                                default:
                                    System.out.println("Invalid orderside error");
                                    break;
                            }
                        }
                    });
            try {
                trade.setTotalValue(trade.getAvailableBudget().add(trade.getSharesHeld()
                        .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                .get("MINUTE::" + trade.getSymbol()))));

                request.addParam(GlobalConstant.ITEM, trade);
                tradeDao.save(request, response);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void checkTrades(final Request request, final Response response) {

        final List<Trade> trades = tradeDao.getRunningTrades();

        if (trades == null || trades.size() == 0) {
            System.out.println("No trades to run");
            return;
        }

        final long today = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/New_York"))
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond();

        trades.stream()
                .filter(trade -> ((request.getParam(trade.getSymbol() + "::CACHE_UPDATED") != null)
                        && (boolean) request.getParam(trade.getSymbol() + "::CACHE_UPDATED")))
                .filter(trade -> !(trade.getEvaluationPeriod().equals("DAY") && trade.getLastOrder() > today))
                .forEach(trade -> {
                    request.addParam(GlobalConstant.TRADE, trade);

                    System.out.println("Checking trade: " + trade.getName());

                    switch (trade.getOrderSide()) {
                        case Trade.BUY:
                            currentBuyTest(request, response);
                            break;
                        case Trade.SELL:
                            currentSellTest(request, response);
                            break;
                        case Trade.BOT:
                            currentBotTest(request, response);
                            break;
                        default:
                            return;
                    }

                    request.addParam(GlobalConstant.ITEM, trade);
                    try {
                        tradeDao.save(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public void currentBuyTest(final Request request, final Response response) {
        try {

            final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

            if (!trade.getIterations().equals("unlimited")
                    && trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations())) {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                return;
            }

            if (trade.getParseableBuyCondition() == null) {
                response.setStatus("Buy Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParseableBuyCondition().equals("")) {
                return;
            }

            final List<String> buyReasons = new ArrayList<String>();
            String sellOrderCondition = "";

            String[] stringArr = trade.getParseableBuyCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                final boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
                        .get(c.getTechnicalIndicatorType() + "::"
                                + c.getTechnicalIndicatorKey() + "::"
                                + c.getEvaluationPeriod() + "::"
                                + trade.getSymbol())
                        .isFlashing();

                if (bool) {
                    buyReasons.add(c.getName());
                }

                return String.valueOf(bool);
            }).toArray(String[]::new);

            final String buyCondition = String.join(" ", stringArr);

            if (!parser.parseExpression(buyCondition).getValue(Boolean.class)) {
                System.out.println(trade.getName() + ":Buy Condition not met");
                return;
            }

            Order sellOrder = null;
            Order buyOrder = null;
            int truncatedSharesAmount = 0;
            double profitLimitPrice = 0;

            switch (trade.getCurrencyType()) {
                case "Dollars":
                    truncatedSharesAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32)
                            .intValue();
                    break;
                case "Shares":
                    truncatedSharesAmount = trade.getCurrencyAmount().intValue();
                    break;
                default:
                    break;
            }
            switch (trade.getOrderType()) {
                case "Market":

                    switch (trade.getCurrencyType()) {

                        case "Dollars":
                            buyOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                            break;
                        case "Shares":
                            buyOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                            break;
                        default:
                            System.out.println("Currency type does not match");
                            break;
                    }
                    break;

                case "Trailing Stop":
                    switch (trade.getTrailingStopType()) {
                        case "Trailing Stop Price":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        case "Trailing Stop Percent":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        default:
                            System.out.println("Trailing stop type does not match");
                            break;
                    }
                    break;

                case "Profit Limit":

                    switch (trade.getProfitLimitType()) {
                        case "Profit Limit Price":
                            profitLimitPrice = trade.getProfitLimitAmount()
                                    .add(tradeSignalCache.getRecentClosingPriceMap()
                                            .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        case "Profit Limit Percent":
                            profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                    .multiply(
                                            tradeSignalCache.getRecentClosingPriceMap()
                                                    .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        default:
                            System.out.println("Profit limit type does not match");
                            break;
                    }
                    buyOrder = alpacaAPI.orders().requestMarketOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.BUY, OrderTimeInForce.DAY);
                    sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.SELL,
                            OrderTimeInForce.DAY, profitLimitPrice, false);
                    sellOrderCondition = "Profit Limit";
                    break;

                case "Trailing Stop & Profit Limit":

                    switch (trade.getTrailingStopType()) {
                        case "Trailing Stop Price":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        case "Trailing Stop Percent":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        default:
                            System.out.println("Trailing stop type does not match");
                            break;
                    }

                    switch (trade.getProfitLimitType()) {
                        case "Profit Limit Price":
                            profitLimitPrice = trade.getProfitLimitAmount()
                                    .add(tradeSignalCache.getRecentClosingPriceMap()
                                            .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        case "Profit Limit Percent":
                            profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                    .multiply(
                                            tradeSignalCache.getRecentClosingPriceMap()
                                                    .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        default:
                            System.out.println("Profit limit type does not match");
                            break;
                    }

                    sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.SELL,
                            OrderTimeInForce.DAY, profitLimitPrice, false);
                    sellOrderCondition = "Profit Limit";
                    break;

                case "Cascading Trailing Stop":

                    break;

                default:
                    System.out.println("Case Not found!");
                    break;

            }

            request.addParam("BOUGHT", true);

            if (buyOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("BUY");
                tradeDetail.setOrderCondition(String.join(",", buyReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
                if (trade.getFirstOrder() == 0)
                    trade.setFirstOrder(Instant.now().getEpochSecond());
                trade.setLastOrder(Instant.now().getEpochSecond());
            }

            if (sellOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(sellOrderCondition);
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
                trade.setLastOrder(Instant.now().getEpochSecond());
            }

            System.out.println(trade.getName() + ":Buy Order Placed!");

        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }

    }

    public void currentSellTest(final Request request, final Response response) {
        try {

            final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

            if (!trade.getIterations().equals("unlimited")
                    && trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations())) {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                return;
            }

            if (trade.getParseableSellCondition() == null) {
                System.out.println("Sell Condition is null in buy test for" + trade.getName());
                response.setStatus("Sell Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParseableSellCondition().equals("")) {
                return;
            }

            final List<String> sellReasons = new ArrayList<String>();

            String[] stringArr = trade.getParseableSellCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                final boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
                        .get(c.getTechnicalIndicatorType() + "::"
                                + c.getTechnicalIndicatorKey() + "::"
                                + c.getEvaluationPeriod() + "::"
                                + trade.getSymbol())
                        .isFlashing();

                if (bool) {
                    sellReasons.add(c.getName());
                }

                return String.valueOf(bool);
            }).toArray(String[]::new);

            final String sellCondition = String.join(" ", stringArr);

            if (!parser.parseExpression(sellCondition).getValue(Boolean.class)) {
                System.out.println(trade.getName() + ":Sell Condition not met");
                return;
            }
            Order sellOrder = null;

            switch (trade.getOrderType()) {
                case "Market":

                    switch (trade.getCurrencyType()) {

                        case "Dollars":
                            sellOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.SELL);
                            break;
                        case "Shares":
                            sellOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.SELL);
                            break;
                        default:
                            System.out.println("Currency type does not match");
                            break;
                    }
                    break;

                default:
                    System.out.println("Case Not found!");
                    break;

            }
            if (trade.getOrderSide().equals("Sell")) {
                trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

                if (!trade.getIterations().equals("unlimited")) {
                    if (trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations()))
                        trade.setStatus("Not Running");
                }
            }

            if (sellOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(sellOrder.getClientOrderId());
                tradeDetail.setStatus(sellOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(String.join(",", sellReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
            }
            System.out.println("Sell Order Placed!");

        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }
    }

    public void currentBotTest(final Request request, final Response response) {
        request.addParam("BOUGHT", false);

        final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
        BigDecimal orderAmount = BigDecimal.ZERO;

        switch (trade.getCurrencyType()) {
            case "Dollars":
                orderAmount = trade.getCurrencyAmount();
                break;
            case "Shares":
                orderAmount = trade.getCurrencyAmount()
                        .multiply(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + trade.getSymbol()));
                break;
            default:
                System.out.println("Invalid currency type at current bot test");
                break;
        }

        if (trade.getAvailableBudget().compareTo(orderAmount.multiply(new BigDecimal(1.05))) > 0) {
            currentBuyTest(request, response);
        }

        if ((!(boolean) request.getParam("BOUGHT"))
                && trade.getSharesHeld()
                        .multiply(BigDecimal.valueOf(1.05))
                        .compareTo(orderAmount
                                .divide(tradeSignalCache.getRecentClosingPriceMap()
                                        .get("MINUTE::" + trade.getSymbol()),
                                        MathContext.DECIMAL32)) > 0) {
            currentSellTest(request, response);
        }
    }

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
                backloadTechnicalIndicator(request, response);
                break;
        }

    }

    @Override
    public void save(final Request request, final Response response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final Request request, final Response response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void item(final Request request, final Response response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void items(final Request request, final Response response) {
        // TODO Auto-generated method stub

    }

}
