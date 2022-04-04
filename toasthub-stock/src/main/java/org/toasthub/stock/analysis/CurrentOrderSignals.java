package org.toasthub.stock.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.cache.TradeSignalCache;

@Service("CurrentOrderSignals")
public class CurrentOrderSignals {

    @Autowired
    protected TradeSignalCache tradeSignalCache;

    public Boolean process(String alg, String symbol) {
        Boolean result = false;
        switch (alg) {

            case "goldenCross":
                result = currentGoldenCross(symbol);
                break;

            case "touchesLBB":
                result = currentTouchesLBB(symbol);
                break;

            case "touchesUBB":
                result = currentTouchesUBB(symbol);
                break;

            case "signalLineCross":
                result = currentSignalLineCross(symbol);
                break;

            default:
                break;
        }
        return result;
    }

    public Boolean currentSignalLineCross(String symbol) {
        return false;
    }

    public Boolean currentTouchesLBB(String symbol) {
        return tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::" + symbol).isBuyIndicator();
    }

    public Boolean currentTouchesUBB(String symbol) {
        return tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::" + symbol).isSellIndicator();
    }

    public Boolean currentGoldenCross(String symbol) {
        return tradeSignalCache.getGoldenCrossMap().get("GLOBAL::" + symbol).isBuyIndicator();
    }
}
