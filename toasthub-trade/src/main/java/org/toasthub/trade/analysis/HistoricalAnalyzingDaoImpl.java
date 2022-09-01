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

package org.toasthub.trade.analysis;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.EMA;
import org.toasthub.trade.model.HistoricalAnalysis;
import org.toasthub.trade.model.LBB;
import org.toasthub.trade.model.MACD;
import org.toasthub.trade.model.SL;
import org.toasthub.trade.model.SMA;
import org.toasthub.trade.model.TradeConstant;


@Repository("TAHistoricalAnalyzingDao")
@Transactional("TransactionManagerData")
public class HistoricalAnalyzingDaoImpl implements HistoricalAnalyzingDao {

	@Autowired
	protected EntityManagerDataSvc entityManagerDataSvc;

	@Override
	public void delete(RestRequest request, RestResponse response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

			HistoricalAnalysis historicalAnalysis = (HistoricalAnalysis) entityManagerDataSvc.getInstance().getReference(HistoricalAnalysis.class,
					Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			entityManagerDataSvc.getInstance().remove(historicalAnalysis);

		} else {
			// utilSvc.addStatus(Response.ERROR, Response.ACTIONFAILED, "Missing ID",
			// response);
		}
	}

	@Override
	public void save(RestRequest request, RestResponse response) throws Exception {
		entityManagerDataSvc.getInstance().merge( (Object) response.getParam(GlobalConstant.ITEM));
	}

	@Override
	public void items(RestRequest request, RestResponse response) throws Exception {
		String queryStr = "SELECT DISTINCT x FROM "
		+request.getParam(TradeConstant.IDENTIFIER)
		+" AS x ";

		Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		List<?> items = query.getResultList();

		response.addParam(GlobalConstant.ITEMS, items);
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response) throws Exception {
		// String queryStr = "SELECT COUNT(DISTINCT x) FROM "
		// +request.getParam(TradeConstant.IDENTIFIER)
		// +" as x ";

		// boolean and = false;
		// if (request.containsParam(TradeConstant.EPOCH_SECONDS)) {
		// 	if (!and)
		// 		queryStr += " WHERE ";
		// 	else
		// 		queryStr += " AND ";

		// 	queryStr += "x.epochSeconds =:epochSeconds ";
		// 	and = true;
		// }
		// if (request.containsParam(TradeConstant.SYMBOL)) {
		// 	if (!and)
		// 		queryStr += " WHERE ";
		// 	else
		// 		queryStr += " AND ";

		// 	queryStr += "x.stock =:stock ";
		// 	and = true;
		// }
		// if (request.containsParam(TradeConstant.TYPE)) {
		// 	if (!and)
		// 		queryStr += " WHERE ";
		// 	else
		// 		queryStr += " AND ";

		// 	queryStr += "x.type =:type ";
		// 	and = true;
		// }

		// Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

		// if (request.containsParam(TradeConstant.EPOCH_SECONDS)) {
		// 	query.setParameter("epochSeconds", (long)request.getParam(TradeConstant.EPOCH_SECONDS));
		// }
		// if (request.containsParam(TradeConstant.TYPE)) {
		// 	query.setParameter("type", (String) request.getParam(TradeConstant.TYPE));
		// }
		// if (request.containsParam(TradeConstant.SYMBOL)) {
		// 	query.setParameter("stock", (String) request.getParam(TradeConstant.SYMBOL));
		// }

		// Long count = (Long) query.getSingleResult();
		// if (count == null) {
		// 	count = 0l;
		// }
		// response.addParam(GlobalConstant.ITEMCOUNT, count);
	}

	@Override
	public void item(RestRequest request, RestResponse response) throws Exception {
		// String x = "";
		// switch ((String) request.getParam(TradeConstant.IDENTIFIER)) {
		// 	case "SMA":
		// 		x = "SMA";
		// 		break;
		// 	case "EMA":
		// 		x= "EMA";
		// 		break;
		// 	case "LBB":
		// 		x = "LBB";
		// 		break;
		// 	case "MACD":
		// 		x = "MACD";
		// 		break;
		// 	case "SL":
		// 		x = "SL";
		// 		break;
		// 	default:
		// 		break;
		// }

		// String queryStr = "SELECT DISTINCT x FROM "
		// + x
		// +" AS x"
		// + " WHERE x.epochSeconds =:epochSeconds"
		// + " AND x.type =: type AND x.stock =:stock";

		// Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		// query.setParameter("epochSeconds", request.getParam(TradeConstant.EPOCH_SECONDS));
		// query.setParameter("type", request.getParam(TradeConstant.TYPE));
		// query.setParameter("stock", request.getParam(TradeConstant.SYMBOL));
		// Object result = query.getSingleResult();

		// response.addParam(GlobalConstant.ITEM , result);
	}

	@Override
	public BigDecimal queryAlgValue(String alg, String stock, String type, long epochSeconds) {
		return BigDecimal.ZERO;
		// String queryStr = "SELECT DISTINCT x FROM " + alg + " AS x"
		// 		+ " WHERE x.epochSeconds =:epochSeconds"
		// 		+ " AND x.type =: type AND x.stock =:stock";
		// Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		// query.setParameter("epochSeconds", epochSeconds);
		// query.setParameter("type", type);
		// query.setParameter("stock", stock);
		// try {
		// 	switch (alg) {
		// 		case "SMA":
		// 			SMA sma = (SMA) query.getSingleResult();
		// 			return sma.getValue();

		// 		case "MACD":
		// 			MACD macd = (MACD) query.getSingleResult();
		// 			return macd.getValue();

		// 		case "SL":
		// 			SL sl = (SL) query.getSingleResult();
		// 			return sl.getValue();

		// 		case "EMA":
		// 			EMA ema = (EMA) query.getSingleResult();
		// 			return ema.getValue();

		// 		case "LBB":
		// 			LBB lbb = (LBB) query.getSingleResult();
		// 			return lbb.getValue();

		// 		default:
		// 			return null;
		// 	}
		// } catch (Exception e) {
		// 	if (e.getMessage().equals("No entity found for query"))
		// 		return null;
		// 	else {
		// 		e.printStackTrace();
		// 		return null;
		// 	}
		// }
	}

}
