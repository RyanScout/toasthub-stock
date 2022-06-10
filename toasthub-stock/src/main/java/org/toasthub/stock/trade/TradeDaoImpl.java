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

package org.toasthub.stock.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.model.Symbol;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("TradeDao")
@Transactional()
public class TradeDaoImpl implements TradeDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void delete(Request request, Response response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			Trade trade = (Trade) entityManager.getReference(Trade.class,
					new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			entityManager.remove(trade);

		} else {
			// utilSvc.addStatus(Response.ERROR, Response.ACTIONFAILED, "Missing ID",
			// response);
		}
	}

	@Override
	public void save(Request request, Response response) throws Exception {
		Trade trade = (Trade) request.getParam(GlobalConstant.ITEM);
		entityManager.merge(trade);
	}

	@Override
	public void items(Request request, Response response) throws Exception {
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

		Query query = entityManager.createQuery(queryStr);

		if (request.containsParam(GlobalConstant.ACTIVE)) {
			query.setParameter("active", (Boolean) request.getParam(GlobalConstant.ACTIVE));
		}
		if (request.containsParam("RUNSTATUS")) {
			query.setParameter("runStatus", (String) request.getParam("RUNSTATUS"));
		}

		response.addParam(GlobalConstant.TRADES, query.getResultList());
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {
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

		Query query = entityManager.createQuery(queryStr);

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
	public void item(Request request, Response response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {
			String queryStr = "SELECT x FROM Trade AS x WHERE x.id =:id";
			Query query = entityManager.createQuery(queryStr);

			if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
				query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
			}
			if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
				query.setParameter("id", new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			}
			Trade trade = (Trade) query.getSingleResult();

			response.addParam(GlobalConstant.ITEM, trade);
		} else {
			// utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED,
			// prefCacheUtil.getPrefText("GLOBAL_SERVICE",
			// "GLOBAL_SERVICE_MISSING_ID",prefCacheUtil.getLang(request)), response);
		}
	}

	@Override
	public List<Trade> getRunningMinuteTrades() {
		String queryStr = "SELECT DISTINCT x FROM Trade AS x WHERE x.status =:status AND x.evaluationPeriod =:evaluationPeriod";

		Query query = entityManager.createQuery(queryStr);
		query.setParameter("status", "Running");
		query.setParameter("evaluationPeriod", "MINUTE");

		List<Trade> trades = new ArrayList<Trade>();
		List<?> objects = query.getResultList();
		for (Object o : objects) {
			Trade t = Trade.class.cast(o);
			Hibernate.initialize(t.getTradeDetails());
			trades.add(t);
		}

		return trades;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Trade> getAllRunningTrades() {
		String queryStr = "SELECT DISTINCT x FROM Trade AS x LEFT JOIN FETCH x.tradeDetails AS d WHERE x.status =:status AND d.status !=:detailStatus";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("status", "Running");
		query.setParameter("detailStatus", "FILLED");
		List<Trade> trades = (List<Trade>) query.getResultList();
		return trades;
	}

	public void resetTrade(Request request, Response response) {

		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			Trade trade = (Trade) entityManager.getReference(Trade.class,
					new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			trade.getTradeDetails().stream().forEach(t -> {
				entityManager.remove(t);
			});

			Set<TradeDetail> trades = new LinkedHashSet<TradeDetail>();
			trade.setTradeDetails(trades);
			trade.setAvailableBudget(trade.getBudget());
			trade.setTotalValue(trade.getBudget());
			trade.setSharesHeld(BigDecimal.ZERO);
			trade.setIterationsExecuted(0);
			entityManager.merge(trade);
		}
	}

	@Override
	public List<Trade> getRunningDayTrades() {
		String queryStr = "SELECT DISTINCT x FROM Trade AS x WHERE x.status =:status AND x.evaluationPeriod =:evaluationPeriod";

		Query query = entityManager.createQuery(queryStr);
		query.setParameter("status", "Running");
		query.setParameter("evaluationPeriod", "DAY");

		List<Trade> trades = new ArrayList<Trade>();
		List<?> objects = query.getResultList();
		for (Object o : objects) {
			Trade t = Trade.class.cast(o);
			Hibernate.initialize(t.getTradeDetails());
			trades.add(t);
		}

		return trades;
	}

	@Override
	public void getSymbolData(Request request, Response response) {
		String symbol = (String) request.getParam("SYMBOL");

		if (!Arrays.asList(Symbol.SYMBOLS).contains(symbol)) {
			return;
		}

		String evaluationPeriod = (String) request.getParam("EVALUATION_PERIOD");
		String classStr = "";
		Integer firstPoint = (Integer) request.getParam("FIRST_POINT");
		Integer lastPoint = (Integer) request.getParam("LAST_POINT");

		switch (evaluationPeriod) {
			case "DAY":
				classStr = "asset_day";
				break;
			case "MINUTE":
				classStr = "asset_minute";
				break;
		}

		String queryStr = "SELECT epoch_seconds , value FROM tradeanalyzer_main.ta_" + classStr
				+ " AS x WHERE epoch_seconds >= "
				+ firstPoint
				+ " AND epoch_seconds <= " + lastPoint + " AND symbol = \"" + symbol + "\"";
		Query query = entityManager.createNativeQuery(queryStr);

		response.addParam("SYMBOLS", query.getResultList());
	}
}
