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

package org.toasthub.trade.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.TradeDetail;

@Repository("TATradeDao")
@Transactional("TransactionManagerData")
public class TradeDaoImpl implements TradeDao {

	@Autowired
	protected EntityManagerDataSvc entityManagerDataSvc;

	@Override
	public void delete(final RestRequest request, final RestResponse response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			final Trade trade = (Trade) entityManagerDataSvc.getInstance().getReference(Trade.class,
					Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			entityManagerDataSvc.getInstance().remove(trade);

		} else {
			// utilSvc.addStatus(Response.ERROR, Response.ACTIONFAILED, "Missing ID",
			// response);
		}
	}

	@Override
	public void save(final RestRequest request, final RestResponse response) throws Exception {
		final Trade trade = (Trade) request.getParam(GlobalConstant.ITEM);
		entityManagerDataSvc.getInstance().merge(trade);
	}

	@Override
	public void items(final RestRequest request, final RestResponse response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM Trade AS x ";

		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) {
				queryStr += " WHERE ";
			}
			queryStr += "x.active =:active ";
			and = true;
		}
		if (request.containsParam("RUNSTATUS")) {
			if (!and) {
				queryStr += " WHERE ";
			}
			queryStr += "x.runStatus =:runStatus ";
			and = true;
		}

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		}
		if (request.containsParam("RUNSTATUS")) {
			query.setParameter("runStatus", (String) request.getParam("RUNSTATUS"));
		}

		response.addParam(TradeConstant.TRADES, query.getResultList());
	}

	@Override
	public void itemCount(final RestRequest request, final RestResponse response) throws Exception {
		String queryStr = "SELECT COUNT(DISTINCT x) FROM Trade as x ";
		boolean and = false;
		if (request.containsParam(GlobalConstant.ACTIVE)) {
			if (!and) {
				queryStr += " WHERE ";
			}
			queryStr += "x.active =:active ";
			and = true;
		}
		if (request.containsParam("RUNSTATUS")) {
			if (!and) {
				queryStr += " WHERE ";
			}
			queryStr += "x.runStatus =:runStatus ";
			and = true;
		}
		if (request.containsParam("NAME")) {
			if (!and) {
				queryStr += " WHERE ";
			}
			queryStr += "x.name =:name ";
			and = true;
		}

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		}
		if (request.containsParam("RUNSTATUS")) {
			query.setParameter("runStatus", (String) request.getParam("RUNSTATUS"));
		}
		if (request.containsParam("NAME")) {
			query.setParameter("name", request.getParam("NAME"));
		}

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);

	}

	@Override
	public void item(final RestRequest request, final RestResponse response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {
			final String queryStr = "SELECT x FROM Trade AS x WHERE x.id =:id";
			final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

			if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
				query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
			}
			if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
				query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			}
			final Trade trade = (Trade) query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, trade);
		} else {
			// utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED,
			// prefCacheUtil.getPrefText("GLOBAL_SERVICE",
			// "GLOBAL_SERVICE_MISSING_ID",prefCacheUtil.getLang(request)), response);
		}
	}

	@Override
	public List<Trade> getRunningTrades() {
		final String queryStr = "SELECT DISTINCT x FROM Trade AS x WHERE x.status =:status";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("status", "Running");

		final List<Trade> trades = new ArrayList<Trade>();
		final List<?> objects = query.getResultList();
		for (final Object o : objects) {
			final Trade t = Trade.class.cast(o);
			Hibernate.initialize(t.getTradeDetails());
			trades.add(t);
		}

		return trades;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Trade> getAllRunningTrades() {
		final String queryStr = "SELECT DISTINCT x FROM Trade AS x LEFT JOIN FETCH x.tradeDetails AS d WHERE x.status =:status AND d.status !=:detailStatus";
		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("status", "Running");
		query.setParameter("detailStatus", "FILLED");
		final List<Trade> trades = (List<Trade>) query.getResultList();
		return trades;
	}

	public void resetTrade(final long itemId) {
		final Trade trade = (Trade) entityManagerDataSvc.getInstance().getReference(Trade.class, itemId);
		trade.getTradeDetails().stream().forEach(t -> {
			entityManagerDataSvc.getInstance().remove(t);
		});

		final Set<TradeDetail> trades = new LinkedHashSet<TradeDetail>();
		trade.setTradeDetails(trades);
		trade.setAvailableBudget(trade.getBudget());
		trade.setTotalValue(trade.getBudget());
		trade.setSharesHeld(BigDecimal.ZERO);
		trade.setIterationsExecuted(0);
		trade.setFirstCheck(0);
		trade.setFirstOrder(0);
		trade.setFirstCheckPrice(BigDecimal.ZERO);
		trade.setLastCheck(0);
		trade.setLastOrder(0);
		trade.setLastCheckPrice(BigDecimal.ZERO);
		entityManagerDataSvc.getInstance().merge(trade);
	}

	@Override
	public List<Trade> getRunningDayTrades() {
		final String queryStr = "SELECT DISTINCT x FROM Trade AS x WHERE x.status =:status AND x.evaluationPeriod =:evaluationPeriod";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		query.setParameter("status", "Running");
		query.setParameter("evaluationPeriod", "DAY");

		final List<Trade> trades = new ArrayList<Trade>();
		final List<?> objects = query.getResultList();
		for (final Object o : objects) {
			final Trade t = Trade.class.cast(o);
			Hibernate.initialize(t.getTradeDetails());
			trades.add(t);
		}

		return trades;
	}

	@Override
	public void saveItem(final Object o) {
		entityManagerDataSvc.getInstance().merge(o);
	}

	public Trade findTradeById(final long id) {
		return entityManagerDataSvc.getInstance().find(Trade.class, id);
	}

	@Override
	public List<Trade> getTrades() {
		final String queryStr = "SELECT DISTINCT x FROM Trade AS x"
				+ " WHERE x.status != :status";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("status", "HISTORICAL_ANALYSIS");

		final List<Trade> items = new ArrayList<Trade>();

		for (final Object o : query.getResultList()) {
			items.add(Trade.class.cast(o));
		}
		return items;
	}

	@Override
	public List<TradeDetail> getTradeDetails(final Trade trade) {
		final String queryStr = "SELECT DISTINCT(x) FROM TradeDetail x WHERE x.trade =: trade";

		final List<TradeDetail> tradeDetails = new ArrayList<TradeDetail>();

		final Query query = entityManagerDataSvc.getInstance()
				.createQuery(queryStr)
				.setParameter("trade", trade);

		for (final Object o : query.getResultList()) {
			tradeDetails.add(TradeDetail.class.cast(o));
		}

		return tradeDetails;
	}

	@Override
	public CustomTechnicalIndicator getCustomTechnicalIndicatorById(final long id) {
		return entityManagerDataSvc.getInstance().find(CustomTechnicalIndicator.class, id);
	}

	public TechnicalIndicator getTechnicalIndicatorByProperties(final String symbol, final String evaluationPeriod,
			final String technicalIndicatorKey, final String technicalIndicatorType) {
		final String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator as x"
				+ " WHERE x.symbol = :symbol"
				+ " AND x.evaluationPeriod = :evaluationPeriod"
				+ " AND x.technicalIndicatorKey = :technicalIndicatorKey"
				+ " AND x.technicalIndicatorType = :technicalIndicatorType";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("symbol", symbol)
				.setParameter("evaluationPeriod", evaluationPeriod)
				.setParameter("technicalIndicatorKey", technicalIndicatorKey)
				.setParameter("technicalIndicatorType", technicalIndicatorType);

		final TechnicalIndicator item = TechnicalIndicator.class.cast(query.getSingleResult());

		return item;
	}

	public CustomTechnicalIndicator getCustomTechnicalIndicatorByProperties(
			final String evaluationPeriod,
			final String technicalIndicatorKey, final String technicalIndicatorType) {
		final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator as x"
				+ " WHERE x.evaluationPeriod = :evaluationPeriod"
				+ " AND x.technicalIndicatorKey = :technicalIndicatorKey"
				+ " AND x.technicalIndicatorType = :technicalIndicatorType";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("evaluationPeriod", evaluationPeriod)
				.setParameter("technicalIndicatorKey", technicalIndicatorKey)
				.setParameter("technicalIndicatorType", technicalIndicatorType);

		final CustomTechnicalIndicator item = CustomTechnicalIndicator.class.cast(query.getSingleResult());

		return item;
	}

	public Trade getTradeById(final long id) {
		return entityManagerDataSvc.getInstance().find(Trade.class, id);
	}

	public List<Object[]> getFilteredSymbolData(final String symbol, final long startTime, final long endTime,
			final int filterFactor) {
		final List<Object[]> items = new ArrayList<Object[]>();

		final String queryStr = "SELECT DISTINCT epochSeconds, value FROM"
				+ " ( SELECT ROW_NUMBER() OVER (ORDER BY x.epoch_seconds ASC) AS rowNumber,"
				+ " x.epoch_seconds as epochSeconds,"
				+ " x.value as value"
				+ " FROM tradeanalyzer_member.ta_asset_minute as x"
				+ " WHERE x.symbol = :symbol"
				+ " AND x.epoch_seconds >= :startTime"
				+ " AND x.epoch_seconds <= :endTime"
				+ " ) as organizedTable"
				+ " WHERE MOD(rowNumber , :filterFactor) = 0";

		final Query query = entityManagerDataSvc.getInstance().createNativeQuery(queryStr)
				.setParameter("symbol", symbol)
				.setParameter("startTime", startTime)
				.setParameter("endTime", endTime)
				.setParameter("filterFactor", filterFactor);

		for (final Object o : query.getResultList()) {
			items.add(Object[].class.cast(o));
		}

		return items;
	}

	public long getAssetMinuteCountWithinTimeFrame(final String symbol, final long startTime, final long endTime) {
		final String queryStr = "SELECT COUNT(DISTINCT x) FROM AssetMinute AS x"
				+ " WHERE x.symbol = :symbol"
				+ " AND x.epochSeconds >= : startTime"
				+ " AND x.epochSeconds <= : endTime";

		final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
				.setParameter("symbol", symbol)
				.setParameter("startTime", startTime)
				.setParameter("endTime", endTime);

		final long count = Long.class.cast(query.getSingleResult());
		return count;
	}

}
