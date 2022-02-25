package org.toasthub.stock.analysis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("CurrentBuySignals")
public class CurrentBuySignals{
    
    @Autowired
    HistoricalAnalyzingDao historicalAnalyzingDao;

    private Instant instant = Instant.now().truncatedTo(ChronoUnit.MINUTES);

    public Boolean process(String alg, String stock) {
        Boolean result = false;
        switch (alg) {

            case "goldenCross":
                result = currentGoldenCross(stock);
                break;

            case "touchesLBB":
                result = currentTouchesLBB(stock);
                break;

            case "signalLineCross":
                result = currentSignalLineCross(stock);
                break;

            default:
                break;
        }
        return result;
    }

    public Boolean currentSignalLineCross(String stock) {
        if (historicalAnalyzingDao.queryAlgValue("MACD", stock, "MACD", instant.getEpochSecond())
        .compareTo(historicalAnalyzingDao.queryAlgValue("SL", stock, "SL", instant.getEpochSecond())) > 0)
            return true;
        return false;
    }
    public Boolean currentTouchesLBB(String stock) {
       if (historicalAnalyzingDao.queryAlgValue("SMA", stock, "20-day", instant.getEpochSecond())
       .compareTo(historicalAnalyzingDao.queryAlgValue("LBB", stock, "20-day",instant.getEpochSecond())) <= 0)
           return true;
       return false;
   }
   public Boolean currentGoldenCross(String stock) {
       if (historicalAnalyzingDao.queryAlgValue("SMA", stock, "15-day",instant.getEpochSecond())
       .compareTo(historicalAnalyzingDao.queryAlgValue("SMA", stock, "50-day",instant.getEpochSecond())) > 0)
           return true;
       return false;
   }
}
