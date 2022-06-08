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

	public static final String BOT = "Bot";
	public static final String BUY = "Buy";
	public static final String SELL = "Sell";
	public static final String[] SUPPORTED_ORDER_SIDES = {
			BOT, BUY, SELL
	};

	public static final String MARKET = "Market";
	public static final String TRAILING_STOP = "Trailing Stop";
	public static final String PROFIT_LIMIT = "Profit Limit";
	public static final String TRAILING_STOP_PROFIT_LIMIT = "Trailing Stop and Profit Limit";
	public static final String[] SUPPORTED_ORDER_TYPES = {
			MARKET, TRAILING_STOP, PROFIT_LIMIT, TRAILING_STOP_PROFIT_LIMIT
	};

	private String name;
	private String symbol;
	private String orderType;
	private String orderSide;
	private String status;
	private String evaluationPeriod;

	private String currencyType;
	private BigDecimal currencyAmount;

	private String buyCondition;
	private String parseableBuyCondition;

	private String sellCondition;
	private String parseableSellCondition;

	private String trailingStopType;
	private String profitLimitType;

	private BigDecimal trailingStopAmount;
	private BigDecimal profitLimitAmount;

	private String iterations;
	private int iterationsExecuted;

	private BigDecimal budget;
	private BigDecimal availableBudget;
	private BigDecimal sharesHeld;
	private BigDecimal totalValue;
	private String recentBuyOrderID;
	private String recentSellOrderID;
	private long firstBuy;

	private Set<TradeDetail> tradeDetails;

	// Constructors
	public Trade() {
		super();
		this.setFirstBuy(0);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	public String getParseableSellCondition() {
		return parseableSellCondition;
	}

	public void setParseableSellCondition(String parseableSellCondition) {
		this.parseableSellCondition = parseableSellCondition;
	}

	public String getParseableBuyCondition() {
		return parseableBuyCondition;
	}

	public void setParseableBuyCondition(String parseableBuyCondition) {
		this.parseableBuyCondition = parseableBuyCondition;
	}

	public int getIterationsExecuted() {
		return iterationsExecuted;
	}

	public void setIterationsExecuted(int iterationsExecuted) {
		this.iterationsExecuted = iterationsExecuted;
	}

	public String getIterations() {
		return iterations;
	}

	public void setIterations(String iterations) {
		this.iterations = iterations;
	}

	public long getFirstBuy() {
		return firstBuy;
	}

	public void setFirstBuy(long firstBuy) {
		this.firstBuy = firstBuy;
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
		this.availableBudget = budget;
		this.totalValue = budget;
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
