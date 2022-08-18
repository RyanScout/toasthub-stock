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

package org.toasthub.trade.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_trade")
public class Trade extends TradeBaseEntity {

	private static final long serialVersionUID = 1L;
	public static final String BOT = "BOT";
	public static final String BUY = "BUY";
	public static final String SELL = "SELL";
	public static final String[] SUPPORTED_ORDER_SIDES = {
			BOT, BUY, SELL
	};
	public static final String MARKET = "MARKET";
	public static final String TRAILING_STOP = "TRAILING STOP";
	public static final String PROFIT_LIMIT = "PROFIT LIMIT";
	public static final String TRAILING_STOP_PROFIT_LIMIT = "TRAILING STOP & PROFIT LIMIT";
	public static final String[] SUPPORTED_ORDER_TYPES = {
			MARKET, TRAILING_STOP, PROFIT_LIMIT, TRAILING_STOP_PROFIT_LIMIT
	};

	private String name = "";
	private String symbol = "";
	private String orderType = "";
	private String orderSide = "";
	private String status = "";
	private String evaluationPeriod = "";
	private String currencyType = "";
	private String parsedBuyCondition = "";
	private String parsedSellCondition = "";
	private String trailingStopType = "";
	private String profitLimitType = "";
	private String iterations = "";

	private int iterationsExecuted = 0;

	private BigDecimal trailingStopAmount = BigDecimal.ZERO;
	private BigDecimal profitLimitAmount = BigDecimal.ZERO;
	private BigDecimal currencyAmount = BigDecimal.ZERO;
	private BigDecimal budget = BigDecimal.ZERO;
	private BigDecimal availableBudget = BigDecimal.ZERO;
	private BigDecimal sharesHeld = BigDecimal.ZERO;
	private BigDecimal totalValue = BigDecimal.ZERO;
	private BigDecimal firstCheckPrice = BigDecimal.ZERO;
	private BigDecimal lastCheckPrice = BigDecimal.ZERO;

	private long firstCheck = 0;
	private long lastCheck = 0;
	private long firstOrder = 0;
	private long lastOrder = 0;

	@Transient
	private String rawBuyCondition = "";
	@Transient
	private String rawSellCondition = "";

	private Set<TradeDetail> tradeDetails = new LinkedHashSet<TradeDetail>();

	// Constructors
	public Trade() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	public Trade(final String code, final Boolean defaultLang, final String dir) {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	// Setter/Getter

	@Override
	public Trade clone() {

		final Trade trade = new Trade();

		trade.setName(this.name);
		trade.setSymbol(this.symbol);
		trade.setOrderType(this.orderType);
		trade.setOrderSide(this.orderSide);
		trade.setStatus(this.status);
		trade.setEvaluationPeriod(this.evaluationPeriod);
		trade.setCurrencyType(this.currencyType);
		trade.setParsedBuyCondition(this.parsedBuyCondition);
		trade.setParsedSellCondition(this.parsedSellCondition);
		trade.setTrailingStopType(this.trailingStopType);
		trade.setProfitLimitType(this.profitLimitType);
		trade.setIterations(this.iterations);
		trade.setIterationsExecuted(this.iterationsExecuted);
		trade.setTrailingStopAmount(this.trailingStopAmount);
		trade.setProfitLimitAmount(this.profitLimitAmount);
		trade.setCurrencyAmount(this.currencyAmount);
		trade.setBudget(this.budget);
		trade.setAvailableBudget(this.availableBudget);
		trade.setSharesHeld(this.sharesHeld);
		trade.setTotalValue(trade.totalValue);
		trade.setFirstOrder(trade.firstOrder);
		trade.setLastOrder(trade.lastOrder);

		return trade;
	}

	@JsonView({})
	@Column(name = "parsed_buy_condition")
	public String getParsedBuyCondition() {
		return parsedBuyCondition;
	}

	public void setParsedBuyCondition(final String parsedBuyCondition) {
		this.parsedBuyCondition = parsedBuyCondition;
	}

	@JsonView({})
	@Column(name = "parsed_sell_condition")
	public String getParsedSellCondition() {
		return parsedSellCondition;
	}

	public void setParsedSellCondition(final String parsedSellCondition) {
		this.parsedSellCondition = parsedSellCondition;
	}

	@JsonView({ View.Member.class })
	@Transient
	public String getRawBuyCondition() {
		return rawBuyCondition;
	}

	public void setRawBuyCondition(final String rawBuyCondition) {
		this.rawBuyCondition = rawBuyCondition;
	}

	@JsonView({ View.Member.class })
	@Transient
	public String getRawSellCondition() {
		return rawSellCondition;
	}

	public void setRawSellCondition(final String rawSellCondition) {
		this.rawSellCondition = rawSellCondition;
	}

	@JsonView({ View.Member.class })
	@Column(name = "last_order")
	public long getLastOrder() {
		return lastOrder;
	}

	public void setLastOrder(final long lastOrder) {
		this.lastOrder = lastOrder;
	}

	@JsonView({ View.Member.class })
	@Column(name = "first_order")
	public long getFirstOrder() {
		return firstOrder;
	}

	public void setFirstOrder(final long firstOrder) {
		this.firstOrder = firstOrder;
	}

	@JsonView({ View.Member.class })
	@Column(name = "iterations_executed")
	public int getIterationsExecuted() {
		return iterationsExecuted;
	}

	public void setIterationsExecuted(final int iterationsExecuted) {
		this.iterationsExecuted = iterationsExecuted;
	}

	@JsonView({ View.Member.class })
	@Column(name = "iterations")
	public String getIterations() {
		return iterations;
	}

	public void setIterations(final String iterations) {
		this.iterations = iterations;
	}

	@JsonView({ View.Member.class })
	@Column(name = "total_value")
	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(final BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	@JsonView({})
	@OneToMany(mappedBy = "trade", cascade = CascadeType.ALL)
	public Set<TradeDetail> getTradeDetails() {
		return tradeDetails;
	}

	public void setTradeDetails(final Set<TradeDetail> tradeDetails) {
		this.tradeDetails = tradeDetails;
	}

	@JsonView({ View.Member.class })
	@Column(name = "evaluation_period")
	public String getEvaluationPeriod() {
		return evaluationPeriod;
	}

	public void setEvaluationPeriod(final String evaluationPeriod) {
		this.evaluationPeriod = evaluationPeriod;
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
	@Column(name = "budget")
	public BigDecimal getBudget() {
		return budget;
	}

	public void setBudget(final BigDecimal budget) {
		this.budget = budget;
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
	@Column(name = "status")
	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	@JsonView({ View.Member.class })
	@Column(name = "symbol")
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(final String symbol) {
		this.symbol = symbol;
	}

	@JsonView({ View.Member.class })
	@Column(name = "profit_limit_amount")
	public BigDecimal getProfitLimitAmount() {
		return profitLimitAmount;
	}

	public void setProfitLimitAmount(final BigDecimal profitLimitAmount) {
		this.profitLimitAmount = profitLimitAmount;
	}

	@JsonView({ View.Member.class })
	@Column(name = "trailing_stop_amount")
	public BigDecimal getTrailingStopAmount() {
		return trailingStopAmount;
	}

	public void setTrailingStopAmount(final BigDecimal trailingStopAmount) {
		this.trailingStopAmount = trailingStopAmount;
	}

	@JsonView({ View.Member.class })
	@Column(name = "profit_limit_type")
	public String getProfitLimitType() {
		return profitLimitType;
	}

	public void setProfitLimitType(final String profitLimitType) {
		this.profitLimitType = profitLimitType;
	}

	@JsonView({ View.Member.class })
	@Column(name = "trailing_stop_type")
	public String getTrailingStopType() {
		return trailingStopType;
	}

	public void setTrailingStopType(final String trailingStopType) {
		this.trailingStopType = trailingStopType;
	}

	@JsonView({ View.Member.class })
	@Column(name = "currency_type")
	public String getCurrencyType() {
		return currencyType;
	}

	public void setCurrencyType(final String currencyType) {
		this.currencyType = currencyType;
	}

	@JsonView({ View.Member.class })
	@Column(name = "currency_amount")
	public BigDecimal getCurrencyAmount() {
		return currencyAmount;
	}

	public void setCurrencyAmount(final BigDecimal currencyAmount) {
		this.currencyAmount = currencyAmount;
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
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@JsonView({ View.Member.class })
	@Column(name = "order_type")
	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(final String orderType) {
		this.orderType = orderType;
	}

	@JsonView({ View.Member.class })
	@Column(name = "first_check_price")
	public BigDecimal getFirstCheckPrice() {
		return firstCheckPrice;
	}

	public void setFirstCheckPrice(BigDecimal firstCheckPrice) {
		this.firstCheckPrice = firstCheckPrice;
	}

	@JsonView({ View.Member.class })
	@Column(name = "last_check_price")
	public BigDecimal getLastCheckPrice() {
		return lastCheckPrice;
	}

	public void setLastCheckPrice(BigDecimal lastCheckPrice) {
		this.lastCheckPrice = lastCheckPrice;
	}

	@JsonView({ View.Member.class })
	@Column(name = "first_check")
	public long getFirstCheck() {
		return firstCheck;
	}

	public void setFirstCheck(long firstCheck) {
		this.firstCheck = firstCheck;
	}

	@JsonView({ View.Member.class })
	@Column(name = "last_check")
	public long getLastCheck() {
		return lastCheck;
	}

	public void setLastCheck(long lastCheck) {
		this.lastCheck = lastCheck;
	}
}
