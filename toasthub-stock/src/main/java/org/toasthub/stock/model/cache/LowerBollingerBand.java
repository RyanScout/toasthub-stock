package org.toasthub.stock.model.cache;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ta_lower_bollinger_band")
public class LowerBollingerBand extends BaseTradeSignal {
    private String LBBType;
    private double standardDeviationValue;
    private Set<LowerBollingerBandDetail> lowerBollingerBandDetails = new LinkedHashSet<LowerBollingerBandDetail>();

    public static final String DEFAULT_LBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_LBB_TYPE_MINUTE = "20-minute";
    public static final double DEFAULT_STANDARD_DEVIATION_VALUE = 2.0;

    public LowerBollingerBand() {
        super();
        setIdentifier("LowerBollingerBand");
    }

    @OneToMany(mappedBy = "lowerBollingerBand", cascade = CascadeType.ALL)
    public Set<LowerBollingerBandDetail> getLowerBollingerBandDetails() {
        return lowerBollingerBandDetails;
    }

    public void setLowerBollingerBandDetails(Set<LowerBollingerBandDetail> lowerBollingerBandDetails) {
        this.lowerBollingerBandDetails = lowerBollingerBandDetails;
    }

    @Column(name = "lbb_type")
    public String getLBBType() {
        return LBBType;
    }

    public void setLBBType(String LBBType) {
        this.LBBType = LBBType;
    }

    @Column(name = "standard_deviation_value")
    public double getStandardDeviationValue() {
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue) {
        this.standardDeviationValue = standardDeviationValue;
    }
}