package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_asset_day")
public class AssetDay extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
    private String symbol;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private long epochSeconds;
    private long volume;
    private BigDecimal vwap;
    private Set<AssetMinute> assetMinutes;

    // Constructors
    public AssetDay() {
        super();
        this.setIdentifier("AssetDay");
    }

    
    // Setter/Getter
    @JsonView({View.Member.class})
    @Column(name = "symbol")
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @JsonView({View.Member.class})
    @OneToMany(mappedBy = "assetDay", cascade = CascadeType.ALL)
    public Set<AssetMinute> getAssetMinutes() {
        return assetMinutes;
    }
    public void setAssetMinutes(final Set<AssetMinute> assetMinutes) {
        this.assetMinutes = assetMinutes;
    }

    @JsonView({View.Member.class})
    @Column(name = "low")
    public BigDecimal getLow() {
        return low;
    }
    public void setLow(final BigDecimal low) {
        this.low = low;
    }

    @JsonView({View.Member.class})
    @Column(name = "high")
    public BigDecimal getHigh() {
        return high;
    }
    public void setHigh(final BigDecimal high) {
        this.high = high;
    }

    @JsonView({View.Member.class})
    @Column(name = "close")
    public BigDecimal getClose() {
        return close;
    }
    public void setClose(final BigDecimal close) {
        this.close = close;
    }

    @JsonView({View.Member.class})
    @Column(name = "open")
    public BigDecimal getOpen() {
        return open;
    }
    public void setOpen(final BigDecimal open) {
        this.open = open;
    }

    @JsonView({View.Member.class})
    @Column(name = "vwap")
    public BigDecimal getVwap() {
        return vwap;
    }
    public void setVwap(final BigDecimal vwap) {
        this.vwap = vwap;
    }

    @JsonView({View.Member.class})
    @Column(name = "volume")
    public long getVolume() {
        return volume;
    }
    public void setVolume(final long volume) {
        this.volume = volume;
    }

    @JsonView({View.Member.class})
    @Column(name = "epoch_seconds")
    public long getEpochSeconds() {
        return epochSeconds;
    }
    public void setEpochSeconds(final long epochSeconds) {
        this.epochSeconds = epochSeconds;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((close == null) ? 0 : close.hashCode());
        result = prime * result + (int) (epochSeconds ^ (epochSeconds >>> 32));
        result = prime * result + ((high == null) ? 0 : high.hashCode());
        result = prime * result + ((low == null) ? 0 : low.hashCode());
        result = prime * result + ((open == null) ? 0 : open.hashCode());
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        result = prime * result + (int) (volume ^ (volume >>> 32));
        result = prime * result + ((vwap == null) ? 0 : vwap.hashCode());
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
        final AssetDay other = (AssetDay) obj;
        if (close == null) {
            if (other.close != null)
                return false;
        } else if (!close.equals(other.close))
            return false;
        if (epochSeconds != other.epochSeconds)
            return false;
        if (high == null) {
            if (other.high != null)
                return false;
        } else if (!high.equals(other.high))
            return false;
        if (low == null) {
            if (other.low != null)
                return false;
        } else if (!low.equals(other.low))
            return false;
        if (open == null) {
            if (other.open != null)
                return false;
        } else if (!open.equals(other.open))
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        if (volume != other.volume)
            return false;
        if (vwap == null) {
            if (other.vwap != null)
                return false;
        } else if (!vwap.equals(other.vwap))
            return false;
        return true;
    }
    
   
}
