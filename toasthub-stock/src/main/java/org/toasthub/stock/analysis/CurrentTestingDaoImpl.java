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

package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.analysis.model.AssetMinute;
import org.toasthub.model.Symbol;
import org.toasthub.stock.model.HistoricalAnalysis;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("CurrentTestingDao")
@Transactional()
public class CurrentTestingDaoImpl implements CurrentTestingDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void delete(Request request, Response response) {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			HistoricalAnalysis historicalAnalysis = (HistoricalAnalysis) entityManager.getReference(
					HistoricalAnalysis.class,
					new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			entityManager.remove(historicalAnalysis);

		} else {
			// utilSvc.addStatus(Response.ERROR, Response.ACTIONFAILED, "Missing ID",
			// response);
		}
	}

	@Override
	public void save(Request request, Response response) throws Exception {
		entityManager.merge((Object) response.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void items(Request request, Response response) throws Exception {
		String x = "";
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
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
			case "GoldenCross":
				x = "GoldenCross";
				break;
			case "LowerBollingerBand":
				x = "LowerBollingerBand";
				break;
			case "UpperBollingerBand":
				x = "UpperBollingerBand";
				break;
			default:
				break;
		}
		String queryStr = "SELECT DISTINCT x FROM "
				+ x
				+ " AS x ";

		Query query = entityManager.createQuery(queryStr);
		List<?> items = null;
		items = (List<?>)(query.getResultList());

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {
		String x = "";

		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
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
			case "GoldenCross":
				x = "GoldenCross";
				break;
			case "LowerBollingerBand":
				x = "LowerBollingerBand";
				break;
			case "UpperBollingerBand":
				x = "UpperBollingerBand";
				break;
			default:
				break;
		}
		String queryStr = "SELECT COUNT(DISTINCT x) FROM "
				+ x
				+ " as x ";

		boolean and = false;
		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.epochSeconds =:epochSeconds ";
			and = true;
		}
		if (request.containsParam(GlobalConstant.SYMBOL)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.symbol =:symbol ";
			and = true;
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			if (!and)
				queryStr += " WHERE ";
			else
				queryStr += " AND ";

			queryStr += "x.type =:type ";
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

		Query query = entityManager.createQuery(queryStr);

		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			query.setParameter("epochSeconds", (long) request.getParam(GlobalConstant.EPOCHSECONDS));
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			query.setParameter("type", (String) request.getParam(GlobalConstant.TYPE));
		}
		if (request.containsParam(GlobalConstant.SYMBOL)) {
			query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
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
	public void item(Request request, Response response) throws Exception {
		String x = "";
		Query query = null;
		Object result = null;
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x = "EMA";
				break;
			case "LBB":
				query = entityManager.createQuery("SELECT DISTINCT x FROM LBB"
						+ " AS x"
						+ " WHERE x.epochSeconds =:epochSeconds"
						+ " AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations");
				query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
				query.setParameter("type", request.getParam(GlobalConstant.TYPE));
				query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
				query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));
				result = query.getSingleResult();
				response.addParam(GlobalConstant.ITEM, result);

				return;
			case "UBB":
				query = entityManager.createQuery("SELECT DISTINCT x FROM UBB"
						+ " AS x"
						+ " WHERE x.epochSeconds =:epochSeconds"
						+ " AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations");
				query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
				query.setParameter("type", request.getParam(GlobalConstant.TYPE));
				query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
				query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));
				result = query.getSingleResult();
				response.addParam(GlobalConstant.ITEM, result);

				return;
			case "MACD":
				x = "MACD";
				break;
			case "SL":
				x = "SL";
				break;
			default:
				break;
		}

		String queryStr = "SELECT DISTINCT x FROM "
				+ x
				+ " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.symbol =:symbol";

		query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
		result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM, result);
	}

	public void getRecentAssetDay(Request request, Response response) {
		String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {
			String queryStr = "SELECT DISTINCT x FROM AssetDay x WHERE (SELECT MAX(x.epochSeconds) FROM AssetDay x WHERE x.symbol = : symbol) = x.epochSeconds AND x.symbol =: symbol";
			Query query = entityManager.createQuery(queryStr);
			query.setParameter("symbol", x);
			response.addParam(GlobalConstant.ITEM, query.getSingleResult());
		} else
			System.out.println("Symbol does not match symbols");
	}

	@Override
	public void getRecentAssetMinute(Request request, Response response) {
		String x = (String) request.getParam(GlobalConstant.SYMBOL);

		if (Arrays.asList(Symbol.SYMBOLS).contains(x)) {
			String queryStr = "SELECT DISTINCT x FROM AssetMinute x WHERE (SELECT MAX(x.epochSeconds) FROM AssetMinute x WHERE x.symbol = : symbol) = x.epochSeconds AND x.symbol =: symbol";
			Query query = entityManager.createQuery(queryStr);
			query.setParameter("symbol", x);
			response.addParam(GlobalConstant.ITEM, query.getSingleResult());
		} else
			System.out.println("Symbol does not match symbols");
	}
}