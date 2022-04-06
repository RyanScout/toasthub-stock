package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.TradeSignalCache;
import org.toasthub.stock.model.cache.UpperBollingerBand;
import org.toasthub.stock.trade.TradeDao;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.common.Symbol;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.bytebuddy.agent.builder.AgentBuilder.CircularityLock.Global;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;

@Service("CurrentTestingSvc")
public class CurrentTestingSvcImpl {

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

        } else {
            new Thread(() -> {
                tradeAnalysisJobRunning.set(true);
                Request request = new Request();
                Response response = new Response();
                updateTradeSignalCache(request, response);
                updateTrades(request, response);
                checkMinuteTrades(request, response);
                tradeAnalysisJobRunning.set(false);
            }).start();
        }
    }

    public void updateTradeSignalCache(Request request, Response response) {
        updateRecentAssetStats(request, response);
        updateGoldenCrossCacheGlobals(request, response);
        updateLowerBollingerBandCacheGlobals(request, response);
        updateUpperBollingerBandCacheGlobals(request, response);
    }

    public void updateRecentAssetStats(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.SYMBOL, symbol);
                currentTestingDao.getRecentAssetDay(request, response);

                AssetDay recentAssetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);

                tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAssetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAssetDay.getEpochSeconds());

                request.addParam(GlobalConstant.SYMBOL, symbol);
                currentTestingDao.getRecentAssetMinute(request, response);

                AssetMinute recentAssetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);

                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, recentAssetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                        recentAssetMinute.getEpochSeconds());
            }
        } catch (Exception e) {
            System.out.println("Error at Recent Assets Cache");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                // updating day stats
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
                request.addParam(GlobalConstant.SYMBOL, symbol);

                GoldenCross globalGoldenCross = new GoldenCross();
                globalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
                globalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);

                request.addParam(GlobalConstant.IDENTIFIER, "SMA");

                request.addParam(GlobalConstant.TYPE, globalGoldenCross.getShortSMAType());
                currentTestingDao.item(request, response);
                SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

                request.addParam(GlobalConstant.TYPE, globalGoldenCross.getLongSMAType());
                currentTestingDao.item(request, response);
                SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

                if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0)
                    globalGoldenCross.setBuyIndicator(true);
                else
                    globalGoldenCross.setBuyIndicator(false);

                tradeSignalCache.getGoldenCrossMap().put("GLOBAL::DAY::" + symbol, globalGoldenCross);

                // updating minute stats
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
                request.addParam(GlobalConstant.SYMBOL, symbol);

                GoldenCross globalGoldenCross2 = new GoldenCross();
                globalGoldenCross2.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
                globalGoldenCross2.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);

                request.addParam(GlobalConstant.IDENTIFIER, "SMA");

                request.addParam(GlobalConstant.TYPE, globalGoldenCross2.getShortSMAType());
                currentTestingDao.item(request, response);
                SMA shortSMA2 = (SMA) response.getParam(GlobalConstant.ITEM);

                request.addParam(GlobalConstant.TYPE, globalGoldenCross2.getLongSMAType());
                currentTestingDao.item(request, response);
                SMA longSMA2 = (SMA) response.getParam(GlobalConstant.ITEM);

                if (shortSMA2.getValue().compareTo(longSMA2.getValue()) > 0)
                    globalGoldenCross2.setBuyIndicator(true);
                else
                    globalGoldenCross2.setBuyIndicator(false);

                tradeSignalCache.getGoldenCrossMap().put("GLOBAL::MINUTE::" + symbol, globalGoldenCross2);
            }
        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache");
            e.printStackTrace();
        }
    }

    public void updateLowerBollingerBandCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                // updating day stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

                LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();
                lowerBollingerBand.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);
                lowerBollingerBand.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);

                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
                currentTestingDao.item(request, response);
                LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::" + symbol).compareTo(lbb.getValue()) < 0)
                    lowerBollingerBand.setBuyIndicator(true);
                else
                    lowerBollingerBand.setBuyIndicator(false);

                tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::DAY::" + symbol, lowerBollingerBand);

                // updating minute stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

                LowerBollingerBand lowerBollingerBand2 = new LowerBollingerBand();
                lowerBollingerBand2.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);
                lowerBollingerBand2.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);

                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand2.getLBBType());
                currentTestingDao.item(request, response);
                LBB lbb2 = (LBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol).compareTo(lbb2.getValue()) < 0)
                    lowerBollingerBand2.setBuyIndicator(true);
                else
                    lowerBollingerBand2.setBuyIndicator(false);

                tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::MINUTE::" + symbol, lowerBollingerBand2);
            }
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    public void updateUpperBollingerBandCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                // updating day stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

                UpperBollingerBand upperBollingerBand = new UpperBollingerBand();
                upperBollingerBand.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);
                upperBollingerBand.setStandardDeviationValue(UpperBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);

                request.addParam(GlobalConstant.IDENTIFIER, "UBB");

                request.addParam(GlobalConstant.TYPE, upperBollingerBand.getUBBType());
                currentTestingDao.item(request, response);
                UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::" + symbol).compareTo(ubb.getValue()) > 0)
                    upperBollingerBand.setSellIndicator(true);
                else
                    upperBollingerBand.setSellIndicator(false);

                tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::DAY::" + symbol, upperBollingerBand);

                // updating minute stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

                UpperBollingerBand upperBollingerBand2 = new UpperBollingerBand();
                upperBollingerBand2.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);
                upperBollingerBand2.setStandardDeviationValue(UpperBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);

                request.addParam(GlobalConstant.IDENTIFIER, "UBB");

                request.addParam(GlobalConstant.TYPE, upperBollingerBand2.getUBBType());
                currentTestingDao.item(request, response);
                UBB ubb2 = (UBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol).compareTo(ubb2.getValue()) > 0)
                    upperBollingerBand2.setSellIndicator(true);
                else
                    upperBollingerBand2.setSellIndicator(false);

                tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::MINUTE::" + symbol, upperBollingerBand);
            }
        } catch (Exception e) {
            System.out.println("Error at Upper Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    public void updateSignalLineCrossGlobals(Request request, Response response) {

    }

    private void updateTrades(Request request, Response response) {
        System.out.println("Updating Trades");
        List<Trade> trades = tradeDao.getAllRunningTrades();
        if (trades != null && trades.size() > 0) {
            for (Trade trade : trades) {
                trade.getTradeDetails().stream()
                        .filter(t -> !t.getStatus().equals("FILLED"))
                        .forEach(t -> {
                            try {
                                Order order = alpacaAPI.orders().getByClientID(t.getOrderID());
                                if (order.getStatus().name().equals("FILLED")) {
                                    if (t.getOrderSide().equals("BUY")) {
                                        switch (trade.getOrderSide()) {
                                            case "Buy":
                                                if (!trade.getFrequency().equals("unlimited")) {
                                                    trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);
                                                    if (trade.getFrequencyExecuted() >= Integer
                                                            .parseInt(trade.getFrequency()))
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
                                                BigDecimal orderQuantity = new BigDecimal(order.getQuantity());
                                                trade.setAvailableBudget(trade.getAvailableBudget()
                                                        .subtract(orderQuantity
                                                                .multiply(new BigDecimal(order.getAverageFillPrice())),
                                                                MathContext.DECIMAL32)
                                                        .setScale(2, BigDecimal.ROUND_HALF_UP));
                                                trade.setSharesHeld(trade.getSharesHeld().add(orderQuantity));
                                                trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);
                                                t.setSharesHeld(trade.getSharesHeld());
                                                t.setAvailableBudget(trade.getAvailableBudget());
                                                t.setDollarAmount(orderQuantity
                                                        .multiply(new BigDecimal(order.getAverageFillPrice()),
                                                                MathContext.DECIMAL32)
                                                        .setScale(2, BigDecimal.ROUND_HALF_UP));
                                                t.setShareAmount(orderQuantity);
                                                t.setFilledAt(order.getFilledAt().toEpochSecond());
                                                t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                                t.setTotalValue(trade.getAvailableBudget().add(trade.getSharesHeld()
                                                        .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                                .get("MINUTE::" + trade.getSymbol()))));
                                                t.setStatus("FILLED");
                                                t.setTrade(trade);
                                                break;
                                            default:
                                                System.out.println("Invalid orderside error");
                                                break;
                                        }
                                    }
                                    if (t.getOrderSide().equals("Sell")) {
                                        switch (trade.getOrderSide()) {
                                            case "Buy":
                                                t.setFilledAt(order.getFilledAt().toEpochSecond());
                                                t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                                t.setStatus("FILLED");
                                                t.setTrade(trade);
                                                break;
                                            case "Sell":
                                                if (!trade.getFrequency().equals("unlimited")) {
                                                    trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);
                                                    if (trade.getFrequencyExecuted() >= Integer
                                                            .parseInt(trade.getFrequency()))
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
                                                t.setSharesHeld(trade.getSharesHeld());
                                                t.setAvailableBudget(trade.getAvailableBudget());
                                                t.setShareAmount(orderQuantity);
                                                t.setFilledAt(order.getFilledAt().toEpochSecond());
                                                t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                                t.setStatus("FILLED");
                                                t.setTrade(trade);
                                                break;
                                            default:
                                                System.out.println("Invalid orderside error");
                                                break;
                                        }
                                    }
                                    request.addParam(GlobalConstant.ITEM, trade);
                                    tradeDao.save(request, response);
                                }
                            } catch (Exception e) {
                                System.out.println("Error retrieving orderId");
                                e.printStackTrace();
                            }
                        });
            }
        }
    }

    private void checkMinuteTrades(Request request, Response response) {
        try {
            System.out.println("Running trade analysis job");
            List<Trade> trades = tradeDao.getRunningMinuteTrades();
            if (trades != null && trades.size() > 0) {
                for (Trade trade : trades) {

                    request.addParam(GlobalConstant.TRADE, trade);
                    System.out.println("Checking trade name: " + trade.getName());
                    switch (trade.getOrderSide()) {
                        case "Buy":
                            currentBuyTest(request, response);
                            break;
                        case "Sell":
                            currentSellTest(request, response);
                            break;
                        case "Bot":
                            currentBotTest(request, response);
                            break;
                        default:
                            return;
                    }

                    request.addParam(GlobalConstant.ITEM, trade);
                    tradeDao.save(request, response);
                }
            } else {
                System.out.println("No trades to run");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean evaluate(boolean bool1, boolean bool2, String operand) {
        boolean result = false;
        if (operand.equals(""))
            result = bool1;
        if (operand.equals("AND"))
            result = bool1 && bool2;
        if (operand.equals("OR"))
            result = bool1 || bool2;
        return result;
    }

    public void currentBuyTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            if (trade.getFrequency().equals("unlimited")
                    || trade.getFrequencyExecuted() < Integer.parseInt(trade.getFrequency())) {
                String buyCondition = trade.getBuyCondition();
                String sellOrderCondition="";
                String buyOrderCondition = "";
                String alg1 = buyCondition;
                String operand = "";
                String alg2 = "";

                if(buyCondition.equals("") || buyCondition.equals("null")||buyCondition == null){
                    if (buyOrderCondition.equals("")){
                        buyOrderCondition= buyOrderCondition +"test";
                    }else{
                        buyOrderCondition = buyOrderCondition+"&test";
                    }
                }


                if (buyCondition.contains(" ")) {
                    alg1 = buyCondition.substring(0, buyCondition.indexOf(" "));
                    operand = buyCondition.substring(buyCondition.indexOf(" ") + 1,
                            buyCondition.indexOf((" "), buyCondition.indexOf(" ") + 1));
                    alg2 = buyCondition.substring(buyCondition.indexOf((" "), buyCondition.indexOf(" ") + 1) + 1,
                            buyCondition.length());
                }

                boolean bool1 =currentOrderSignals.process(alg1, trade.getSymbol(), trade.getEvaluationPeriod());
                boolean bool2 =currentOrderSignals.process(alg2, trade.getSymbol(), trade.getEvaluationPeriod());

                if(bool1){
                    if (buyOrderCondition.equals("")){
                        buyOrderCondition= buyOrderCondition +alg1;
                    }else{
                        buyOrderCondition = buyOrderCondition+"&"+alg1;
                    }
                }

                if(bool2){
                    if (buyOrderCondition.equals("")){
                        buyOrderCondition= buyOrderCondition +alg2;
                    }else{
                        buyOrderCondition = buyOrderCondition+"&"+alg2;
                    }
                }

                if (evaluate(bool1 , bool2, operand) || buyCondition.equals("") || buyCondition.equals("null")) {

                    Order sellOrder = null;
                    Order buyOrder = null;
                    int truncatedSharesAmount = 0;
                    double trailingStopPrice = 0;
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
                            sellOrderCondition="Profit Limit";
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

                        default:
                            System.out.println("Case Not found!");
                            break;

                    }
                    request.addParam("BUYORDER", buyOrder);
                    request.addParam("SELLORDER", sellOrder);

                    if (buyOrder != null) {
                        TradeDetail tradeDetail = new TradeDetail();
                        tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                        tradeDetail.setOrderID(buyOrder.getClientOrderId());
                        tradeDetail.setStatus(buyOrder.getStatus().name());
                        tradeDetail.setOrderSide("BUY");
                        tradeDetail.setOrderCondition(buyOrderCondition);
                        tradeDetail.setTrade(trade);
                        trade.getTradeDetails().add(tradeDetail);
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

                    System.out.println("Order Placed!");

                } else
                    System.out.println("Buy Condition not met");

            } else {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
            }
        } catch (Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }

    }

    public void currentSellTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            if (trade.getFrequency().equals("unlimited")
                    || trade.getFrequencyExecuted() < Integer.parseInt(trade.getFrequency())) {
                String sellCondition = trade.getSellCondition();
                String alg1 = sellCondition;
                String operand = "";
                String alg2 = "";

                if (sellCondition.contains(" ")) {
                    alg1 = sellCondition.substring(0, sellCondition.indexOf(" "));
                    operand = sellCondition.substring(sellCondition.indexOf(" ") + 1,
                            sellCondition.indexOf((" "), sellCondition.indexOf(" ") + 1));
                    alg2 = sellCondition.substring(sellCondition.indexOf((" "), sellCondition.indexOf(" ") + 1) + 1,
                            sellCondition.length());
                }

                if (evaluate(currentOrderSignals.process(alg1, trade.getSymbol(), trade.getEvaluationPeriod()),
                        currentOrderSignals.process(alg2, trade.getSymbol(), trade.getEvaluationPeriod()),
                        operand)) {
                    Order sellOrder = new Order();
                    int truncatedSharesAmount = 0;
                    double trailingStopPrice = 0;
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
                                    sellOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                            trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                                    break;
                                case "Shares":
                                    sellOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                            trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
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
                    request.addParam("SELLORDER", sellOrder);
                    if (trade.getOrderSide().equals("Sell")) {
                        trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);

                        if (!trade.getFrequency().equals("unlimited")) {
                            if (trade.getFrequencyExecuted() >= Integer.parseInt(trade.getFrequency()))
                                trade.setStatus("Not Running");
                        }

                    }
                    System.out.println("Order Placed!");

                } else
                    System.out.println("Sell Condition not met");

            } else {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
            }
        } catch (Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }
    }

    public void currentBotTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            BigDecimal orderAmount = BigDecimal.ZERO;
            BigDecimal orderQuantity = BigDecimal.ZERO;
            Order buyOrder = new Order();
            Order sellOrder = new Order();

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
            if (request.getParam("BUYORDER") == null
                    && trade.getSharesHeld().multiply(new BigDecimal(1.05)).compareTo(orderAmount
                            .divide(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32)) > 0) {
                currentSellTest(request, response);
            }
        } catch (Exception e) {
            System.out.println("Error at current bot test");
            e.printStackTrace();
        }
    }
}
