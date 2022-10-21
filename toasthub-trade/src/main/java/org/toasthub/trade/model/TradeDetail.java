package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "ta_trade_detail")
public class TradeDetail extends TradeBaseEntity {

    private static final long serialVersionUID = 1L;
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

    @Transient
    private String rawOrderCondition = "";

    // Constructors
    public TradeDetail() {
        super();
        this.setActive(true);
        this.setArchive(false);
        this.setLocked(false);
        this.setCreated(Instant.now());
        this.setIdentifier("TradeDetail");
    }

    // Setter/Getter
    @JsonView({ View.Member.class })
    @Column(name = "filled_at")
    public long getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(final long filledAt) {
        this.filledAt = filledAt;
    }

    @JsonView({ View.Member.class })
    @Column(name = "placed_at")
    public long getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(final long placedAt) {
        this.placedAt = placedAt;
    }

    @JsonView({ View.Member.class })
    @Column(name = "dollar_amount")
    public BigDecimal getDollarAmount() {
        return dollarAmount;
    }

    public void setDollarAmount(final BigDecimal dollarAmount) {
        this.dollarAmount = dollarAmount;
    }

    @JsonView({ View.Member.class })
    @Column(name = "share_amount")
    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(final BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    @JsonIgnore
    @ManyToOne(targetEntity = Trade.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id")
    public Trade getTrade() {
        return trade;
    }

    public void setTrade(final Trade trade) {
        this.trade = trade;
    }

    @JsonView({ View.Member.class })
    @Column(name = "asset_price")
    public BigDecimal getAssetPrice() {
        return assetPrice;
    }

    public void setAssetPrice(final BigDecimal assetPrice) {
        this.assetPrice = assetPrice;
    }

    @JsonView({ View.Member.class })
    @Column(name = "order_side")
    public String getOrderSide() {
        return orderSide;
    }

    public void setOrderSide(final String orderSide) {
        this.orderSide = orderSide;
    }

    @JsonView({ View.Member.class })
    @Column(name = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonView({ View.Member.class })
    @Column(name = "order_id")
    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(final String orderID) {
        this.orderID = orderID;
    }

    @JsonView({ View.Member.class })
    @Column(name = "total_value")
    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    @JsonView({ View.Member.class })
    @Column(name = "available_budget")
    public BigDecimal getAvailableBudget() {
        return availableBudget;
    }

    public void setAvailableBudget(final BigDecimal availableBudget) {
        this.availableBudget = availableBudget;
    }

    @JsonView({ View.Member.class })
    @Column(name = "shares_held")
    public BigDecimal getSharesHeld() {
        return sharesHeld;
    }

    public void setSharesHeld(final BigDecimal sharesHeld) {
        this.sharesHeld = sharesHeld;
    }

    @JsonView({ View.Member.class })
    @Column(name = "order_condition")
    public String getOrderCondition() {
        return orderCondition;
    }

    public void setOrderCondition(final String orderCondition) {
        this.orderCondition = orderCondition;
    }

    @JsonView({ View.Member.class })
    @Transient
    public String getRawOrderCondition() {
        return rawOrderCondition;
    }

    public void setRawOrderCondition(final String rawOrderCondition) {
        this.rawOrderCondition = rawOrderCondition;
    }

}
