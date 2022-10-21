package org.toasthub.trade.historical_analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeDetail;
import org.toasthub.trade.trade.TradeDao;

@Service("TAHistoricalAnalysisSvc")
public class HistoricalAnalysisSvcImpl implements ServiceProcessor, HistoricalAnalysisSvc {

	final ExpressionParser parser = new SpelExpressionParser();

	@Autowired
	@Qualifier("TARequestValidation")
	private RequestValidation validator;

	@Autowired
	@Qualifier("TAHistoricalAnalysisDao")
	private HistoricalAnalysisDao historicalAnalysisDao;

	@Autowired
	@Qualifier("TATradeDao")
	private TradeDao tradeDao;

	@Autowired
	@Qualifier("TACustomTechnicalIndicatorDao")
	private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

	@Autowired
	private AlgorithmCruncherSvc algorithmCruncherSvc;

	@Autowired
	private CacheManager cacheManager;

	// constructor
	public HistoricalAnalysisSvcImpl() {
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
				case "BACKLOAD":
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

					final Trade trade = historicalAnalysisDao.findTradeById(tradeId);

					final Set<Long> technicalIndicatorIds = new HashSet<Long>();

					Stream.of(trade.getParsedBuyCondition().split(" ")).forEach(s -> {

						if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
							return;
						}
						final long id = Long.valueOf(s);

						technicalIndicatorIds.add(id);
					});

					Stream.of(trade.getParsedSellCondition().split(" ")).forEach(s -> {
						if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
							return;
						}
						final long id = Long.valueOf(s);

						technicalIndicatorIds.add(id);
					});

					final long startTime = validator.validateDate(itemProperties.get("startTime"));

					final long endTime = validator.validateDate(itemProperties.get("endTime"));

					// ensure technical indicators have sufficient data to historically analyze
					for (final long id : technicalIndicatorIds) {
						algorithmCruncherSvc.backloadAlgorithm(id, startTime, endTime);
						cacheManager.backloadTechnicalIndicator(id, startTime);
					}

					historicalAnalysis(trade, startTime, endTime);

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

	}

	@Override
	public void delete(final RestRequest request, final RestResponse response) {
		try {
			historicalAnalysisDao.delete(request, response);
			historicalAnalysisDao.itemCount(request, response);
			if ((Long) response.getParam(GlobalConstant.ITEMCOUNT) > 0) {
				historicalAnalysisDao.items(request, response);
			}
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void item(final RestRequest request, final RestResponse response) {
		try {
			historicalAnalysisDao.item(request, response);
			response.setStatus(RestResponse.SUCCESS);
		} catch (final Exception e) {
			response.setStatus(RestResponse.ACTIONFAILED);
			e.printStackTrace();
		}

	}

	@Override
	public void items(final RestRequest request, final RestResponse response) {

	}

	@Override
	public List<Trade> getHistoricalAnalyses() throws Exception {
		final List<Trade> items = historicalAnalysisDao.getHistoricalAnalyses();

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

		return items;
	}

	@Override
	public void historicalAnalysis(final Trade initTrade, final long startTime, final long endTime) throws Exception {

		final Trade trade = initTrade.clone();

		trade.setAvailableBudget(trade.getBudget());
		trade.setSharesHeld(BigDecimal.ZERO);
		trade.setTotalValue(trade.getAvailableBudget());
		trade.setFirstOrder(0);
		trade.setLastOrder(0);

		final List<TechnicalIndicatorDetail> buyOrderList = new ArrayList<TechnicalIndicatorDetail>();

		final List<TechnicalIndicatorDetail> sellOrderList = new ArrayList<TechnicalIndicatorDetail>();

		final Map<Long, List<TechnicalIndicatorDetail>> buyOrderMap = new HashMap<Long, List<TechnicalIndicatorDetail>>();

		final Map<Long, List<TechnicalIndicatorDetail>> sellOrderMap = new HashMap<Long, List<TechnicalIndicatorDetail>>();

		final Set<Long> relevantEpochSeconds = new LinkedHashSet<Long>();

		Stream.of(trade.getParsedBuyCondition().split(" ")).forEach(s -> {
			if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
				return;
			}
			final long customTechnicalIndicatorId = Long.valueOf(s);

			final CustomTechnicalIndicator customTechnicalIndicator = historicalAnalysisDao
					.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);

			final TechnicalIndicator technicalIndicator = tradeDao.getTechnicalIndicatorByProperties(
					trade.getSymbol(),
					customTechnicalIndicator.getEvaluationPeriod(),
					customTechnicalIndicator.getTechnicalIndicatorKey(),
					customTechnicalIndicator.getTechnicalIndicatorType());

			final List<TechnicalIndicatorDetail> orders = historicalAnalysisDao.getTechnicalIndicatorDetails(
					technicalIndicator,
					startTime, endTime);

			buyOrderList.addAll(orders);
		});

		Stream.of(trade.getParsedSellCondition().split(" ")).forEach(s -> {
			if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
				return;
			}

			final long customTechnicalIndicatorId = Long.valueOf(s);

			final CustomTechnicalIndicator customTechnicalIndicator = historicalAnalysisDao
					.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);

			final TechnicalIndicator technicalIndicator = tradeDao.getTechnicalIndicatorByProperties(
					trade.getSymbol(),
					customTechnicalIndicator.getEvaluationPeriod(),
					customTechnicalIndicator.getTechnicalIndicatorKey(),
					customTechnicalIndicator.getTechnicalIndicatorType());

			final List<TechnicalIndicatorDetail> orders = historicalAnalysisDao.getTechnicalIndicatorDetails(
					technicalIndicator,
					startTime, endTime);

			sellOrderList.addAll(orders);
		});

		buyOrderList.stream().forEachOrdered(detail -> {

			final long epochSeconds = detail.getFlashTime();

			final List<TechnicalIndicatorDetail> details = new ArrayList<TechnicalIndicatorDetail>();

			if (buyOrderMap.containsKey(epochSeconds)) {
				final List<TechnicalIndicatorDetail> existingDetails = buyOrderMap.get(epochSeconds);
				details.addAll(existingDetails);
			}

			details.add(detail);
			buyOrderMap.put(epochSeconds, details);
			relevantEpochSeconds.add(epochSeconds);
		});

		sellOrderList.stream().forEachOrdered(detail -> {

			final long epochSeconds = detail.getFlashTime();

			final List<TechnicalIndicatorDetail> details = new ArrayList<TechnicalIndicatorDetail>();

			if (sellOrderMap.containsKey(epochSeconds)) {
				final List<TechnicalIndicatorDetail> existingDetails = sellOrderMap.get(epochSeconds);
				details.addAll(existingDetails);
			}

			details.add(detail);
			sellOrderMap.put(epochSeconds, details);
			relevantEpochSeconds.add(epochSeconds);
		});

		relevantEpochSeconds.stream().sorted().forEachOrdered(epochSeconds -> {

			final List<TechnicalIndicatorDetail> buyDetails = buyOrderMap.get(epochSeconds);

			final List<TechnicalIndicatorDetail> sellDetails = sellOrderMap.get(epochSeconds);

			final TechnicalIndicatorDetail technicalIndicatorDetail;

			if (buyDetails != null && buyDetails.get(0) != null) {
				technicalIndicatorDetail = buyDetails.get(0);
			} else {
				technicalIndicatorDetail = sellDetails.get(0);
			}

			final BigDecimal assetPrice = technicalIndicatorDetail.getFlashPrice();

			final BigDecimal orderAmount;

			final String orderType = trade.getCurrencyType().toUpperCase();

			if (orderType.equals("DOLLARS")) {
				orderAmount = trade.getCurrencyAmount();
			} else {
				orderAmount = trade.getCurrencyAmount().multiply(assetPrice);
			}

			final ZonedDateTime currentDay = ZonedDateTime
					.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.of("America/New_York"))
					.truncatedTo(ChronoUnit.DAYS);

			final boolean checkedThisDay = trade.getLastOrder() > currentDay.toEpochSecond();

			if (trade.getEvaluationPeriod().toUpperCase().equals("DAY") && checkedThisDay) {
				return;
			}

			final long currentTime = epochSeconds;
			final BigDecimal currentPrice = assetPrice;
			if (trade.getFirstCheck() == 0) {
				trade.setFirstCheck(currentTime);
				trade.setFirstCheckPrice(currentPrice);
			}

			trade.setLastCheck(currentTime);

			trade.setLastCheckPrice(currentPrice);

			if (trade.getAvailableBudget().compareTo(orderAmount) > 0 && buyDetails != null) {

				final List<String> buyReasons = new ArrayList<String>();
				final String[] buyStringArr = Stream.of(trade.getParsedBuyCondition().split(" ")).map(s -> {
					if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
						return s;
					}

					final long customTechnicalIndicatorId = Long.valueOf(s);

					final CustomTechnicalIndicator customTechnicalIndicator = historicalAnalysisDao
							.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);

					final TechnicalIndicator technicalIndicator = tradeDao.getTechnicalIndicatorByProperties(
							trade.getSymbol(),
							customTechnicalIndicator.getEvaluationPeriod(),
							customTechnicalIndicator.getTechnicalIndicatorKey(),
							customTechnicalIndicator.getTechnicalIndicatorType());

					final boolean flashing = buyDetails.stream().anyMatch(detail -> {
						final TechnicalIndicator parent = historicalAnalysisDao.getTechnicalIndicatorFromChild(detail);
						final boolean match = parent.getId() == technicalIndicator.getId();
						return match;
					});

					if (flashing) {
						buyReasons.add(String.valueOf(customTechnicalIndicatorId));
					}

					return String.valueOf(flashing);

				}).toArray(String[]::new);

				final String buyCondition = String.join(" ", buyStringArr);

				final boolean buySignalFlashing;

				if (buyCondition.equals("")) {
					buySignalFlashing = false;
				} else {
					buySignalFlashing = parser.parseExpression(buyCondition).getValue(Boolean.class);
				}

				if (buySignalFlashing) {

					final TradeDetail tradeDetail = new TradeDetail();

					tradeDetail.setPlacedAt(epochSeconds);
					tradeDetail.setOrderID("HISTORICAL_ANALYSIS");
					tradeDetail.setStatus("FILLED");
					tradeDetail.setOrderSide("BUY");
					tradeDetail.setOrderCondition(String.join(",", buyReasons));
					tradeDetail.setTrade(trade);
					trade.getTradeDetails().add(tradeDetail);
					if (trade.getFirstOrder() == 0) {
						trade.setFirstOrder(epochSeconds);
					}
					trade.setLastOrder(epochSeconds);

					final BigDecimal orderQuantity = orderAmount.divide(assetPrice, MathContext.DECIMAL32);

					final BigDecimal fillPrice = technicalIndicatorDetail.getFlashPrice().setScale(2,
							RoundingMode.HALF_UP);

					trade.setAvailableBudget(
							trade.getAvailableBudget()
									.subtract((orderQuantity.multiply(fillPrice)),
											MathContext.DECIMAL32)
									.setScale(2, RoundingMode.HALF_UP));

					trade.setSharesHeld(trade.getSharesHeld().add(orderQuantity));

					trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

					trade.setTotalValue(
							trade.getAvailableBudget().add(
									trade.getSharesHeld()
											.multiply(technicalIndicatorDetail.getFlashPrice())));

					tradeDetail.setSharesHeld(trade.getSharesHeld());

					tradeDetail.setAvailableBudget(trade.getAvailableBudget());

					tradeDetail.setDollarAmount(
							(orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
									.setScale(2, RoundingMode.HALF_UP));

					tradeDetail.setShareAmount(orderQuantity);

					tradeDetail.setFilledAt(epochSeconds);

					tradeDetail.setAssetPrice(fillPrice);

					tradeDetail.setTotalValue(trade.getTotalValue());

					tradeDetail.setStatus("FILLED");

					return;

				}
			}

			final BigDecimal sharesToBeSold = orderAmount.divide(assetPrice, MathContext.DECIMAL32);

			if (trade.getSharesHeld().compareTo(sharesToBeSold) > 0 && sellDetails != null) {
				final List<String> sellReasons = new ArrayList<String>();

				final String[] sellStringArr = Stream.of(trade.getParsedSellCondition().split(" ")).map(s -> {
					if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
						return s;
					}

					final long customTechnicalIndicatorId = Long.valueOf(s);

					final CustomTechnicalIndicator customTechnicalIndicator = historicalAnalysisDao
							.getCustomTechnicalIndicatorById(customTechnicalIndicatorId);

					final TechnicalIndicator technicalIndicator = tradeDao.getTechnicalIndicatorByProperties(
							trade.getSymbol(),
							customTechnicalIndicator.getEvaluationPeriod(),
							customTechnicalIndicator.getTechnicalIndicatorKey(),
							customTechnicalIndicator.getTechnicalIndicatorType());

					final boolean flashing = sellDetails.stream().anyMatch(detail -> {
						final TechnicalIndicator parent = historicalAnalysisDao.getTechnicalIndicatorFromChild(detail);
						final boolean match = parent.getId() == technicalIndicator.getId();
						return match;
					});

					if (flashing) {
						sellReasons.add(String.valueOf(customTechnicalIndicatorId));
					}

					return String.valueOf(flashing);

				}).toArray(String[]::new);

				final String sellCondition = String.join(" ", sellStringArr);

				final boolean sellSignalFlashing;

				if (sellCondition.equals("")) {
					sellSignalFlashing = false;
				} else {
					sellSignalFlashing = parser.parseExpression(sellCondition).getValue(Boolean.class);
				}

				if (sellSignalFlashing) {

					final TradeDetail tradeDetail = new TradeDetail();

					tradeDetail.setPlacedAt(epochSeconds);
					tradeDetail.setOrderID("HISTORICAL_ANALYSIS");
					tradeDetail.setStatus("FILLED");
					tradeDetail.setOrderSide("SELL");
					tradeDetail.setOrderCondition(String.join(",", sellReasons));
					tradeDetail.setTrade(trade);
					trade.getTradeDetails().add(tradeDetail);
					if (trade.getFirstOrder() == 0) {
						trade.setFirstOrder(epochSeconds);
					}
					trade.setLastOrder(epochSeconds);

					final BigDecimal orderQuantity = orderAmount.divide(assetPrice, MathContext.DECIMAL32);

					final BigDecimal fillPrice = technicalIndicatorDetail.getFlashPrice().setScale(2,
							RoundingMode.HALF_UP);

					trade.setAvailableBudget(
							trade.getAvailableBudget()
									.add((orderQuantity.multiply(fillPrice)),
											MathContext.DECIMAL32)
									.setScale(2, RoundingMode.HALF_UP));

					trade.setSharesHeld(trade.getSharesHeld().subtract(orderQuantity));

					trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

					trade.setTotalValue(
							trade.getAvailableBudget().add(
									trade.getSharesHeld()
											.multiply(assetPrice)));

					tradeDetail.setSharesHeld(trade.getSharesHeld());

					tradeDetail.setAvailableBudget(trade.getAvailableBudget());

					tradeDetail.setDollarAmount(
							(orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
									.setScale(2, RoundingMode.HALF_UP));

					tradeDetail.setShareAmount(orderQuantity);

					tradeDetail.setFilledAt(epochSeconds);

					tradeDetail.setAssetPrice(fillPrice);

					tradeDetail.setTotalValue(trade.getTotalValue());

					return;
				}
			}
		});

		trade.setStatus("HISTORICAL_ANALYSIS");

		historicalAnalysisDao.saveItem(trade);
	}

}
