package org.toasthub.stock.model.cache;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_golden_cross")
public class GoldenCross extends BaseEntity {
    private boolean flashing;
    private String shortSMAType;
    private String longSMAType;
    private String symbol;
    private int checked;
    private int flashed;
    private long firstCheck;
    private long lastCheck;
    private long lastFlash;
    public static final String DEFAULT_SHORT_SMA_TYPE_DAY = "15-day";
    public static final String DEFAULT_LONG_SMA_TYPE_DAY = "50-day";
    public static final String DEFAULT_SHORT_SMA_TYPE_MINUTE = "15-minute";
    public static final String DEFAULT_LONG_SMA_TYPE_MINUTE = "50-minute";

    public GoldenCross() {
        super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
        setChecked(0);
        setFlashed(0);
        setLastCheck(0);
        setLastFlash(0);
        setIdentifier("GoldenCross");
    }

    public boolean isFlashing() {
        return flashing;
    }

    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }

    public long getFirstCheck() {
        return firstCheck;
    }

    public void setFirstCheck(long firstCheck) {
        this.firstCheck = firstCheck;
    }

    public long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public int getFlashed() {
        return flashed;
    }

    public void setFlashed(int flashed) {
        this.flashed = flashed;
    }

    public long getLastFlash() {
        return lastFlash;
    }

    public void setLastFlash(long lastFlash) {
        this.lastFlash = lastFlash;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
}
