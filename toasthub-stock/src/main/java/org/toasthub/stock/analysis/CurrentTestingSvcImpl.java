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
import org.toasthub.analysis.model.StockDay;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.TradeSignalCache;
import org.toasthub.stock.trade.TradeDao;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

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

    // @Scheduled(cron = "30 * * * * ?")
    public void tradeAnalysisTask() {

        if (tradeAnalysisJobRunning.get()) {
            System.out.println("Trade analysis is currently running ,  skipping this time");
            return;

        } else {
            tradeAnalysisJobRunning.set(true);
            updateTradeSignalCache();
            checkTrades();
            tradeAnalysisJobRunning.set(false);
        }
    }

    public void updateTradeSignalCache() {
        Request request = new Request();
        Response response = new Response();
        updateRecentStockDayStats(request, response);
        updateGoldenCrossCacheGlobals(request, response);
        updateLowerBollingerBandCacheGlobals(request, response);
    }

    public void updateRecentStockDayStats(Request request, Response response) {
        try {
            currentTestingDao.getRecentStockDay(request, response);

            StockDay recentStockDay = (StockDay) response.getParam(GlobalConstant.ITEM);

            tradeSignalCache.setRecentClosingPrice(recentStockDay.getClose());
            tradeSignalCache.setRecentEpochSeconds(recentStockDay.getEpochSeconds());

        } catch (Exception e) {
            System.out.println("Error at Closing Price Cache");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobals(Request request, Response response) {
        try {

            request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSeconds());
            request.addParam(GlobalConstant.STOCK, "SPY");

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

            tradeSignalCache.getGoldenCrossMap().put("GLOBAL", globalGoldenCross);

        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache");
            e.printStackTrace();
        }
    }

    public void updateLowerBollingerBandCacheGlobals(Request request, Response response) {
        try {
            request.addParam(GlobalConstant.EPOCHSECONDS, tradeSignalCache.getRecentEpochSeconds());
            request.addParam(GlobalConstant.STOCK, "SPY");

            LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();
            request.addParam(GlobalConstant.IDENTIFIER, "LBB");

            request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
            currentTestingDao.item(request, response);
            LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

            if (lbb.getValue().compareTo(tradeSignalCache.getRecentClosingPrice()) < 0)
                lowerBollingerBand.setBuyIndicator(true);
            else
                lowerBollingerBand.setBuyIndicator(false);

            tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL", lowerBollingerBand);
        } catch (Exception e) {
            System.out.println("Error at LowerBollingerBand Cache");
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
                        .divide(tradeSignalCache.getRecentClosingPrice(), MathContext.DECIMAL32).intValue();
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
            //     currentOrderSignals.process(alg2),
            //     operand)
            ) {
            try {
                switch (trade.getOrderType()) {
                    case "Market":

                        switch (trade.getCurrencyType()) {

                            case "Dollars":
                                alpacaAPI.orders().requestNotionalMarketOrder(trade.getStock(),
                                        trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                                break;
                            case "Shares":
                                alpacaAPI.orders().requestFractionalMarketOrder(trade.getStock(),
                                        trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                                break;
                            default:
                                return;
                        }

                    case "Trailing Stop":
                        switch (trade.getTrailingStopType()) {
                            case "Trailing Stop Price":
                                trailingStopPrice = trade.getTrailingStopAmount().doubleValue();
                                break;
                            case "Trailing Stop Percent":
                                trailingStopPrice = trade.getTrailingStopAmount()
                                        .multiply(tradeSignalCache.getRecentClosingPrice())
                                        .doubleValue();
                                break;
                        }

                        alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getStock(),
                                truncatedSharesAmount, OrderSide.BUY,
                                OrderTimeInForce.FILL_OR_KILL, trailingStopPrice,
                                true);
                        break;

                    case "Profit Limit":

                        switch (trade.getTrailingStopType()) {
                            case "Profit Limit Price":
                                profitLimitPrice = trade.getProfitLimitAmount().doubleValue();
                                break;
                            case "Profit Limit Percent":
                                profitLimitPrice = trade.getProfitLimitAmount()
                                        .multiply(tradeSignalCache.getRecentClosingPrice())
                                        .doubleValue();
                                break;
                        }

                        alpacaAPI.orders().requestOTOMarketOrder(trade.getStock(), truncatedSharesAmount,
                                OrderSide.BUY, OrderTimeInForce.FILL_OR_KILL,
                                profitLimitPrice, null, null);
                        break;

                    case "Trailing Stop & Profit Limit":
                        switch (trade.getTrailingStopType()) {
                            case "Trailing Stop Price":
                                trailingStopPrice = trade.getTrailingStopAmount().doubleValue();
                            case "Trailing Stop Percent":
                                trailingStopPrice = trade.getTrailingStopAmount()
                                        .multiply(tradeSignalCache.getRecentClosingPrice())
                                        .doubleValue();
                        }

                        switch (trade.getTrailingStopType()) {
                            case "Profit Limit Price":
                                profitLimitPrice = trade.getProfitLimitAmount().doubleValue();
                                break;
                            case "Profit Limit Percent":
                                profitLimitPrice = trade.getProfitLimitAmount()
                                        .multiply(tradeSignalCache.getRecentClosingPrice())
                                        .doubleValue();
                                break;
                        }

                        alpacaAPI.orders().requestOTOMarketOrder(trade.getStock(), truncatedSharesAmount,
                                OrderSide.BUY, OrderTimeInForce.FILL_OR_KILL,
                                profitLimitPrice, trailingStopPrice,
                                null);
                        break;

                    default:
                        System.out.println("Case Not found!");
                    
                }
                System.out.println("Order Placed!");
            } catch (Exception e) {
                System.out.println("Not Executed!");
                System.out.println(e.getMessage());
            }
        } else
            System.out.println("Buy Condition not met");
    }

    public void currentSellTest(Trade trade) {

    }

    public void currentBotTest(Trade trade) {
    }
}
