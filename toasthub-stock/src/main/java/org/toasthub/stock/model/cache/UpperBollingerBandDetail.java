package org.toasthub.stock.model.cache;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ta_upper_bollinger_band_detail")
public class UpperBollingerBandDetail extends BaseTradeSignalDetail {
    private UpperBollingerBand upperBollingerBand;

    public UpperBollingerBandDetail() {
        super();
        this.setIdentifier("UpperBollingerBandDetail");
    }

    @JsonIgnore
    @ManyToOne(targetEntity = UpperBollingerBand.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "upper_bollinger_band_id")
    public UpperBollingerBand getUpperBollingerBand() {
        return upperBollingerBand;
    }

    public void setUpperBollingerBand(UpperBollingerBand upperBollingerBand) {
        this.upperBollingerBand = upperBollingerBand;
    }
}
