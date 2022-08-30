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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

//Technical Indicator Snapshot
@Entity
@Table(name = "ta_ti_snapshot")
public class TISnapshot extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;

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
    private BigDecimal averageSuccessPercent = BigDecimal.ZERO;

    private CustomTechnicalIndicator customTechnicalIndicator;

    private Set<TISnapshotDetail> details = new LinkedHashSet<TISnapshotDetail>();

    @Transient
    private List<TISnapshotDetail> effectiveDetails = new ArrayList<TISnapshotDetail>();

    // Constructors
    public TISnapshot() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }

    public void resetSnapshot() {
        this.setChecked(0);
        this.setFlashed(0);
        this.setSuccesses(0);
        this.setFirstCheck(0);
        this.setLastCheck(0);
        this.setLastFlash(0);
        this.setDetails(new LinkedHashSet<TISnapshotDetail>());
    }

    public void copyProperties(final TechnicalIndicator technicalIndicator) {
        this.setSymbol(technicalIndicator.getSymbol());
        this.setTechnicalIndicatorType(technicalIndicator.getTechnicalIndicatorType());
        this.setTechnicalIndicatorKey(technicalIndicator.getTechnicalIndicatorKey());
        this.setEvaluationPeriod(technicalIndicator.getEvaluationPeriod());
        this.setShortSMAEvaluationDuration(technicalIndicator.getShortSMAEvaluationDuration());
        this.setLongSMAEvaluationDuration(technicalIndicator.getLongSMAEvaluationDuration());
        this.setStandardDeviations(technicalIndicator.getStandardDeviations());
        this.setLbbEvaluationDuration(technicalIndicator.getLbbEvaluationDuration());
        this.setUbbEvaluationDuration(technicalIndicator.getUbbEvaluationDuration());
    }

    // Setter/Getter

    @JsonIgnore
    @ManyToOne(targetEntity = CustomTechnicalIndicator.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_technical_indicator_id")
    public CustomTechnicalIndicator getCustomTechnicalIndicator() {
        return customTechnicalIndicator;
    }

    public void setCustomTechnicalIndicator(final CustomTechnicalIndicator customTechnicalIndicator) {
        this.customTechnicalIndicator = customTechnicalIndicator;
    }

    @JsonView({ View.Member.class })
    @Column(name = "average_success_percent")
    public BigDecimal getAverageSuccessPercent() {
        return averageSuccessPercent;
    }

    public void setAverageSuccessPercent(BigDecimal averageSuccessPercent) {
        this.averageSuccessPercent = averageSuccessPercent;
    }

    @JsonView({ View.Member.class })
    @Transient
    public List<TISnapshotDetail> getEffectiveDetails() {
        return effectiveDetails;
    }

    public void setEffectiveDetails(final List<TISnapshotDetail> effectiveDetails) {
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

    @OneToMany(mappedBy = "TISnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TISnapshotDetail> getDetails() {
        return details;
    }

    public void setDetails(final Set<TISnapshotDetail> details) {
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
