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
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "ta_SMA")
// Simple Moving Average
public class SMA extends BaseAlg {

	private static final long serialVersionUID = 1L;

	// Constructors
	public SMA() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");
	}

	public SMA(final String symbol) {
		super();
		setSymbol(symbol);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");
	}

	public SMA(final String code, final Boolean defaultLang, final String dir) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("SMA");

	}

	// Methods
	@Transient
	public static BigDecimal calculateSMA(final List<BigDecimal> list) {
		BigDecimal sma = BigDecimal.ZERO;
		for (int i = 0; i < list.size(); i++)
			sma = sma.add(list.get(i));
		return sma.divide(new BigDecimal(list.size()), MathContext.DECIMAL32);
	}

	@Transient
	public static BigDecimal calculateSD(final List<BigDecimal> list) {
		double sum = 0.0, standardDeviation = 0.0;
		final int length = list.size();

		for (int i = 0; i < length; i++) {
			sum += list.get(i).doubleValue();
		}
		final double mean = sum / length;

		for (int i = 0; i < length; i++) {
			standardDeviation += Math.pow(list.get(i).doubleValue() - mean, 2);
		}
		return BigDecimal.valueOf(Math.sqrt(standardDeviation / length));
	}

	public SMA configureSMA(final List<AssetMinute> assetMinutes) throws Exception {

		final SMA configuredSMA = new SMA();

		configuredSMA.setSymbol(this.symbol);
		configuredSMA.setEvaluationPeriod(this.evaluationPeriod);
		configuredSMA.setEvaluationDuration(this.evaluationDuration);

		// ensures there is enough data to configure SMA value
		if (assetMinutes.size() < configuredSMA.getEvaluationDuration()) {
			throw new Exception("Insufficient data to configure SMA.");
		}

		configuredSMA.setEpochSeconds(assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());

		final List<BigDecimal> values = assetMinutes
				.subList(assetMinutes.size() - evaluationDuration, assetMinutes.size())
				.stream()
				.map(assetMinute -> assetMinute.getValue())
				.toList();

		configuredSMA.setValue(calculateSMA(values));
		return configuredSMA;
	}

	public SMA configureSMA(final List<AssetDay> assetDays, final AssetMinute assetMinute)
			throws Exception {

		final SMA configuredSMA = new SMA();

		configuredSMA.setSymbol(this.symbol);
		configuredSMA.setEvaluationPeriod(this.evaluationPeriod);
		configuredSMA.setEvaluationDuration(this.evaluationDuration);

		// ensures there is enough data to configure SMA value
		if (assetDays.size() < configuredSMA.getEvaluationDuration()) {
			throw new Exception("Insufficient data to configure SMA.");
		}

		configuredSMA.setEpochSeconds(assetMinute.getEpochSeconds());
		configuredSMA.setCorrespondingDay(assetDays.get(assetDays.size() - 1).getEpochSeconds());

		final List<BigDecimal> values = assetDays
				.subList(assetDays.size() - this.evaluationDuration, assetDays.size())
				.stream()
				.map(assetDay -> assetDay.getClose())
				.collect(Collectors.toCollection(ArrayList::new));

		// configures calculation of assetDay with minute based accuracy
		values.set(values.size() - 1, assetMinute.getValue());

		configuredSMA.setValue(calculateSMA(values));
		return configuredSMA;
	}

	public SMA withSymbol(String symbol) {
		final SMA clonedSMA = cloneSMA(this);
		clonedSMA.setSymbol(symbol);
		return clonedSMA;
	}

	public SMA cloneSMA(SMA sma) {
		final SMA clonedSMA = new SMA();
		clonedSMA.setSymbol(sma.getSymbol());
		clonedSMA.setEvaluationPeriod(sma.getEvaluationPeriod());
		clonedSMA.setEvaluationDuration(sma.getEvaluationDuration());
		clonedSMA.setValue(sma.getValue());
		return clonedSMA;
	}

}
