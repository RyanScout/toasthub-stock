package org.toasthub.stock.trade;


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

				trade.setOrderSide((String)m.get("orderSide"));

				trade.setOrderType((String) m.get("orderType"));

				trade.setFrequency((String)m.get("frequency"));

				trade.setSymbol((String) m.get("symbol"));
				
				if (m.containsKey("status")) {
					trade.setStatus((String) m.get("status"));
				} else {
					trade.setStatus("Not Running");
				}

				trade.setCurrencyType((String)m.get("currencyType"));
				if (m.get("currencyAmount") instanceof Integer) {
					trade.setCurrencyAmount(new BigDecimal((Integer)m.get("currencyAmount")));
				} else if (m.get("currencyAmount") instanceof String) {
					trade.setCurrencyAmount(new BigDecimal((String)m.get("currencyAmount")));
				}

				trade.setProfitLimitType((String)m.get("profitLimitType"));
				if (m.get("profitLimitAmount") instanceof Integer) {
					trade.setProfitLimitAmount(new BigDecimal((Integer)m.get("profitLimitAmount")));
				} else if (m.get("profitLimitAmount") instanceof String) {
					trade.setProfitLimitAmount((new BigDecimal((String)m.get("profitLimitAmount"))));
				}

				trade.setTrailingStopType((String)m.get("trailingStopType"));
				if (m.get("trailingStopAmount") instanceof Integer) {
					trade.setTrailingStopAmount(new BigDecimal((Integer)m.get("trailingStopAmount")));
				} else if (m.get("trailingStopAmount") instanceof String) {
					trade.setTrailingStopAmount(new BigDecimal((String)m.get("trailingStopAmount")));
				}

				trade.setBuyCondition((String)m.get("buyCondition"));

				trade.setSellCondition((String)m.get("sellCondition"));
			}
			request.addParam(GlobalConstant.ITEM, trade);
			
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
}
