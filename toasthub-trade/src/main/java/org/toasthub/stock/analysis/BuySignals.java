package org.toasthub.stock.analysis;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.stock.model.LBB;
import org.toasthub.stock.model.MACD;
import org.toasthub.stock.model.SL;
import org.toasthub.stock.model.SMA;
import org.toasthub.stock.model.TradeConstant;


@Service("BuySignals")
public class BuySignals {

    @Autowired
    @Qualifier("TAHistoricalAnalyzingDao")
    HistoricalAnalyzingDao historicalAnalyzingDao;

    // signals return true if buy signal is present, false otherwise

    // measures whether a 15 day moving average is above a 50 day moving average

    public Boolean process(String alg , RestRequest request, RestResponse response) {
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

    

    public Boolean goldenCross(RestRequest request, RestResponse response) {
        try{
        request.addParam(TradeConstant.IDENTIFIER , "SMA");

        request.addParam(TradeConstant.TYPE, "15-day");
        historicalAnalyzingDao.item(request, response);
        SMA shortMovingAverage = (SMA) response.getParam(GlobalConstant.ITEM);

        request.addParam(TradeConstant.TYPE, "50-day");
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
    public Boolean touchesLBB(RestRequest request, RestResponse response) {
        try{
        request.addParam(TradeConstant.IDENTIFIER, "LBB");
        request.addParam(TradeConstant.TYPE, "20-day");
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
    public Boolean signalLineCross(RestRequest request, RestResponse response) {
        try{
        request.addParam(TradeConstant.IDENTIFIER, "MACD");
        request.addParam(TradeConstant.TYPE, "Day");
        historicalAnalyzingDao.item(request, response);
        MACD macd = (MACD) response.getParam(GlobalConstant.ITEM);

        request.addParam(TradeConstant.IDENTIFIER, "SL");
        request.addParam(TradeConstant.TYPE, "Day");
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
