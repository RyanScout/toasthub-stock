package org.toasthub.stock.model.cache;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ta_golden_cross_detail")
public class GoldenCrossDetail extends BaseTradeSignalDetail {
    private GoldenCross goldenCross;

    public GoldenCrossDetail() {
        super();
        this.setIdentifier("GoldenCrossDetail");
    }

    @JsonIgnore
    @ManyToOne(targetEntity = GoldenCross.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "golden_cross_id")
    public GoldenCross getGoldenCross() {
        return goldenCross;
    }

    public void setGoldenCross(GoldenCross goldenCross) {
        this.goldenCross = goldenCross;
    }
}
