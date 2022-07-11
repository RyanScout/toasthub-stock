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
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@Entity
@Table(name = "ta_historical_analysis")
public class HistoricalAnalysis extends TradeBaseEntity {

	private static final long serialVersionUID = 1L;

	protected String name;
	private String symbol;
	private String orderType;
	protected BigDecimal amount;
	protected String algorithm;
	protected BigDecimal trailingStopPercent;
	protected BigDecimal profitLimit;
	private BigDecimal moneySpent;
	private BigDecimal totalValue;
	private long startTime;
	private long endTime;
	private String historicalAnalysisType;
	private Set<HistoricalDetail> historicalDetails;

	// Constructors
	public HistoricalAnalysis() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("HistoricalAnalysis");
	}
	public HistoricalAnalysis(String code, Boolean defaultLang, String dir) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("HistoricalAnalysis");
	}
	public HistoricalAnalysis(Map<String, ?> map){
		setSymbol((String) map.get("symbol"));
		setAlgorithm((String) map.get("algorithm"));
		setOrderType((String) map.get("orderType"));
		setHistoricalAnalysisType((String)map.get("type"));
        setAmount(new BigDecimal((Integer) map.get("amount")));
        setTrailingStopPercent(new BigDecimal((Double) map.get("trailingStopPercent")));
        setProfitLimit(new BigDecimal((Double) map.get("profitLimit")));
        setName((String) map.get("name"));
		setIdentifier("HistoricalAnalysis");
	}

	// Setter/Getter
	@Column(name = "symbol")
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Column(name = "historical_analysis_type")
	public String getHistoricalAnalysisType() {
		return historicalAnalysisType;
	}
	public void setHistoricalAnalysisType(String historicalAnalysisType) {
		this.historicalAnalysisType = historicalAnalysisType;
	}
	
	@Column(name = "order_type")
	public String getOrderType() {
		return orderType;
	}
	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	@Column(name = "algorithm")
	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	
	@Column(name = "trailing_stop_percent")
	public BigDecimal getTrailingStopPercent() {
		return trailingStopPercent;
	}
	public void setTrailingStopPercent(BigDecimal trailingStopPercent) {
		this.trailingStopPercent = trailingStopPercent;
	}
	
	@Column(name = "profit_limit")
	public BigDecimal getProfitLimit() {
		return profitLimit;
	}
	public void setProfitLimit(BigDecimal profitLimit) {
		this.profitLimit = profitLimit;
	}
	
	@Column(name = "total_value")
	public BigDecimal getTotalValue() {
		return totalValue;
	}
	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}
	
	@Column(name = "money_spent")
	public BigDecimal getMoneySpent() {
		return moneySpent;
	}
	public void setMoneySpent(BigDecimal moneySpent) {
		this.moneySpent = moneySpent;
	}
	
	@Column(name="end_time")
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	@Column(name="start_time")
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	@OneToMany(mappedBy = "historicalAnalysis" , cascade = CascadeType.ALL)
	public Set<HistoricalDetail> getHistoricalDetails() {
		return historicalDetails;
	}
	public void setHistoricalDetails(Set<HistoricalDetail> historicalDetails) {
		this.historicalDetails = historicalDetails;
	}
}
