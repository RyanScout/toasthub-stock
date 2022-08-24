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

//Technical Indicator Snapshot Detail
@Entity
@Table(name = "ta_ti_snapshot_detail")
public class TISnapshotDetail extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
    private long flashTime = 0;
    private BigDecimal flashPrice;
    private int checked = 0;
    private long volume = 0;
    private BigDecimal vwap;
    private boolean success = false;
    private BigDecimal successPercent = BigDecimal.ZERO;
    private TISnapshot snapshot;

    // Constructors
    public TISnapshotDetail() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }

    // Setter/Getter
    @JsonIgnore
    @ManyToOne(targetEntity = TISnapshot.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "ti_snapshot_id")
    public TISnapshot getTISnapshot() {
        return snapshot;
    }

    public void setTISnapshot(final TISnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @JsonView({ View.Member.class })
    @Column(name = "success_percent")
    public BigDecimal getSuccessPercent() {
        return successPercent;
    }

    public void setSuccessPercent(final BigDecimal successPercent) {
        this.successPercent = successPercent;
    }

    @JsonView({ View.Member.class })
    @Column(name = "success")
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    @JsonView({ View.Member.class })
    @Column(name = "vwap")
    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(final BigDecimal vwap) {
        this.vwap = vwap;
    }

    @JsonView({ View.Member.class })
    @Column(name = "volume")
    public long getVolume() {
        return volume;
    }

    public void setVolume(final long volume) {
        this.volume = volume;
    }

    @JsonView({ View.Member.class })
    @Column(name = "checked")
    public int getChecked() {
        return checked;
    }

    public void setChecked(final int checked) {
        this.checked = checked;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flash_price")
    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public void setFlashPrice(final BigDecimal flashPrice) {
        this.flashPrice = flashPrice;
    }

    @JsonView({ View.Member.class })
    @Column(name = "flash_time")
    public long getFlashTime() {
        return flashTime;
    }

    public void setFlashTime(final long flashTime) {
        this.flashTime = flashTime;
    }
}
