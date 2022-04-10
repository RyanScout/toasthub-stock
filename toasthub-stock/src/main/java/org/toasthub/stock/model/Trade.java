/*
 * Copyright (C) 2020 The ToastHub Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Edward H. Seufert
 */

package org.toasthub.stock.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_trade")
public class Trade extends BaseEntity {

	private static final long serialVersionUID = 1L;

	protected String name;
	private String symbol;
	protected String orderType;
	private String orderSide;
	private String currencyType;
	private BigDecimal currencyAmount;
	private String buyCondition;
	private String sellCondition;
	private String status;
	private String trailingStopType;
	private String profitLimitType;
	private BigDecimal trailingStopAmount;
	private BigDecimal profitLimitAmount;
	private String frequency;
	private int frequencyExecuted;
	private BigDecimal budget;
	private BigDecimal availableBudget;
	private BigDecimal sharesHeld;
	private BigDecimal totalValue;
	private String recentBuyOrderID;
	private String recentSellOrderID;
	private String evaluationPeriod;
	private Set<TradeDetail> tradeDetails;

	// Constructors
	public Trade() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	@OneToMany(mappedBy = "trade", cascade = CascadeType.ALL)
	public Set<TradeDetail> getTradeDetails() {
		return tradeDetails;
	}

	public void setTradeDetails(Set<TradeDetail> tradeDetails) {
		this.tradeDetails = tradeDetails;
	}

	public String getEvaluationPeriod() {
		return evaluationPeriod;
	}

	public void setEvaluationPeriod(String evaluationPeriod) {
		this.evaluationPeriod = evaluationPeriod;
	}

	@Column(name = "recent_sell_order_id")
	public String getRecentSellOrderID() {
		return recentSellOrderID;
	}

	public void setRecentSellOrderID(String recentSellOrderID) {
		this.recentSellOrderID = recentSellOrderID;
	}

	@Column(name = "recent_buy_order_id")
	public String getRecentBuyOrderID() {
		return recentBuyOrderID;
	}

	public void setRecentBuyOrderID(String recentBuyOrderID) {
		this.recentBuyOrderID = recentBuyOrderID;
	}

	public BigDecimal getSharesHeld() {
		return sharesHeld;
	}

	public void setSharesHeld(BigDecimal sharesHeld) {
		this.sharesHeld = sharesHeld;
	}

	public BigDecimal getBudget() {
		return budget;
	}

	public void setBudget(BigDecimal budget) {
		this.budget = budget;
		if (availableBudget == null) {
			this.availableBudget = budget;
			this.totalValue = budget;
		}
		if (sharesHeld == null)
			this.sharesHeld = BigDecimal.ZERO;
	}

	public BigDecimal getAvailableBudget() {
		return availableBudget;
	}

	public void setAvailableBudget(BigDecimal availableBudget) {
		this.availableBudget = availableBudget;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getProfitLimitAmount() {
		return profitLimitAmount;
	}

	public void setProfitLimitAmount(BigDecimal profitLimitAmount) {
		this.profitLimitAmount = profitLimitAmount;
	}

	public BigDecimal getTrailingStopAmount() {
		return trailingStopAmount;
	}

	public void setTrailingStopAmount(BigDecimal trailingStopAmount) {
		this.trailingStopAmount = trailingStopAmount;
	}

	public String getProfitLimitType() {
		return profitLimitType;
	}

	public void setProfitLimitType(String profitLimitType) {
		this.profitLimitType = profitLimitType;
	}

	public String getTrailingStopType() {
		return trailingStopType;
	}

	public void setTrailingStopType(String trailingStopType) {
		this.trailingStopType = trailingStopType;
	}

	public String getCurrencyType() {
		return currencyType;
	}

	public void setCurrencyType(String currencyType) {
		this.currencyType = currencyType;
	}

	public String getSellCondition() {
		return sellCondition;
	}

	public void setSellCondition(String sellCondition) {
		this.sellCondition = sellCondition;
	}

	public String getBuyCondition() {
		return buyCondition;
	}

	public void setBuyCondition(String buyCondition) {
		this.buyCondition = buyCondition;
	}

	public BigDecimal getCurrencyAmount() {
		return currencyAmount;
	}

	public void setCurrencyAmount(BigDecimal currencyAmount) {
		this.currencyAmount = currencyAmount;
	}

	public String getOrderSide() {
		return orderSide;
	}

	public void setOrderSide(String orderSide) {
		this.orderSide = orderSide;
	}

	@Column(name = "frequency_executed")
	public int getFrequencyExecuted() {
		return frequencyExecuted;
	}

	public void setFrequencyExecuted(int frequencyExecuted) {
		this.frequencyExecuted = frequencyExecuted;
	}

	@Column(name = "frequency")
	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	public Trade(String code, Boolean defaultLang, String dir) {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	// Methods
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "order_type")
	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}
}
