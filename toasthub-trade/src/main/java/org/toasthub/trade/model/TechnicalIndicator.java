package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_technical_indicator")
public class TechnicalIndicator extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
	public static final String GOLDENCROSS = "GoldenCross";
    public static final String LOWERBOLLINGERBAND = "LowerBollingerBand";
    public static final String UPPERBOLLINGERBAND = "UpperBollingerBand";
    public static final String[] TECHNICALINDICATORTYPES = {
            GOLDENCROSS, LOWERBOLLINGERBAND, UPPERBOLLINGERBAND
    };
    private boolean flashing = false;
    private boolean updating = false;
    private String evaluationPeriod = "";
    private String technicalIndicatorKey = "";
    private String technicalIndicatorType = "";
    private String symbol = "";
    private long checked = 0;
    private long flashed = 0;
    private long successes = 0;
    private long firstCheck = 0;
    private long lastCheck = 0;
    private long lastFlash = 0;
    private String shortSMAType;
    private String longSMAType;
    private String LBBType;
    private String UBBType;
    private BigDecimal standardDeviations;

    private Set<TechnicalIndicatorDetail> details = new LinkedHashSet<TechnicalIndicatorDetail>();

    // Constructors
    public TechnicalIndicator() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }   

    
    // Setter/Getter
    @JsonView({View.Member.class})
    @OneToMany(mappedBy = "technicalIndicator", cascade = CascadeType.ALL)
    public Set<TechnicalIndicatorDetail> getDetails() {
        return details;
    }
    public void setDetails(Set<TechnicalIndicatorDetail> details) {
        this.details = details;
    }

    @JsonView({View.Member.class})
    @Column(name = "symbol")
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @JsonView({View.Member.class})
    @Column(name = "technical_indicator_type")
    public String getTechnicalIndicatorType() {
        return technicalIndicatorType;
    }
    public void setTechnicalIndicatorType(String technicalIndicatorType) {
        this.technicalIndicatorType = technicalIndicatorType;
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
    @Column(name = "evaluation_period")
    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }
    public void setEvaluationPeriod(String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
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
    @Column(name = "standard_deviations")
    public BigDecimal getStandardDeviations() {
        return standardDeviations;
    }
    public void setStandardDeviations(BigDecimal standardDeviations) {
        this.standardDeviations = standardDeviations;
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
    @Column(name = "short_sma_type")
    public String getShortSMAType() {
        return shortSMAType;
    }
    public void setShortSMAType(String shortSMAType) {
        this.shortSMAType = shortSMAType;
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
    @Column(name = "last_flash")
    public long getLastFlash() {
        return lastFlash;
    }
    public void setLastFlash(long lastFlash) {
        this.lastFlash = lastFlash;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "flashing")
    public boolean isFlashing() {
        return flashing;
    }
    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }

    @JsonView({View.Member.class})
    @Column(name = "last_check")
    public long getLastCheck() {
        return lastCheck;
    }
    public void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }

    @JsonView({View.Member.class})
    @Column(name = "first_check")
    public long getFirstCheck() {
        return firstCheck;
    }
    public void setFirstCheck(long firstCheck) {
        this.firstCheck = firstCheck;
    }

    @JsonView({View.Member.class})
    @Column(name = "checked")
    public long getChecked() {
        return checked;
    }
    public void setChecked(long checked) {
        this.checked = checked;
    }

    @JsonView({View.Member.class})
    @Column(name = "flashed")
    public long getFlashed() {
        return flashed;
    }
    public void setFlashed(long flashed) {
        this.flashed = flashed;
    }

    @JsonView({View.Member.class})
    @Column(name = "successes")
    public long getSuccesses() {
        return successes;
    }
    public void setSuccesses(long successes) {
        this.successes = successes;
    }

    @JsonView({View.Member.class})
    @Column(name = "updating")
    public boolean isUpdating() {
        return updating;
    }
    public void setUpdating(boolean updating) {
        this.updating = updating;
    }
}
