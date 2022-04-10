package org.toasthub.stock.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.cache.TradeSignalCache;

@Service("CurrentOrderSignals")
public class CurrentOrderSignals {

    @Autowired
    protected TradeSignalCache tradeSignalCache;

    public Boolean process(String alg, String symbol, String evaluationPeriod) {
        Boolean result = false;
        evaluationPeriod = evaluationPeriod.toUpperCase();
        switch (alg) {

            case "goldenCross":
                result = currentGoldenCross(symbol, evaluationPeriod);
                break;

            case "touchesLBB":
                result = currentTouchesLBB(symbol, evaluationPeriod);
                break;

            case "touchesUBB":
                result = currentTouchesUBB(symbol, evaluationPeriod);
                break;

            case "signalLineCross":
                result = currentSignalLineCross(symbol, evaluationPeriod);
                break;

            default:
                System.out.println("invalid order condition");
                break;
        }
        return result;
    }

    public Boolean currentSignalLineCross(String symbol, String evaluationPeriod) {
        return false;
    }

    public Boolean currentTouchesLBB(String symbol, String evaluationPeriod) {
        if (tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol) == null) {
            System.out.println("Cache has not been initialized");
            return false;
        }
        return tradeSignalCache.getLowerBollingerBandMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol)
                .isBuyIndicator();
    }

    public Boolean currentTouchesUBB(String symbol, String evaluationPeriod) {
        if (tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol) == null) {
            System.out.println("Cache has not been initialized");
            return false;
        }
        return tradeSignalCache.getUpperBollingerBandMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol)
                .isSellIndicator();
    }

    public Boolean currentGoldenCross(String symbol, String evaluationPeriod) {
        if (tradeSignalCache.getGoldenCrossMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol) == null) {
            System.out.println("Cache has not been initialized");
            return false;
        }
        return tradeSignalCache.getGoldenCrossMap().get("GLOBAL::" + evaluationPeriod + "::" + symbol).isBuyIndicator();
    }
}
