package org.toasthub.trade.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@MappedSuperclass()
public abstract class BaseAlg extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;

    protected String symbol = "";
    protected String evaluationPeriod = "";

    protected int evaluationDuration = 0;

    protected long epochSeconds = 0;
    protected long correspondingDay = 0;

    protected BigDecimal value = BigDecimal.ZERO;

    // Constructor
    public BaseAlg() {
        super();
    }

    // Setter/Getter

    @JsonView({ View.Member.class })
    @Column(name = "evaluation_period")
    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(final String evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    @JsonView({ View.Member.class })
    @Column(name = "evaluation_duration")
    public int getEvaluationDuration() {
        return evaluationDuration;
    }

    public void setEvaluationDuration(final int evaluationDuration) {
        this.evaluationDuration = evaluationDuration;
    }

    @JsonView({ View.Member.class })
    @Column(name = "corresponding_day")
    public long getCorrespondingDay() {
        return correspondingDay;
    }

    public void setCorrespondingDay(final long correspondingDay) {
        this.correspondingDay = correspondingDay;
    }

    @JsonView({ View.Member.class })
    @Column(name = "value")
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
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
    @Column(name = "epoch_seconds")
    public long getEpochSeconds() {
        return epochSeconds;
    }

    public void setEpochSeconds(final long epochSeconds) {

        this.epochSeconds = epochSeconds;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (correspondingDay ^ (correspondingDay >>> 32));
        result = prime * result + (int) (epochSeconds ^ (epochSeconds >>> 32));
        result = prime * result + evaluationDuration;
        result = prime * result + ((evaluationPeriod == null) ? 0 : evaluationPeriod.hashCode());
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BaseAlg other = (BaseAlg) obj;
        if (correspondingDay != other.correspondingDay)
            return false;
        if (epochSeconds != other.epochSeconds)
            return false;
        if (evaluationDuration != other.evaluationDuration)
            return false;
        if (evaluationPeriod == null) {
            if (other.evaluationPeriod != null)
                return false;
        } else if (!evaluationPeriod.equals(other.evaluationPeriod))
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
