package org.toasthub.stock.model.cache;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ta_lower_bollinger_band_detail")
public class LowerBollingerBandDetail extends BaseTradeSignalDetail {
    private LowerBollingerBand lowerBollingerBand;

    public LowerBollingerBandDetail() {
        super();
        this.setIdentifier("LowerBollingerBandDetail");
    }

    @JsonIgnore
    @ManyToOne(targetEntity = LowerBollingerBand.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "lower_bollinger_band_id")
    public LowerBollingerBand getLowerBollingerBand() {
        return lowerBollingerBand;
    }

    public void setLowerBollingerBand(LowerBollingerBand lowerBollingerBand) {
        this.lowerBollingerBand = lowerBollingerBand;
    }
}
