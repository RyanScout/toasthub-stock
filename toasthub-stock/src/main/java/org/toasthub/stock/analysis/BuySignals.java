package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;

@Service("BuySignals")
public class BuySignals {


    // signals return true if buy signal is present, false otherwise

    // measures whether a 15 day moving average is above a 50 day moving average

    public Boolean process(List<StockBar> stockBars, int start, String key, String stockName) {
        Boolean result = false;
        switch (key) {

            case "goldenCross":
                result = goldenCross(stockBars, start);
                break;

            case "touchesLBB":
                result = touchesLBB(stockBars, start);
                break;

            case "signalLineCross":
                result = signalLineCross(stockBars,stockName);
                break;
            case "":
                result = false;
            default:
                break;
        }
        return result;
    }

    

    public Boolean goldenCross(List<StockBar> stockBars, int start) {
        List<StockBar> shortStockBars = stockBars.subList(start - 15, start);
        BigDecimal shortMovingAverage = Functions.simpleMovingAverage(shortStockBars);
        List<StockBar> longStockBars = stockBars.subList(start - 50, start);
        BigDecimal longMovingAverage = Functions.simpleMovingAverage(longStockBars);

        if (shortMovingAverage.compareTo(longMovingAverage) > 0)
            return true;
        return false;
    }

    // indicates whether the 20 day sma touches or falls beneath the lower bollinger
    // band
    public Boolean touchesLBB(List<StockBar> stockBars, int start) {
        List<StockBar> stockBar = stockBars.subList(start - 20, start);
        BigDecimal sma = Functions.simpleMovingAverage(stockBar);
        BigDecimal lowerBollingerBand = Functions.lowerBollingerBand(stockBar);
        if (sma.compareTo(lowerBollingerBand) <= 0)
            return true;
        return false;
    }

    //indicates whether or not macd has crossed over the signal line within the period
    public Boolean signalLineCross(List<StockBar> stockBars, String stockName) {
//        MACD macd = new MACD(stockName);
//        macd.initializer(stockBars);
//        SL sl = new SL(stockName);
//        sl.initializer(stockBars);
//        EMA ema = new EMA(stockName);
//        ema.initializer(stockBars, 100);
//        if (tradeBlasterDao.queryMACDValue(macd)
//        .compareTo(tradeBlasterDao.querySLValue(sl)) > 0)
//            return true;
        return false;
    }
}
