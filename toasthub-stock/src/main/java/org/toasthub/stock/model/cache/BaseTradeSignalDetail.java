package org.toasthub.stock.model.cache;

import java.math.BigDecimal;
import java.time.Instant;

import javax.persistence.MappedSuperclass;

import org.toasthub.common.BaseEntity;

@MappedSuperclass()
public abstract class BaseTradeSignalDetail extends BaseEntity{
    private long flashTime = 0;
    private BigDecimal flashPrice;
    private int checked = 0;
    private long volume = 0;
    private BigDecimal vwap;
    private boolean success = false;
    
    public BaseTradeSignalDetail(){
        super();
        this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
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
}
