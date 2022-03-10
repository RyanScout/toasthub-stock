package org.toasthub.stock.analysis;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.stock.model.cache.BuySignalCache;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Service("CurrentBuySignals")
public class CurrentBuySignals {

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
        return BuySignalCache.getInstance().getGoldenCrossMap().get("GLOBAL").isBuyIndicator();
    }
}
