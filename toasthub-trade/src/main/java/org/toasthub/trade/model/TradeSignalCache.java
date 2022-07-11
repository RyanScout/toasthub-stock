package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class TradeSignalCache {

    private Map<String, TechnicalIndicator> technicalIndicatorMap = new ConcurrentHashMap<String, TechnicalIndicator>();
    private Map<String, BigDecimal> recentClosingPriceMap = new ConcurrentHashMap<String, BigDecimal>();
    private Map<String, Long> recentVolumeMap = new ConcurrentHashMap<String, Long>();
    private Map<String, BigDecimal> recentVwapMap = new ConcurrentHashMap<String, BigDecimal>();
    private Map<String, Long> recentEpochSecondsMap = new ConcurrentHashMap<String, Long>();

    // Constructors
    private TradeSignalCache() {
    }

    // Setter/Getter
    public Map<String, Long> getRecentEpochSecondsMap() {
        return recentEpochSecondsMap;
    }
    public void setRecentEpochSecondsMap(final Map<String, Long> recentEpochSecondsMap) {
        this.recentEpochSecondsMap = recentEpochSecondsMap;
    }

    public Map<String, BigDecimal> getRecentClosingPriceMap() {
        return recentClosingPriceMap;
    }
    public void setRecentClosingPriceMap(final Map<String, BigDecimal> recentClosingPriceMap) {
        this.recentClosingPriceMap = recentClosingPriceMap;
    }

    public Map<String, TechnicalIndicator> getTechnicalIndicatorMap() {
        return technicalIndicatorMap;
    }
    public void setTechnicalIndicatorMap(final Map<String, TechnicalIndicator> technicalIndicatorMap) {
        this.technicalIndicatorMap = technicalIndicatorMap;
    }

    public Map<String, Long> getRecentVolumeMap() {
        return recentVolumeMap;
    }
    public void setRecentVolumeMap(final Map<String, Long> recentVolumeMap) {
        this.recentVolumeMap = recentVolumeMap;
    }

    public Map<String, BigDecimal> getRecentVwapMap() {
        return recentVwapMap;
    }
    public void setRecentVwapMap(final Map<String, BigDecimal> recentVwapMap) {
        this.recentVwapMap = recentVwapMap;
    }

    public void insertTechnicalIndicator(final TechnicalIndicator technicalIndicator) {
        technicalIndicatorMap.put(technicalIndicator.getTechnicalIndicatorType() + "::"
                + technicalIndicator.getTechnicalIndicatorKey() + "::"
                + technicalIndicator.getEvaluationPeriod() + "::"
                + technicalIndicator.getSymbol(), technicalIndicator);
    }

}
