package org.toasthub.stock.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.cache.TradeSignalCache;

@Service("CurrentOrderSignals")
public class CurrentOrderSignals {

    @Autowired
    protected TradeSignalCache tradeSignalCache;
    

    public Boolean process(String alg) {
        Boolean result = false;
        switch (alg) {

            case "goldenCross":
                result = currentGoldenCross();
                break;

            case "touchesLBB":
                result = currentTouchesLBB();
                break;

            case "signalLineCross":
                result = currentSignalLineCross();
                break;

            default:
                break;
        }
        return result;
    }

    public Boolean currentSignalLineCross() {
        return false;
    }

    public Boolean currentTouchesLBB() {
        return false;
    }

    public Boolean currentGoldenCross() {
        return tradeSignalCache.getGoldenCrossMap().get("GLOBAL").isBuyIndicator();
    }
}
