package org.toasthub.stock.analysis;

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
import org.toasthub.analysis.model.SMA;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.cache.BuySignalCache;
import org.toasthub.stock.model.cache.BuySignalCacheDao;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.trade.TradeDao;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import ch.qos.logback.core.joran.conditional.ElseAction;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderClass;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;

@Service("CurrentTestingSvc")
public class CurrentTestingSvcImpl {

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected TradeDao tradeDao;

    @Autowired
    protected CurrentBuySignals currentBuySignals;

    @Autowired
    protected BuySignalCacheDao buySignalCacheDao;

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
            tradeAnalysisJobRunning.set(true);
            setBuySignalCacheGlobals();
            checkTrades();
            tradeAnalysisJobRunning.set(false);
        }
    }

    public void setBuySignalCacheGlobals() {
        try {
            BuySignalCache buySignalCache = BuySignalCache.getInstance();
            Request request = new Request();
            Response response = new Response();
            request.addParam(GlobalConstant.EPOCHSECONDS, now);
            request.addParam(GlobalConstant.STOCK, "SPY");

            GoldenCross globalGoldenCross = new GoldenCross();
            request.addParam(GlobalConstant.IDENTIFIER, "SMA");

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getShortSMAType());
            buySignalCacheDao.item(request, response);
            SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getLongSMAType());
            buySignalCacheDao.item(request, response);
            SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0)
                globalGoldenCross.setBuyIndicator(true);
            else
                globalGoldenCross.setBuyIndicator(false);

            Map<String, GoldenCross> clone = buySignalCache.getGoldenCrossMap();
            clone.put("GLOBAL", globalGoldenCross);
            buySignalCache.setGoldenCrossMap(clone);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkTrades() {
        try {
            System.out.println("Running trade analysis job");
            List<Trade> trades = tradeDao.getAutomatedTrades("Yes");

            if (trades != null && trades.size() > 0) {
                for (Trade trade : trades) {
                    System.out.println("Checking trade name:" + trade.getName());
                    currentTest(trade);
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

    public void currentTest(Trade trade) {
        String algorithm = trade.getAlgorithm();
        String alg1 = algorithm;
        String operand = "";
        String alg2 = "";
        if (algorithm.contains(" ")) {
            alg1 = algorithm.substring(0, algorithm.indexOf(" "));
            operand = algorithm.substring(algorithm.indexOf(" ") + 1,
                    algorithm.indexOf((" "), algorithm.indexOf(" ") + 1));
            alg2 = algorithm.substring(algorithm.indexOf((" "), algorithm.indexOf(" ") + 1) + 1,
                    algorithm.length());
        }
        if (evaluate(currentBuySignals.process(alg1),
                currentBuySignals.process(alg2),
                operand)) {

            try {
                alpacaAPI.orders().requestNotionalMarketOrder(trade.getStock(), trade.getBuyAmount().doubleValue(),
                        OrderSide.BUY);
                System.out.print("Trade Executed!");

            } catch (Exception e) {
                System.out.println("Not Executed!");
                System.out.println(e.getMessage());
            }
        } else
            System.out.println("Buy Condition not met");
    }
}
