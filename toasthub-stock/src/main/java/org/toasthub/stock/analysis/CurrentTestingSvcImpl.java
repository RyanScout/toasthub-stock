package org.toasthub.stock.analysis;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.SMA;
import org.toasthub.analysis.model.UBB;
import org.toasthub.model.Symbol;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.stock.cache.CacheDao;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.GoldenCrossDetail;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.LowerBollingerBandDetail;
import org.toasthub.stock.model.cache.TradeSignalCache;
import org.toasthub.stock.model.cache.UpperBollingerBand;
import org.toasthub.stock.model.cache.UpperBollingerBandDetail;
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

    final ExpressionParser parser = new SpelExpressionParser();

    final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

    // Constructors
    public CurrentTestingSvcImpl() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void printSystemStats() {
        /* Total number of processors or cores available to the JVM */
        System.out.println("Available processors (cores): " +
                Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        System.out.println("Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently available to the JVM */
        System.out.println("Total memory available to JVM (bytes): " +
                Runtime.getRuntime().totalMemory());

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            System.out.println("File system root: " + root.getAbsolutePath());
            System.out.println("Total space (bytes): " + root.getTotalSpace());
            System.out.println("Free space (bytes): " + root.getFreeSpace());
            System.out.println("Usable space (bytes): " + root.getUsableSpace());
        }
    }

    @Scheduled(cron = "30 * * * * ?")
    public void tradeAnalysisTask() {

        // printSystemStats();

        if (tradeAnalysisJobRunning.get()) {
            System.out.println("Trade analysis is currently running ,  skipping this time");
            return;

        } else {
            new Thread(() -> {
                tradeAnalysisJobRunning.set(true);
                Request request = new Request();
                Response response = new Response();
                request.addParam("DAY_CACHE_UPDATED", false);
                request.addParam("MINUTE_CACHE_UPDATED", false);
                request.addParam("DAY_CACHE_CHECKED", false);
                request.addParam("MINUTE_CACHE_CHECKED", false);
                updateTradeSignalCache(request, response);
                updateTrades(request, response);
                if ((boolean) request.getParam("DAY_CACHE_UPDATED")) {
                    updateGeneralGlobalsDay(request, response);
                    if ((boolean) request.getParam("DAY_CACHE_CHECKED"))
                        checkDayTrades(request, response);
                }
                if ((boolean) request.getParam("MINUTE_CACHE_UPDATED")) {
                    updateGeneralGlobalsMinute(request, response);
                    if ((boolean) request.getParam("MINUTE_CACHE_CHECKED"))
                        checkMinuteTrades(request, response);
                }
                tradeAnalysisJobRunning.set(false);
            }).start();
        }
    }

    public void updateTradeSignalCache(Request request, Response response) {
        try {
            for (String symbol : Symbol.SYMBOLS) {
                request.addParam(GlobalConstant.SYMBOL, symbol);

                AssetDay recentAsesetDay = null;

                try{
                currentTestingDao.getRecentAssetDay(request, response);
                recentAsesetDay = (AssetDay) response.getParam(GlobalConstant.ITEM);
                }catch(NoResultException e){
                }

                boolean cacheIsInitialized = (tradeSignalCache.getRecentEpochSecondsMap()
                        .get("DAY::" + symbol) != null);

                if (recentAsesetDay != null && (!cacheIsInitialized || tradeSignalCache.getRecentEpochSecondsMap()
                        .get("DAY::" + symbol) < recentAsesetDay.getEpochSeconds())) {
                    tradeSignalCache.getRecentClosingPriceMap().put("DAY::" + symbol, recentAsesetDay.getClose());
                    tradeSignalCache.getRecentEpochSecondsMap().put("DAY::" + symbol, recentAsesetDay.getEpochSeconds());
                    tradeSignalCache.getRecentVolumeMap().put("DAY::" + symbol, recentAsesetDay.getVolume());
                    tradeSignalCache.getRecentVwapMap().put("DAY::" + symbol, recentAsesetDay.getVwap());
                    request.addParam("EVAL_PERIOD", "DAY");
                    updateGoldenCrossCache(request, response);
                    updateLowerBollingerBandCache(request, response);
                    updateUpperBollingerBandCache(request, response);
                    request.addParam("DAY_CACHE_UPDATED", true);
                }

                currentTestingDao.getRecentAssetMinute(request, response);

                AssetMinute recentAsesetMinute = null;

                try{
                currentTestingDao.getRecentAssetMinute(request, response);
                recentAsesetMinute = (AssetMinute) response.getParam(GlobalConstant.ITEM);
                }catch(NoResultException e){
                }

                cacheIsInitialized = (tradeSignalCache.getRecentEpochSecondsMap().get("MINUTE::" + symbol) != null);

                if (recentAssetMinute != null && (!cacheIsInitialized || tradeSignalCache.getRecentEpochSecondsMap()
                        .get("MINUTE::" + symbol) < recentAssetMinute.getEpochSeconds())) {
                    tradeSignalCache.getRecentClosingPriceMap().put("MINUTE::" + symbol, recentAssetMinute.getValue());
                    tradeSignalCache.getRecentEpochSecondsMap().put("MINUTE::" + symbol,
                            recentAssetMinute.getEpochSeconds());
                    tradeSignalCache.getRecentVolumeMap().put("MINUTE::" + symbol, recentAssetMinute.getVolume());
                    tradeSignalCache.getRecentVwapMap().put("MINUTE::" + symbol, recentAssetMinute.getVwap());
                    request.addParam("EVAL_PERIOD", "MINUTE");
                    updateGoldenCrossCache(request, response);
                    updateLowerBollingerBandCache(request, response);
                    updateUpperBollingerBandCache(request, response);
                    request.addParam("MINUTE_CACHE_UPDATED", true);
                }
            }
        } catch (Exception e) {
            System.out.println("Error at Update Trade Signal Cache");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateGoldenCrossCache(Request request, Response response) {
        try {
            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
            String evalPeriod = (String) request.getParam("EVAL_PERIOD");

            request.addParam(GlobalConstant.IDENTIFIER, "GoldenCross");
            cacheDao.items(request, response);
            List<GoldenCross> goldenCrosses = (List<GoldenCross>) response.getParam(GlobalConstant.ITEMS);

            for (GoldenCross goldenCross : goldenCrosses) {

                if (goldenCross.getFirstCheck() == 0) {
                    goldenCross
                            .setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                }

                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                request.addParam(GlobalConstant.IDENTIFIER, "SMA");

                request.addParam(GlobalConstant.TYPE, goldenCross.getShortSMAType());
                currentTestingDao.itemCount(request, response);
                if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                    return;
                }
                currentTestingDao.item(request, response);
                SMA shortSMA = (SMA) response.getParam(GlobalConstant.ITEM);

                request.addParam(GlobalConstant.TYPE, goldenCross.getLongSMAType());
                currentTestingDao.itemCount(request, response);
                if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                    return;
                }
                currentTestingDao.item(request, response);
                SMA longSMA = (SMA) response.getParam(GlobalConstant.ITEM);

                // if cache is being updated with new data, update goldenCross cache data
                // including, lastflash, times checked ,etc..
                if (goldenCross.getLastCheck() != tradeSignalCache.getRecentEpochSecondsMap()
                        .get(evalPeriod + "::" + symbol)) {

                    // update each goldencross child
                    for (GoldenCrossDetail g : goldenCross.getDetails()) {
                        if (g.getChecked() < 100) {
                            g.setChecked(g.getChecked() + 1);

                            BigDecimal tempSuccessPercent = (tradeSignalCache.getRecentClosingPriceMap()
                                    .get(evalPeriod + "::" + symbol).subtract(g.getFlashPrice()))
                                    .divide(g.getFlashPrice(), MathContext.DECIMAL32).multiply(BigDecimal.valueOf(100));

                            if (g.getSuccessPercent() == null
                                    || g.getSuccessPercent().compareTo(tempSuccessPercent) < 0) {
                                g.setSuccessPercent(tempSuccessPercent);
                            }

                            if (g.isSuccess() == false && g.getFlashPrice().compareTo(
                                    tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol)) < 0) {
                                g.setSuccess(true);
                                goldenCross.setSuccesses(goldenCross.getSuccesses() + 1);
                            }
                        }
                    }

                    if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0) {
                        goldenCross.setFlashing(true);
                        goldenCross.setLastFlash(
                                tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                        goldenCross.setFlashed(goldenCross.getFlashed() + 1);

                        GoldenCrossDetail goldenCrossDetail = new GoldenCrossDetail();
                        goldenCrossDetail.setGoldenCross(goldenCross);
                        goldenCrossDetail.setFlashTime(
                                tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                        goldenCrossDetail.setFlashPrice(
                                tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol));
                        goldenCrossDetail
                                .setVolume(tradeSignalCache.getRecentVolumeMap().get(evalPeriod + "::" + symbol));
                        goldenCrossDetail.setVwap(tradeSignalCache.getRecentVwapMap().get(evalPeriod + "::" + symbol));
                        goldenCross.getDetails().add(goldenCrossDetail);

                    } else
                        goldenCross.setFlashing(false);

                    goldenCross.setChecked(goldenCross.getChecked() + 1);

                    goldenCross
                            .setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                    request.addParam(evalPeriod + "_CACHE_CHECKED", true);
                }

                tradeSignalCache.getGoldenCrossMap()
                        .put(goldenCross.getTradeSignalKey() + "::" + evalPeriod + "::" + symbol, goldenCross);

                request.addParam(GlobalConstant.ITEM, goldenCross);
                cacheDao.save(request, response);
            }
        } catch (Exception e) {
            System.out.println("Error at Golden Cross Cache");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateLowerBollingerBandCache(Request request, Response response) {
        try {
            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
            String evalPeriod = (String) request.getParam("EVAL_PERIOD");

            request.addParam(GlobalConstant.IDENTIFIER, "LowerBollingerBand");
            cacheDao.items(request, response);
            List<LowerBollingerBand> lowerBollingerBands = (List<LowerBollingerBand>) response
                    .getParam(GlobalConstant.ITEMS);

            for (LowerBollingerBand lowerBollingerBand : lowerBollingerBands) {

                if (lowerBollingerBand.getFirstCheck() == 0) {
                    lowerBollingerBand
                            .setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                }

                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                request.addParam(GlobalConstant.IDENTIFIER, "LBB");

                request.addParam(GlobalConstant.TYPE, lowerBollingerBand.getLBBType());
                request.addParam("STANDARD_DEVIATIONS", lowerBollingerBand.getStandardDeviations());
                currentTestingDao.itemCount(request, response);
                if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                    return;
                }
                currentTestingDao.item(request, response);
                LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

                // if cache is being updated with new data, update goldenCross cache data
                // including, lastflash, times checked ,etc..
                if (lowerBollingerBand.getLastCheck() != tradeSignalCache.getRecentEpochSecondsMap()
                        .get(evalPeriod + "::" + symbol)) {

                    for (LowerBollingerBandDetail l : lowerBollingerBand.getDetails()) {
                        if (l.getChecked() < 100) {
                            l.setChecked(l.getChecked() + 1);

                            BigDecimal tempSuccessPercent = (tradeSignalCache.getRecentClosingPriceMap()
                                    .get(evalPeriod + "::" + symbol).subtract(l.getFlashPrice()))
                                    .divide(l.getFlashPrice(), MathContext.DECIMAL32).multiply(BigDecimal.valueOf(100));

                            if (l.getSuccessPercent() == null
                                    || l.getSuccessPercent().compareTo(tempSuccessPercent) < 0) {
                                l.setSuccessPercent(tempSuccessPercent);
                            }

                            if (l.isSuccess() == false && l.getFlashPrice().compareTo(
                                    tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol)) < 0) {
                                l.setSuccess(true);
                                lowerBollingerBand.setSuccesses(lowerBollingerBand.getSuccesses() + 1);
                            }
                        }
                    }
                    if (tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol)
                            .compareTo(lbb.getValue()) < 0) {
                        lowerBollingerBand.setFlashing(true);
                        lowerBollingerBand.setFlashed(lowerBollingerBand.getFlashed() + 1);
                        lowerBollingerBand
                                .setLastFlash(
                                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                        LowerBollingerBandDetail lowerBollingerBandDetail = new LowerBollingerBandDetail();
                        lowerBollingerBandDetail.setLowerBollingerBand(lowerBollingerBand);
                        lowerBollingerBandDetail
                                .setFlashTime(
                                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                        lowerBollingerBandDetail
                                .setFlashPrice(
                                        tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol));
                        lowerBollingerBandDetail
                                .setVolume(tradeSignalCache.getRecentVolumeMap().get(evalPeriod + "::" + symbol));
                        lowerBollingerBandDetail
                                .setVwap(tradeSignalCache.getRecentVwapMap().get(evalPeriod + "::" + symbol));
                        lowerBollingerBand.getDetails().add(lowerBollingerBandDetail);
                    } else
                        lowerBollingerBand.setFlashing(false);

                    lowerBollingerBand.setChecked(lowerBollingerBand.getChecked() + 1);
                    lowerBollingerBand
                            .setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                    request.addParam(evalPeriod + "_CACHE_CHECKED", true);
                }
                tradeSignalCache.getLowerBollingerBandMap().put(
                        lowerBollingerBand.getTradeSignalKey() + "::" + evalPeriod + "::" + symbol, lowerBollingerBand);

                request.addParam(GlobalConstant.ITEM, lowerBollingerBand);
                cacheDao.save(request, response);
            }
        } catch (Exception e) {
            System.out.println("Error at Lower Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateUpperBollingerBandCache(Request request, Response response) {
        try {
            String symbol = (String) request.getParam(GlobalConstant.SYMBOL);
            String evalPeriod = (String) request.getParam("EVAL_PERIOD");

            request.addParam(GlobalConstant.IDENTIFIER, "UpperBollingerBand");
            cacheDao.items(request, response);
            List<UpperBollingerBand> upperBollingerBands = (List<UpperBollingerBand>) response
                    .getParam(GlobalConstant.ITEMS);

            for (UpperBollingerBand upperBollingerBand : upperBollingerBands) {

                if (upperBollingerBand.getFirstCheck() == 0) {
                    upperBollingerBand
                            .setFirstCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                }

                request.addParam(GlobalConstant.EPOCHSECONDS,
                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                request.addParam(GlobalConstant.IDENTIFIER, "UBB");

                request.addParam(GlobalConstant.TYPE, upperBollingerBand.getUBBType());
                request.addParam("STANDARD_DEVIATIONS", upperBollingerBand.getStandardDeviations());
                currentTestingDao.itemCount(request, response);
                if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                    return;
                }
                currentTestingDao.item(request, response);
                UBB ubb = (UBB) response.getParam(GlobalConstant.ITEM);

                // if cache is being updated with new data, update goldenCross cache data
                // including, lastflash, times checked ,etc..
                if (upperBollingerBand.getLastCheck() != tradeSignalCache.getRecentEpochSecondsMap()
                        .get(evalPeriod + "::" + symbol)) {

                    for (UpperBollingerBandDetail u : upperBollingerBand.getDetails()) {
                        if (u.getChecked() < 100) {
                            u.setChecked(u.getChecked() + 1);

                            BigDecimal tempSuccessPercent = (tradeSignalCache.getRecentClosingPriceMap()
                                    .get(evalPeriod + "::" + symbol).subtract(u.getFlashPrice()))
                                    .divide(u.getFlashPrice(), MathContext.DECIMAL32).multiply(BigDecimal.valueOf(100))
                                    .negate();

                            if (u.getSuccessPercent() == null
                                    || u.getSuccessPercent().compareTo(tempSuccessPercent) < 0) {
                                u.setSuccessPercent(tempSuccessPercent);
                            }

                            if (u.isSuccess() == false && u.getFlashPrice().compareTo(
                                    tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol)) > 0) {
                                u.setSuccess(true);
                                upperBollingerBand.setSuccesses(upperBollingerBand.getSuccesses() + 1);
                            }
                        }
                    }

                    if (tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol)
                            .compareTo(ubb.getValue()) > 0) {
                        upperBollingerBand.setFlashing(true);
                        upperBollingerBand.setFlashed(upperBollingerBand.getFlashed() + 1);
                        upperBollingerBand.setLastFlash(
                                tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                        UpperBollingerBandDetail upperBollingerBandDetail = new UpperBollingerBandDetail();
                        upperBollingerBandDetail.setUpperBollingerBand(upperBollingerBand);
                        upperBollingerBandDetail
                                .setFlashTime(
                                        tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));
                        upperBollingerBandDetail
                                .setFlashPrice(
                                        tradeSignalCache.getRecentClosingPriceMap().get(evalPeriod + "::" + symbol));
                        upperBollingerBandDetail
                                .setVolume(tradeSignalCache.getRecentVolumeMap().get(evalPeriod + "::" + symbol));
                        upperBollingerBandDetail
                                .setVwap(tradeSignalCache.getRecentVwapMap().get(evalPeriod + "::" + symbol));
                        upperBollingerBand.getDetails().add(upperBollingerBandDetail);
                    } else
                        upperBollingerBand.setFlashing(false);

                    upperBollingerBand.setChecked(upperBollingerBand.getChecked() + 1);
                    upperBollingerBand
                            .setLastCheck(tradeSignalCache.getRecentEpochSecondsMap().get(evalPeriod + "::" + symbol));

                    request.addParam(evalPeriod + "_CACHE_CHECKED", true);
                }
                tradeSignalCache.getUpperBollingerBandMap().put(
                        upperBollingerBand.getTradeSignalKey() + "::" + evalPeriod + "::" + symbol, upperBollingerBand);

                request.addParam(GlobalConstant.ITEM, upperBollingerBand);
                cacheDao.save(request, response);
            }
        } catch (Exception e) {
            System.out.println("Error at Upper Bollinger Band Cache");
            e.printStackTrace();
        }
    }

    public void updateGeneralGlobalsDay(Request request, Response response) {
        GoldenCross generalGoldenCross = new GoldenCross();
        generalGoldenCross.setSymbol("GENERAL");
        generalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
        generalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);

        LowerBollingerBand generalLowerBollingerBand = new LowerBollingerBand();
        generalLowerBollingerBand.setSymbol("GENERAL");
        generalLowerBollingerBand.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);
        generalLowerBollingerBand.setStandardDeviations(LowerBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

        UpperBollingerBand generalUpperBollingerBand = new UpperBollingerBand();
        generalUpperBollingerBand.setSymbol("GENERAL");
        generalUpperBollingerBand.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);
        generalUpperBollingerBand.setStandardDeviations(UpperBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

        GoldenCross tempGoldenCross;
        LowerBollingerBand tempLowerBollingerBand;
        UpperBollingerBand tempUpperBollingerBand;

        for (String symbol : Symbol.SYMBOLS) {

            if (tradeSignalCache.getGoldenCrossMap().get("GLOBAL::DAY::" + symbol) != null) {
                tempGoldenCross = tradeSignalCache.getGoldenCrossMap().get("GLOBAL::DAY::" + symbol);
                generalGoldenCross.setChecked(generalGoldenCross.getChecked() + tempGoldenCross.getChecked());
                generalGoldenCross.setFlashed(generalGoldenCross.getFlashed() + tempGoldenCross.getFlashed());
                generalGoldenCross.setSuccesses(generalGoldenCross.getSuccesses() + tempGoldenCross.getSuccesses());
            }

            if (tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::DAY::" + symbol) != null) {
                tempLowerBollingerBand = tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::DAY::" + symbol);
                generalLowerBollingerBand
                        .setChecked(generalLowerBollingerBand.getChecked() + tempLowerBollingerBand.getChecked());
                generalLowerBollingerBand
                        .setFlashed(generalLowerBollingerBand.getFlashed() + tempLowerBollingerBand.getFlashed());
                generalLowerBollingerBand
                        .setSuccesses(generalLowerBollingerBand.getSuccesses() + tempLowerBollingerBand.getSuccesses());
            }

            if (tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::DAY::" + symbol) != null) {
                tempUpperBollingerBand = tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::DAY::" + symbol);
                generalUpperBollingerBand
                        .setChecked(generalUpperBollingerBand.getChecked() + tempUpperBollingerBand.getChecked());
                generalUpperBollingerBand
                        .setFlashed(generalUpperBollingerBand.getFlashed() + tempUpperBollingerBand.getFlashed());
                generalUpperBollingerBand
                        .setSuccesses(generalUpperBollingerBand.getSuccesses() + tempUpperBollingerBand.getSuccesses());
            }
        }

        tradeSignalCache.getGoldenCrossMap().put("GLOBAL::DAY::GENERAL", generalGoldenCross);
        tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::DAY::GENERAL", generalLowerBollingerBand);
        tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::DAY::GENERAL", generalUpperBollingerBand);
    }

    public void createGeneralTradeSignals(Request request, Response response) {
        request.addParam(GlobalConstant.IDENTIFIER, "GoldenCross");
        try {
            currentTestingDao.items(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<GoldenCross> goldenCrosses = new ArrayList<GoldenCross>();
        GoldenCross generalGoldenCross = new GoldenCross();
        for (GoldenCross goldenCross : goldenCrosses) {
            generalGoldenCross.setSymbol("GENERAL");
            generalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
            generalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);
        }
    }

    public void updateGeneralGlobalsMinute(Request request, Response response) {
        GoldenCross generalGoldenCross = new GoldenCross();
        generalGoldenCross.setSymbol("GENERAL");
        generalGoldenCross.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
        generalGoldenCross.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);

        LowerBollingerBand generalLowerBollingerBand = new LowerBollingerBand();
        generalLowerBollingerBand.setSymbol("GENERAL");
        generalLowerBollingerBand.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);
        generalLowerBollingerBand.setStandardDeviations(LowerBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

        UpperBollingerBand generalUpperBollingerBand = new UpperBollingerBand();
        generalUpperBollingerBand.setSymbol("GENERAL");
        generalUpperBollingerBand.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);
        generalUpperBollingerBand.setStandardDeviations(UpperBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

        GoldenCross tempGoldenCross;
        LowerBollingerBand tempLowerBollingerBand;
        UpperBollingerBand tempUpperBollingerBand;

        for (String symbol : Symbol.SYMBOLS) {
            if (tradeSignalCache.getGoldenCrossMap().get("GLOBAL::MINUTE::" + symbol) != null) {
                tempGoldenCross = tradeSignalCache.getGoldenCrossMap().get("GLOBAL::MINUTE::" + symbol);
                generalGoldenCross.setChecked(generalGoldenCross.getChecked() + tempGoldenCross.getChecked());
                generalGoldenCross.setFlashed(generalGoldenCross.getFlashed() + tempGoldenCross.getFlashed());
                generalGoldenCross.setSuccesses(generalGoldenCross.getSuccesses() + tempGoldenCross.getSuccesses());
            }

            if (tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::MINUTE::" + symbol) != null) {
                tempLowerBollingerBand = tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::MINUTE::" + symbol);
                generalLowerBollingerBand
                        .setChecked(generalLowerBollingerBand.getChecked() + tempLowerBollingerBand.getChecked());
                generalLowerBollingerBand
                        .setFlashed(generalLowerBollingerBand.getFlashed() + tempLowerBollingerBand.getFlashed());
                generalLowerBollingerBand
                        .setSuccesses(generalLowerBollingerBand.getSuccesses() + tempLowerBollingerBand.getSuccesses());
            }
            if (tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::MINUTE::" + symbol) != null) {
                tempUpperBollingerBand = tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::MINUTE::" + symbol);
                generalUpperBollingerBand
                        .setChecked(generalUpperBollingerBand.getChecked() + tempUpperBollingerBand.getChecked());
                generalUpperBollingerBand
                        .setFlashed(generalUpperBollingerBand.getFlashed() + tempUpperBollingerBand.getFlashed());
                generalUpperBollingerBand
                        .setSuccesses(generalUpperBollingerBand.getSuccesses() + tempUpperBollingerBand.getSuccesses());
            }
        }

        tradeSignalCache.getGoldenCrossMap().put("GLOBAL::MINUTE::GENERAL", generalGoldenCross);
        tradeSignalCache.getLowerBollingerBandMap().put("GLOBAL::MINUTE::GENERAL", generalLowerBollingerBand);
        tradeSignalCache.getUpperBollingerBandMap().put("GLOBAL::MINUTE::GENERAL", generalUpperBollingerBand);
    }

    public void updateSignalLineCrossGlobal(Request request, Response response) {

    }

    private void updateTrades(Request request, Response response) {
        List<Trade> trades = tradeDao.getAllRunningTrades();
        if (trades != null && trades.size() > 0) {
            for (Trade trade : trades) {
                trade.getTradeDetails().stream()
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
                                            BigDecimal orderQuantity = new BigDecimal(order.getQuantity());
                                            trade.setAvailableBudget(trade.getAvailableBudget()
                                                    .subtract(orderQuantity
                                                            .multiply(new BigDecimal(order.getAverageFillPrice())),
                                                            MathContext.DECIMAL32)
                                                    .setScale(2, BigDecimal.ROUND_HALF_UP));
                                            trade.setSharesHeld(trade.getSharesHeld().add(orderQuantity));
                                            trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
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
            }
        }
    }

    private void checkMinuteTrades(Request request, Response response) {
        try {
            List<Trade> trades = tradeDao.getRunningMinuteTrades();
            if (trades != null && trades.size() > 0) {
                for (Trade trade : trades) {

                    request.addParam(GlobalConstant.TRADE, trade);
                    System.out.println("Checking minute trade: " + trade.getName());
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
                System.out.println("No Minute trades to run");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDayTrades(Request request, Response response) {
        try {
            List<Trade> trades = tradeDao.getRunningDayTrades();
            if (trades != null && trades.size() > 0) {
                for (Trade trade : trades) {

                    request.addParam(GlobalConstant.TRADE, trade);
                    System.out.println("Checking day trade: " + trade.getName());
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
                System.out.println("No Day trades to run");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void currentBuyTest(Request request, Response response) {
        try {
            Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
            if (trade.getIterations().equals("unlimited")
                    || trade.getIterationsExecuted() < Integer.parseInt(trade.getIterations())) {
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

                } else
                    System.out.println(trade.getName() + ":Buy Condition not met");

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
            if (trade.getIterations().equals("unlimited")
                    || trade.getIterationsExecuted() < Integer.parseInt(trade.getIterations())) {
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
                        tradeDetail.setOrderCondition(sellOrderCondition);
                        tradeDetail.setTrade(trade);
                        trade.getTradeDetails().add(tradeDetail);
                    }
                    System.out.println("Sell Order Placed!");

                } else
                    System.out.println(trade.getName() + ":Sell Condition not met");

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
