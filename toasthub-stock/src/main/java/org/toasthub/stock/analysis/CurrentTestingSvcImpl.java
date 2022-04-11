package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.stock.model.cache.CacheDao;
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

    final ExpressionParser parser = new SpelExpressionParser();

    final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

    // Constructors
    public CurrentTestingSvcImpl() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void process(Request request, Response response) {
        String action = (String) request.getParams().get("action");
        switch (action) {
            case "LIST":
                items(request, response);
                break;
            default:
                break;
        }
    }

    public void items(Request request, Response response) {
        response.addParam("SYMBOLS", Symbol.SYMBOLS);

        String[] evaluationPeriod = {
                "GLOBAL::DAY::", "GLOBAL::MINUTE::"
        };

        List<Object> goldenCrosses = new ArrayList<Object>();
        List<Object> lowerBollingerBands = new ArrayList<Object>();
        List<Object> upperBollingerBands = new ArrayList<Object>();

        for (String symbol : Symbol.SYMBOLS) {
            List<Object> tempGC = new ArrayList<Object>();
            List<Object> tempLBB = new ArrayList<Object>();
            List<Object> tempUBB = new ArrayList<Object>();
            for (String temp : evaluationPeriod) {
                tempGC.add(tradeSignalCache.getGoldenCrossMap().get(temp + symbol));
                tempLBB.add(tradeSignalCache.getLowerBollingerBandMap().get(temp + symbol));
                tempUBB.add(tradeSignalCache.getUpperBollingerBandMap().get(temp + symbol));
            }
            goldenCrosses.add(tempGC);
            lowerBollingerBands.add(tempLBB);
            upperBollingerBands.add(tempUBB);
        }

        response.addParam("GOLDENCROSS", goldenCrosses);
        response.addParam("LOWERBOLLINGERBAND", lowerBollingerBands);
        response.addParam("UPPERBOLLINGERBAND", upperBollingerBands);
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
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.SYMBOL, symbol);

                currentTestingDao.getRecentAssetDay(request, response);

                AssetDay recentAssetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol) == null
                        || tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol) < recentAssetDay
                                .getEpochSeconds()) {
                    tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAssetDay.getClose());
                    tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAssetDay.getEpochSeconds());
                    updateGoldenCrossCacheGlobalDay(request, response);
                    updateLowerBollingerBandCacheGlobalDay(request, response);
                    updateUpperBollingerBandCacheGlobalDay(request, response);
                }

                currentTestingDao.getRecentAssetMinute(request, response);

                AssetMinute recentAssetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);

                if (tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol) == null
                        || tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol) < recentAssetMinute
                                .getEpochSeconds()) {
                    tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, recentAssetMinute.getValue());
                    tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                            recentAssetMinute.getEpochSeconds());
                    updateGoldenCrossCacheGlobalMinute(request, response);
                    updateLowerBollingerBandCacheGlobalMinute(request, response);
                    updateUpperBollingerBandCacheGlobalMinute(request, response);
                }
            }
        } catch (Exception e) {
            System.out.println("Error at Recent Assets Cache");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobalDay(Request request, Response response) {
        try {
            GoldenCross globalGoldenCross = new GoldenCross();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "GoldenCross");
            request.addParam("SHORT_SMA_TYPE", GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
            request.addParam("LONG_SMA_TYPE", GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                globalGoldenCross = (GoldenCross) response.getParam(GlobalConstant.ITEM);
            } else {
                globalGoldenCross.setSymbol(symbol);
                globalGoldenCross.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
                globalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
                globalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "SMA");

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getShortSMAType());
            currentTestingDao.item(request, response);
            SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getLongSMAType());
            currentTestingDao.item(request, response);
            SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0) {
                globalGoldenCross.setFlashing(true);
                globalGoldenCross.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
                globalGoldenCross.setFlashed(globalGoldenCross.getFlashed() + 1);
            } else
                globalGoldenCross.setFlashing(false);

            globalGoldenCross.setChecked(globalGoldenCross.getChecked() + 1);
            globalGoldenCross.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            tradeSignalCache.getGoldenCrossMap().put("GLOBAL::DAY::" + symbol, globalGoldenCross);

            request.addParam(GlobalConstant.ITEM, globalGoldenCross);
            cacheDao.save(request, response);

        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache Day");
            e.printStackTrace();
        }
    }

    public void updateGoldenCrossCacheGlobalMinute(Request request, Response response) {
        try {
            GoldenCross globalGoldenCross = new GoldenCross();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "GoldenCross");
            request.addParam("SHORT_SMA_TYPE", GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
            request.addParam("LONG_SMA_TYPE", GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                globalGoldenCross = (GoldenCross) response.getParam(GlobalConstant.ITEM);
            } else {
                globalGoldenCross.setSymbol(symbol);
                globalGoldenCross.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
                globalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
                globalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "SMA");

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getShortSMAType());
            currentTestingDao.item(request, response);
            SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getLongSMAType());
            currentTestingDao.item(request, response);
            SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

            if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0) {
                globalGoldenCross.setFlashing(true);
                globalGoldenCross.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
                globalGoldenCross.setFlashed(globalGoldenCross.getFlashed() + 1);
            } else
                globalGoldenCross.setFlashing(false);

            globalGoldenCross.setChecked(globalGoldenCross.getChecked() + 1);
            globalGoldenCross.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            tradeSignalCache.getGoldenCrossMap().put("GLOBAL::MINUTE::" + symbol, globalGoldenCross);

            request.addParam(GlobalConstant.ITEM, globalGoldenCross);
            cacheDao.save(request, response);

        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache Minute");
            e.printStackTrace();
        }
    }

    public void updateLowerBollingerBandCacheGlobalDay(Request request, Response response) {
        try {
            LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "LowerBollingerBand");
            request.addParam("LBB_TYPE", LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);
            request.addParam("STANDARD_DEVIATION_VALUE", LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                lowerBollingerBand = (LowerBollingerBand) response.getParam(GlobalConstant.ITEM);
            } else {
                lowerBollingerBand.setSymbol(symbol);
                lowerBollingerBand.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
                lowerBollingerBand.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);
                lowerBollingerBand.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "LBB");

            request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
            currentTestingDao.item(request, response);
            LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

            if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::" + symbol).compareTo(lbb.getValue()) < 0) {
                lowerBollingerBand.setFlashing(true);
                lowerBollingerBand.setFlashed(lowerBollingerBand.getFlashed() + 1);
                lowerBollingerBand.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
            } else
                lowerBollingerBand.setFlashing(false);

            lowerBollingerBand.setChecked(lowerBollingerBand.getChecked() + 1);
            lowerBollingerBand.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::DAY::" + symbol, lowerBollingerBand);

            request.addParam(GlobalConstant.ITEM, lowerBollingerBand);
            cacheDao.save(request, response);
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache Day");
            e.printStackTrace();
        }
    }

    public void updateLowerBollingerBandCacheGlobalMinute(Request request, Response response) {
        try {
            LowerBollingerBand lowerBollingerBand = new LowerBollingerBand();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "LowerBollingerBand");
            request.addParam("LBB_TYPE", LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);
            request.addParam("STANDARD_DEVIATION_VALUE", LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                lowerBollingerBand = (LowerBollingerBand) response.getParam(GlobalConstant.ITEM);
            } else {
                lowerBollingerBand.setSymbol(symbol);
                lowerBollingerBand.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
                lowerBollingerBand.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);
                lowerBollingerBand.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "LBB");

            request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
            currentTestingDao.item(request, response);
            LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

            if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol).compareTo(lbb.getValue()) < 0) {
                lowerBollingerBand.setFlashing(true);
                lowerBollingerBand.setFlashed(lowerBollingerBand.getFlashed() + 1);
                lowerBollingerBand.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
            } else
                lowerBollingerBand.setFlashing(false);

            lowerBollingerBand.setChecked(lowerBollingerBand.getChecked() + 1);
            lowerBollingerBand.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::MINUTE::" + symbol, lowerBollingerBand);

            request.addParam(GlobalConstant.ITEM, lowerBollingerBand);
            cacheDao.save(request, response);
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache Minute");
            e.printStackTrace();
        }
    }

    public void updateUpperBollingerBandCacheGlobalDay(Request request, Response response) {
        try {
            UpperBollingerBand upperBollingerBand = new UpperBollingerBand();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "UpperBollingerBand");
            request.addParam("UBB_TYPE", UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);
            request.addParam("STANDARD_DEVIATION_VALUE", UpperBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                upperBollingerBand = (UpperBollingerBand) response.getParam(GlobalConstant.ITEM);
            } else {
                upperBollingerBand.setSymbol(symbol);
                upperBollingerBand.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
                upperBollingerBand.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);
                upperBollingerBand.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "UBB");

            request.addParam(GlobalConstant.TYPE, upperBollingerBand.getUBBType());
            currentTestingDao.item(request, response);
            UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

            if (tradeSignalCache.getRecentClosingPriceMap().get("DAY::" + symbol).compareTo(ubb.getValue()) > 0) {
                upperBollingerBand.setFlashing(true);
                upperBollingerBand.setFlashed(upperBollingerBand.getFlashed() + 1);
                upperBollingerBand.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));
            } else
                upperBollingerBand.setFlashing(false);

            upperBollingerBand.setChecked(upperBollingerBand.getChecked() + 1);
            upperBollingerBand.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("DAY::" + symbol));

            tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::DAY::" + symbol, upperBollingerBand);

            request.addParam(GlobalConstant.ITEM, upperBollingerBand);
            cacheDao.save(request, response);
        } catch (Exception e) {
            System.out.println("Error at Upper Bollinger Band Cache Day");
            e.printStackTrace();
        }
    }

    public void updateUpperBollingerBandCacheGlobalMinute(Request request, Response response) {
        try {
            UpperBollingerBand upperBollingerBand = new UpperBollingerBand();

            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);

            request.addParam(GlobalConstant.IDENTIFIER, "UpperBollingerBand");
            request.addParam("UBB_TYPE", UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);
            request.addParam("STANDARD_DEVIATION_VALUE", UpperBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                cacheDao.item(request, response);
                upperBollingerBand = (UpperBollingerBand) response.getParam(GlobalConstant.ITEM);
            } else {
                upperBollingerBand.setSymbol(symbol);
                upperBollingerBand.setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
                upperBollingerBand.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);
                upperBollingerBand.setStandardDeviationValue(LowerBollingerBand.DEFAULT_STANDARD_DEVIATION_VALUE);
            }

            request.addParam(GlobalConstant.EPOCHSECONDS,
                    tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            request.addParam(GlobalConstant.IDENTIFIER, "UBB");

            request.addParam(GlobalConstant.TYPE, upperBollingerBand.getUBBType());
            currentTestingDao.item(request, response);
            UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

            if (tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol).compareTo(ubb.getValue()) > 0) {
                upperBollingerBand.setFlashing(true);
                upperBollingerBand.setFlashed(upperBollingerBand.getFlashed() + 1);
                upperBollingerBand.setLastFlash(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));
            } else
                upperBollingerBand.setFlashing(false);

            upperBollingerBand.setChecked(upperBollingerBand.getChecked() + 1);
            upperBollingerBand.setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol));

            tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::MINUTE::" + symbol, upperBollingerBand);

            request.addParam(GlobalConstant.ITEM, upperBollingerBand);
            cacheDao.save(request, response);
        } catch (Exception e) {
            System.out.println("Error at Upper Bollinger Band Cache Minute");
            e.printStackTrace();
        }
    }

    public void updateSignalLineCrossGlobal(Request request, Response response) {

    }

    private void updateTrades(Request request, Response response) {
        System.out.println("Updating Trades");
        List<Trade> trades = tradeDao.getAllRunningTrades();
        if (trades != null && trades.size() > 0) {
            for (Trade trade : trades) {
                trade.getTradeDetails().stream()
                        .filter(t -> !t.getStatus().equals("FILLED"))
                        .forEach(t -> {
                            Order order = null;
                            try {
                                order = alpacaAPI.orders().getByClientID(t.getOrderID());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                                if (t.getOrderSide().equals("SELL")) {
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

                            }
                        });
                try {
                    request.addParam(GlobalConstant.ITEM, trade);
                    tradeDao.save(request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                trade.setTotalValue(trade.getAvailableBudget().add(trade.getSharesHeld()
                        .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                .get("MINUTE::" + trade.getSymbol()))));
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

    public void currentBuyTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            if (trade.getFrequency().equals("unlimited")
                    || trade.getFrequencyExecuted() < Integer.parseInt(trade.getFrequency())) {
                String buyCondition = trade.getBuyCondition();
                String sellOrderCondition = "";
                String buyOrderCondition = "";
                String parseableBuyCondition = "";

                if (buyCondition.equals("test")) {
                    parseableBuyCondition = "true";
                    buyOrderCondition = "test";
                } else {
                    int trailingIndex = 0;
                    int leadingIndex = 0;
                    String evalStr = "";
                    boolean tempBool = false;

                    while (leadingIndex < buyCondition.length()) {
                        if (buyCondition.indexOf((" "), trailingIndex + 1) == -1)
                            leadingIndex = buyCondition.length();
                        else
                            leadingIndex = buyCondition.indexOf((" "), trailingIndex + 1);

                        evalStr = buyCondition.substring(trailingIndex, leadingIndex).trim();
                        if (evalStr.equals("and") || evalStr.equals("or"))
                            parseableBuyCondition = parseableBuyCondition.concat(" ".concat(evalStr).trim());
                        else {
                            tempBool = currentOrderSignals.process(evalStr, trade.getSymbol(),
                                    trade.getEvaluationPeriod());

                            if (tempBool) {
                                if (buyOrderCondition.equals(""))
                                    buyOrderCondition = buyOrderCondition.concat(evalStr);
                                else
                                    buyOrderCondition = buyOrderCondition.concat("&".concat(evalStr));

                            }
                            parseableBuyCondition = parseableBuyCondition.concat(" ".concat(String.valueOf(tempBool)))
                                    .trim();
                        }
                        trailingIndex = leadingIndex;
                    }
                }
                if (parser.parseExpression(parseableBuyCondition).getValue(Boolean.class)) {

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

                    System.out.println("Buy Order Placed!");

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
                String sellOrderCondition = "";
                String parseableSellCondition = "";

                if (sellCondition.equals("test")) {
                    parseableSellCondition = "true";
                    sellOrderCondition = "test";
                } else {
                    int trailingIndex = 0;
                    int leadingIndex = 0;
                    String evalStr = "";
                    boolean tempBool = false;

                    while (leadingIndex < sellCondition.length()) {
                        if (sellCondition.indexOf((" "), trailingIndex + 1) == -1)
                            leadingIndex = sellCondition.length();
                        else
                            leadingIndex = sellCondition.indexOf((" "), trailingIndex + 1);

                        evalStr = sellCondition.substring(trailingIndex, leadingIndex).trim();
                        if (evalStr.equals("and") || evalStr.equals("or"))
                            parseableSellCondition = parseableSellCondition.concat(" ".concat(evalStr).trim());
                        else {
                            tempBool = currentOrderSignals.process(evalStr, trade.getSymbol(),
                                    trade.getEvaluationPeriod());

                            if (tempBool) {
                                if (sellOrderCondition.equals(""))
                                    sellOrderCondition = sellOrderCondition.concat(evalStr);
                                else
                                    sellOrderCondition = sellOrderCondition.concat("&".concat(evalStr));

                            }
                            parseableSellCondition = parseableSellCondition.concat(" ".concat(String.valueOf(tempBool)))
                                    .trim();
                        }
                        trailingIndex = leadingIndex;
                    }
                }

                if (parser.parseExpression(parseableSellCondition).getValue(Boolean.class)) {
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
                    request.addParam("SELLORDER", sellOrder);
                    if (trade.getOrderSide().equals("Sell")) {
                        trade.setFrequencyExecuted(trade.getFrequencyExecuted() + 1);

                        if (!trade.getFrequency().equals("unlimited")) {
                            if (trade.getFrequencyExecuted() >= Integer.parseInt(trade.getFrequency()))
                                trade.setStatus("Not Running");
                        }
                    }

                    if (sellOrder != null) {
                        TradeDetail tradeDetail = new TradeDetail();
                        tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                        tradeDetail.setOrderID(sellOrder.getClientOrderId());
                        tradeDetail.setStatus(sellOrder.getStatus().name());
                        tradeDetail.setOrderSide("SELL");
                        tradeDetail.setOrderCondition(sellOrderCondition);
                        tradeDetail.setTrade(trade);
                        trade.getTradeDetails().add(tradeDetail);
                    }
                    System.out.println("Sell Order Placed!");

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
                    && trade.getSharesHeld().multiply(BigDecimal.valueOf(1.05)).compareTo(orderAmount
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
