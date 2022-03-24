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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "sa_trade")
public class Trade extends BaseEntity{

	private static final long serialVersionUID = 1L;
	
	protected String name;
	protected String stock;
	protected String orderType;
	protected BigDecimal amount;
	protected String algorithm;
	protected String runStatus;
	protected BigDecimal trailingStopPercent;
	protected BigDecimal profitLimit;
	private String frequency;
	private int frequencyExecuted;


	//Constructors
	public Trade() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
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

	public Trade(String code, Boolean defaultLang, String dir){
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("Trade");
	}

	// Methods
	@Column(name = "name")
	public String getName(){
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

	@Column(name = "order_type")
	public String getOrderType(){
		return orderType;
	}

	public void setOrderType(String orderType){
		this.orderType = orderType;
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

	@Column(name = "run_status")
	public String getRunStatus() {
		return runStatus;
	}
	public void setRunStatus(String runStatus) {
		this.runStatus = runStatus;
	}
	@Column(name = "trailing_stop_percent")
	public BigDecimal getTrailingStopPercent(){
		return trailingStopPercent;
	}
	public void setTrailingStopPercent(BigDecimal trailingStopPercent){
		this.trailingStopPercent = trailingStopPercent;
	}
	@Column(name = "profit_limit")
	public BigDecimal getProfitLimit(){
		return profitLimit;
	}
	public void setProfitLimit(BigDecimal profitLimit){
		this.profitLimit = profitLimit;
	}
}
