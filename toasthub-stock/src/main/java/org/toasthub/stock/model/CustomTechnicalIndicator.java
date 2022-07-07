package org.toasthub.stock.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_custom_technical_indicator")
public class CustomTechnicalIndicator extends BaseEntity {
    private String name;
    private String evaluationPeriod;
    private String technicalIndicatorType;
    private String technicalIndicatorKey;

    private String shortSMAType;
    private String longSMAType;

    private String LBBType;
    private String UBBType;
    private BigDecimal standardDeviations;

    
    private Set<Symbol> symbols = new LinkedHashSet<Symbol>();
    private ArrayList<Object> technicalIndicators = new ArrayList<Object>();
   

    
    public String getTechnicalIndicatorType() {
        return technicalIndicatorType;
    }

    public BigDecimal getStandardDeviations() {
        return standardDeviations;
    }

    public void setStandardDeviations(BigDecimal standardDeviations) {
        this.standardDeviations = standardDeviations;
    }

    @Column(name = "ubb_type")
    public String getUBBType() {
        return UBBType;
    }

    public void setUBBType(String uBBType) {
        this.UBBType = uBBType;
    }

    @Column(name = "lbb_type")
    public String getLBBType() {
        return LBBType;
    }

    public void setLBBType(String lBBType) {
        this.LBBType = lBBType;
    }

    @Column(name = "long_sma_type")
    public String getLongSMAType() {
        return longSMAType;
    }

    public void setLongSMAType(String longSMAType) {
        this.longSMAType = longSMAType;
    }

    @Column(name = "short_sma_type")
    public String getShortSMAType() {
        return shortSMAType;
    }

    public void setShortSMAType(String shortSMAType) {
        this.shortSMAType = shortSMAType;
    }

    @OneToMany(mappedBy = "customTechnicalIndicator", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(Set<Symbol> symbols) {
        this.symbols = symbols;
    }

    public String getTechnicalIndicatorKey() {
        return technicalIndicatorKey;
    }


    public void setTechnicalIndicatorKey(String technicalIndicatorKey) {
        this.technicalIndicatorKey = technicalIndicatorKey;
    }

    @Transient
    public ArrayList<Object> getTechnicalIndicators() {
        return technicalIndicators;
    }

    public void setTechnicalIndicators(ArrayList<Object> technicalIndicators) {
        this.technicalIndicators = technicalIndicators;
    }

    public void setTechnicalIndicatorType(String technicalIndicatorType) {
        this.technicalIndicatorType = technicalIndicatorType;
    }

    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
