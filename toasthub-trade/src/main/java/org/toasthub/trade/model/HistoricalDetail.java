package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;


@Entity
@Table(name = "ta_historical_detail")
public class HistoricalDetail extends TradeBaseEntity {

	private static final long serialVersionUID = 1L;
    
	private HistoricalAnalysis historicalAnalysis;
    private long boughtAtTime;
    private BigDecimal boughtAt;
    private long soldAtTime;
    private BigDecimal soldAt;
    private BigDecimal highPrice;

    //Constructors
    public HistoricalDetail() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
        this.setIdentifier("HistoricalDetail");
	}
    public HistoricalDetail(String code, Boolean defaultLang, String dir) {
        super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
        this.setIdentifier("HistoricalDetail");
	}

    // Setter/Getter
    @JsonIgnore
    @ManyToOne(targetEntity = HistoricalAnalysis.class , fetch = FetchType.LAZY)
    @JoinColumn(name = "historical_analysis_id")
    public HistoricalAnalysis getHistoricalAnalysis() {
        return historicalAnalysis;
    }
    public void setHistoricalAnalysis(HistoricalAnalysis historicalAnalysis) {
        this.historicalAnalysis = historicalAnalysis;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "high_price")
    public BigDecimal getHighPrice() {
        return highPrice;
    }
    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "sold_at")
    public BigDecimal getSoldAt() {
        return soldAt;
    }
    public void setSoldAt(BigDecimal soldAt) {
        this.soldAt = soldAt;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "bought_at")
    public BigDecimal getBoughtAt() {
        return boughtAt;
    }
    public void setBoughtAt(BigDecimal boughtAt) {
        this.boughtAt = boughtAt;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "sold_at_time")
    public long getSoldAtTime() {
        return soldAtTime;
    }
    public void setSoldAtTime(long soldAtTime) {
        this.soldAtTime = soldAtTime;
    }
    
    @JsonView({View.Member.class})
    @Column(name = "bought_at_time")
    public long getBoughtAtTime() {
        return boughtAtTime;
    }
    public void setBoughtAtTime(long boughtAtTime) {
        this.boughtAtTime = boughtAtTime;
    }

}
