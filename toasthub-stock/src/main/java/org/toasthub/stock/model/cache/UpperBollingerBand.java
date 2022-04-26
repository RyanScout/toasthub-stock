package org.toasthub.stock.model.cache;

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
    private double standardDeviationValue;
    private Set<UpperBollingerBandDetail> upperBollingerBandDetails = new LinkedHashSet<UpperBollingerBandDetail>();

    public static final String DEFAULT_UBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_UBB_TYPE_MINUTE = "20-minute";
    public static final double DEFAULT_STANDARD_DEVIATION_VALUE = 2.0;

    public UpperBollingerBand() {
        super();
        setIdentifier("UpperBollingerBand");
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

    @Column(name = "standard_deviation_value")
    public double getStandardDeviationValue() {
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue) {
        this.standardDeviationValue = standardDeviationValue;
    }
}