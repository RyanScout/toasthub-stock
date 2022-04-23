package org.toasthub.stock.model.cache;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_golden_cross_detail")
public class GoldenCrossDetail extends BaseEntity{
    private GoldenCross goldenCross;
    private long flashTime;
    private BigDecimal flashPrice;
    private int checked = 0;
    private long volume;
    private BigDecimal vwap;
    private boolean success = false;

    public GoldenCrossDetail(){
        this.setIdentifier("GoldenCrossDetail");
    }

    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public void setFlashPrice(BigDecimal flashPrice) {
        this.flashPrice = flashPrice;
    }

    public long getFlashTime() {
        return flashTime;
    }

    public void setFlashTime(long flashTime) {
        this.flashTime = flashTime;
    }


    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }

    @JsonIgnore
    @ManyToOne(targetEntity = GoldenCross.class , fetch = FetchType.LAZY)
    @JoinColumn(name = "golden_cross_id")
    public GoldenCross getGoldenCross() {
        return goldenCross;
    }

    public void setGoldenCross(GoldenCross goldenCross) {
        this.goldenCross = goldenCross;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    
}
