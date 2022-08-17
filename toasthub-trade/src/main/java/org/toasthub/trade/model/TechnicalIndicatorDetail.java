package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_technical_indicator_detail")
public class TechnicalIndicatorDetail extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
    private long flashTime = 0;
    private BigDecimal flashPrice;
    private int checked = 0;
    private long volume = 0;
    private BigDecimal vwap;
    private boolean success = false;
    private BigDecimal successPercent = BigDecimal.ZERO;
    private TechnicalIndicator technicalIndicator;

    // Constructors
    public TechnicalIndicatorDetail() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }

    // Setter/Getter
    @JsonIgnore
    @ManyToOne(targetEntity = TechnicalIndicator.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_indicator_id")
    public TechnicalIndicator getTechnicalIndicator() {
        return technicalIndicator;
    }

    public void setTechnicalIndicator(TechnicalIndicator technicalIndicator) {
        this.technicalIndicator = technicalIndicator;
    }

    @JsonView({ View.Member.class })
    @Column(name = "success_percent")
    public BigDecimal getSuccessPercent() {
        return successPercent;
    }

    public void setSuccessPercent(BigDecimal successPercent) {
        this.successPercent = successPercent;
    }

    @JsonView({ View.Member.class })
    @Column(name = "success")
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @JsonView({ View.Member.class })
    @Column(name = "vwap")
    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }

    @JsonView({ View.Member.class })
    @Column(name = "volume")
    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    @JsonView({ View.Member.class })
    @Column(name = "checked")
    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flash_price")
    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public void setFlashPrice(BigDecimal flashPrice) {
        this.flashPrice = flashPrice;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flash_time")
    public long getFlashTime() {
        return flashTime;
    }

    public void setFlashTime(long flashTime) {
        this.flashTime = flashTime;
    }
}
