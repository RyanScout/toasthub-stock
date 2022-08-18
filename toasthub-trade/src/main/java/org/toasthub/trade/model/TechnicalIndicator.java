package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
@Table(name = "ta_technical_indicator")
public class TechnicalIndicator extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
    public static final String GOLDENCROSS = "GoldenCross";
    public static final String LOWERBOLLINGERBAND = "LowerBollingerBand";
    public static final String UPPERBOLLINGERBAND = "UpperBollingerBand";
    public static final String[] TECHNICAL_INDICATOR_TYPES = {
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

    private int shortSMAEvaluationDuration = 0;
    private int longSMAEvaluationDuration = 0;
    private int lbbEvaluationDuration = 0;
    private int ubbEvaluationDuration = 0;

    private BigDecimal standardDeviations = BigDecimal.ZERO;

    private Set<TechnicalIndicatorDetail> details = new LinkedHashSet<TechnicalIndicatorDetail>();

    @Transient private List<TechnicalIndicatorDetail> effectiveDetails = new ArrayList<TechnicalIndicatorDetail>();

    // Constructors
    public TechnicalIndicator() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }

    // Setter/Getter

    @JsonView({ View.Member.class })
    @Transient
    public List<TechnicalIndicatorDetail> getEffectiveDetails() {
        return effectiveDetails;
    }

    public void setEffectiveDetails(final List<TechnicalIndicatorDetail> effectiveDetails) {
        this.effectiveDetails = effectiveDetails;
    }

    @JsonView({ View.Member.class })
    @Column(name = "short_sma_evaluation_duration")
    public int getShortSMAEvaluationDuration() {
        return shortSMAEvaluationDuration;
    }

    public void setShortSMAEvaluationDuration(final int shortSMAEvaluationDuration) {
        this.shortSMAEvaluationDuration = shortSMAEvaluationDuration;
    }

    @JsonView({ View.Member.class })
    @Column(name = "long_sma_evaluation_duration")
    public int getLongSMAEvaluationDuration() {
        return longSMAEvaluationDuration;
    }

    public void setLongSMAEvaluationDuration(final int longSMAEvaluationDuration) {
        this.longSMAEvaluationDuration = longSMAEvaluationDuration;
    }

    @JsonView({ View.Member.class })
    @Column(name = "lbb_evaluation_duration")
    public int getLbbEvaluationDuration() {
        return lbbEvaluationDuration;
    }

    public void setLbbEvaluationDuration(final int lbbEvaluationDuration) {
        this.lbbEvaluationDuration = lbbEvaluationDuration;
    }

    @JsonView({ View.Member.class })
    @Column(name = "ubb_evaluation_duration")
    public int getUbbEvaluationDuration() {
        return ubbEvaluationDuration;
    }

    public void setUbbEvaluationDuration(final int ubbEvaluationDuration) {
        this.ubbEvaluationDuration = ubbEvaluationDuration;
    }

    @JsonView({ View.Member.class })
    @Column(name = "standard_deviations")
    public BigDecimal getStandardDeviations() {
        return standardDeviations;
    }

    public void setStandardDeviations(final BigDecimal standardDeviations) {
        this.standardDeviations = standardDeviations;
    }

    @OneToMany(mappedBy = "technicalIndicator", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TechnicalIndicatorDetail> getDetails() {
        return details;
    }

    public void setDetails(final Set<TechnicalIndicatorDetail> details) {
        this.details = details;
    }

    @JsonView({ View.Member.class })
    @Column(name = "symbol")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @JsonView({ View.Member.class })
    @Column(name = "technical_indicator_type")
    public String getTechnicalIndicatorType() {
        return technicalIndicatorType;
    }

    public void setTechnicalIndicatorType(final String technicalIndicatorType) {
        this.technicalIndicatorType = technicalIndicatorType;
    }

    @JsonView({ View.Member.class })
    @Column(name = "technical_indicator_key")
    public String getTechnicalIndicatorKey() {
        return technicalIndicatorKey;
    }

    public void setTechnicalIndicatorKey(final String technicalIndicatorKey) {
        this.technicalIndicatorKey = technicalIndicatorKey;
    }

    @JsonView({ View.Member.class })
    @Column(name = "evaluation_period")
    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(final String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    @JsonView({ View.Member.class })
    @Column(name = "last_flash")
    public long getLastFlash() {
        return lastFlash;
    }

    public void setLastFlash(final long lastFlash) {
        this.lastFlash = lastFlash;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flashing")
    public boolean isFlashing() {
        return flashing;
    }

    public void setFlashing(final boolean flashing) {
        this.flashing = flashing;
    }

    @JsonView({ View.Member.class })
    @Column(name = "last_check")
    public long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(final long lastCheck) {
        this.lastCheck = lastCheck;
    }

    @JsonView({ View.Member.class })
    @Column(name = "first_check")
    public long getFirstCheck() {
        return firstCheck;
    }

    public void setFirstCheck(final long firstCheck) {
        this.firstCheck = firstCheck;
    }

    @JsonView({ View.Member.class })
    @Column(name = "checked")
    public long getChecked() {
        return checked;
    }

    public void setChecked(final long checked) {
        this.checked = checked;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flashed")
    public long getFlashed() {
        return flashed;
    }

    public void setFlashed(final long flashed) {
        this.flashed = flashed;
    }

    @JsonView({ View.Member.class })
    @Column(name = "successes")
    public long getSuccesses() {
        return successes;
    }

    public void setSuccesses(final long successes) {
        this.successes = successes;
    }

    @JsonView({ View.Member.class })
    @Column(name = "updating")
    public boolean isUpdating() {
        return updating;
    }

    public void setUpdating(final boolean updating) {
        this.updating = updating;
    }
}
