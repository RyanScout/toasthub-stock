package org.toasthub.stock.analysis;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Service("BuySignals")
public class BuySignals {

    @Autowired
    HistoricalAnalyzingDao historicalAnalyzingDao;

    // signals return true if buy signal is present, false otherwise

    // measures whether a 15 day moving average is above a 50 day moving average

    public Boolean process(String alg , Request request, Response response) {
        Boolean result = false;
        switch (alg) {

            case "goldenCross":
                result = goldenCross(request, response);
                break;

            case "touchesLBB":
                result = touchesLBB(request , response);
                break;

            case "signalLineCross":
                result = signalLineCross(request , response);
                break;
            default:
                result = false;
                break;
        }
        return result;
    }

    

    public Boolean goldenCross(Request request, Response response) {
        try{
        request.addParam(GlobalConstant.IDENTIFIER , "SMA");

        request.addParam(GlobalConstant.TYPE, "15-day");
        historicalAnalyzingDao.item(request, response);
        SMA shortMovingAverage = (SMA) response.getParam(GlobalConstant.ITEM);

        request.addParam(GlobalConstant.TYPE, "50-day");
        historicalAnalyzingDao.item(request, response);
        SMA longMovingAverage = (SMA) response.getParam(GlobalConstant.ITEM);


        if (shortMovingAverage.getValue().compareTo(longMovingAverage.getValue()) > 0)
            return true;
        return false;

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    // indicates whether the 20 day sma touches or falls beneath the lower bollinger
    // band
    public Boolean touchesLBB(Request request, Response response) {
        try{
        request.addParam(GlobalConstant.IDENTIFIER, "LBB");
        request.addParam(GlobalConstant.TYPE, "20-day");
        historicalAnalyzingDao.item(request, response);
        LBB lbb = (LBB) response.getParam(GlobalConstant.ITEM);

        if ( ((BigDecimal) (request.getParam("STOCKPRICE"))) . compareTo(lbb.getValue()) <= 0)
            return true;
        return false;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //indicates whether or not macd has crossed over the signal line within the period
    public Boolean signalLineCross(Request request, Response response) {
        try{
        request.addParam(GlobalConstant.IDENTIFIER, "MACD");
        request.addParam(GlobalConstant.TYPE, "Day");
        historicalAnalyzingDao.item(request, response);
        MACD macd = (MACD) response.getParam(GlobalConstant.ITEM);

        request.addParam(GlobalConstant.IDENTIFIER, "SL");
        request.addParam(GlobalConstant.TYPE, "Day");
        historicalAnalyzingDao.item(request, response);
        SL sl = (SL) response.getParam(GlobalConstant.ITEM);

       if (macd.getValue()
       .compareTo(sl.getValue()) > 0)
           return true;
        return false;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
