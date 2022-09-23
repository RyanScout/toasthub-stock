package org.toasthub.trade.historical_analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.RequestValidation;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeDetail;

@Service("TAHistoricalAnalysisManager")
public class HistoricalAnalysisManager {

    final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    @Qualifier("TARequestValidation")
    private RequestValidation validator;

    @Autowired
    @Qualifier("TAHistoricalAnalysisDao")
    private HistoricalAnalysisDao historicalAnalysisDao;

    private void historicallyAnalyzeSwingTrade(final RestRequest request, final RestResponse response) {
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

            final long startDate = validator.validateDate(itemProperties.get("startDate"));

            final long endDate = validator.validateDate(itemProperties.get("endDate"));

            final List<TechnicalIndicatorDetail> buyOrderList = new ArrayList<TechnicalIndicatorDetail>();

            final List<TechnicalIndicatorDetail> sellOrderList = new ArrayList<TechnicalIndicatorDetail>();

            final Map<Long, List<TechnicalIndicatorDetail>> buyOrderMap = new HashMap<Long, List<TechnicalIndicatorDetail>>();

            final Map<Long, List<TechnicalIndicatorDetail>> sellOrderMap = new HashMap<Long, List<TechnicalIndicatorDetail>>();

            final Set<Long> relevantEpochSeconds = new LinkedHashSet<Long>();

            Stream.of(trade.getParsedBuyCondition().split(" ")).forEach(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return;
                }
                final Long id = Long.valueOf(s);

                final TechnicalIndicator technicalIndicator = historicalAnalysisDao.findTechnicalIndicatorById(id);

                final List<TechnicalIndicatorDetail> orders = historicalAnalysisDao.getTechnicalIndicatorDetails(
                        technicalIndicator,
                        startDate, endDate);

                buyOrderList.addAll(orders);
            });

            Stream.of(trade.getParsedSellCondition().split(" ")).forEach(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return;
                }
                final Long id = Long.valueOf(s);

                final TechnicalIndicator technicalIndicator = historicalAnalysisDao.findTechnicalIndicatorById(id);

                final List<TechnicalIndicatorDetail> orders = historicalAnalysisDao.getTechnicalIndicatorDetails(
                        technicalIndicator,
                        startDate, endDate);

                sellOrderList.addAll(orders);
            });

            buyOrderList.forEach(detail -> {

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

            sellOrderList.forEach(detail -> {

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

            relevantEpochSeconds.stream().sorted().forEach(epochSeconds -> {

                final List<TechnicalIndicatorDetail> buyDetails = buyOrderMap.get(epochSeconds);

                final List<TechnicalIndicatorDetail> sellDetails = sellOrderMap.get(epochSeconds);

                final TechnicalIndicatorDetail technicalIndicatorDetail;

                if (buyDetails.get(0) != null) {
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

                if (trade.getAvailableBudget().compareTo(orderAmount) > 0) {

                    final List<String> buyReasons = new ArrayList<String>();
                    final String[] buyStringArr = Stream.of(trade.getParsedBuyCondition().split(" ")).map(s -> {
                        if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                            return s;
                        }

                        final long id = Long.valueOf(s);

                        final boolean flashing = buyDetails.stream().anyMatch(detail -> detail.getId() == id);

                        if (flashing) {
                            buyReasons.add(String.valueOf(id));
                        }

                        return String.valueOf(flashing);

                    }).toArray(String[]::new);

                    final String buyCondition = String.join(" ", buyStringArr);

                    final boolean buySignalFlashing = parser.parseExpression(buyCondition).getValue(Boolean.class);

                    if (buySignalFlashing) {

                        final TradeDetail tradeDetail = new TradeDetail();

                        tradeDetail.setPlacedAt(epochSeconds);
                        tradeDetail.setOrderID("HISTORICAL_ANALYSIS");
                        tradeDetail.setStatus("FILLED");
                        tradeDetail.setOrderSide("BUY");
                        tradeDetail.setOrderCondition(String.join(",", buyReasons));
                        tradeDetail.setTrade(trade);
                        trade.getTradeDetails().add(tradeDetail);
                        if (epochSeconds < trade.getFirstOrder())
                            trade.setFirstOrder(epochSeconds);
                        trade.setLastOrder(epochSeconds);

                        return;

                    }
                }

                final BigDecimal sharesToBeSold = orderAmount.divide(assetPrice, MathContext.DECIMAL32);

                if (trade.getSharesHeld().compareTo(sharesToBeSold) > 0) {
                    final List<String> sellReasons = new ArrayList<String>();

                    final String[] sellStringArr = Stream.of(trade.getParsedSellCondition().split(" ")).map(s -> {
                        if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                            return s;
                        }

                        final long id = Long.valueOf(s);

                        final boolean flashing = sellDetails.stream().anyMatch(detail -> detail.getId() == id);

                        if (flashing) {
                            sellReasons.add(String.valueOf(id));
                        }

                        return String.valueOf(flashing);

                    }).toArray(String[]::new);

                    final String sellCondition = String.join(" ", sellStringArr);

                    final boolean sellSignalFlashing = parser.parseExpression(sellCondition).getValue(Boolean.class);

                    if (sellSignalFlashing) {

                        final TradeDetail tradeDetail = new TradeDetail();

                        tradeDetail.setPlacedAt(epochSeconds);
                        tradeDetail.setOrderID("HISTORICAL_ANALYSIS");
                        tradeDetail.setStatus("FILLED");
                        tradeDetail.setOrderSide("SELL");
                        tradeDetail.setOrderCondition(String.join(",", sellReasons));
                        tradeDetail.setTrade(trade);
                        trade.getTradeDetails().add(tradeDetail);
                        if (epochSeconds < trade.getFirstOrder())
                            trade.setFirstOrder(epochSeconds);
                        trade.setLastOrder(epochSeconds);

                        return;
                    }
                }

            });

        } catch (final Exception e) {
            e.printStackTrace();
            response.setStatus("Exception: " + e.getMessage());
        }
    }
}
