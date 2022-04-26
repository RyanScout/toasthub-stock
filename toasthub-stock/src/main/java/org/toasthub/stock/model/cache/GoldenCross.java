package org.toasthub.stock.model.cache;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ta_golden_cross")
public class GoldenCross extends BaseTradeSignal {
    private String shortSMAType;
    private String longSMAType;
    private Set<GoldenCrossDetail> goldenCrossDetails = new LinkedHashSet<GoldenCrossDetail>();

    public static final String DEFAULT_SHORT_SMA_TYPE_DAY = "15-day";
    public static final String DEFAULT_LONG_SMA_TYPE_DAY = "50-day";
    public static final String DEFAULT_SHORT_SMA_TYPE_MINUTE = "15-minute";
    public static final String DEFAULT_LONG_SMA_TYPE_MINUTE = "50-minute";

    public GoldenCross() {
        super();
        setIdentifier("GoldenCross");
    }

    @OneToMany(mappedBy = "goldenCross", cascade = CascadeType.ALL)
    public Set<GoldenCrossDetail> getGoldenCrossDetails() {
        return goldenCrossDetails;
    }

    public void setGoldenCrossDetails(Set<GoldenCrossDetail> goldenCrossDetails) {
        this.goldenCrossDetails = goldenCrossDetails;
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
