package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.stock.model.Trade;
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

    // @Scheduled(cron = "30 * * * * ?")
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
                checkTrades(request, response);
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

                tradeSignalCache.getRecentClosingPriceMap().put("DAY::"+symbol, recentAssetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put("DAY::"+symbol, recentAssetDay.getEpochSeconds());

                request.addParam(GlobalConstant.SYMBOL, symbol);
                currentTestingDao.getRecentAssetMinute(request, response);

                AssetMinute recentAssetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);

                tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::"+symbol, recentAssetMinute.getValue());
                tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::"+symbol, recentAssetMinute.getEpochSeconds());
            }
        } catch (Exception e) {
            System.out.println("Error at Recent Assets Cache");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                //updating day stats
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("DAY::"+symbol));
                request.addParam(GlobalConstant.SYMBOL, symbol);

                GoldenCross globalGoldenCross = new GoldenCross();
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

                //updating minute stats
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::"+symbol));
                request.addParam(GlobalConstant.SYMBOL, symbol);

                GoldenCross globalGoldenCross2 = new GoldenCross();
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
                //updating day stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("DAY::"+symbol));

                LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();
                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
                currentTestingDao.item(request, response);
                LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::"+symbol).compareTo(lbb.getValue()) < 0)
                    lowerBollingerBand.setBuyIndicator(true);
                else
                    lowerBollingerBand.setBuyIndicator(false);

                tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::DAY" + symbol, lowerBollingerBand);


                //updating minute stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::"+symbol));

                LowerBollingerBand lowerBollingerBand2 = new LowerBollingerBand();
                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand2.getLBBType());
                currentTestingDao.item(request, response);
                LBB lbb2 = (LBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+symbol).compareTo(lbb2.getValue()) < 0)
                    lowerBollingerBand2.setBuyIndicator(true);
                else
                    lowerBollingerBand2.setBuyIndicator(false);

                tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::MINUTE" + symbol, lowerBollingerBand2);
            }
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    public void updateUpperBollingerBandCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                //updating day stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("DAY::"+symbol));

                UpperBollingerBand upperBollingerBand = new UpperBollingerBand();
                request.addParam(GlobalConstant.IDENTIFIER, "UBB");

                request.addParam(GlobalConstant.TYPE, upperBollingerBand.getUBBType());
                currentTestingDao.item(request, response);
                UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::"+symbol).compareTo(ubb.getValue()) > 0)
                    upperBollingerBand.setSellIndicator(true);
                else
                    upperBollingerBand.setSellIndicator(false);

                tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::DAY::" + symbol, upperBollingerBand);


                //updating minute stats
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::"+symbol));

                UpperBollingerBand upperBollingerBand2 = new UpperBollingerBand();
                request.addParam(GlobalConstant.IDENTIFIER, "UBB");

                request.addParam(GlobalConstant.TYPE, upperBollingerBand2.getUBBType());
                currentTestingDao.item(request, response);
                UBB ubb2 = (UBB) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+symbol).compareTo(ubb2.getValue()) > 0)
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

    private void checkTrades(Request request, Response response) {
        try {
            System.out.println("Running trade analysis job");
            List<Trade> trades = tradeDao.getRunningTrades();

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
                }
            } else {
                System.out.println("No trades to run");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean evaluate(boolean alg1, boolean alg2, String operand) {
        boolean result = false;

        if (operand.equals(""))
            result = alg1;
        if (operand.equals("AND"))
            result = alg1 && alg2;
        if (operand.equals("OR"))
            result = alg1 || alg2;
        return result;
    }

    public void currentBuyTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            if (trade.getFrequency().equals("unlimited")
                    || trade.getFrequencyExecuted() < Integer.parseInt(trade.getFrequency())) {
                String buyCondition = trade.getBuyCondition();
                String alg1 = buyCondition;
                String operand = "";
                String alg2 = "";

                if (buyCondition.contains(" ")) {
                    alg1 = buyCondition.substring(0, buyCondition.indexOf(" "));
                    operand = buyCondition.substring(buyCondition.indexOf(" ") + 1,
                            buyCondition.indexOf((" "), buyCondition.indexOf(" ") + 1));
                    alg2 = buyCondition.substring(buyCondition.indexOf((" "), buyCondition.indexOf(" ") + 1) + 1,
                            buyCondition.length());
                }

                if (evaluate(currentOrderSignals.process(alg1, trade.getSymbol()),
                        currentOrderSignals.process(alg2, trade.getSymbol()),
                        operand)) {
                    Order sellOrder = new Order();
                    Order buyOrder = new Order();
                    int truncatedSharesAmount = 0;
                    double trailingStopPrice = 0;
                    double profitLimitPrice = 0;
                    switch (trade.getCurrencyType()) {
                        case "Dollars":
                            truncatedSharesAmount = trade.getCurrencyAmount()
                                    .divide(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()),
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
                                            .add(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()))
                                            .doubleValue();
                                    break;
                                case "Profit Limit Percent":
                                    profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                            .multiply(
                                                    tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()))
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
                                            .add(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()))
                                            .doubleValue();
                                    break;
                                case "Profit Limit Percent":
                                    profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                            .multiply(
                                                    tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()))
                                            .doubleValue();
                                    break;
                                default:
                                    System.out.println("Profit limit type does not match");
                                    break;
                            }

                            sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                                    OrderSide.SELL,
                                    OrderTimeInForce.DAY, profitLimitPrice, false);
                            break;

                        default:
                            System.out.println("Case Not found!");
                            break;

                    }
                    request.addParam("BUYORDER", buyOrder);
                    request.addParam("SELLORDER", sellOrder);
                    trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);

                    if (!trade.getFrequency().equals("unlimited")) {
                        if (trade.getFrequencyExecuted() >= Integer.parseInt(trade.getFrequency()))
                            trade.setStatus("Not Running");
                    }
                    if (trade.getOrderSide().equals("Buy")) {
                        request.addParam(GlobalConstant.ITEM, trade);
                        tradeDao.save(request, response);
                    }
                    System.out.println("Order Placed!");

                } else
                    System.out.println("Buy Condition not met");

            } else {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                request.addParam(GlobalConstant.ITEM, trade);
                tradeDao.save(request, response);
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

                if (evaluate(currentOrderSignals.process(alg1, trade.getSymbol()),
                        currentOrderSignals.process(alg2, trade.getSymbol()),
                        operand)) {
                    Order sellOrder = new Order();
                    int truncatedSharesAmount = 0;
                    double trailingStopPrice = 0;
                    double profitLimitPrice = 0;
                    switch (trade.getCurrencyType()) {
                        case "Dollars":
                            truncatedSharesAmount = trade.getCurrencyAmount()
                                    .divide(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()),
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
                        request.addParam(GlobalConstant.ITEM, trade);
                        tradeDao.save(request, response);
                    }
                    System.out.println("Order Placed!");

                } else
                    System.out.println("Sell Condition not met");

            } else {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                request.addParam(GlobalConstant.ITEM, trade);
                tradeDao.save(request, response);
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

            if (trade.getRecentBuyOrderID() != null) {
                buyOrder = alpacaAPI.orders().getByClientID(trade.getRecentBuyOrderID());
                orderQuantity = new BigDecimal(buyOrder.getQuantity());
                trade.setAvailableBudget(trade.getAvailableBudget()
                        .subtract(orderQuantity.multiply(new BigDecimal(buyOrder.getAverageFillPrice())),
                                MathContext.DECIMAL32)
                        .setScale(2, BigDecimal.ROUND_HALF_DOWN));
                trade.setSharesHeld(trade.getSharesHeld().add(orderQuantity));
                trade.setRecentBuyOrderID(null);
            }
            if (trade.getRecentSellOrderID() != null) {
                sellOrder = alpacaAPI.orders().getByClientID(trade.getRecentBuyOrderID());
                orderQuantity = new BigDecimal(sellOrder.getQuantity());
                trade.setAvailableBudget(trade.getAvailableBudget()
                        .add(orderQuantity.multiply(new BigDecimal(sellOrder.getAverageFillPrice())),
                                MathContext.DECIMAL32)
                        .setScale(2, BigDecimal.ROUND_HALF_DOWN));
                trade.setSharesHeld(trade.getSharesHeld().subtract(orderQuantity));
                trade.setRecentSellOrderID(null);
            }
            switch (trade.getCurrencyType()) {
                case "Dollars":
                    orderAmount = trade.getCurrencyAmount();
                    break;
                case "Shares":
                    orderAmount = trade.getCurrencyAmount()
                            .multiply(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()));
                    break;
                default:
                    System.out.println("Invalid currency type at current bot test");
                    break;
            }
            if (trade.getAvailableBudget().compareTo(orderAmount.multiply(BigDecimal.ONE.movePointLeft(1))) > 0) {
                currentBuyTest(request, response);
                if (request.getParam("BUYORDER") != null) {
                    buyOrder = (Order) request.getParam("BUYORDER");
                    trade.setRecentBuyOrderID(buyOrder.getClientOrderId());
                }
            }
            if (request.getParam("BUYORDER") == null
                    && trade.getSharesHeld().multiply(BigDecimal.ONE.movePointLeft(1)).compareTo(orderAmount
                            .divide(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::"+trade.getSymbol()),
                                    MathContext.DECIMAL32)) > 0) {
                currentSellTest(request, response);
                if (request.getParam("SELLORDER") != null) {
                    sellOrder = (Order) request.getParam("SELLORDER");
                    trade.setRecentSellOrderID(buyOrder.getClientOrderId());
                }
            }

            request.addParam(GlobalConstant.ITEM, trade);
            tradeDao.save(request, response);
        } catch (Exception e) {
            System.out.println("Error at current bot test");
            e.printStackTrace();
        }
    }
}
