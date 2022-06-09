package org.toasthub.stock.analysis;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.model.TechnicalIndicatorDetail;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
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
    protected CurrentOrderSignals currentOrderSignals;

    @Autowired
    protected CurrentTestingDao currentTestingDao;

    @Autowired
    protected TradeSignalCache tradeSignalCache;

    @Autowired
    protected CacheDao cacheDao;

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    final ExpressionParser parser = new SpelExpressionParser();

    final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

    // Constructors
    public CurrentTestingSvcImpl() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Scheduled(cron = "30 * * * * ?")
    public void tradeAnalysisTask() {

        if (tradeAnalysisJobRunning.get()) {
            System.out.println("Trade analysis is currently running ,  skipping this time");
            return;
        }

        new Thread(() -> {
            tradeAnalysisJobRunning.set(true);

            Request request = new Request();
            Response response = new Response();

            updateRawData(request, response);

            updateTechnicalIndicatorCache(request, response);

            updateTrades(request, response);

            checkMinuteTrades(request, response);

            tradeAnalysisJobRunning.set(false);
        }).start();
    }

    public void updateRawData(Request request, Response response) {
        Stream.of(Symbol.SYMBOLS).forEach(symbol -> {
            request.addParam(GlobalConstant.SYMBOL, symbol);

            AssetDay recentAsesetDay = null;

            try {
                currentTestingDao.getRecentAssetDay(request, response);
                recentAsesetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
            } catch (NoResultException e) {
            }

            if (recentAsesetDay != null) {
                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAsesetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAsesetDay.getEpochSeconds());
                tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, recentAsesetDay.getVolume());
                tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, recentAsesetDay.getVwap());
            }

            AssetMinute recentAsesetMinute = null;

            try {
                currentTestingDao.getRecentAssetMinute(request, response);
                recentAsesetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);
            } catch (NoResultException e) {
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

    public boolean goldenCrossIsFlashing(Request request, Response response) {
        boolean result = false;

        String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
        String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
        String shortSMAType = (String) request.getParam("SHORT_SMA_TYPE");
        String longSMAType = (String) request.getParam("LONG_SMA_TYPE");

        request.addParam(GlobalConstant.EPOCHSECONDS,
                tradeSignalCache.getRecentEpochSecondsMap().get(evaluationPeriod + "::" + symbol));

        request.addParam(GlobalConstant.IDENTIFIER, "SMA");

        request.addParam(GlobalConstant.TYPE, shortSMAType);
        try {
            currentTestingDao.itemCount(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

        request.addParam(GlobalConstant.TYPE, longSMAType);
        try {
            currentTestingDao.itemCount(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

        if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0) {
            result = true;
        }
        return result;
    }

    public boolean lowerBollingerBandIsFlashing(Request request, Response response) {
        boolean result = false;

        String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
        String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
        String lbbType = (String) request.getParam("LBB_TYPE");
        BigDecimal standardDeviations = (BigDecimal) request.getParam("STANDARD_DEVIATIONS");

        request.addParam(GlobalConstant.EPOCHSECONDS,
                tradeSignalCache.getRecentEpochSecondsMap().get(evaluationPeriod + "::" + symbol));

        request.addParam(GlobalConstant.IDENTIFIER, "LBB");

        request.addParam(GlobalConstant.TYPE, lbbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            currentTestingDao.itemCount(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

        if (tradeSignalCache.getRecentClosingPriceMap().get(evaluationPeriod + "::" + symbol)
                .compareTo(lbb.getValue()) < 0) {
            result = true;
        }

        return result;
    }

    public boolean upperBollingerBandIsFlashing(Request request, Response response) {
        boolean result = false;

        String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
        String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
        String ubbType = (String) request.getParam("UBB_TYPE");
        BigDecimal standardDeviations = (BigDecimal) request.getParam("STANDARD_DEVIATIONS");

        request.addParam(GlobalConstant.EPOCHSECONDS,
                tradeSignalCache.getRecentEpochSecondsMap().get(evaluationPeriod + "::" + symbol));

        request.addParam(GlobalConstant.IDENTIFIER, "UBB");

        request.addParam(GlobalConstant.TYPE, ubbType);
        request.addParam("STANDARD_DEVIATIONS", standardDeviations);

        try {
            currentTestingDao.itemCount(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
            response.addParam("INSUFFICIENT_DATA", true);
            return result;
        }
        try {
            currentTestingDao.item(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

        if (tradeSignalCache.getRecentClosingPriceMap().get(evaluationPeriod + "::" + symbol)
                .compareTo(ubb.getValue()) > 0) {
            result = true;
        }

        return result;
    }

    public void updateTechnicalIndicatorCache(Request request, Response response) {
        try {
            cacheDao.items(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

        for (Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            technicalIndicators.add((TechnicalIndicator) o);
        }

        technicalIndicators.stream()
                .forEach(technicalIndicator -> {

                    String symbol = technicalIndicator.getSymbol();
                    String evaluationPeriod = technicalIndicator.getEvaluationPeriod();

                    tradeSignalCache.getTechnicalIndicatorMap()
                            .put(technicalIndicator.getTechnicalIndicatorType() + "::"
                                    + technicalIndicator.getTechnicalIndicatorKey() + "::"
                                    + evaluationPeriod + "::"
                                    + symbol, technicalIndicator);

                    if (tradeSignalCache.getRecentEpochSecondsMap()
                            .get(evaluationPeriod + "::" + symbol) == null) {
                        return;
                    }

                    boolean flashing = false;

                    request.addParam(GlobalConstant.SYMBOL, symbol);
                    request.addParam("EVALUATION_PERIOD", technicalIndicator.getEvaluationPeriod());

                    switch (technicalIndicator.getTechnicalIndicatorType()) {
                        case TechnicalIndicator.GOLDENCROSS:
                            request.addParam("SHORT_SMA_TYPE", technicalIndicator.getShortSMAType());
                            request.addParam("LONG_SMA_TYPE", technicalIndicator.getLongSMAType());
                            flashing = goldenCrossIsFlashing(request, response);
                            break;
                        case TechnicalIndicator.LOWERBOLLINGERBAND:
                            request.addParam("LBB_TYPE", technicalIndicator.getLBBType());
                            request.addParam("STANDARD_DEVIATIONS", technicalIndicator.getStandardDeviations());
                            flashing = lowerBollingerBandIsFlashing(request, response);
                            break;
                        case TechnicalIndicator.UPPERBOLLINGERBAND:
                            request.addParam("UBB_TYPE", technicalIndicator.getUBBType());
                            request.addParam("STANDARD_DEVIATIONS", technicalIndicator.getStandardDeviations());
                            flashing = upperBollingerBandIsFlashing(request, response);
                            break;
                    }

                    if (response.getParam("INSUFFICIENT_DATA") != null
                            && (boolean) response.getParam("INSUFFICIENT_DATA") == true) {
                        return;
                    }

                    technicalIndicator.setFlashing(flashing);

                    if (technicalIndicator.getFirstCheck() == 0) {
                        technicalIndicator
                                .setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap()
                                        .get(evaluationPeriod + "::" + symbol));
                    }

                    if (technicalIndicator.getLastCheck() < tradeSignalCache.getRecentEpochSecondsMap()
                            .get(evaluationPeriod + "::" + symbol)) {

                        request.addParam(technicalIndicator.getEvaluationPeriod() + "_CACHE_UPDATED::"
                                + technicalIndicator.getSymbol(), true);

                        technicalIndicator.getDetails().stream()
                                .filter(detail -> detail.getChecked() < 100)
                                .forEach(detail -> {
                                    detail.setChecked(detail.getChecked() + 1);

                                    BigDecimal tempSuccessPercent = (tradeSignalCache.getRecentClosingPriceMap()
                                            .get(evaluationPeriod + "::" + symbol)
                                            .subtract(detail.getFlashPrice()))
                                            .divide(detail.getFlashPrice(), MathContext.DECIMAL32)
                                            .multiply(BigDecimal.valueOf(100));

                                    if (technicalIndicator.getTechnicalIndicatorType()
                                            .equals(TechnicalIndicator.GOLDENCROSS)
                                            || technicalIndicator.getTechnicalIndicatorType()
                                                    .equals(TechnicalIndicator.LOWERBOLLINGERBAND)) {
                                        if (detail.getSuccessPercent() == null
                                                || detail.getSuccessPercent().compareTo(tempSuccessPercent) < 0) {
                                            detail.setSuccessPercent(tempSuccessPercent);
                                        }
                                    }

                                    if (technicalIndicator.getTechnicalIndicatorType()
                                            .equals(TechnicalIndicator.UPPERBOLLINGERBAND)) {
                                        if (detail.getSuccessPercent() == null
                                                || detail.getSuccessPercent()
                                                        .compareTo(tempSuccessPercent.negate()) < 0) {
                                            detail.setSuccessPercent(tempSuccessPercent.negate());
                                        }
                                    }

                                    if (detail.isSuccess() == false && detail.getFlashPrice().compareTo(
                                            tradeSignalCache.getRecentClosingPriceMap()
                                                    .get(evaluationPeriod + "::" + symbol)) < 0) {
                                        detail.setSuccess(true);
                                        technicalIndicator.setSuccesses(technicalIndicator.getSuccesses() + 1);
                                    }
                                });

                        if (technicalIndicator.isFlashing()) {

                            technicalIndicator.setLastFlash(
                                    tradeSignalCache.getRecentEpochSecondsMap()
                                            .get(evaluationPeriod + "::" + symbol));

                            technicalIndicator.setFlashed(technicalIndicator.getFlashed() + 1);

                            TechnicalIndicatorDetail technicalIndicatorDetail = new TechnicalIndicatorDetail();

                            technicalIndicatorDetail.setTechnicalIndicator(technicalIndicator);

                            technicalIndicatorDetail.setFlashTime(
                                    tradeSignalCache.getRecentEpochSecondsMap()
                                            .get(evaluationPeriod + "::" + symbol));

                            technicalIndicatorDetail.setFlashPrice(
                                    tradeSignalCache.getRecentClosingPriceMap()
                                            .get(evaluationPeriod + "::" + symbol));

                            technicalIndicatorDetail
                                    .setVolume(tradeSignalCache.getRecentVolumeMap()
                                            .get(evaluationPeriod + "::" + symbol));

                            technicalIndicatorDetail.setVwap(
                                    tradeSignalCache.getRecentVwapMap()
                                            .get(evaluationPeriod + "::" + symbol));

                            technicalIndicator.getDetails().add(technicalIndicatorDetail);
                        }

                        technicalIndicator.setChecked(technicalIndicator.getChecked() + 1);

                        technicalIndicator
                                .setLastCheck(tradeSignalCache.getRecentEpochSecondsMap()
                                        .get(evaluationPeriod + "::" + symbol));
                    }

                    request.addParam(GlobalConstant.ITEM, technicalIndicator);

                    try {
                        cacheDao.save(request, response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
    }

    private void updateTrades(Request request, Response response) {
        List<Trade> trades = tradeDao.getAllRunningTrades();

        if (trades == null || trades.size() == 0) {
            return;
        }

        trades.stream().forEach(trade -> {
            trade.getTradeDetails().stream()
                    .forEach(t -> {
                        Order order = null;
                        try {
                            order = alpacaAPI.orders().getByClientID(t.getOrderID());
                        } catch (Exception e) {
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
                                    BigDecimal orderQuantity = new BigDecimal(order.getFilledQuantity());
                                    BigDecimal fillPrice = new BigDecimal(order.getAverageFillPrice());

                                    trade.setAvailableBudget(
                                            trade.getAvailableBudget()
                                                    .subtract((orderQuantity.multiply(fillPrice)),
                                                            MathContext.DECIMAL32)
                                                    .setScale(2, BigDecimal.ROUND_HALF_UP));

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
                                                    .setScale(2, BigDecimal.ROUND_HALF_UP));

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
                                    BigDecimal orderQuantity = new BigDecimal(order.getQuantity());
                                    trade.setAvailableBudget(trade.getAvailableBudget()
                                            .add(orderQuantity
                                                    .multiply(new BigDecimal(order.getAverageFillPrice())),
                                                    MathContext.DECIMAL32)
                                            .setScale(2, BigDecimal.ROUND_HALF_DOWN));
                                    trade.setSharesHeld(trade.getSharesHeld().subtract(orderQuantity));
                                    trade.setTotalValue(trade.getAvailableBudget().add(trade.getSharesHeld()
                                            .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                    .get("MINUTE::" + trade.getSymbol()))));
                                    t.setSharesHeld(trade.getSharesHeld());
                                    t.setAvailableBudget(trade.getAvailableBudget());
                                    t.setDollarAmount(orderQuantity
                                            .multiply(new BigDecimal(order.getAverageFillPrice()),
                                                    MathContext.DECIMAL32)
                                            .setScale(2, BigDecimal.ROUND_HALF_UP));
                                    t.setShareAmount(orderQuantity);
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void checkMinuteTrades(Request request, Response response) {

        List<Trade> trades = tradeDao.getRunningMinuteTrades();

        if (trades == null || trades.size() == 0) {
            System.out.println("No Minute trades to run");
            return;
        }

        trades.stream()
                .filter(trade -> ((request.getParam("MINUTE_CACHE_UPDATED::" + trade.getSymbol()) != null)
                        && (boolean) request.getParam("MINUTE_CACHE_UPDATED::" + trade.getSymbol())))
                .forEach(trade -> {
                    request.addParam(GlobalConstant.TRADE, trade);

                    System.out.println("Checking minute trade: " + trade.getName());

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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public void currentBuyTest(Request request, Response response) {
        try {

            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

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

            List<String> buyReasons = new ArrayList<String>();
            String sellOrderCondition = "";

            String[] stringArr = trade.getParseableBuyCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
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

            String buyCondition = String.join(" ", stringArr);

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
                    if (trade.getRecentBuyOrderID() == null) {
                        buyOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                        trade.setRecentBuyOrderID(buyOrder.getClientOrderId());
                    }

                    break;

                default:
                    System.out.println("Case Not found!");
                    break;

            }

            request.addParam("BOUGHT", true);

            if (buyOrder != null) {
                TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("BUY");
                tradeDetail.setOrderCondition(String.join(",", buyReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
                if (trade.getFirstBuy() == 0)
                    trade.setFirstBuy(Instant.now().getEpochSecond());
            }

            if (sellOrder != null) {
                TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(sellOrderCondition);
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
            }

            System.out.println(trade.getName() + ":Buy Order Placed!");

        } catch (Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }

    }

    public void currentSellTest(Request request, Response response) {
        try {

            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

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

            List<String> sellReasons = new ArrayList<String>();

            String[] stringArr = trade.getParseableSellCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
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

            String sellCondition = String.join(" ", stringArr);

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
                TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(sellOrder.getClientOrderId());
                tradeDetail.setStatus(sellOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(String.join(",", sellReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
            }
            System.out.println("Sell Order Placed!");

        } catch (Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }
    }

    public void currentBotTest(Request request, Response response) {
        request.addParam("BOUGHT", false);

        Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
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
}
