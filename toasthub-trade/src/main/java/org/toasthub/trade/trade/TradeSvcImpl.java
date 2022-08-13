package org.toasthub.trade.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.Trade;

@Service("TATradeSvc")
public class TradeSvcImpl implements ServiceProcessor, TradeSvc {

	@Autowired
	@Qualifier("TATradeDao")
	protected TradeDao tradeDao;

	@Autowired
	@Qualifier("TACustomTechnicalIndicatorDao")
	private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

	@Autowired
	@Qualifier("TARequestValidation")
	private RequestValidation validator;

	final ExpressionParser parser = new SpelExpressionParser();

	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

	// Constructors
	public TradeSvcImpl() {
	}

	@Override
	public void process(final RestRequest request, final RestResponse response) {
		final String action = (String) request.getParams().get("action");

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
				break;
			case "SYMBOL_DATA":
				getSymbolData(request, response);
				break;

			default:
				break;
		}

	}

	@Override
	public void save(final RestRequest request, final RestResponse response) {
		try {

			if ((!request.containsParam(GlobalConstant.ITEM))
					|| (request.getParam(GlobalConstant.ITEM) == null)
					|| !(request.getParam(GlobalConstant.ITEM) instanceof LinkedHashMap)) {
				throw new Exception("Item is null or not an instance of a linked hash map");
			}
			final Map<?, ?> m = Map.class.cast(request.getParam(GlobalConstant.ITEM));

			final Map<String, Object> itemProperties = new HashMap<String, Object>();

			for (final Object o : m.keySet()) {
				itemProperties.put(String.class.cast(o), m.get(String.class.cast(o)));
			}

			final Trade trade = validator.validateTradeID(itemProperties.get("id"));

			final String status = validator.validateStatus(itemProperties.get("status"));

			if (trade.getStatus() == "Running" && status == "Running") {
				throw new Exception("Cannot change properties of trade while trade is running");
			}

			trade.setStatus(status);

			final String name = validator.validateTradeName(itemProperties.get("name"));
			trade.setName(name);

			final String symbol = validator.validateSymbol(itemProperties.get("symbol"));
			trade.setSymbol(symbol);

			final String orderSide = validator.validateOrderSide(itemProperties.get("orderSide"));
			trade.setOrderSide(orderSide);

			final String orderType = validator.validateOrderType(itemProperties.get("orderType"));
			trade.setOrderType(orderType);

			final String evaluationPeriod = validator.validateEvaluationPeriod(itemProperties.get("evaluationPeriod"));
			trade.setEvaluationPeriod(evaluationPeriod);

			final String currencyType = validator.validateCurrencyType(itemProperties.get("currencyType"));
			trade.setCurrencyType(currencyType);

			switch (currencyType.toUpperCase()) {
				case "DOLLARS":
					final BigDecimal dollarAmount = validator
							.validateDollarAmount(itemProperties.get("currencyAmount"));
					trade.setCurrencyAmount(dollarAmount);
					break;
				case "SHARES":
					final BigDecimal shareAmount = validator
							.validateShareAmount(itemProperties.get("currencyAmount"));
					trade.setCurrencyAmount(shareAmount);
					break;
				default:
					throw new Exception("Unrecognized currency type");
			}

			switch (orderType.toUpperCase()) {
				case Trade.MARKET:
					break;
				case Trade.PROFIT_LIMIT:

					final BigDecimal profitLimitAmount = validator
							.validateProfitLimitAmount(itemProperties.get("profitLimitAmount"));

					trade.setProfitLimitAmount(profitLimitAmount);
					break;

				case Trade.TRAILING_STOP:

					final BigDecimal trailingStopAmount = validator
							.validateTrailingStopAmount(itemProperties.get("trailingStopAmount"));

					trade.setTrailingStopAmount(trailingStopAmount);

					break;
				case Trade.TRAILING_STOP_PROFIT_LIMIT:

					final BigDecimal combinationProfitLimitAmount = validator
							.validateProfitLimitAmount(itemProperties.get("profitLimitAmount"));

					trade.setProfitLimitAmount(combinationProfitLimitAmount);

					final BigDecimal combinationTrailingStopAmount = validator
							.validateTrailingStopAmount(itemProperties.get("trailingStopAmount"));

					trade.setTrailingStopAmount(combinationTrailingStopAmount);
					break;
				default:
					throw new Exception("Unrecognized order type");
			}

			switch (orderSide.toUpperCase()) {
				case Trade.BOT:

					final String botBuyCondition = validator.validateBuyCondition(trade,
							itemProperties.get("rawBuyCondition"));
					trade.setParsedBuyCondition(botBuyCondition);

					final String botSellCondition = validator.validateSellCondition(trade,
							itemProperties.get("rawSellCondition"));
					trade.setParsedSellCondition(botSellCondition);

					final BigDecimal budget = validator.validateBudget(itemProperties.get("budget"));
					trade.setBudget(budget);

					break;
				case Trade.BUY:
					final String buyCondition = validator.validateBuyCondition(trade,
							itemProperties.get("rawBuyCondition"));
					trade.setParsedBuyCondition(buyCondition);

					break;
				case Trade.SELL:
					final String sellCondition = validator.validateSellCondition(trade,
							itemProperties.get("rawSellCondition"));
					trade.setParsedSellCondition(sellCondition);

					break;
				default:
					throw new Exception("Orderside not supported");
			}

			tradeDao.saveItem(trade);

		} catch (final Exception e) {
			e.printStackTrace();
			response.setStatus("Exception: " + e.getMessage());
			return;
		}
		response.setStatus(RestResponse.SUCCESS);
	}

	@Override
	public void delete(final RestRequest request, final RestResponse response) {
		try {
			tradeDao.delete(request, response);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	public void reset(final RestRequest request, final RestResponse response) {
		try {
			tradeDao.resetTrade(request, response);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void item(final RestRequest request, final RestResponse response) {
		try {
			tradeDao.item(request, response);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void items(final RestRequest request, final RestResponse response) {
		try {
			final List<Trade> items = tradeDao.getTrades();

			items.stream().forEach(trade -> {
				final List<String> unparsedBuyCondition = new ArrayList<String>();

				for (final String string : trade.getParsedBuyCondition().split(" ")) {
					if (Arrays.asList("(", ")", "||", "&&", "").contains(string)) {
						unparsedBuyCondition.add(string);
						continue;
					}
					final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
							.findById(Long.valueOf(string));

					unparsedBuyCondition.add(customTechnicalIndicator.getName());
				}
				final String rawBuyCondition = String.join(" ", unparsedBuyCondition);
				trade.setRawBuyCondition(rawBuyCondition);

				final List<String> unparsedSellCondition = new ArrayList<String>();

				for (final String string : trade.getParsedSellCondition().split(" ")) {
					if (Arrays.asList("(", ")", "||", "&&", "").contains(string)) {
						unparsedSellCondition.add(string);
						continue;
					}
					final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao
							.findById(Long.valueOf(string));

					unparsedSellCondition.add(customTechnicalIndicator.getName());
				}
				final String rawSellCondition = String.join(" ", unparsedSellCondition);
				trade.setRawSellCondition(rawSellCondition);
			});

			response.addParam(GlobalConstant.ITEMS, items);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public void getSymbolData(final RestRequest request, final RestResponse response) {
		if (request.getParam("FIRST_POINT") == null || request.getParam("LAST_POINT") == null
				|| request.getParam("SYMBOL") == null || request.getParam("EVALUATION_PERIOD") == null) {
			return;
		}
		tradeDao.getSymbolData(request, response);
	}

}
