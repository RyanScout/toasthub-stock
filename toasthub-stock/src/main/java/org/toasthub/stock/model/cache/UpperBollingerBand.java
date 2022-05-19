package org.toasthub.stock.model.cache;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ta_upper_bollinger_band")
public class UpperBollingerBand extends BaseTradeSignal {
    private String UBBType;
    private BigDecimal standardDeviations;
    private Set<UpperBollingerBandDetail> upperBollingerBandDetails = new LinkedHashSet<UpperBollingerBandDetail>();

    public static final String DEFAULT_UBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_UBB_TYPE_MINUTE = "20-minute";
    public static final BigDecimal DEFAULT_STANDARD_DEVIATIONS = BigDecimal.valueOf(2.0);

    public UpperBollingerBand() {
        super();
        setIdentifier("UpperBollingerBand");
    }

    public BigDecimal getStandardDeviations() {
        return standardDeviations;
    }

    public void setStandardDeviations(BigDecimal standardDeviations) {
        this.standardDeviations = standardDeviations;
    }

    @OneToMany(mappedBy = "upperBollingerBand", cascade = CascadeType.ALL)
    public Set<UpperBollingerBandDetail> getUpperBollingerBandDetails() {
        return upperBollingerBandDetails;
    }

    public void setUpperBollingerBandDetails(Set<UpperBollingerBandDetail> upperBollingerBandDetails) {
        this.upperBollingerBandDetails = upperBollingerBandDetails;
    }

    @Column(name = "ubb_type")
    public String getUBBType() {
        return UBBType;
    }

    public void setUBBType(String UBBType) {
        this.UBBType = UBBType;
    }
}