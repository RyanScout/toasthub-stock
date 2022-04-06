package org.toasthub.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_trade_detail")
public class TradeDetail extends BaseEntity{
    private Trade trade;
    private String orderID;
    private String status;
    private String orderSide;
    private BigDecimal dollarAmount;
    private BigDecimal shareAmount;
    private BigDecimal assetPrice;
    private BigDecimal sharesHeld;
    private BigDecimal availableBudget;
    private BigDecimal totalValue;
    private long placedAt;
    private long filledAt;
    private String orderCondition;


    public TradeDetail() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("TradeDetail");
	}


   


    public long getFilledAt() {
        return filledAt;
    }





    public void setFilledAt(long filledAt) {
        this.filledAt = filledAt;
    }





    public long getPlacedAt() {
        return placedAt;
    }





    public void setPlacedAt(long placedAt) {
        this.placedAt = placedAt;
    }





    public BigDecimal getDollarAmount() {
        return dollarAmount;
    }





    public void setDollarAmount(BigDecimal dollarAmount) {
        this.dollarAmount = dollarAmount;
    }





    public BigDecimal getShareAmount() {
        return shareAmount;
    }





    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }





    @JsonIgnore
    @ManyToOne(targetEntity = Trade.class , fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    public Trade getTrade() {
        return trade;
    }
    
    public void setTrade(Trade trade){
        this.trade = trade;
    }

    public BigDecimal getAssetPrice() {
        return assetPrice;
    }
    public String getOrderSide() {
        return orderSide;
    }
    public void setOrderSide(String orderSide) {
        this.orderSide = orderSide;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    @Column(name = "order_id")
    public String getOrderID() {
        return orderID;
    }
    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    public BigDecimal getAvailableBudget() {
        return availableBudget;
    }
    public void setAvailableBudget(BigDecimal availableBudget) {
        this.availableBudget = availableBudget;
    }
    public BigDecimal getSharesHeld() {
        return sharesHeld;
    }
    public void setSharesHeld(BigDecimal sharesHeld) {
        this.sharesHeld = sharesHeld;
    }
    public String getOrderCondition() {
        return orderCondition;
    }
    public void setOrderCondition(String orderCondition) {
        this.orderCondition = orderCondition;
    }
    public void setAssetPrice(BigDecimal assetPrice) {
        this.assetPrice = assetPrice;
    }
}
