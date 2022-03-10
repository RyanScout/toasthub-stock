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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.common.BaseEntity;


@Entity
@Table(name = "sa_historical_analysis")
public class HistoricalAnalysis extends BaseEntity {

	private static final long serialVersionUID = 1L;

	protected String name;
	protected String stock;
	protected BigDecimal buyAmount;
	protected BigDecimal sellAmount;
	protected String algorithm;
	protected BigDecimal trailingStopPercent;
	protected BigDecimal profitLimit;
	private BigDecimal moneySpent;
	private BigDecimal totalValue;
	private long startTime;
	private long endTime;
	private String type;
	private String stringedStartTime;
	private String stringedEndTime;
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
		setStock((String) map.get("stock"));
		setAlgorithm((String) map.get("algorithm"));
		setType((String)map.get("type"));
        setBuyAmount(new BigDecimal((Integer) map.get("buyAmount")));
        setSellAmount(new BigDecimal((Integer) map.get("sellAmount")));
        setTrailingStopPercent(new BigDecimal((Double) map.get("trailingStopPercent")));
        setProfitLimit(new BigDecimal((Double) map.get("profitLimit")));
        setName((String) map.get("name"));
		setIdentifier("HistoricalAnalysis");
	}

	// Methods

	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Column(name = "stock")
	public String getStock() {
		return stock;
	}
	public void setStock(String stock) {
		this.stock = stock;
	}
	@Column(name = "buy_amount")
	public BigDecimal getBuyAmount() {
		return buyAmount;
	}
	public void setBuyAmount(BigDecimal buyAmount) {
		this.buyAmount = buyAmount;
	}
	@Column(name = "sell_amount")
	public BigDecimal getSellAmount() {
		return sellAmount;
	}
	public void setSellAmount(BigDecimal sellAmount) {
		this.sellAmount = sellAmount;
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
		setStringedEndTime(endTime);
	}
	@Column(name="start_time")
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
		setStringedStartTime(startTime);
	}
	@Column(name="type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	@Transient
    public String getStringedStartTime() {
        return stringedStartTime;
    }
    public void setStringedStartTime(long startTime) {
        Date date = new Date(startTime * 1000);
        DateFormat df = new SimpleDateFormat("dd MMM yyyy");
        this.stringedStartTime = df.format(date);
    }
    public void setStringedStartTime(String stringedStartTime){
        this.stringedStartTime = stringedStartTime;
    }
    @Transient
    public String getStringedEndTime() {
        return stringedEndTime;
    }
    public void setStringedEndTime(long endTime) {
        Date date = new Date(endTime * 1000);
        DateFormat df = new SimpleDateFormat("dd MMM yyyy");
        this.stringedEndTime = df.format(date);
    }
	public void setStringedEndTime(String stringedEndTime){
        this.stringedEndTime = stringedEndTime;
    }
	@OneToMany(mappedBy = "historicalAnalysis" , cascade = CascadeType.ALL)
	public Set<HistoricalDetail> getHistoricalDetails() {
		return historicalDetails;
	}
	public void setHistoricalDetails(Set<HistoricalDetail> historicalDetails) {
		this.historicalDetails = historicalDetails;
	}
}
