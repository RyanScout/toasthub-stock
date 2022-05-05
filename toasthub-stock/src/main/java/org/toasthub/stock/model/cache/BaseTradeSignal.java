package org.toasthub.stock.model.cache;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.toasthub.common.BaseEntity;

@MappedSuperclass
public abstract class BaseTradeSignal extends BaseEntity {
    private boolean flashing = false;
    private String symbol = "";
    private String evalPeriod = "";
    private String tradeSignalKey = "";
    private int checked = 0;
    private int flashed = 0;
    private int successes = 0;
    private long firstCheck = 0;
    private long lastCheck = 0;
    private long lastFlash = 0;

    public BaseTradeSignal() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
    }

    @Column(name = "trade_signal_key")
    public String getTradeSignalKey() {
        return tradeSignalKey;
    }

    public void setTradeSignalKey(String tradeSignalKey) {
        this.tradeSignalKey = tradeSignalKey;
    }

    @Column(name="eval_period")
    public String getEvalPeriod() {
        return evalPeriod;
    }

    public void setEvalPeriod(String evalPeriod) {
        this.evalPeriod = evalPeriod;
    }

    public boolean isFlashing() {
        return flashing;
    }

    public long getLastFlash() {
        return lastFlash;
    }

    public void setLastFlash(long lastFlash) {
        this.lastFlash = lastFlash;
    }

    public long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public long getFirstCheck() {
        return firstCheck;
    }

    public void setFirstCheck(long firstCheck) {
        this.firstCheck = firstCheck;
    }

    public int getSuccesses() {
        return successes;
    }

    public void setSuccesses(int successes) {
        this.successes = successes;
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

    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }
}
