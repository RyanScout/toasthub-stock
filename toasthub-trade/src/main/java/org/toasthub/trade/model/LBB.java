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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_LBB")
// Lower Bollinger Band
public class LBB extends BaseAlg {

	private static final long serialVersionUID = 1L;
	private BigDecimal standardDeviations = BigDecimal.ZERO;

	// Constructors
	public LBB() {
		super();
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("LBB");
	}

	public LBB(final String symbol) {
		super();
		this.setSymbol(symbol);
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("LBB");
	}

	public LBB(final String code, final Boolean defaultLang, final String dir) {
		this.setActive(true);
		this.setArchive(false);
		this.setLocked(false);
		this.setCreated(Instant.now());
		this.setIdentifier("LBB");
	}

	// Setter/Getter
	@JsonView({ View.Member.class })
	@Column(name = "standard_deviations")
	public BigDecimal getStandardDeviations() {
		return standardDeviations;
	}

	public void setStandardDeviations(final BigDecimal standardDeviations) {
		this.standardDeviations = standardDeviations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((standardDeviations == null) ? 0 : standardDeviations.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final LBB other = (LBB) obj;
		if (standardDeviations == null) {
			if (other.standardDeviations != null)
				return false;
		} else if (!standardDeviations.equals(other.standardDeviations))
			return false;
		return true;
	}

	@Transient
	public static BigDecimal calculateLBB(final List<BigDecimal> list, final BigDecimal standardDeviations) {
		final BigDecimal sma = SMA.calculateSMA(list);

		return sma.subtract(
				SMA.calculateSD(list).multiply(standardDeviations));
	}

	@Transient
	public static BigDecimal calculateLBB(final List<BigDecimal> list, final BigDecimal sma,
			final BigDecimal standardDeviations) {
		return sma.subtract(
				SMA.calculateSD(list).multiply(standardDeviations));
	}

	public LBB configureLBB(final List<AssetMinute> assetMinutes) throws InsufficientDataException {
		final LBB configuredLBB = new LBB();

		configuredLBB.setSymbol(this.symbol);
		configuredLBB.setEvaluationPeriod(this.evaluationPeriod);
		configuredLBB.setEvaluationDuration(this.evaluationDuration);
		configuredLBB.setStandardDeviations(this.standardDeviations);

		// ensures there is enough data to configure SMA value
		if (assetMinutes.size() < configuredLBB.evaluationDuration) {
			throw new InsufficientDataException();
		}

		configuredLBB.setEpochSeconds(assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());

		final List<BigDecimal> values = assetMinutes
				.subList(assetMinutes.size() - evaluationDuration, assetMinutes.size())
				.stream()
				.map(assetMinute -> assetMinute.getValue())
				.toList();

		configuredLBB.setValue(
				calculateLBB(
						values,
						configuredLBB.standardDeviations));

		return configuredLBB;
	}

	public LBB configureLBB(final List<AssetMinute> assetMinutes, final BigDecimal smaValue)
			throws InsufficientDataException {
		final LBB configuredLBB = new LBB();

		configuredLBB.setSymbol(this.symbol);
		configuredLBB.setEvaluationPeriod(this.evaluationPeriod);
		configuredLBB.setEvaluationDuration(this.evaluationDuration);
		configuredLBB.setStandardDeviations(this.standardDeviations);

		// ensures there is enough data to configure SMA value
		if (assetMinutes.size() < configuredLBB.evaluationDuration) {
			throw new InsufficientDataException();
		}

		configuredLBB.setEpochSeconds(assetMinutes.get(assetMinutes.size() - 1).getEpochSeconds());

		final List<BigDecimal> values = assetMinutes
				.subList(assetMinutes.size() - evaluationDuration, assetMinutes.size())
				.stream()
				.map(assetMinute -> assetMinute.getValue())
				.toList();

		configuredLBB.setValue(
				calculateLBB(
						values,
						smaValue,
						configuredLBB.standardDeviations));

		return configuredLBB;
	}

	public LBB configureLBB(final List<AssetDay> assetDays, final AssetMinute assetMinute)
			throws InsufficientDataException {
		final LBB configuredLBB = new LBB();

		configuredLBB.setSymbol(this.symbol);
		configuredLBB.setEvaluationPeriod(this.evaluationPeriod);
		configuredLBB.setEvaluationDuration(this.evaluationDuration);
		configuredLBB.setStandardDeviations(this.standardDeviations);

		// ensures there is enough data to configure SMA value
		if (assetDays.size() < configuredLBB.evaluationDuration) {
			throw new InsufficientDataException();
		}

		configuredLBB.setEpochSeconds(assetMinute.getEpochSeconds());

		final List<BigDecimal> values = assetDays
				.subList(assetDays.size() - this.evaluationDuration, assetDays.size())
				.stream()
				.map(assetDay -> assetDay.getClose())
				.collect(Collectors.toCollection(ArrayList::new));

		// configures calculation of assetDay with minute based accuracy
		values.set(values.size() - 1, assetMinute.getValue());

		configuredLBB.setValue(
				calculateLBB(
						values,
						configuredLBB.standardDeviations));

		return configuredLBB;
	}

	public LBB configureLBB(final List<AssetDay> assetDays, final AssetMinute assetMinute, final BigDecimal smaValue)
			throws InsufficientDataException {
		final LBB configuredLBB = new LBB();

		configuredLBB.setSymbol(this.symbol);
		configuredLBB.setEvaluationPeriod(this.evaluationPeriod);
		configuredLBB.setEvaluationDuration(this.evaluationDuration);
		configuredLBB.setStandardDeviations(this.standardDeviations);

		// ensures there is enough data to configure SMA value
		if (assetDays.size() < configuredLBB.evaluationDuration) {
			throw new InsufficientDataException();
		}

		configuredLBB.setEpochSeconds(assetMinute.getEpochSeconds());

		final List<BigDecimal> values = assetDays
				.subList(assetDays.size() - this.evaluationDuration, assetDays.size())
				.stream()
				.map(assetDay -> assetDay.getClose())
				.collect(Collectors.toCollection(ArrayList::new));

		// configures calculation of assetDay with minute based accuracy
		values.set(values.size() - 1, assetMinute.getValue());

		configuredLBB.setValue(
				calculateLBB(
						values,
						smaValue,
						configuredLBB.standardDeviations));

		return configuredLBB;
	}

}
