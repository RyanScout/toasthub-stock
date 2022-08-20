package org.toasthub.trade.trade;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.Symbol;
import org.toasthub.trade.model.Trade;
import org.toasthub.trade.model.TradeConstant;
import org.toasthub.trade.model.TradeDetail;
import org.toasthub.trade.model.TradeSignalCache;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderClass;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

@Component
public class TradeManager {
    final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    @Qualifier("TATradeDao")
    protected TradeDao tradeDao;

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    @Qualifier("TACustomTechnicalIndicatorDao")
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    public void updateTrades(final RestRequest request, final RestResponse response) {
        final List<Trade> trades = tradeDao.getAllRunningTrades();

        if (trades == null || trades.size() == 0) {
            return;
        }

        trades.stream().forEach(trade -> {
            trade.getTradeDetails().stream()
                    .forEach(t -> {
                        Order order = null;
                        try {
                            order = alpacaAPI.orders().getByClientID(t.getOrderID());
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        if (!order.getStatus().name().equals("FILLED")) {
                            return;
                        }
                        if (t.getOrderSide().equals("BUY")) {
                            switch (trade.getOrderSide()) {
                                case "Buy":
                                    trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    final boolean tradeHasUnlimitedIterations = trade.getIterations() == -1;
                                    if (!tradeHasUnlimitedIterations
                                            && trade.getIterationsExecuted() >= trade.getIterations()) {
                                        trade.setStatus("Not Running");
                                    }
                                    break;
                                case "Sell":
                                    System.out.println("Unknown case");
                                    break;
                                case "Bot":
                                    final BigDecimal orderQuantity = new BigDecimal(order.getFilledQuantity());
                                    final BigDecimal fillPrice = new BigDecimal(order.getAverageFillPrice()).setScale(2,
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
                                                            .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                                    .get("MINUTE::" + trade.getSymbol()))));

                                    t.setSharesHeld(trade.getSharesHeld());

                                    t.setAvailableBudget(trade.getAvailableBudget());

                                    t.setDollarAmount(
                                            (orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    t.setShareAmount(orderQuantity);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());

                                    t.setAssetPrice(fillPrice);

                                    t.setTotalValue(trade.getTotalValue());

                                    t.setStatus("FILLED");

                                    t.setTrade(trade);
                                    break;
                                default:
                                    System.out.println("Invalid orderside error");
                                    break;
                            }
                        }
                        if (t.getOrderSide().equals("SELL")) {
                            switch (trade.getOrderSide()) {
                                case "Buy":
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    break;
                                case "Sell":
                                    trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
                                    final boolean tradeHasUnlimitedIterations = trade.getIterations() == -1;
                                    if (!tradeHasUnlimitedIterations
                                            && trade.getIterationsExecuted() >= trade.getIterations()) {
                                        trade.setStatus("Not Running");
                                    }
                                    break;
                                case "Bot":
                                    final BigDecimal orderQuantity = new BigDecimal(order.getFilledQuantity());
                                    final BigDecimal fillPrice = new BigDecimal(order.getAverageFillPrice()).setScale(2,
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
                                                            .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                                                    .get("MINUTE::" + trade.getSymbol()))));

                                    t.setSharesHeld(trade.getSharesHeld());

                                    t.setAvailableBudget(trade.getAvailableBudget());

                                    t.setDollarAmount(
                                            (orderQuantity.multiply(fillPrice, MathContext.DECIMAL32))
                                                    .setScale(2, RoundingMode.HALF_UP));

                                    t.setShareAmount(orderQuantity);

                                    t.setFilledAt(order.getFilledAt().toEpochSecond());

                                    t.setAssetPrice(fillPrice);

                                    t.setTotalValue(trade.getTotalValue());

                                    t.setStatus("FILLED");

                                    t.setTrade(trade);
                                    break;
                                default:
                                    System.out.println("Invalid orderside error");
                                    break;
                            }
                        }
                    });
            try {
                trade.setTotalValue(trade.getAvailableBudget().add(trade.getSharesHeld()
                        .multiply(tradeSignalCache.getRecentClosingPriceMap()
                                .get("MINUTE::" + trade.getSymbol()))));

                request.addParam(GlobalConstant.ITEM, trade);
                tradeDao.save(request, response);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void checkTrades(final RestRequest request, final RestResponse response) {
        final List<Trade> trades = tradeDao.getRunningTrades();

        if (trades.size() == 0) {
            System.out.println("No trades to run");
            return;
        }

        final long today = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/New_York"))
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond();

        trades.stream()
                .filter(trade -> request.getParam(trade.getSymbol() + "::CACHE_UPDATED") != null)
                .filter(trade -> (boolean) request.getParam(trade.getSymbol() + "::CACHE_UPDATED"))
                .filter(trade -> !(trade.getEvaluationPeriod().equals("DAY") && trade.getLastOrder() > today))
                .forEach(trade -> {

                    final long currentTime = Instant.now().getEpochSecond();

                    final BigDecimal currentPrice = tradeSignalCache.getRecentClosingPriceMap()
                            .get("MINUTE::" + trade.getSymbol());

                    if (trade.getFirstCheck() == 0) {
                        trade.setFirstCheck(currentTime);
                        trade.setFirstCheckPrice(currentPrice);
                    }

                    trade.setLastCheck(currentTime);

                    trade.setLastCheckPrice(currentPrice);

                    System.out.println("Checking trade: " + trade.getName());

                    try {
                        switch (trade.getOrderSide().toUpperCase()) {
                            case Trade.BUY: {
                                final String flashingBuyCondition = getFlashingBuyCondition(trade);

                                final boolean buyConditionIsFlashing = !flashingBuyCondition.equals("");

                                if (buyConditionIsFlashing) {
                                    placeBuyOrder(trade, flashingBuyCondition);

                                    if (trade.getFirstOrder() == 0) {
                                        trade.setFirstOrder(Instant.now().getEpochSecond());
                                    }

                                    trade.setLastOrder(Instant.now().getEpochSecond());

                                    System.out.println("Buy order placed: " + trade.getName());
                                }

                                break;
                            }
                            case Trade.SELL: {
                                final String flashingSellCondition = getFlashingSellCondition(trade);

                                final boolean sellConditionIsFlashing = !flashingSellCondition.equals("");

                                if (sellConditionIsFlashing) {
                                    placeSellOrder(trade, flashingSellCondition);

                                    if (trade.getFirstOrder() == 0) {
                                        trade.setFirstOrder(Instant.now().getEpochSecond());
                                    }

                                    trade.setLastOrder(Instant.now().getEpochSecond());
                                    System.out.println("Sell order placed: " + trade.getName());
                                }

                                break;
                            }
                            case Trade.BOT: {

                                final BigDecimal availableBudget = trade.getAvailableBudget();
                                final String currencyType = trade.getCurrencyType().toUpperCase();
                                final BigDecimal orderAmount;

                                if (currencyType.equals("DOLLARS")) {
                                    orderAmount = trade.getCurrencyAmount();
                                } else if (currencyType.equals("SHARES")) {
                                    orderAmount = trade.getCurrencyAmount().multiply(currentPrice);
                                } else {
                                    throw new Exception("Currency type unrecognized");
                                }

                                if (availableBudget.compareTo(orderAmount.multiply(new BigDecimal("1.10"))) > 0) {

                                    final String flashingBuyCondition = getFlashingBuyCondition(trade);

                                    final boolean buyConditionIsFlashing = !flashingBuyCondition.equals("");

                                    if (buyConditionIsFlashing) {
                                        placeBuyOrder(trade, flashingBuyCondition);
                                        System.out.println("Buy order placed: " + trade.getName());
                                    } else {
                                        final String flashingSellCondition = getFlashingSellCondition(trade);

                                        final boolean sellConditionIsFlashing = !flashingSellCondition.equals("");

                                        if (sellConditionIsFlashing) {

                                            placeSellOrder(trade, flashingSellCondition);

                                            if (trade.getFirstOrder() == 0) {
                                                trade.setFirstOrder(Instant.now().getEpochSecond());
                                            }

                                            trade.setLastOrder(Instant.now().getEpochSecond());
                                            System.out.println("Sell order placed: " + trade.getName());
                                        }
                                    }

                                } else {
                                    final BigDecimal sharesHeld = trade.getSharesHeld();
                                    final BigDecimal sharesToBeBought = orderAmount.divide(currentPrice,
                                            MathContext.DECIMAL32);
                                    if (sharesHeld.compareTo(sharesToBeBought.multiply(new BigDecimal("1.10"))) > 0) {
                                        final String flashingSellCondition = getFlashingSellCondition(trade);

                                        final boolean sellConditionIsFlashing = !flashingSellCondition.equals("");

                                        if (sellConditionIsFlashing) {

                                            placeSellOrder(trade, flashingSellCondition);

                                            if (trade.getFirstOrder() == 0) {
                                                trade.setFirstOrder(Instant.now().getEpochSecond());
                                            }

                                            trade.setLastOrder(Instant.now().getEpochSecond());
                                        }
                                    }
                                }

                                break;
                            }
                            default:
                                throw new Exception("INVALID ORDERSIDE");
                        }

                    } catch (final Exception e) {
                        trade.setStatus("ERROR");
                        trade.setStatusMessage(e.getMessage());
                        e.printStackTrace();
                    }

                    tradeDao.saveItem(trade);
                });
    }

    public String getFlashingBuyCondition(Trade trade) throws Exception {

        if (trade.getParsedBuyCondition() == "") {
            final String statusMessage = "Trade is expected to have buy condition but none is present";
            throw new Exception(statusMessage);
        }

        final List<String> flashingBuyConditions = new ArrayList<String>();

        // Converts buy condition to a logical statement to be evaluated
        // Records all flashing buy conditions while converting
        final String logicalBuyCondition = String.join(" ",
                Stream.of(trade.getParsedBuyCondition().split(" ")).map(s -> {
                    if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
                        return s;
                    }

                    final long id = Long.valueOf(s);

                    final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao.findById(id);

                    final boolean buyConditionIsFlashing = tradeSignalCache.getTechnicalIndicatorMap()
                            .get(customTechnicalIndicator.getTechnicalIndicatorType() + "::"
                                    + customTechnicalIndicator.getTechnicalIndicatorKey() + "::"
                                    + customTechnicalIndicator.getEvaluationPeriod() + "::"
                                    + trade.getSymbol())
                            .isFlashing();

                    if (buyConditionIsFlashing) {
                        flashingBuyConditions.add(String.valueOf(customTechnicalIndicator.getId()));
                    }

                    return String.valueOf(buyConditionIsFlashing);
                }).toArray(String[]::new));

        final boolean logicalBuyConditionIsFlashing = parser.parseExpression(logicalBuyCondition)
                .getValue(Boolean.class);
        // Indicates no reason to buy
        if (!logicalBuyConditionIsFlashing) {
            return "";
        }

        final String listedFlashingBuyConditions = String.join(", ", flashingBuyConditions);

        return listedFlashingBuyConditions;
    }

    public String getFlashingSellCondition(Trade trade) throws Exception {

        if (trade.getParsedSellCondition() == "") {
            final String statusMessage = "Trade is expected to have sell condition but none is present";
            throw new Exception(statusMessage);
        }

        final List<String> flashingSellConditions = new ArrayList<String>();

        // Converts buy condition to a logical statement to be evaluated
        // Records all flashing buy conditions while converting
        final String logicalSellCondition = String.join(" ",
                Stream.of(trade.getParsedSellCondition().split(" ")).map(s -> {
                    if (Arrays.asList("(", ")", "||", "&&", "").contains(s)) {
                        return s;
                    }

                    final long id = Long.valueOf(s);

                    final CustomTechnicalIndicator customTechnicalIndicator = customTechnicalIndicatorDao.findById(id);

                    final boolean sellConditionIsFlashing = tradeSignalCache.getTechnicalIndicatorMap()
                            .get(customTechnicalIndicator.getTechnicalIndicatorType() + "::"
                                    + customTechnicalIndicator.getTechnicalIndicatorKey() + "::"
                                    + customTechnicalIndicator.getEvaluationPeriod() + "::"
                                    + trade.getSymbol())
                            .isFlashing();

                    if (sellConditionIsFlashing) {
                        flashingSellConditions.add(String.valueOf(customTechnicalIndicator.getId()));
                    }

                    return String.valueOf(sellConditionIsFlashing);
                }).toArray(String[]::new));

        final boolean logicalSellConditionIsFlashing = parser.parseExpression(logicalSellCondition)
                .getValue(Boolean.class);
        // Indicates no reason to buy
        if (!logicalSellConditionIsFlashing) {
            return "";
        }

        final String listedFlashingSellConditions = String.join(", ", flashingSellConditions);

        return listedFlashingSellConditions;
    }

    public void placeBuyOrder(Trade trade, String flashingBuyCondition) throws Exception {
        String sellOrderCondition = "";
        Order sellOrder = null;
        Order buyOrder = null;
        int truncatedSharesAmount = 0;
        double profitLimitPrice = 0;

        final String symbol = trade.getSymbol();
        final BigDecimal currentPrice = tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol);
        final boolean tradeIsStockTrade = Symbol.STOCK_SYMBOLS.contains(symbol);
        final String currencyType = trade.getCurrencyType().toUpperCase();
        final String orderType = trade.getOrderType().toUpperCase();
        final double effectiveShareAmount;

        if (currencyType.equals("DOLLARS")) {
            effectiveShareAmount = trade.getCurrencyAmount().divide(currentPrice, MathContext.DECIMAL32).doubleValue();
        } else {
            effectiveShareAmount = trade.getCurrencyAmount().doubleValue();
        }

        switch (orderType) {
            case "MARKET":
                switch (currencyType) {
                    case "DOLLARS":
                        if (tradeIsStockTrade) {
                            buyOrder = alpacaAPI.orders().requestOrder(
                                    symbol,
                                    effectiveShareAmount,
                                    null,
                                    OrderSide.BUY,
                                    OrderType.MARKET,
                                    OrderTimeInForce.DAY,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    null,
                                    OrderClass.SIMPLE,
                                    null,
                                    null,
                                    null);
                        } else {
                            buyOrder = alpacaAPI.orders().requestOrder(
                                    symbol,
                                    effectiveShareAmount,
                                    null,
                                    OrderSide.BUY,
                                    OrderType.MARKET,
                                    OrderTimeInForce.GOOD_UNTIL_CANCELLED,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    null,
                                    OrderClass.SIMPLE,
                                    null,
                                    null,
                                    null);
                        }
                        break;
                    case "SHARES":
                        buyOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                        break;
                    default:
                        throw new Exception("Currency type does not match");
                }
                break;

            case Trade.TRAILING_STOP:
                switch (trade.getTrailingStopType()) {
                    case "Trailing Stop Price":
                        buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                truncatedSharesAmount, OrderSide.BUY,
                                OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                false);
                        break;
                    case "Trailing Stop Percent":
                        buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                truncatedSharesAmount, OrderSide.BUY,
                                OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                false);
                        break;
                    default:
                        throw new Exception("Trailing stop type does not match");
                }
                break;

            case Trade.PROFIT_LIMIT:

                switch (trade.getProfitLimitType()) {
                    case "Profit Limit Price":
                        profitLimitPrice = trade.getProfitLimitAmount()
                                .add(tradeSignalCache.getRecentClosingPriceMap()
                                        .get("MINUTE::" + trade.getSymbol()))
                                .doubleValue();
                        break;
                    case "Profit Limit Percent":
                        profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                .multiply(
                                        tradeSignalCache.getRecentClosingPriceMap()
                                                .get("MINUTE::" + trade.getSymbol()))
                                .doubleValue();
                        break;
                    default:
                        throw new Exception("Profit limit type does not match");
                }
                buyOrder = alpacaAPI.orders().requestMarketOrder(trade.getSymbol(), truncatedSharesAmount,
                        OrderSide.BUY, OrderTimeInForce.DAY);
                sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                        OrderSide.SELL,
                        OrderTimeInForce.DAY, profitLimitPrice, false);
                sellOrderCondition = "Profit Limit";
                break;

            case Trade.TRAILING_STOP_PROFIT_LIMIT:

                switch (trade.getTrailingStopType()) {
                    case "Trailing Stop Price":
                        buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                truncatedSharesAmount, OrderSide.BUY,
                                OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                false);
                        break;
                    case "Trailing Stop Percent":
                        buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                truncatedSharesAmount, OrderSide.BUY,
                                OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                false);
                        break;
                    default:
                        throw new Exception("Trailing stop type does not match");
                }

                switch (trade.getProfitLimitType()) {
                    case "Profit Limit Price":
                        profitLimitPrice = trade.getProfitLimitAmount()
                                .add(tradeSignalCache.getRecentClosingPriceMap()
                                        .get("MINUTE::" + trade.getSymbol()))
                                .doubleValue();
                        break;
                    case "Profit Limit Percent":
                        profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                .multiply(
                                        tradeSignalCache.getRecentClosingPriceMap()
                                                .get("MINUTE::" + trade.getSymbol()))
                                .doubleValue();
                        break;
                    default:
                        throw new Exception("Profit limit type does not match");
                }

                sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                        OrderSide.SELL,
                        OrderTimeInForce.DAY, profitLimitPrice, false);
                sellOrderCondition = "Profit Limit";
                break;

            default:
                throw new Exception("Case Not found!");

        }

        if (buyOrder != null) {
            final TradeDetail tradeDetail = new TradeDetail();
            tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
            tradeDetail.setOrderID(buyOrder.getClientOrderId());
            tradeDetail.setStatus(buyOrder.getStatus().name());
            tradeDetail.setOrderSide("BUY");
            tradeDetail.setOrderCondition(flashingBuyCondition);
            tradeDetail.setTrade(trade);
            tradeDao.saveItem(tradeDetail);
        }

        if (sellOrder != null) {
            final TradeDetail tradeDetail = new TradeDetail();
            tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
            tradeDetail.setOrderID(buyOrder.getClientOrderId());
            tradeDetail.setStatus(buyOrder.getStatus().name());
            tradeDetail.setOrderSide("SELL");
            tradeDetail.setOrderCondition(sellOrderCondition);
            tradeDetail.setTrade(trade);
            tradeDao.saveItem(tradeDetail);
        }

    }

    public void placeSellOrder(Trade trade, String flashingSellCondition) throws Exception {
        Order sellOrder = null;

        int truncatedSharesAmount = 0;
        BigDecimal shareAmount = BigDecimal.ZERO;

        String sellOrderCondition = "";
        Order buyOrder = null;
        double profitLimitPrice = 0;

        final String symbol = trade.getSymbol();
        final BigDecimal currentPrice = tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + symbol);
        final boolean tradeIsStockTrade = Symbol.STOCK_SYMBOLS.contains(symbol);
        final String currencyType = trade.getCurrencyType().toUpperCase();
        final String orderType = trade.getOrderType().toUpperCase();
        final double effectiveShareAmount;

        if (currencyType.equals("DOLLARS")) {
            effectiveShareAmount = trade.getCurrencyAmount().divide(currentPrice, MathContext.DECIMAL32).doubleValue();
        } else {
            effectiveShareAmount = trade.getCurrencyAmount().doubleValue();
        }

        switch (orderType) {
            case Trade.MARKET:

                switch (currencyType) {

                    case "DOLLARS":
                        sellOrder = alpacaAPI.orders().requestOrder(
                                symbol,
                                effectiveShareAmount,
                                null,
                                OrderSide.SELL,
                                OrderType.MARKET,
                                OrderTimeInForce.GOOD_UNTIL_CANCELLED,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null,
                                OrderClass.SIMPLE,
                                null,
                                null,
                                null);
                        break;
                    case "SHARES":
                        sellOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                trade.getCurrencyAmount().doubleValue(), OrderSide.SELL);
                        break;
                    default:
                        throw new Exception("Currency type does not match");
                }
                break;

            default:
                throw new Exception("Case Not found!");

        }

        if (sellOrder != null) {
            final TradeDetail tradeDetail = new TradeDetail();
            tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
            tradeDetail.setOrderID(sellOrder.getClientOrderId());
            tradeDetail.setStatus(sellOrder.getStatus().name());
            tradeDetail.setOrderSide("SELL");
            tradeDetail.setOrderCondition(flashingSellCondition);
            tradeDetail.setTrade(trade);
            tradeDao.saveItem(tradeDetail);
        }
    }

    public void currentBuyTest(final RestRequest request, final RestResponse response) {
        try {
            final Trade trade = (Trade) request.getParam(TradeConstant.TRADE);

            if (trade.getParsedBuyCondition() == null) {
                response.setStatus("Buy Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParsedBuyCondition().equals("")) {
                return;
            }

            final List<String> buyReasons = new ArrayList<String>();
            String sellOrderCondition = "";

            String[] stringArr = trade.getParsedBuyCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                final boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
                        .get(c.getTechnicalIndicatorType() + "::"
                                + c.getTechnicalIndicatorKey() + "::"
                                + c.getEvaluationPeriod() + "::"
                                + trade.getSymbol())
                        .isFlashing();

                if (bool) {
                    buyReasons.add(String.valueOf(c.getId()));
                }

                return String.valueOf(bool);
            }).toArray(String[]::new);

            final String buyCondition = String.join(" ", stringArr);

            if (!parser.parseExpression(buyCondition).getValue(Boolean.class)) {
                System.out.println(trade.getName() + ":Buy Condition not met");
                return;
            }

            Order sellOrder = null;
            Order buyOrder = null;
            int truncatedSharesAmount = 0;
            BigDecimal shareAmount = BigDecimal.ZERO;
            double profitLimitPrice = 0;

            switch (trade.getCurrencyType()) {
                case "Dollars":
                    truncatedSharesAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32)
                            .intValue();
                    shareAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32);
                    break;
                case "Shares":
                    truncatedSharesAmount = trade.getCurrencyAmount().intValue();
                    break;
                default:
                    break;
            }
            switch (trade.getOrderType()) {
                case "Market":

                    switch (trade.getCurrencyType()) {

                        case "Dollars":
                            if (Symbol.CRYPTO_SYMBOLS.contains(trade.getSymbol())) {
                                buyOrder = alpacaAPI.orders().requestOrder(
                                        trade.getSymbol(),
                                        shareAmount.doubleValue(),
                                        null,
                                        OrderSide.BUY,
                                        OrderType.MARKET,
                                        OrderTimeInForce.GOOD_UNTIL_CANCELLED,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        OrderClass.SIMPLE,
                                        null,
                                        null,
                                        null);
                            } else {
                                buyOrder = alpacaAPI.orders().requestOrder(
                                        trade.getSymbol(),
                                        shareAmount.doubleValue(),
                                        null,
                                        OrderSide.BUY,
                                        OrderType.MARKET,
                                        OrderTimeInForce.DAY,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null,
                                        OrderClass.SIMPLE,
                                        null,
                                        null,
                                        null);
                            }
                            break;
                        case "Shares":
                            buyOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
                            break;
                        default:
                            System.out.println("Currency type does not match");
                            break;
                    }
                    break;

                case "Trailing Stop":
                    switch (trade.getTrailingStopType()) {
                        case "Trailing Stop Price":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        case "Trailing Stop Percent":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        default:
                            System.out.println("Trailing stop type does not match");
                            break;
                    }
                    break;

                case "Profit Limit":

                    switch (trade.getProfitLimitType()) {
                        case "Profit Limit Price":
                            profitLimitPrice = trade.getProfitLimitAmount()
                                    .add(tradeSignalCache.getRecentClosingPriceMap()
                                            .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        case "Profit Limit Percent":
                            profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                    .multiply(
                                            tradeSignalCache.getRecentClosingPriceMap()
                                                    .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        default:
                            System.out.println("Profit limit type does not match");
                            break;
                    }
                    buyOrder = alpacaAPI.orders().requestMarketOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.BUY, OrderTimeInForce.DAY);
                    sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.SELL,
                            OrderTimeInForce.DAY, profitLimitPrice, false);
                    sellOrderCondition = "Profit Limit";
                    break;

                case "Trailing Stop & Profit Limit":

                    switch (trade.getTrailingStopType()) {
                        case "Trailing Stop Price":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPriceOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        case "Trailing Stop Percent":
                            buyOrder = alpacaAPI.orders().requestTrailingStopPercentOrder(trade.getSymbol(),
                                    truncatedSharesAmount, OrderSide.BUY,
                                    OrderTimeInForce.DAY, trade.getTrailingStopAmount().doubleValue(),
                                    false);
                            break;
                        default:
                            System.out.println("Trailing stop type does not match");
                            break;
                    }

                    switch (trade.getProfitLimitType()) {
                        case "Profit Limit Price":
                            profitLimitPrice = trade.getProfitLimitAmount()
                                    .add(tradeSignalCache.getRecentClosingPriceMap()
                                            .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        case "Profit Limit Percent":
                            profitLimitPrice = trade.getProfitLimitAmount().movePointLeft(2).add(BigDecimal.ONE)
                                    .multiply(
                                            tradeSignalCache.getRecentClosingPriceMap()
                                                    .get("MINUTE::" + trade.getSymbol()))
                                    .doubleValue();
                            break;
                        default:
                            System.out.println("Profit limit type does not match");
                            break;
                    }

                    sellOrder = alpacaAPI.orders().requestLimitOrder(trade.getSymbol(), truncatedSharesAmount,
                            OrderSide.SELL,
                            OrderTimeInForce.DAY, profitLimitPrice, false);
                    sellOrderCondition = "Profit Limit";
                    break;

                case "Cascading Trailing Stop":

                    break;

                default:
                    System.out.println("Case Not found!");
                    break;

            }

            request.addParam("BOUGHT", true);

            if (buyOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("BUY");
                tradeDetail.setOrderCondition(String.join(", ", buyReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
                if (trade.getFirstOrder() == 0)
                    trade.setFirstOrder(Instant.now().getEpochSecond());
                trade.setLastOrder(Instant.now().getEpochSecond());
            }

            if (sellOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(buyOrder.getClientOrderId());
                tradeDetail.setStatus(buyOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(sellOrderCondition);
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
                trade.setLastOrder(Instant.now().getEpochSecond());
            }

            System.out.println(trade.getName() + ":Buy Order Placed!");

        } catch (final AlpacaClientException e) {
        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }

    }

    public void currentSellTest(final RestRequest request, final RestResponse response) {
        try {

            final Trade trade = (Trade) request.getParam(TradeConstant.TRADE);

            if (trade.getParsedSellCondition() == null) {
                System.out.println("Sell Condition is null in buy test for" + trade.getName());
                response.setStatus("Sell Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParsedSellCondition().equals("")) {
                return;
            }

            final List<String> sellReasons = new ArrayList<String>();

            String[] stringArr = trade.getParsedSellCondition().split(" ");
            stringArr = Stream.of(stringArr).map(s -> {
                if (s.equals("(") || s.equals(")") || s.equals("||") || s.equals("&&")) {
                    return s;
                }
                request.addParam(GlobalConstant.ITEMID, s);
                try {
                    customTechnicalIndicatorDao.item(request, response);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final CustomTechnicalIndicator c = ((CustomTechnicalIndicator) response.getParam(GlobalConstant.ITEM));

                final boolean bool = tradeSignalCache.getTechnicalIndicatorMap()
                        .get(c.getTechnicalIndicatorType() + "::"
                                + c.getTechnicalIndicatorKey() + "::"
                                + c.getEvaluationPeriod() + "::"
                                + trade.getSymbol())
                        .isFlashing();

                if (bool) {
                    sellReasons.add(String.valueOf(c.getId()));
                }

                return String.valueOf(bool);
            }).toArray(String[]::new);

            final String sellCondition = String.join(" ", stringArr);

            if (!parser.parseExpression(sellCondition).getValue(Boolean.class)) {
                System.out.println(trade.getName() + ":Sell Condition not met");
                return;
            }

            Order sellOrder = null;

            int truncatedSharesAmount = 0;
            BigDecimal shareAmount = BigDecimal.ZERO;
            switch (trade.getCurrencyType()) {
                case "Dollars":
                    truncatedSharesAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32)
                            .intValue();
                    shareAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32);
                    break;
                case "Shares":
                    truncatedSharesAmount = trade.getCurrencyAmount().intValue();
                    break;
                default:
                    break;
            }

            switch (trade.getOrderType()) {
                case "Market":

                    switch (trade.getCurrencyType()) {

                        case "Dollars":
                            sellOrder = alpacaAPI.orders().requestOrder(
                                    trade.getSymbol(),
                                    shareAmount.doubleValue(),
                                    null,
                                    OrderSide.SELL,
                                    OrderType.MARKET,
                                    OrderTimeInForce.GOOD_UNTIL_CANCELLED,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    null,
                                    OrderClass.SIMPLE,
                                    null,
                                    null,
                                    null);
                            break;
                        case "Shares":
                            sellOrder = alpacaAPI.orders().requestFractionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.SELL);
                            break;
                        default:
                            System.out.println("Currency type does not match");
                            break;
                    }
                    break;

                default:
                    System.out.println("Case Not found!");
                    break;

            }
            if (trade.getOrderSide().equals("Sell")) {
                trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
            }

            if (sellOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(sellOrder.getClientOrderId());
                tradeDetail.setStatus(sellOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(String.join(", ", sellReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
            }
            System.out.println("Sell Order Placed!");

        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }
    }

    public void currentBotTest(final RestRequest request, final RestResponse response) {
        request.addParam("BOUGHT", false);

        final Trade trade = (Trade) request.getParam(TradeConstant.TRADE);
        BigDecimal orderAmount = BigDecimal.ZERO;

        switch (trade.getCurrencyType()) {
            case "Dollars":
                orderAmount = trade.getCurrencyAmount();
                break;
            case "Shares":
                orderAmount = trade.getCurrencyAmount()
                        .multiply(tradeSignalCache.getRecentClosingPriceMap().get("MINUTE::" + trade.getSymbol()));
                break;
            default:
                System.out.println("Invalid currency type at current bot test");
                break;
        }

        if (trade.getAvailableBudget().compareTo(orderAmount.multiply(new BigDecimal(1.05))) > 0) {
            currentBuyTest(request, response);
        }

        if ((!(boolean) request.getParam("BOUGHT"))
                && trade.getSharesHeld()
                        .compareTo(
                                (orderAmount
                                        .divide(tradeSignalCache.getRecentClosingPriceMap()
                                                .get("MINUTE::" + trade.getSymbol()),
                                                MathContext.DECIMAL32))
                                        .multiply(new BigDecimal("1.10"))) > 0) {
            currentSellTest(request, response);
        }
    }

}
