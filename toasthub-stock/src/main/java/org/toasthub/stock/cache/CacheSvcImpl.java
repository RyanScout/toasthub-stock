package org.toasthub.stock.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.stock.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.TradeSignalCache;
import org.toasthub.stock.model.cache.UpperBollingerBand;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Service("CacheSvc")
public class CacheSvcImpl implements CacheSvc {

    @Autowired
    protected CacheDao cacheDao;

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    @Autowired
    protected TradeSignalCache tradeSignalCache;

    @Override
    public void process(Request request, Response response) {
        String action = (String) request.getParams().get("action");
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
            case "LIST_GENERALS":
                listGenerals(request, response);
            case "CREATE_GLOBALS":
                createGlobals(request, response);
                break;
        }

    }

    @Override
    public void save(Request request, Response response) {
        Collection<String> symbols = new ArrayList<String>();

        for (Object o : ArrayList.class.cast(request.getParam("SYMBOLS"))) {
            symbols.add(String.class.cast(o));
        }

        symbols.stream()
                .distinct()
                .filter(symbol -> Arrays.asList(Symbol.SYMBOLS).contains(symbol))
                .forEach(symbol -> {
                    request.addParam(GlobalConstant.SYMBOL, symbol);
                    try {
                        cacheDao.itemCount(request, response);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) == 1) {
                        return;
                    }

                    TechnicalIndicator temp = new TechnicalIndicator();

                    temp.setEvaluationPeriod((String) request.getParam("EVALUATION_PERIOD"));
                    temp.setTechnicalIndicatorType((String) request.getParam("TECHNICAL_INDICATOR_TYPE"));
                    temp.setTechnicalIndicatorKey((String) request.getParam("TECHNICAL_INDICATOR_KEY"));

                    temp.setSymbol(symbol);

                    if (request.getParam("SHORT_SMA_TYPE") != null) {
                        temp.setShortSMAType((String) request.getParam("SHORT_SMA_TYPE"));
                    }

                    if (request.getParam("LONG_SMA_TYPE") != null) {
                        temp.setLongSMAType((String) request.getParam("LONG_SMA_TYPE"));
                    }

                    request.addParam(GlobalConstant.ITEM, temp);

                    try {
                        cacheDao.save(request, response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        response.setStatus(Response.SUCCESS);

    }

    @Override
    public void delete(Request request, Response response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void item(Request request, Response response) {
        if (!request.containsParam("TRADE_SIGNAL")) {
            System.out.println("INVALID REQUEST");
            response.setStatus(Response.ACTIONFAILED);
            return;
        }
        switch ((String) request.getParam("TRADE_SIGNAL")) {
            case "GOLDEN_CROSS_DAY":
                response.addParam("GOLDEN_CROSS_DAY",
                        tradeSignalCache.getGoldenCrossMap().get("GLOBAL::DAY::" + request.getParam("SYMBOL")));
                break;
            case "GOLDEN_CROSS_MINUTE":
                response.addParam("GOLDEN_CROSS_MINUTE",
                        tradeSignalCache.getGoldenCrossMap().get("GLOBAL::MINUTE::" + request.getParam("SYMBOL")));
                break;
            case "LOWER_BOLLINGER_BAND_DAY":
                response.addParam("LOWER_BOLLINGER_BAND_DAY",
                        tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::DAY::" + request.getParam("SYMBOL")));
                break;
            case "LOWER_BOLLINGER_BAND_MINUTE":
                response.addParam("LOWER_BOLLINGER_BAND_MINUTE",
                        tradeSignalCache.getLowerBollingerBandMap()
                                .get("GLOBAL::MINUTE::" + request.getParam("SYMBOL")));
                break;
            case "UPPER_BOLLINGER_BAND_DAY":
                response.addParam("UPPER_BOLLINGER_BAND_DAY",
                        tradeSignalCache.getUpperBollingerBandMap()
                                .get("GLOBAL::DAY::" + request.getParam("SYMBOL")));
                break;
            case "UPPER_BOLLINGER_BAND_MINUTE":
                response.addParam("UPPER_BOLLINGER_BAND_MINUTE",
                        tradeSignalCache.getUpperBollingerBandMap()
                                .get("GLOBAL::MINUTE::" + request.getParam("SYMBOL")));
                break;
            default:
                System.out.println("INVALID REQUEST");
                response.setStatus(Response.ACTIONFAILED);
                break;
        }
    }

    @Override
    public void items(Request request, Response response) {
        try {
            customTechnicalIndicatorDao.items(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<CustomTechnicalIndicator> keys = new ArrayList<CustomTechnicalIndicator>();

        for (Object o : ArrayList.class.cast(response.getParam(GlobalConstant.ITEMS))) {
            keys.add(CustomTechnicalIndicator.class.cast(o));
        }

        keys.stream().forEach(item -> {
            item.getSymbols().stream()
                    .map(symbol -> symbol.getSymbol())
                    .forEach(symbol -> {
                        item.getTechnicalIndicators()
                                .add(tradeSignalCache.getTechnicalIndicatorMap()
                                        .get(item.getTechnicalIndicatorType() + "::" + item.getTechnicalIndicatorKey()
                                                + "::"
                                                + item.getEvaluationPeriod() + "::" + symbol));
                    });
        });

        response.addParam(GlobalConstant.ITEMS, keys);
    }

    public void listGenerals(Request request, Response response) {
        response.addParam("GOLDEN_CROSS_DAY", tradeSignalCache.getGoldenCrossMap().get("GLOBAL::DAY::GENERAL"));
        response.addParam("LOWER_BOLLINGER_BAND_DAY",
                tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::DAY::GENERAL"));
        response.addParam("UPPER_BOLLINGER_BAND_DAY",
                tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::DAY::GENERAL"));
        response.addParam("GOLDEN_CROSS_MINUTE", tradeSignalCache.getGoldenCrossMap().get("GLOBAL::MINUTE::GENERAL"));
        response.addParam("LOWER_BOLLINGER_BAND_MINUTE",
                tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::MINUTE::GENERAL"));
        response.addParam("UPPER_BOLLINGER_BAND_MINUTE",
                tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::MINUTE::GENERAL"));
    }

    @SuppressWarnings("unchecked")
    public void createGlobals(Request request, Response response) {
        try {
            Map<String, Object> m = new HashMap<String, Object>();

            if (request.containsParam(GlobalConstant.ITEM)) {
                m = (Map<String, Object>) request.getParam(GlobalConstant.ITEM);
            }
            request.setParams(m);
            request.addParam(GlobalConstant.IDENTIFIER, m.get("identifier"));
            request.addParam("EVAL_PERIOD", m.get("evalPeriod"));

            List<Object> tradeSignals = new ArrayList<Object>();

            request.addParam(GlobalConstant.IDENTIFIER, "GoldenCross");

            request.addParam("SHORT_SMA_TYPE", GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
            request.addParam("LONG_SMA_TYPE", GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);

            GoldenCross goldenCrossDay = new GoldenCross();
            goldenCrossDay.setEvalPeriod("DAY");
            goldenCrossDay.setTradeSignalKey("GLOBAL");
            goldenCrossDay.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_DAY);
            goldenCrossDay.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_DAY);
            request.addParam(GlobalConstant.ITEM, goldenCrossDay);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    GoldenCross tempGoldenCross = new GoldenCross();
                    tempGoldenCross.setTradeSignalKey(goldenCrossDay.getTradeSignalKey());
                    tempGoldenCross.setShortSMAType(goldenCrossDay.getShortSMAType());
                    tempGoldenCross.setLongSMAType(goldenCrossDay.getLongSMAType());
                    tempGoldenCross.setEvalPeriod(goldenCrossDay.getEvalPeriod());
                    tempGoldenCross.setSymbol(symbol);
                    tradeSignals.add(tempGoldenCross);
                }
            }

            request.addParam("SHORT_SMA_TYPE", GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
            request.addParam("LONG_SMA_TYPE", GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);

            GoldenCross goldenCrossMinute = new GoldenCross();
            goldenCrossMinute.setEvalPeriod("MINUTE");
            goldenCrossMinute.setTradeSignalKey("GLOBAL");
            goldenCrossMinute.setShortSMAType(GoldenCross.DEFAULT_SHORT_SMA_TYPE_MINUTE);
            goldenCrossMinute.setLongSMAType(GoldenCross.DEFAULT_LONG_SMA_TYPE_MINUTE);
            request.addParam(GlobalConstant.ITEM, goldenCrossMinute);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    GoldenCross tempGoldenCross = new GoldenCross();
                    tempGoldenCross.setTradeSignalKey(goldenCrossMinute.getTradeSignalKey());
                    tempGoldenCross.setShortSMAType(goldenCrossMinute.getShortSMAType());
                    tempGoldenCross.setLongSMAType(goldenCrossMinute.getLongSMAType());
                    tempGoldenCross.setEvalPeriod(goldenCrossMinute.getEvalPeriod());
                    tempGoldenCross.setSymbol(symbol);
                    tradeSignals.add(tempGoldenCross);
                }
            }

            request.addParam(GlobalConstant.IDENTIFIER, "LowerBollingerBand");
            request.addParam("STANDARD_DEVIATION_VALUE", LowerBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

            request.addParam("LBB_TYPE", LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);

            LowerBollingerBand lowerBollingerbandDay = new LowerBollingerBand();
            lowerBollingerbandDay.setEvalPeriod("DAY");
            lowerBollingerbandDay.setTradeSignalKey("GLOBAL");
            lowerBollingerbandDay.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_DAY);
            lowerBollingerbandDay.setStandardDeviations(LowerBollingerBand.DEFAULT_STANDARD_DEVIATIONS);
            request.addParam(GlobalConstant.ITEM, lowerBollingerbandDay);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    LowerBollingerBand tempLowerBollingerBand = new LowerBollingerBand();
                    tempLowerBollingerBand.setTradeSignalKey(lowerBollingerbandDay.getTradeSignalKey());
                    tempLowerBollingerBand.setLBBType(lowerBollingerbandDay.getLBBType());
                    tempLowerBollingerBand.setStandardDeviations(lowerBollingerbandDay.getStandardDeviations());
                    tempLowerBollingerBand.setEvalPeriod(lowerBollingerbandDay.getEvalPeriod());
                    tempLowerBollingerBand.setSymbol(symbol);
                    tradeSignals.add(tempLowerBollingerBand);
                }
            }

            request.addParam("LBB_TYPE", LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);

            LowerBollingerBand lowerBollingerbandMinute = new LowerBollingerBand();
            lowerBollingerbandMinute.setEvalPeriod("MINUTE");
            lowerBollingerbandMinute.setTradeSignalKey("GLOBAL");
            lowerBollingerbandMinute.setLBBType(LowerBollingerBand.DEFAULT_LBB_TYPE_MINUTE);
            lowerBollingerbandMinute.setStandardDeviations(LowerBollingerBand.DEFAULT_STANDARD_DEVIATIONS);
            request.addParam(GlobalConstant.ITEM, lowerBollingerbandMinute);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    LowerBollingerBand tempLowerBollingerBand = new LowerBollingerBand();
                    tempLowerBollingerBand.setTradeSignalKey(lowerBollingerbandMinute.getTradeSignalKey());
                    tempLowerBollingerBand.setLBBType(lowerBollingerbandMinute.getLBBType());
                    tempLowerBollingerBand.setStandardDeviations(lowerBollingerbandMinute.getStandardDeviations());
                    tempLowerBollingerBand.setEvalPeriod(lowerBollingerbandMinute.getEvalPeriod());
                    tempLowerBollingerBand.setSymbol(symbol);
                    tradeSignals.add(tempLowerBollingerBand);
                }
            }

            request.addParam(GlobalConstant.IDENTIFIER, "UpperBollingerBand");
            request.addParam("STANDARD_DEVIATION_VALUE", UpperBollingerBand.DEFAULT_STANDARD_DEVIATIONS);

            request.addParam("UBB_TYPE", UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);

            UpperBollingerBand upperBollingerbandDay = new UpperBollingerBand();
            upperBollingerbandDay.setEvalPeriod("DAY");
            upperBollingerbandDay.setTradeSignalKey("GLOBAL");
            upperBollingerbandDay.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_DAY);
            upperBollingerbandDay.setStandardDeviations(UpperBollingerBand.DEFAULT_STANDARD_DEVIATIONS);
            request.addParam(GlobalConstant.ITEM, upperBollingerbandDay);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    UpperBollingerBand tempUpperBollingerBand = new UpperBollingerBand();
                    tempUpperBollingerBand.setTradeSignalKey(upperBollingerbandDay.getTradeSignalKey());
                    tempUpperBollingerBand.setUBBType(upperBollingerbandDay.getUBBType());
                    tempUpperBollingerBand.setStandardDeviations(upperBollingerbandDay.getStandardDeviations());
                    tempUpperBollingerBand.setEvalPeriod(upperBollingerbandDay.getEvalPeriod());
                    tempUpperBollingerBand.setSymbol(symbol);
                    tradeSignals.add(tempUpperBollingerBand);
                }
            }

            request.addParam("UBB_TYPE", UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);

            UpperBollingerBand upperBollingerbandMinute = new UpperBollingerBand();
            upperBollingerbandMinute.setEvalPeriod("MINUTE");
            upperBollingerbandMinute.setTradeSignalKey("GLOBAL");
            upperBollingerbandMinute.setUBBType(UpperBollingerBand.DEFAULT_UBB_TYPE_MINUTE);
            upperBollingerbandMinute.setStandardDeviations(UpperBollingerBand.DEFAULT_STANDARD_DEVIATIONS);
            request.addParam(GlobalConstant.ITEM, upperBollingerbandMinute);
            cacheDao.itemCount(request, response);

            if ((long) response.getParam(GlobalConstant.ITEMCOUNT) == 0) {
                for (String symbol : Symbol.SYMBOLS) {
                    UpperBollingerBand tempUpperBollingerBand = new UpperBollingerBand();
                    tempUpperBollingerBand.setTradeSignalKey(upperBollingerbandMinute.getTradeSignalKey());
                    tempUpperBollingerBand.setUBBType(upperBollingerbandMinute.getUBBType());
                    tempUpperBollingerBand.setStandardDeviations(upperBollingerbandMinute.getStandardDeviations());
                    tempUpperBollingerBand.setEvalPeriod(upperBollingerbandMinute.getEvalPeriod());
                    tempUpperBollingerBand.setSymbol(symbol);
                    tradeSignals.add(tempUpperBollingerBand);
                }
            }

            request.addParam(GlobalConstant.ITEMS, tradeSignals);
            cacheDao.saveAll(request, response);
            response.setStatus(Response.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
