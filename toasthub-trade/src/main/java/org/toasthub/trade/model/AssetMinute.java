package org.toasthub.trade.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_asset_minute")
public class AssetMinute extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
    private AssetDay assetDay;
    private String symbol;
    private BigDecimal value;
    private long epochSeconds;
    private long volume;
    private BigDecimal vwap;
    private String type;

    // Constructors
    public AssetMinute() {
        super();
        this.setIdentifier("AssetMinute");
        this.setType("AssetMinute");
    }
    
    public AssetMinute(final String code, final Boolean defaultLang, final String dir) {
        super();
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
    @Column(name = "type")
    public String getType() {
        return type;
    }
    public void setType(final String type) {
        this.type = type;
    }

    @JsonView({View.Member.class})
    @Column(name = "value")
    public BigDecimal getValue() {
        return value;
    }
    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @JsonIgnore
    @ManyToOne(targetEntity = AssetDay.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_day_id")
    public AssetDay getAssetDay() {
        return assetDay;
    }
    public void setAssetDay(final AssetDay assetDay) {
        this.assetDay = assetDay;
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
        result = prime * result + (int) (epochSeconds ^ (epochSeconds >>> 32));
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        final AssetMinute other = (AssetMinute) obj;
        if (epochSeconds != other.epochSeconds)
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
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
