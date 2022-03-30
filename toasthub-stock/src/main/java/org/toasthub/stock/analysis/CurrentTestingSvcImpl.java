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
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.TradeSignalCache;
import org.toasthub.stock.trade.TradeDao;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.common.Symbol;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.bytebuddy.agent.builder.AgentBuilder.CircularityLock.Global;
import net.jacobpeterson.alpaca.AlpacaAPI;
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

    private long now = ZonedDateTime.ofInstant(Instant.now(),
            ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS).toEpochSecond();

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
                updateTradeSignalCache();
                checkTrades();
                tradeAnalysisJobRunning.set(false);
            }).start();
        }
    }

    public void updateTradeSignalCache() {
        Request request = new Request();
        Response response = new Response();
        updateRecentAssetStats(request, response);
        updateGoldenCrossCacheGlobals(request, response);
        updateLowerBollingerBandCacheGlobals(request, response);
    }

    public void updateRecentAssetStats(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.SYMBOL, symbol);
                currentTestingDao.getRecentAssetDay(request, response);

                AssetDay recentAssetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);

                tradeSignalCache.getRecentClosingPriceMap().put(symbol, recentAssetDay.getClose());
                tradeSignalCache.getRecentEpochSecondsMap().put(symbol, recentAssetDay.getEpochSeconds());
            }
        } catch (Exception e) {
            System.out.println("Error at Recent Assets Cache");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get(symbol));
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

                tradeSignalCache.getGoldenCrossMap().put("GLOBAL::" + symbol, globalGoldenCross);
            }
        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache");
            e.printStackTrace();
        }
    }

    public void updateLowerBollingerBandCacheGlobals(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.SYMBOL, symbol);
                request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSecondsMap().get(symbol));

                LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();
                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
                currentTestingDao.item(request, response);
                LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

                if (lbb.getValue().compareTo(tradeSignalCache.getRecentClosingPriceMap().get(symbol)) < 0)
                    lowerBollingerBand.setBuyIndicator(true);
                else
                    lowerBollingerBand.setBuyIndicator(false);

                tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::" + symbol, lowerBollingerBand);
            }
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    public void updateSignalLineCrossGlobals(Request request, Response response) {

    }

    private void checkTrades() {
        try {
            System.out.println("Running trade analysis job");
            List<Trade> trades = tradeDao.getAutomatedTrades("Yes");

            if (trades != null && trades.size() > 0) {
                for (Trade trade : trades) {
                    System.out.println("Checking trade name:" + trade.getName());
                    switch (trade.getOrderSide()) {
                        case "Buy":
                            currentBuyTest(trade);
                            break;
                        case "Sell":
                            currentSellTest(trade);
                            break;
                        case "Bot":
                            currentBotTest(trade);
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

    public void currentBuyTest(Trade trade) {

        int truncatedSharesAmount = 0;
        switch (trade.getCurrencyType()) {
            case "Dollars":
                truncatedSharesAmount = trade.getCurrencyAmount()
                        .divide(tradeSignalCache.getRecentClosingPriceMap().get(trade.getSymbol()),
                                MathContext.DECIMAL32)
                        .intValue();
                break;
            case "Shares":
                truncatedSharesAmount = trade.getCurrencyAmount().intValue();
                break;
            default:
                return;
        }

        double trailingStopPrice = 0;
        double profitLimitPrice = 0;
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

        if (true
        // evaluate(currentOrderSignals.process(alg1),
        // currentOrderSignals.process(alg2),
        // operand)
        ) {
            try {
                switch (trade.getOrderType()) {
                    case "Market":

                        switch (trade.getCurrencyType()) {

                            case "Dollars":
                                alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                        trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                                break;
                            case "Shares":
                                alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
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
                                alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                        truncatedSharesAmount, OrderSide.BUY,
                                        OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                        false);
                                break;
                            case "Trailing Stop Percent":
                                alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
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
                                        .add(tradeSignalCache.getRecentClosingPriceMap().get(trade.getSymbol()))
                                        .doubleValue();
                                break;
                            case "Profit Limit Percent":
                                profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                        .multiply(tradeSignalCache.getRecentClosingPriceMap().get(trade.getSymbol()))
                                        .doubleValue();
                                break;
                            default:
                                System.out.println("Profit limit type does not match");
                                break;
                        }
                        alpacaAPI.orders().requestMarketOrder(trade.getSymbol(), truncatedSharesAmount,
                                OrderSide.BUY, OrderTimeInForce.DAY);
                        alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount, OrderSide.SELL,
                                OrderTimeInForce.DAY, profitLimitPrice, false);
                        break;

                    case "Trailing Stop & Profit Limit":

                        switch (trade.getTrailingStopType()) {
                            case "Trailing Stop Price":
                                alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                        truncatedSharesAmount, OrderSide.BUY,
                                        OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                        false);
                                break;
                            case "Trailing Stop Percent":
                                alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
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
                                        .add(tradeSignalCache.getRecentClosingPriceMap().get(trade.getSymbol()))
                                        .doubleValue();
                                break;
                            case "Profit Limit Percent":
                                profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                        .multiply(tradeSignalCache.getRecentClosingPriceMap().get(trade.getSymbol()))
                                        .doubleValue();
                                break;
                            default:
                                System.out.println("Profit limit type does not match");
                                break;
                        }

                        alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount, OrderSide.SELL,
                                OrderTimeInForce.DAY, profitLimitPrice, false);
                        break;

                    default:
                        System.out.println("Case Not found!");

                }
                System.out.println("Order Placed!");
            } catch (Exception e) {
                System.out.println("Not Executed!");
                e.printStackTrace();
            }
        } else
            System.out.println("Buy Condition not met");
    }

    public void currentSellTest(Trade trade) {

    }

    public void currentBotTest(Trade trade) {
    }
}
