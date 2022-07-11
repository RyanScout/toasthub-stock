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

package org.toasthub.trade.historical_analysis;

import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.HistoricalAnalysis;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeConstant;

@Repository("TAHistoricalAnalysisDao")
@Transactional("TransactionManagerData")
public class HistoricalAnalysisDaoImpl implements HistoricalAnalysisDao {

	@Autowired
	protected EntityManagerDataSvc entityManagerDataSvc;

	@Override
	public void delete(RestRequest request, RestResponse response){
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
		HistoricalAnalysis historicalAnalysis = (HistoricalAnalysis) request.getParam(GlobalConstant.ITEM);
		entityManagerDataSvc.getInstance().merge(historicalAnalysis);
	}

	@Override
	public void items(RestRequest request, RestResponse response){
		String queryStr = "SELECT DISTINCT x FROM HistoricalAnalysis AS x ";
		Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
		@SuppressWarnings("unchecked")
		List<HistoricalAnalysis> historicalAnalyses = query.getResultList();

		response.addParam(TradeConstant.HISTORICAL_ANALYSES, historicalAnalyses);
	}

	@Override
	public void itemCount(RestRequest request, RestResponse response){
		String queryStr = "SELECT COUNT(DISTINCT x) FROM HistoricalAnalysis as x ";
		Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

		Long count = (Long) query.getSingleResult();
		if (count == null) {
			count = 0l;
		}
		response.addParam(GlobalConstant.ITEMCOUNT, count);

	}


	@Override
	public void item(RestRequest request, RestResponse response) throws Exception {
		if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {
			String queryStr = "SELECT x FROM Trade AS x WHERE x.id =:id";
			Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

			query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
			Trade trade = (Trade) query.getSingleResult();

			response.addParam("item", trade);
		} else {
			// utilSvc.addStatus(RestResponse.ERROR, RestResponse.EXECUTIONFAILED,
			// prefCacheUtil.getPrefText("GLOBAL_SERVICE",
			// "GLOBAL_SERVICE_MISSING_ID",prefCacheUtil.getLang(request)), response);
		}
	}
}
