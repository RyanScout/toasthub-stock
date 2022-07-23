package org.toasthub.trade.model;

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

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_custom_technical_indicator")
public class CustomTechnicalIndicator extends TradeBaseEntity {
   
	private static final long serialVersionUID = 1L;
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
   

    // Setter/Getter
    @JsonView({View.Member.class})
    @Column(name = "technical_indicator_type")
    public String getTechnicalIndicatorType() {
        return technicalIndicatorType;
    }
    public void setTechnicalIndicatorType(String technicalIndicatorType) {
        this.technicalIndicatorType = technicalIndicatorType;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "standard_deviations")
    public BigDecimal getStandardDeviations() {
        return standardDeviations;
    }
    public void setStandardDeviations(BigDecimal standardDeviations) {
        this.standardDeviations = standardDeviations;
    }

    @JsonView({View.Member.class})
    @Column(name = "ubb_type")
    public String getUBBType() {
        return UBBType;
    }
    public void setUBBType(String uBBType) {
        this.UBBType = uBBType;
    }

    @JsonView({View.Member.class})
    @Column(name = "lbb_type")
    public String getLBBType() {
        return LBBType;
    }
    public void setLBBType(String lBBType) {
        this.LBBType = lBBType;
    }

    @JsonView({View.Member.class})
    @Column(name = "long_sma_type")
    public String getLongSMAType() {
        return longSMAType;
    }
    public void setLongSMAType(String longSMAType) {
        this.longSMAType = longSMAType;
    }

    @JsonView({View.Member.class})
    @Column(name = "short_sma_type")
    public String getShortSMAType() {
        return shortSMAType;
    }
    public void setShortSMAType(String shortSMAType) {
        this.shortSMAType = shortSMAType;
    }

    @JsonView({View.Member.class})
    @OneToMany(mappedBy = "customTechnicalIndicator", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Symbol> getSymbols() {
        return symbols;
    }
    public void setSymbols(Set<Symbol> symbols) {
        this.symbols = symbols;
    }

    @JsonView({View.Member.class})
    @Column(name = "technical_indicator_key")
    public String getTechnicalIndicatorKey() {
        return technicalIndicatorKey;
    }
    public void setTechnicalIndicatorKey(String technicalIndicatorKey) {
        this.technicalIndicatorKey = technicalIndicatorKey;
    }

    @JsonView({View.Member.class})
    @Transient
    public ArrayList<Object> getTechnicalIndicators() {
        return technicalIndicators;
    }
    public void setTechnicalIndicators(ArrayList<Object> technicalIndicators) {
        this.technicalIndicators = technicalIndicators;
    }

    @JsonView({View.Member.class})
    @Column(name = "evaluation_period")
    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }
    public void setEvaluationPeriod(String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    @JsonView({View.Member.class})
    @Column(name = "name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
