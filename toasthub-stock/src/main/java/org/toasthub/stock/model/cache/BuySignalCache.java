package org.toasthub.stock.model.cache;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.toasthub.analysis.model.SMA;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Component
@Scope("singleton")
public class BuySignalCache {

    @Autowired
    protected BuySignalCacheDao buySignalCacheDao;

    private long now = ZonedDateTime.ofInstant(Instant.now(),
        ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS).toEpochSecond();

    private Map<String,GoldenCross> goldenCrossMap = new ConcurrentHashMap<String, GoldenCross>();
    private Map<String,LowerBollingerBand> lowerBollingerBandMap = new ConcurrentHashMap<String, LowerBollingerBand>();
    private Map<String,SignalLineCross> signalLineCrossMap = new ConcurrentHashMap<String,SignalLineCross>();

    private static BuySignalCache buySignalCache = new BuySignalCache();

    private BuySignalCache(){
    }

    public void setGlobals(){
        try{
            Request request = new Request();
            Response response = new Response();
            request.addParam(GlobalConstant.EPOCHSECONDS, now);
            request.addParam(GlobalConstant.STOCK , "SPY");
    
            GoldenCross globalGoldenCross = new GoldenCross();
            request.addParam(GlobalConstant.IDENTIFIER , "SMA");
    
            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getShortSMAType());
            buySignalCacheDao.item(request, response);
            SMA shortSMA = (SMA)response.getParam(GlobalConstant.ITEM);
    
            request.addParam(GlobalConstant.TYPE, globalGoldenCross.getLongSMAType());
            buySignalCacheDao.item(request, response);
            SMA longSMA = (SMA)response.getParam(GlobalConstant.ITEM);
    
            if (shortSMA.getValue().compareTo(longSMA.getValue()) > 0)
            globalGoldenCross.setBuyIndicator(true);
            else
            globalGoldenCross.setBuyIndicator(false);
    
            goldenCrossMap.put("GLOBAL" , globalGoldenCross);
    
            }catch(Exception e){
                e.printStackTrace();
            } 
    }

    public static BuySignalCache getInstance() {
        return buySignalCache;
    }

    public Map<String,GoldenCross> getGoldenCrossMap() {
        return goldenCrossMap;
    }
    public Map<String,SignalLineCross> getSignalLineCrossMap() {
        return signalLineCrossMap;
    }
    public void setSignalLineCrossMap(Map<String,SignalLineCross> signalLineCrossMap) {
        this.signalLineCrossMap = signalLineCrossMap;
    }
    public Map<String,LowerBollingerBand> getLowerBollingerBandMap() {
        return lowerBollingerBandMap;
    }
    public void setLowerBollingerBandMap(Map<String,LowerBollingerBand> lowerBollingerBandMap) {
        this.lowerBollingerBandMap = lowerBollingerBandMap;
    }
    public void setGoldenCrossMap(Map<String,GoldenCross> goldenCrossMap) {
        this.goldenCrossMap = goldenCrossMap;
    } 

}
