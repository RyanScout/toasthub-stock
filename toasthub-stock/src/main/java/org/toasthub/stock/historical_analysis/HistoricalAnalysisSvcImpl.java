package org.toasthub.stock.historical_analysis;


import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.Trade;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;


@Service("HistoricalAnalysisSvc")
public class HistoricalAnalysisSvcImpl implements HistoricalAnalysisSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;
	
	@Autowired
	protected HistoricalAnalysisDao historicalAnalysisDao;

	
	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);
	
	// Constructors
	public HistoricalAnalysisSvcImpl() {
	}

	@Override
	public void process(Request request, Response response) {
		String action = (String) request.getParams().get("action");
		
		switch (action) {
		case "ITEM":
			item(request, response);
			break;
		case "LIST":
			items(request, response);
			break;
		case "SAVE":
			save(request, response);
			break;
		case "DELETE":
			delete(request, response);
			break;
		default:
			break;
		}
		
	}


	@Override
	@SuppressWarnings("unchecked")
	public void save(Request request, Response response) {
		// try {
		// 	Trade trade =  null;
		// 	if (request.containsParam(GlobalConstant.ITEM)) {
		// 		Map<String,Object> m = (Map<String,Object>) request.getParam(GlobalConstant.ITEM);
				
		// 		if (m.containsKey("id") && !"".equals(m.get("id")) ) {
		// 			request.addParam(GlobalConstant.ITEMID, m.get("id"));
		// 			historicalAnalysisDao.item(request, response);
		// 			trade = (Trade) response.getParam("item");
		// 			response.getParams().remove("item");
		// 		} else {
		// 			trade = new Trade();
		// 		}
		// 		if (m.containsKey("name")) {
		// 			trade.setName((String) m.get("name"));
		// 		} else {
		// 			trade.setName("Test");
		// 		}

		// 		trade.setName((String) m.get("orderType"));

		// 		trade.setStock((String) m.get("stock"));

		// 		if (m.containsKey("status")) {
		// 			trade.setRunStatus((String) m.get("status"));
		// 		} else {
		// 			trade.setRunStatus("No");
		// 		}
		// 		if (m.get("amount") instanceof Integer) {
		// 			trade.setAmount(new BigDecimal((Integer)m.get("amount")));
		// 		} else if (m.get("amount") instanceof String) {
		// 			trade.setAmount(new BigDecimal((String)m.get("amount")));
		// 		}
		// 		if (m.get("profitLimit") instanceof Integer) {
		// 			trade.setProfitLimit(new BigDecimal((Integer)m.get("profitLimit")));
		// 		} else if (m.get("profitLimit") instanceof String) {
		// 			trade.setProfitLimit((new BigDecimal((String)m.get("profitLimit"))));
		// 		}
		// 		if (m.get("trailingStopPercent") instanceof Integer) {
		// 			trade.setTrailingStopPercent(new BigDecimal((Integer)m.get("trailingStopPercent")));
		// 		} else if (m.get("trailingStopPercent") instanceof String) {
		// 			trade.setTrailingStopPercent(new BigDecimal((String)m.get("trailingStopPercent")));
		// 		}
		// 		if (m.containsKey("Algorithm")) {
		// 			trade.setAlgorithm((String)m.get("Algorithm"));
		// 		}else
		// 		trade.setAlgorithm("touchesLBB");
		// 		String Algorithm2 ="";
		// 		if(m.containsKey("Algorithm2"))
		// 		Algorithm2 = " "+(String)m.get("Algorithm2");
		// 		else
		// 		Algorithm2 = " touchesLBB";
		// 		if(m.containsKey("operand")){
		// 			trade.setAlgorithm(trade.getAlgorithm() + " "+ m.get("operand") + Algorithm2);
		// 		}
		// 	}
		// 	request.addParam("item", trade);
			
		// 	historicalAnalysisDao.save(request, response);
		// 	response.setStatus(Response.SUCCESS);
		// } catch (Exception e) {
		// 	response.setStatus(Response.ACTIONFAILED);
		// 	e.printStackTrace();
		// }
		
	}


	@Override
	public void delete(Request request, Response response) {
		try {
			historicalAnalysisDao.delete(request, response);
			historicalAnalysisDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				historicalAnalysisDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}
		


	@Override
	public void item(Request request, Response response) {
		try {
			historicalAnalysisDao.item(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}


	@Override
	public void items(Request request, Response response) {
		try {
			historicalAnalysisDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				historicalAnalysisDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}
	
}
