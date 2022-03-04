package org.toasthub.stock.trade;


import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.toasthub.stock.model.Trade;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;


@Service("TradeSvc")
public class TradeSvcImpl implements TradeSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;
	
	@Autowired
	protected TradeDao tradeDao;
	
	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);
	
	// Constructors
	public TradeSvcImpl() {
	}
	
		
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
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
		try {
			Trade trade =  null;
			if (request.containsParam(GlobalConstant.ITEM)) {
				Map<String,Object> m = (Map<String,Object>) request.getParam(GlobalConstant.ITEM);
				
				if (m.containsKey("id") && !"".equals(m.get("id")) ) {
					request.addParam(GlobalConstant.ITEMID, m.get("id"));
					tradeDao.item(request, response);
					trade = (Trade) response.getParam("item");
					response.getParams().remove("item");
				} else {
					trade = new Trade();
				}
				if (m.containsKey("name")) {
					trade.setName((String) m.get("name"));
				} else {
					trade.setName("Test");
				}
				trade.setStock((String) m.get("stock"));
				if (m.containsKey("status")) {
					trade.setRunStatus((String) m.get("status"));
				} else {
					trade.setRunStatus("No");
				}
				if (m.get("buyAmount") instanceof Integer) {
					trade.setBuyAmount(new BigDecimal((Integer)m.get("buyAmount")));
				} else if (m.get("buyAmount") instanceof String) {
					trade.setBuyAmount(new BigDecimal((String)m.get("buyAmount")));
				}
				if (m.get("sellAmount") instanceof Integer) {
					trade.setSellAmount(new BigDecimal((Integer)m.get("sellAmount")));
				} else if (m.get("sellAmount") instanceof String) {
					trade.setSellAmount(new BigDecimal((String)m.get("sellAmount")));
				}
				if (m.get("profitLimit") instanceof Integer) {
					trade.setProfitLimit(new BigDecimal((Integer)m.get("profitLimit")));
				} else if (m.get("profitLimit") instanceof String) {
					trade.setProfitLimit((new BigDecimal((String)m.get("profitLimit"))));
				}
				if (m.get("trailingStopPercent") instanceof Integer) {
					trade.setTrailingStopPercent(new BigDecimal((Integer)m.get("trailingStopPercent")));
				} else if (m.get("trailingStopPercent") instanceof String) {
					trade.setTrailingStopPercent(new BigDecimal((String)m.get("trailingStopPercent")));
				}
				if (m.containsKey("algorithm")) {
					trade.setAlgorithm((String)m.get("algorithm"));
				}else
				trade.setAlgorithm("touchesLBB");
				String algorithm2 ="";
				if(m.containsKey("algorithm2"))
				algorithm2 = " "+(String)m.get("algorithm2");
				else
				algorithm2 = " touchesLBB";
				if(m.containsKey("operand")){
					trade.setAlgorithm(trade.getAlgorithm() + " "+ m.get("operand") + algorithm2);
				}
			}
			request.addParam("item", trade);
			
			tradeDao.save(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}


	@Override
	public void delete(Request request, Response response) {
		try {
			tradeDao.delete(request, response);
			tradeDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				tradeDao.items(request, response);
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
			tradeDao.item(request, response);
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}


	@Override
	public void items(Request request, Response response) {
		try {
			tradeDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				tradeDao.items(request, response);
			}
			response.setStatus(Response.SUCCESS);
		} catch (Exception e) {
			response.setStatus(Response.ACTIONFAILED);
			e.printStackTrace();
		}
		
	}
	
	@Scheduled(cron="0 * * * * ?")
	public void tradeAnalysisTask() {
		
		// if (tradeAnalysisJobRunning.get()) {
		// 	System.out.println("Trade analysis is currently running skipping this time");
		// 	return;

		// } else {
		// 	new Thread(()->{
		// 		tradeAnalysisJobRunning.set(true);
		// 		algorithmCruncherSvc.load();
		// 		checkTrades();
		// 		tradeAnalysisJobRunning.set(false);
		// 	}).start();
		// }
	}
	
//	private void checkTrades() {
//		try {
//			System.out.println("Running trade analysis job");
//			List<Trade> trades = tradeDao.getAutomatedTrades("Yes");
//			
//			if (trades != null && trades.size() > 0) {
//				for(Trade trade : trades) {
//					System.out.println("Checking trade name:" + trade.getName());
//				}
//			} else {
//				System.out.println("No trades to run");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}


	
}
