package org.toasthub.stock.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.toasthub.model.TechnicalIndicator;

@Component
@Scope("singleton")
public class TradeSignalCache {

    private Map<String, TechnicalIndicator> technicalIndicatorMap = new ConcurrentHashMap<String, TechnicalIndicator>();

    private Map<String, BigDecimal> recentClosingPriceMap = new ConcurrentHashMap<String, BigDecimal>();
    private Map<String, Long> recentVolumeMap = new ConcurrentHashMap<String, Long>();
    private Map<String, BigDecimal> recentVwapMap = new ConcurrentHashMap<String, BigDecimal>();
    private Map<String, Long> recentEpochSecondsMap = new ConcurrentHashMap<String, Long>();

    private TradeSignalCache() {
    }

    public Map<String, Long> getRecentEpochSecondsMap() {
        return recentEpochSecondsMap;
    }

    public void setRecentEpochSecondsMap(Map<String, Long> recentEpochSecondsMap) {
        this.recentEpochSecondsMap = recentEpochSecondsMap;
    }

    public Map<String, BigDecimal> getRecentClosingPriceMap() {
        return recentClosingPriceMap;
    }

    public void setRecentClosingPriceMap(Map<String, BigDecimal> recentClosingPriceMap) {
        this.recentClosingPriceMap = recentClosingPriceMap;
    }

    public Map<String, TechnicalIndicator> getTechnicalIndicatorMap() {
        return technicalIndicatorMap;
    }

    public void setTechnicalIndicatorMap(Map<String, TechnicalIndicator> technicalIndicatorMap) {
        this.technicalIndicatorMap = technicalIndicatorMap;
    }

    public Map<String, Long> getRecentVolumeMap() {
        return recentVolumeMap;
    }

    public void setRecentVolumeMap(Map<String, Long> recentVolumeMap) {
        this.recentVolumeMap = recentVolumeMap;
    }

    public Map<String, BigDecimal> getRecentVwapMap() {
        return recentVwapMap;
    }

    public void setRecentVwapMap(Map<String, BigDecimal> recentVwapMap) {
        this.recentVwapMap = recentVwapMap;
    }


}
