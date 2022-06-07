package org.toasthub.stock.trade;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.hibernate.type.CurrencyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.model.CustomTechnicalIndicator;
import org.toasthub.stock.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.stock.model.Trade;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.RequestValidation;
import org.toasthub.utils.Response;

@Service("TradeSvc")
public class TradeSvcImpl implements TradeSvc {

	@Autowired
	protected TradeDao tradeDao;

	@Autowired
	private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

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
			case "RESET":
				reset(request, response);
			default:
				break;
		}

	}

	@Override
	public void save(Request request, Response response) {
		if ((!request.containsParam(GlobalConstant.ITEM)) || (request.getParam(GlobalConstant.ITEM) == null)) {
			response.setStatus(Response.ERROR);
			return;
		}

		response.setStatus("Starting !");

		Map<?, ?> m = Map.class.cast(request.getParam(GlobalConstant.ITEM));

		Map<String, Object> tempMap = new HashMap<String, Object>();

		for (Object o : m.keySet()) {
			tempMap.put(String.class.cast(o), m.get(String.class.cast(o)));
		}

		request.setParams(tempMap);

		if (request.getParam("name") == null || ((String) request.getParam("name")).trim().isEmpty()) {
			response.setStatus(Response.EMPTY);
			return;
		}

		if (request.getParam("orderSide") == null
				|| !Arrays.asList(Trade.SUPPORTED_ORDER_SIDES).contains((String) request.getParam("orderSide"))) {
			response.setStatus(Response.ERROR);
			return;
		}

		if (request.getParam("orderType") == null
				|| !Arrays.asList(Trade.SUPPORTED_ORDER_TYPES).contains((String) request.getParam("orderType"))) {
			response.setStatus(Response.ERROR);
			return;
		}

		if (request.getParam("evaluationPeriod") == null) {
			response.setStatus(Response.EMPTY);
			return;
		}

		if (request.getParam("symbol") == null) {
			response.setStatus(Response.ERROR);
			return;
		}

		if (request.getParam("status") == null) {
			response.setStatus(Response.EMPTY);
			return;
		}

		if (request.getParam("currencyType") == null) {
			response.setStatus(Response.EMPTY);
			return;
		}

		if (request.getParam("currencyAmount") == null) {
			response.setStatus(Response.EMPTY);
			return;
		}

		switch ((String) request.getParam("currencyType")) {
			case "Dollars":
				RequestValidation.validateDollars(request, response);
				break;
			case "Shares":
				RequestValidation.validateShares(request, response);
				break;
			default:
				response.setStatus(Response.ERROR);
				return;
		}

		switch ((String) request.getParam("orderType")) {
			case Trade.MARKET:
				break;
			case Trade.PROFIT_LIMIT:
				request.addParam("PROFIT_LIMIT_TYPE", request.getParam("profitLimitType"));
				RequestValidation.validateProfitLimitAmount(request, response);
				break;
			case Trade.TRAILING_STOP:
				request.addParam("TRAILING_STOP_TYPE", request.getParam("trailingStopType"));
				RequestValidation.validateTrailingStopAmount(request, response);
				break;
			case Trade.TRAILING_STOP_PROFIT_LIMIT:
				request.addParam("TRAILING_STOP_TYPE", request.getParam("trailingStopType"));
				request.addParam("PROFIT_LIMIT_TYPE", request.getParam("profitLimitType"));
				RequestValidation.validateProfitLimitAmount(request, response);
				RequestValidation.validateTrailingStopAmount(request, response);
				break;
			default:
				response.setStatus(Response.ERROR);
				return;
		}

		Long itemId = null;

		if (request.getParam("id") instanceof Integer) {
			itemId = new Long((Integer) request.getParams().remove("id"));
		}

		switch ((String) request.getParam("orderSide")) {
			case Trade.BOT:
				validateBuyCondition(request, response);
				validateSellCondition(request, response);

				if (request.getParam("budget") == null) {
					response.setStatus(Response.ERROR);
					return;
				}
				RequestValidation.validateBudget(request, response);

				request.addParam("ITERATIONS", "unlimited");
				break;
			case Trade.BUY:
				validateBuyCondition(request, response);
				request.addParam("ITERATIONS", request.getParam("iterations"));
				break;
			case Trade.SELL:
				validateSellCondition(request, response);
				request.addParam("ITERATIONS", request.getParam("iterations"));
				break;
			default:
				response.setStatus(Response.ERROR);
				return;
		}

		request.addParam("NAME", request.getParam("name"));
		request.addParam("ORDER_SIDE", request.getParam("orderSide"));
		request.addParam("ORDER_TYPE", request.getParam("orderType"));
		request.addParam("EVALUATION_PERIOD", request.getParam("evaluationPeriod"));
		request.addParam("SYMBOL", request.getParam("symbol"));
		request.addParam("STATUS", request.getParam("status"));
		request.addParam("CURRENCY_TYPE", request.getParam("currencyType"));

		request.getParams().remove(GlobalConstant.ACTIVE);
		request.getParams().remove("RUNSTATUS");

		Trade trade = new Trade();

		if (itemId != null) {
			request.addParam(GlobalConstant.ITEMID, itemId);
			try {
				tradeDao.item(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}
			trade = (Trade) response.getParam(GlobalConstant.ITEM);
		}
		if (itemId == null) {
			try {
				tradeDao.itemCount(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if ((long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				response.setStatus(Response.ERROR);
				return;
			}
		}

		if (response.getStatus().equals(Response.ERROR) || response.getStatus().equals(Response.EMPTY)) {
			return;
		}

		trade.setName((String) request.getParam("NAME"));
		trade.setOrderSide((String) request.getParam("ORDER_SIDE"));
		trade.setOrderType((String) request.getParam("ORDER_TYPE"));
		trade.setIterations((String) request.getParam("ITERATIONS"));
		trade.setEvaluationPeriod((String) request.getParam("EVALUATION_PERIOD"));
		trade.setSymbol((String) request.getParam("SYMBOL"));
		trade.setStatus((String) request.getParam("STATUS"));
		trade.setCurrencyType((String) request.getParam("CURRENCY_TYPE"));
		trade.setCurrencyAmount((BigDecimal) request.getParam("CURRENCY_AMOUNT"));

		if (request.getParam("ORDER_TYPE").equals(Trade.TRAILING_STOP)) {
			trade.setTrailingStopType((String) request.getParam("TRAILING_STOP_TYPE"));
			trade.setTrailingStopAmount((BigDecimal) request.getParam("TRAILING_STOP_AMOUNT"));
		}

		if (request.getParam("ORDER_TYPE").equals(Trade.PROFIT_LIMIT)) {
			trade.setProfitLimitType((String) request.getParam("PROFIT_LIMIT_TYPE"));
			trade.setProfitLimitAmount((BigDecimal) request.getParam("PROFIT_LIMIT_AMOUNT"));
		}

		if (request.getParam("ORDER_TYPE").equals(Trade.TRAILING_STOP_PROFIT_LIMIT)) {
			trade.setTrailingStopType((String) request.getParam("TRAILING_STOP_TYPE"));
			trade.setTrailingStopAmount((BigDecimal) request.getParam("TRAILING_STOP_AMOUNT"));
			trade.setProfitLimitType((String) request.getParam("PROFIT_LIMIT_TYPE"));
			trade.setProfitLimitAmount((BigDecimal) request.getParam("PROFIT_LIMIT_AMOUNT"));
		}

		if (request.getParam("BUY_CONDITION") != null) {
			trade.setBuyCondition((String) request.getParam("BUY_CONDITION"));
			trade.setParseableBuyCondition((String) request.getParam("PARSEABLE_BUY_CONDITION"));
		}

		if (request.getParam("SELL_CONDITION") != null) {
			trade.setSellCondition((String) request.getParam("SELL_CONDITION"));
			trade.setParseableSellCondition((String) request.getParam("PARSEABLE_SELL_CONDITION"));
		}

		if (request.getParam("BUDGET") != null) {
			trade.setBudget((BigDecimal) request.getParam("BUDGET"));
		}

		request.addParam(GlobalConstant.ITEM, trade);

		try {
			tradeDao.save(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		response.setStatus(Response.SUCCESS);
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

	public void reset(Request request, Response response) {
		try {
			tradeDao.resetTrade(request, response);
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

	public void validateBuyCondition(Request request, Response response) {
		String initialString = "";
		if (request.getParam("buyCondition") instanceof String) {
			initialString = (String) request.getParam("buyCondition");
		}

		// "( Super_Golden_Cross_! ) &(lbb | ubb) -> initialString"
		// " ( Super_Golden_Cross_! ) & ( lbb | ubb ) " -> paddedString
		// "Super_Golden_Cross_! lbb ubb -> modifiedString
		// "5 4 7 -> modifiedArr"

		String paddedString = " "
				+ initialString.replace("(", " ( ").replace(")", " ) ").replace("&", " & ").replace("|",
						" | ")
				+ " ";
		String modifiedString = initialString.replace("(", " ").replace(")", " ").replace("&", " ").replace("|", " ")
				.replace("//s+", " ").trim();
		String[] initialArr = modifiedString.split(" ");

		String[] modifiedArr = Stream.of(initialArr).map(s -> {
			if (s.equals("")) {
				return s;
			}
			request.addParam("NAME", s);
			try {
				customTechnicalIndicatorDao.item(request, response);
			} catch (NoResultException e) {
				response.setStatus(Response.ERROR);
				return s;
			}
			long id = CustomTechnicalIndicator.class.cast(response.getParam(GlobalConstant.ITEM)).getId();
			s = String.valueOf(id);
			return s;
		}).toArray(String[]::new);

		for (int i = 0; i < initialArr.length; i++) {
			paddedString = paddedString.replace(" " + initialArr[i] + " ", " " + modifiedArr[i] + " ");
		}

		paddedString = paddedString.replaceAll("//s+", "");

		request.addParam("BUY_CONDITION", initialString);
		request.addParam("PARSEABLE_BUY_CONDITION", paddedString);
	}

	public void validateSellCondition(Request request, Response response) {
		String initialString = "";
		if (request.getParam("sellCondition") instanceof String) {
			initialString = (String) request.getParam("sellCondition");
		}

		String paddedString = " "
				+ initialString.replace("(", " ( ").replace(")", " ) ").replace("&", " & ").replace("|",
						" | ")
				+ " ";
		String modifiedString = initialString.replace("(", " ").replace(")", " ").replace("&", " ").replace("|", " ")
				.replace("//s+", " ").trim();
		String[] initialArr = modifiedString.split(" ");

		String[] modifiedArr = Stream.of(initialArr).map(s -> {

			if (s.equals("")) {
				return s;
			}

			request.addParam("NAME", s);
			try {
				customTechnicalIndicatorDao.item(request, response);
			} catch (NoResultException e) {
				response.setStatus(Response.ERROR);
				return s;
			}
			long id = CustomTechnicalIndicator.class.cast(response.getParam(GlobalConstant.ITEM)).getId();
			s = String.valueOf(id);
			return s;
		}).toArray(String[]::new);

		for (int i = 0; i < initialArr.length; i++) {
			paddedString = paddedString.replace(" " + initialArr[i] + " ", " " + modifiedArr[i] + " ");
		}

		paddedString = paddedString.replaceAll("//s+", "");

		request.addParam("SELL_CONDITION", initialString);
		request.addParam("PARSEABLE_SELL_CONDITION", paddedString);
	}
}
