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

package org.toasthub.trade.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.AssetDay;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.LBB;
import org.toasthub.trade.model.SMA;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.UBB;

@Repository("TAAlgorithmCruncherDao")
@Transactional("TransactionManagerData")
public class AlgorithmCruncherDaoImpl implements AlgorithmCruncherDao {

	@Autowired
	protected EntityManagerDataSvc entityManagerDataSvc;

	@Override
	public void delete(final RestRequest request, final RestResponse response) throws Exception {
	}

	@Override
	public void save(final RestRequest request, final RestResponse response) throws Exception {
		entityManagerDataSvc.getInstance().merge((Object) request.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void saveObject(final Object object) {
		entityManagerDataSvc.getInstance().merge(object);
	}

	@Override
	public void saveList(final List<Object> list) {
		for (final Object o : list) {
			entityManagerDataSvc.getInstance().merge(o);
		}
	}

	@Override
	public void items(final RestRequest request, final RestResponse response) throws Exception {
	}

	@Override
	public List<AssetDay> getAssetDays(final String symbol, final long startingEpochSeconds,
			final long endingEpochSeconds) {
		final String queryStr = "SELECT DISTINCT x FROM AssetDay AS x WHERE x.symbol =:symbol AND x.epochSeconds >=: startingEpochSeconds AND x.epochSeconds <=:endingEpochSeconds";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("symbol", symbol)
				.setParameter("startingEpochSeconds", startingEpochSeconds)
				.setParameter("endingEpochSeconds", endingEpochSeconds);

		final List<AssetDay> assetDays = new ArrayList<AssetDay>();

		for (final Object o : query.getResultList()) {
			assetDays.add(AssetDay.class.cast(o));
		}
		return assetDays;
	}

	@Override
	public List<AssetMinute> getAssetMinutes(final String symbol, final long startingEpochSeconds,
			final long endingEpochSeconds) {
		final String queryStr = "SELECT DISTINCT x FROM AssetMinute AS x WHERE x.symbol =:symbol AND x.epochSeconds >=: startingEpochSeconds AND x.epochSeconds <=:endingEpochSeconds";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("symbol", symbol);
		query.setParameter("startingEpochSeconds", startingEpochSeconds);
		query.setParameter("endingEpochSeconds", endingEpochSeconds);

		final List<AssetMinute> assetMinutes = new ArrayList<AssetMinute>();

		for (final Object o : query.getResultList()) {
			assetMinutes.add(AssetMinute.class.cast(o));
		}
		return assetMinutes;
	}

	@Override
	public void itemCount(final RestRequest request, final RestResponse response) throws Exception {

		String x = "";
		switch ((String) request.getParam(TradeConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			case "CONFIGURATION":
				x = "Configuration";
				break;
			default:
				break;
		}

		String queryStr = "SELECT COUNT(DISTINCT x) FROM " + x + " AS x ";

		boolean and = false;
		if (request.containsParam(TradeConstant.EPOCH_SECONDS)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.EPOCH_SECONDS =:EPOCH_SECONDS ";
			and = true;
		}
		if (request.containsParam(TradeConstant.SYMBOL)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.symbol =:symbol ";
			and = true;
		}
		if (request.containsParam(TradeConstant.EVALUATION_PERIOD)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.evaluationPeriod =:evaluationPeriod ";
			and = true;
		}
		if (request.containsParam(TradeConstant.EVALUATION_DURATION)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.evaluationDuration =:evaluationDuration ";
			and = true;
		}
		if (x.equals("LBB") || x.equals("UBB")) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.standardDeviations =:standardDeviations ";
			and = true;
		}

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

		if (request.containsParam(TradeConstant.EPOCH_SECONDS)) {
			query.setParameter("EPOCH_SECONDS", (long) request.getParam(TradeConstant.EPOCH_SECONDS));
		}
		if (request.containsParam(TradeConstant.EVALUATION_PERIOD)) {
			query.setParameter("evaluationPeriod", (String) request.getParam(TradeConstant.EVALUATION_PERIOD));
		}
		if (request.containsParam(TradeConstant.EVALUATION_DURATION)) {
			query.setParameter("evaluationDuration", (String) request.getParam(TradeConstant.EVALUATION_DURATION));
		}
		if (request.containsParam(TradeConstant.SYMBOL)) {
			query.setParameter("symbol", (String) request.getParam(TradeConstant.SYMBOL));
		}
		if (x.equals("LBB") || x.equals("UBB")) {
			query.setParameter("standardDeviations", (BigDecimal) request.getParam("STANDARD_DEVIATIONS"));
		}

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);
	}

	@Override
	public void item(final RestRequest request, final RestResponse response) throws NoResultException {

		String x = "";
		switch ((String) request.getParam(TradeConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			case "AssetDay":
				x = "AssetDay";
				break;
			case "AssetMinute":
				x = "AssetMinute";
				break;
			case "CONFIGURATION":
				getConfiguration(request, response);
				return;
			default:
				break;
		}

		if (!Arrays.asList(Symbol.SYMBOLS).contains((String) request.getParam(TradeConstant.SYMBOL))) {
			System.out.println("request param symbol does not contain valid symbol at algorithm cruncher dao item");
			return;
		}

		final String queryStr = "SELECT DISTINCT x FROM " + x + " AS x"
				+ " WHERE x.EPOCH_SECONDS =:EPOCH_SECONDS"
				+ " AND x.symbol =:symbol";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("EPOCH_SECONDS", request.getParam(TradeConstant.EPOCH_SECONDS));
		query.setParameter("symbol", request.getParam(TradeConstant.SYMBOL));
		final Object result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public void initializedAssetDay(final RestRequest request, final RestResponse response) throws NoResultException {
		final String queryStr = "SELECT DISTINCT x FROM AssetDay" + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.symbol =:symbol";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(TradeConstant.EPOCH_SECONDS));
		query.setParameter("symbol", request.getParam(TradeConstant.SYMBOL));
		final AssetDay result = (AssetDay) query.getSingleResult();
		Hibernate.initialize(result.getAssetMinutes());

		response.addParam(GlobalConstant.ITEM, result);
	}

	@Override
	public Set<SMA> getSMAPrototypes() {
		final Set<SMA> smaSet = new HashSet<SMA>();

		final String queryStr = "Select DISTINCT x FROM TechnicalIndicator x WHERE x.technicalIndicatorType =: technicalIndicatorType";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("technicalIndicatorType", TechnicalIndicator.GOLDENCROSS);

		for (final Object o : query.getResultList()) {
			final TechnicalIndicator t = TechnicalIndicator.class.cast(o);
			final SMA shortSMA = new SMA();
			shortSMA.setSymbol(t.getSymbol());
			shortSMA.setEvaluationDuration(t.getShortSMAEvaluationDuration());
			shortSMA.setEvaluationPeriod(t.getEvaluationPeriod());

			final SMA longSMA = new SMA();
			longSMA.setSymbol(t.getSymbol());
			longSMA.setEvaluationDuration(t.getLongSMAEvaluationDuration());
			longSMA.setEvaluationPeriod(t.getEvaluationPeriod());

			smaSet.add(shortSMA);
			smaSet.add(longSMA);
		}

		return smaSet;
	}

	@Override
	public Set<LBB> getLBBPrototypes() {
		final Set<LBB> lbbSet = new HashSet<LBB>();

		final String queryStr = "Select DISTINCT x FROM TechnicalIndicator x WHERE x.technicalIndicatorType =: technicalIndicatorType";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("technicalIndicatorType", TechnicalIndicator.LOWERBOLLINGERBAND);

		for (final Object o : query.getResultList()) {
			final TechnicalIndicator t = TechnicalIndicator.class.cast(o);
			final LBB lbb = new LBB();
			lbb.setSymbol(t.getSymbol());
			lbb.setEvaluationDuration(t.getLbbEvaluationDuration());
			lbb.setEvaluationPeriod(t.getEvaluationPeriod());
			lbb.setStandardDeviations(t.getStandardDeviations());
			lbbSet.add(lbb);
		}

		return lbbSet;
	}

	@Override
	public Set<UBB> getUBBPrototypes() {
		final Set<UBB> ubbSet = new HashSet<UBB>();

		final String queryStr = "Select DISTINCT x FROM TechnicalIndicator x WHERE x.technicalIndicatorType =: technicalIndicatorType";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("technicalIndicatorType", TechnicalIndicator.UPPERBOLLINGERBAND);

		for (final Object o : query.getResultList()) {
			final TechnicalIndicator t = TechnicalIndicator.class.cast(o);
			final UBB ubb = new UBB();
			ubb.setSymbol(t.getSymbol());
			ubb.setEvaluationDuration(t.getUbbEvaluationDuration());
			ubb.setEvaluationPeriod(t.getEvaluationPeriod());
			ubb.setStandardDeviations(t.getStandardDeviations());
			ubbSet.add(ubb);
		}

		return ubbSet;
	}

	public void getConfiguration(final RestRequest request, final RestResponse response) {
		final String queryStr = "SELECT DISTINCT x FROM Configuration AS x";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		response.addParam(GlobalConstant.ITEM, query.getSingleResult());
	}

	@Override
	public void getEarliestAlgTime(final RestRequest request, final RestResponse response) {
		String x = "";
		switch ((String) request.getParam(TradeConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
			case "UBB":
				x = "UBB";
				break;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			default:
				return;
		}

		final String queryStr = "SELECT DISTINCT x.EPOCH_SECONDS FROM " + x
				+ " x WHERE x.symbol =: symbol AND x.evaluationPeriod =:evaluationPeriod AND x.evaluationDuration =:evaluationDuration ORDER BY x.EPOCH_SECONDS ASC";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("evaluationPeriod", request.getParam(TradeConstant.EVALUATION_PERIOD))
				.setParameter("evaluationDuration", request.getParam(TradeConstant.EVALUATION_DURATION))
				.setParameter("symbol", request.getParam(TradeConstant.SYMBOL))
				.setMaxResults(1);

		response.addParam(GlobalConstant.ITEM, query.getSingleResult());

	}

	@Override
	public void getTechicalIndicator(final RestRequest request, final RestResponse response) {
		if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
			final String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator AS x WHERE x.id =:id";
			final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

			if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
				query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			}

			if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
				query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
			}

			if (request.getParam(GlobalConstant.ITEMID) instanceof String) {
				query.setParameter("id", Long.valueOf((String) request.getParam(GlobalConstant.ITEMID)));
			}

			final TechnicalIndicator t = TechnicalIndicator.class.cast(query.getSingleResult());

			response.addParam(GlobalConstant.ITEM, t);

			return;
		}
	}

	@Override
	public TechnicalIndicator findTechnicalIndicatorById(final long id) {
		return entityManagerDataSvc.getInstance().find(TechnicalIndicator.class, id);
	}

	@Override
	public long getSMAItemCount(final String symbol, final String evaluationPeriod, final int evaluationDuration,
			final long epochSeconds) {
		final String queryStr = "SELECT COUNT(DISTINCT x) FROM SMA x WHERE x.symbol =: symbol AND x.evaluationPeriod =: evaluationPeriod AND x.evaluationDuration =: evaluationDuration AND x.epochSeconds =: epochSeconds";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("symbol", symbol);
		query.setParameter("evaluationPeriod", evaluationPeriod);
		query.setParameter("evaluationDuration", evaluationDuration);
		query.setParameter("epochSeconds", epochSeconds);

		return Long.class.cast(query.getSingleResult());
	}

	@Override
	public long getLBBItemCount(final String symbol, final String evaluationPeriod, final int evaluationDuration,
			final long epochSeconds, final BigDecimal standardDeviations) {
		final String queryStr = "SELECT COUNT(DISTINCT x) FROM LBB x WHERE x.symbol =: symbol AND x.evaluationPeriod =: evaluationPeriod AND x.evaluationDuration =: evaluationDuration AND x.epochSeconds =: epochSeconds AND x.standardDeviations =: standardDeviations";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("symbol", symbol);
		query.setParameter("evaluationPeriod", evaluationPeriod);
		query.setParameter("evaluationDuration", evaluationDuration);
		query.setParameter("epochSeconds", epochSeconds);
		query.setParameter("standardDeviations", standardDeviations);

		return Long.class.cast(query.getSingleResult());
	}

	@Override
	public long getUBBItemCount(final String symbol, final String evaluationPeriod, final int evaluationDuration,
			final long epochSeconds, final BigDecimal standardDeviations) {
		final String queryStr = "SELECT COUNT(DISTINCT x) FROM UBB x WHERE x.symbol =: symbol AND x.evaluationPeriod =: evaluationPeriod AND x.evaluationDuration =: evaluationDuration AND x.epochSeconds =: epochSeconds AND x.standardDeviations =: standardDeviations";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("symbol", symbol);
		query.setParameter("evaluationPeriod", evaluationPeriod);
		query.setParameter("evaluationDuration", evaluationDuration);
		query.setParameter("epochSeconds", epochSeconds);
		query.setParameter("standardDeviations", standardDeviations);

		return Long.class.cast(query.getSingleResult());
	}

	@Override
	public List<AssetMinute> getAssetMinutesWithoutSma(final String symbol, final long startTime, final long endTime,
			final String evaluationPeriod, final int evaluationDuration) {
		final List<AssetMinute> items = new ArrayList<AssetMinute>();

		final String queryStr = "SELECT DISTINCT assetMinute FROM AssetMinute as assetMinute"
				+ " WHERE assetMinute.epochSeconds > :startTime"
				+ " AND assetMinute.epochSeconds < :endTime"
				+ " AND assetMinute.symbol = :symbol"
				+ " AND "
				+ " (SELECT COUNT(DISTINCT sma) FROM SMA as sma "
				+ " WHERE sma.symbol = :symbol "
				+ " AND sma.evaluationDuration = :evaluationDuration"
				+ " AND sma.evaluationPeriod = :evaluationPeriod"
				+ " AND sma.epochSeconds = assetMinute.epochSeconds) = 0";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("startTime", startTime)
				.setParameter("endTime", endTime)
				.setParameter("symbol", symbol)
				.setParameter("evaluationPeriod", evaluationPeriod)
				.setParameter("evaluationDuration", evaluationDuration);

		for (final Object o : query.getResultList()) {
			items.add(AssetMinute.class.cast(o));
		}

		return items;
	}

	@Override
	public List<AssetMinute> getAssetMinutesWithoutLbb(final String symbol, final long startTime, final long endTime,
			final String evaluationPeriod, final int evaluationDuration, final BigDecimal standardDeviations) {
		final List<AssetMinute> items = new ArrayList<AssetMinute>();

		final String queryStr = "SELECT DISTINCT assetMinute FROM AssetMinute as assetMinute"
				+ " WHERE assetMinute.epochSeconds > :startTime"
				+ " AND assetMinute.epochSeconds < :endTime"
				+ " AND assetMinute.symbol = :symbol"
				+ " AND"
				+ " (SELECT COUNT(DISTINCT lbb) FROM LBB as lbb "
				+ " WHERE lbb.symbol = :symbol "
				+ " AND lbb.evaluationDuration = :evaluationDuration"
				+ " AND lbb.evaluationPeriod = :evaluationPeriod"
				+ " AND lbb.standardDeviations = :standardDeviations"
				+ " AND lbb.epochSeconds = assetMinute.epochSeconds) = 0";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("startTime", startTime)
				.setParameter("endTime", endTime)
				.setParameter("symbol", symbol)
				.setParameter("evaluationPeriod", evaluationPeriod)
				.setParameter("evaluationDuration", evaluationDuration)
				.setParameter("standardDeviations", standardDeviations);

		for (final Object o : query.getResultList()) {
			items.add(AssetMinute.class.cast(o));
		}

		return items;
	}

	@Override
	public List<AssetMinute> getAssetMinutesWithoutUbb(final String symbol, final long startTime, final long endTime,
			final String evaluationPeriod, final int evaluationDuration, final BigDecimal standardDeviations) {
		final List<AssetMinute> items = new ArrayList<AssetMinute>();

		final String queryStr = "SELECT DISTINCT assetMinute FROM AssetMinute as assetMinute"
				+ " WHERE assetMinute.epochSeconds > :startTime"
				+ " AND assetMinute.epochSeconds < :endTime"
				+ " AND assetMinute.symbol = :symbol"
				+ " AND"
				+ " (SELECT COUNT(DISTINCT ubb) FROM UBB as ubb "
				+ " WHERE ubb.symbol = :symbol "
				+ " AND ubb.evaluationDuration = :evaluationDuration"
				+ " AND ubb.evaluationPeriod = :evaluationPeriod"
				+ " AND ubb.standardDeviations = :standardDeviations"
				+ " AND ubb.epochSeconds = assetMinute.epochSeconds) = 0";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("startTime", startTime)
				.setParameter("endTime", endTime)
				.setParameter("symbol", symbol)
				.setParameter("evaluationPeriod", evaluationPeriod)
				.setParameter("evaluationDuration", evaluationDuration)
				.setParameter("standardDeviations", standardDeviations);

		for (final Object o : query.getResultList()) {
			items.add(AssetMinute.class.cast(o));
		}

		return items;
	}
}
