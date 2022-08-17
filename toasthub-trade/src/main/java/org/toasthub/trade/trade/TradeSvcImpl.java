package org.toasthub.trade.trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.algorithm.AlgorithmCruncherSvc;
import org.toasthub.trade.cache.CacheManager;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.historical_analysis.HistoricalAnalysisSvc;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeDetail;

@Service("TATradeSvc")
public class TradeSvcImpl implements ServiceProcessor, TradeSvc {

	@Autowired
	@Qualifier("TAHistoricalAnalysisSvc")
	private HistoricalAnalysisSvc historicalAnalysisSvc;

	@Autowired
	@Qualifier("TATradeDao")
	protected TradeDao tradeDao;

	@Autowired
	@Qualifier("TACustomTechnicalIndicatorDao")
	private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

	@Autowired
	@Qualifier("TARequestValidation")
	private RequestValidation validator;

	@Autowired
	@Qualifier("TACacheManager")
	private CacheManager cacheManager;

	@Autowired
	@Qualifier("TAAlgorithmCruncherSvc")
	private AlgorithmCruncherSvc algorithmCruncherSvc;

	final ExpressionParser parser = new SpelExpressionParser();

	final AtomicBoolean tradeAnalysisJobRunning = new AtomicBoolean(false);

	// Constructors
	public TradeSvcImpl() {
	}

	@Override
	public void process(final RestRequest request, final RestResponse response) {
		try {
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
				case "HISTORICAL_ANALYSIS":

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

					final long tradeId = validator.validateId(itemProperties.get("id"));

					final Trade trade = tradeDao.findTradeById(tradeId);

					final Set<Long> technicalIndicatorIds = new HashSet<Long>();

					Stream.of(trade.getParsedBuyCondition().split(" ")).forEach(s -> {

						if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
							return;
						}
						final long customTechnicalIndicatorId = Long.valueOf(s);

						final CustomTechnicalIndicator c = tradeDao
								.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);
						final TechnicalIndicator t = tradeDao.getTechnicalIndicatorByProperties(trade.getSymbol(),
								c.getEvaluationPeriod(), c.getTechnicalIndicatorKey());
						technicalIndicatorIds.add(t.getId());
					});

					Stream.of(trade.getParsedSellCondition().split(" ")).forEach(s -> {
						if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
							return;
						}
						final long customTechnicalIndicatorId = Long.valueOf(s);

						final CustomTechnicalIndicator c = tradeDao
								.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);
						final TechnicalIndicator t = tradeDao.getTechnicalIndicatorByProperties(trade.getSymbol(),
								c.getEvaluationPeriod(), c.getTechnicalIndicatorKey());
						technicalIndicatorIds.add(t.getId());
					});

					final long startTime = validator.validateDate(itemProperties.get("startTime"));

					final long endTime = validator.validateDate(itemProperties.get("endTime"));

					// ensure technical indicators have sufficient data to historically analyze
					for (final long id : technicalIndicatorIds) {
						algorithmCruncherSvc.backloadAlgorithm(id, startTime);
						System.out.println("Backloaded algorithms for technical indicator " + id);
						cacheManager.backloadTechnicalIndicator(id, startTime);
						System.out.println("Backloaded technical indicator " + id);
					}

					historicalAnalysisSvc.historicalAnalysis(trade, startTime, endTime);

					System.out.println("Historical Analysis Complete");

					response.setStatus(RestResponse.SUCCESS);

					break;
				default:
					throw new Exception("Action : " + action + "is not recognized");
			}
		} catch (final Exception e) {
			response.setStatus("Exception : " + e.getMessage());
			e.printStackTrace();
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

			final boolean preExisting = !trade.getSymbol().equals("");

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

					if (!preExisting) {
						trade.setBudget(budget);

						trade.setAvailableBudget(budget);

						trade.setTotalValue(budget);

						trade.setSharesHeld(BigDecimal.ZERO);
					} else {

						final List<TradeDetail> tradeDetails = tradeDao.getTradeDetails(trade);

						final boolean tradeHasBeenReset = tradeDetails.size() == 0;

						final boolean budgetHasBeenUpdated = trade.getBudget().compareTo(budget) != 0;

						if (!budgetHasBeenUpdated) {
							break;
						}

						if (!tradeHasBeenReset) {
							throw new Exception("Trade must be reset to update budget");
						}

						trade.setBudget(budget);

						trade.setAvailableBudget(budget);

						trade.setTotalValue(budget);

						trade.setSharesHeld(BigDecimal.ZERO);
					}

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

			response.setStatus(RestResponse.SUCCESS);

		} catch (final Exception e) {
			e.printStackTrace();
			response.setStatus("Exception: " + e.getMessage());
		}
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

}
