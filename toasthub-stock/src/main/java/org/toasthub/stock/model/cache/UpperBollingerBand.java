package org.toasthub.stock.model.cache;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_upper_bollinger_band")
public class UpperBollingerBand extends BaseEntity {
    private boolean flashing;
    private String UBBType;
    private double standardDeviationValue;
    private String symbol;
    private int checked;
    private int flashed;
    private long firstCheck;
    private long lastFlash;
    private long lastCheck;
    public static final String DEFAULT_UBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_UBB_TYPE_MINUTE = "20-minute";
    public static final double DEFAULT_STANDARD_DEVIATION_VALUE = 2.0;

    public UpperBollingerBand() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
        setChecked(0);
        setFlashed(0);
        setLastCheck(0);
        setLastFlash(0);
        setIdentifier("UpperBollingerBand");
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

    public boolean isFlashing() {
        return flashing;
    }

    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }

    public long getLastFlash() {
        return lastFlash;
    }

    public void setLastFlash(long lastFlash) {
        this.lastFlash = lastFlash;
    }

    public int getFlashed() {
        return flashed;
    }

    public void setFlashed(int flashed) {
        this.flashed = flashed;
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

    @Column(name = "ubb_type")
    public String getUBBType() {
        return UBBType;
    }

    public void setUBBType(String UBBType) {
        this.UBBType = UBBType;
    }

    @Column(name = "standard_deviation_value")
    public double getStandardDeviationValue() {
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue) {
        this.standardDeviationValue = standardDeviationValue;
    }
}