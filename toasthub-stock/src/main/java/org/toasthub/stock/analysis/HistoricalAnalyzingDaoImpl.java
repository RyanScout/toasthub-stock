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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.analysis.model.EMA;
import org.toasthub.analysis.model.LBB;
import org.toasthub.analysis.model.MACD;
import org.toasthub.analysis.model.SL;
import org.toasthub.analysis.model.SMA;
import org.toasthub.stock.model.HistoricalAnalysis;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("HistoricalAnalyzingDao")
@Transactional()
public class HistoricalAnalyzingDaoImpl implements HistoricalAnalyzingDao {

	@Autowired
	protected EntityManager entityManager;

	@Override
	public void delete(Request request, Response response){
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			HistoricalAnalysis historicalAnalysis = (HistoricalAnalysis) entityManager.getReference(HistoricalAnalysis.class,
					new Long((Integer) request.getParam(GlobalConstant.ITEMID)));
			entityManager.remove(historicalAnalysis);

		} else {
			// utilSvc.addStatus(Response.ERROR, Response.ACTIONFAILED, "Missing ID",
			// response);
		}
	}

	@Override
	public void save(Request request, Response response) throws Exception {
		entityManager.merge( (Object) response.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void items(Request request, Response response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM "
		+request.getParam(GlobalConstant.IDENTIFIER)
		+" AS x ";

		Query query = entityManager.createQuery(queryStr);
		List<?> items = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(Request request, Response response) throws Exception {
		String queryStr = "SELECT COUNT(DISTINCT x) FROM "
		+request.getParam(GlobalConstant.IDENTIFIER)
		+" as x ";

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

			queryStr += "x.stock =:stock ";
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

		Query query = entityManager.createQuery(queryStr);

		if (request.containsParam(GlobalConstant.EPOCHSECONDS)) {
			query.setParameter("epochSeconds", (long)request.getParam(GlobalConstant.EPOCHSECONDS));
		}
		if (request.containsParam(GlobalConstant.TYPE)) {
			query.setParameter("type", (String) request.getParam(GlobalConstant.TYPE));
		}
		if (request.containsParam(GlobalConstant.SYMBOL)) {
			query.setParameter("stock", (String) request.getParam(GlobalConstant.SYMBOL));
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
		switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
			case "SMA":
				x = "SMA";
				break;
			case "EMA":
				x= "EMA";
				break;
			case "LBB":
				x = "LBB";
				break;
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
		+" AS x"
		+ " WHERE x.epochSeconds =:epochSeconds"
		+ " AND x.type =: type AND x.stock =:stock";

		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
		query.setParameter("type", request.getParam(GlobalConstant.TYPE));
		query.setParameter("stock", request.getParam(GlobalConstant.SYMBOL));
		Object result = query.getSingleResult();

		response.addParam(GlobalConstant.ITEM , result);
	}

	@Override
	public BigDecimal queryAlgValue(String alg, String stock, String type, long epochSeconds) {
		String queryStr = "SELECT DISTINCT x FROM " + alg + " AS x"
				+ " WHERE x.epochSeconds =:epochSeconds"
				+ " AND x.type =: type AND x.stock =:stock";
		Query query = entityManager.createQuery(queryStr);
		query.setParameter("epochSeconds", epochSeconds);
		query.setParameter("type", type);
		query.setParameter("stock", stock);
		try {
			switch (alg) {
				case "SMA":
					SMA sma = (SMA) query.getSingleResult();
					return sma.getValue();

				case "MACD":
					MACD macd = (MACD) query.getSingleResult();
					return macd.getValue();

				case "SL":
					SL sl = (SL) query.getSingleResult();
					return sl.getValue();

				case "EMA":
					EMA ema = (EMA) query.getSingleResult();
					return ema.getValue();

				case "LBB":
					LBB lbb = (LBB) query.getSingleResult();
					return lbb.getValue();

				default:
					return null;
			}
		} catch (Exception e) {
			if (e.getMessage().equals("No entity found for query"))
				return null;
			else {
				e.printStackTrace();
				return null;
			}
		}
	}
}
