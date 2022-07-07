package org.toasthub.stock.trade;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.toasthub.core.model.CustomTechnicalIndicator;
import org.toasthub.core.model.TradeSignalCache;
import org.toasthub.stock.custom_technical_indicator.CustomTechnicalIndicatorDao;
import org.toasthub.stock.model.Trade;
import org.toasthub.stock.model.TradeDetail;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;

@Component
public class TradeManager {
    final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected TradeDao tradeDao;

    @Autowired
    private TradeSignalCache tradeSignalCache;

    @Autowired
    private CustomTechnicalIndicatorDao customTechnicalIndicatorDao;

    public void updateTrades(final Request request, final Response response) {
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
                                    if (!trade.getIterations().equals("unlimited")) {
                                        trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
                                        if (trade.getIterationsExecuted() >= Integer
                                                .parseInt(trade.getIterations()))
                                            trade.setStatus("Not Running");
                                    }
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
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
                                    if (!trade.getIterations().equals("unlimited")) {
                                        trade.setIterationsExecuted(trade.getIterationsExecuted() + 1);
                                        if (trade.getIterationsExecuted() >= Integer
                                                .parseInt(trade.getIterations()))
                                            trade.setStatus("Not Running");
                                    }
                                    t.setFilledAt(order.getFilledAt().toEpochSecond());
                                    t.setAssetPrice(new BigDecimal(order.getAverageFillPrice()));
                                    t.setStatus("FILLED");
                                    t.setTrade(trade);
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

    public void checkTrades(final Request request, final Response response) {
        final List<Trade> trades = tradeDao.getRunningTrades();

        if (trades == null || trades.size() == 0) {
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

                    request.addParam(GlobalConstant.TRADE, trade);

                    System.out.println("Checking trade: " + trade.getName());

                    switch (trade.getOrderSide()) {
                        case Trade.BUY:
                            currentBuyTest(request, response);
                            break;
                        case Trade.SELL:
                            currentSellTest(request, response);
                            break;
                        case Trade.BOT:
                            currentBotTest(request, response);
                            break;
                        default:
                            return;
                    }

                    request.addParam(GlobalConstant.ITEM, trade);
                    try {
                        tradeDao.save(request, response);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public void currentBuyTest(final Request request, final Response response) {
        try {

            final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

            if (!trade.getIterations().equals("unlimited")
                    && trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations())) {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                return;
            }

            if (trade.getParseableBuyCondition() == null) {
                response.setStatus("Buy Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParseableBuyCondition().equals("")) {
                return;
            }

            final List<String> buyReasons = new ArrayList<String>();
            String sellOrderCondition = "";

            String[] stringArr = trade.getParseableBuyCondition().split(" ");
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
                    buyReasons.add(c.getName());
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
            double profitLimitPrice = 0;

            switch (trade.getCurrencyType()) {
                case "Dollars":
                    truncatedSharesAmount = trade.getCurrencyAmount()
                            .divide(tradeSignalCache.getRecentClosingPriceMap()
                                    .get("MINUTE::" + trade.getSymbol()),
                                    MathContext.DECIMAL32)
                            .intValue();
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
                            buyOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.BUY);
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
                tradeDetail.setOrderCondition(String.join(",", buyReasons));
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

        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }

    }

    public void currentSellTest(final Request request, final Response response) {
        try {

            final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);

            if (!trade.getIterations().equals("unlimited")
                    && trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations())) {
                System.out.println("Trade frequency met - changing status to not running");
                trade.setStatus("Not Running");
                return;
            }

            if (trade.getParseableSellCondition() == null) {
                System.out.println("Sell Condition is null in buy test for" + trade.getName());
                response.setStatus("Sell Condition is null in buy test for" + trade.getName());
                return;
            }

            if (trade.getParseableSellCondition().equals("")) {
                return;
            }

            final List<String> sellReasons = new ArrayList<String>();

            String[] stringArr = trade.getParseableSellCondition().split(" ");
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
                    sellReasons.add(c.getName());
                }

                return String.valueOf(bool);
            }).toArray(String[]::new);

            final String sellCondition = String.join(" ", stringArr);

            if (!parser.parseExpression(sellCondition).getValue(Boolean.class)) {
                System.out.println(trade.getName() + ":Sell Condition not met");
                return;
            }
            Order sellOrder = null;

            switch (trade.getOrderType()) {
                case "Market":

                    switch (trade.getCurrencyType()) {

                        case "Dollars":
                            sellOrder = alpacaAPI.orders().requestNotionalMarketOrder(trade.getSymbol(),
                                    trade.getCurrencyAmount().doubleValue(), OrderSide.SELL);
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

                if (!trade.getIterations().equals("unlimited")) {
                    if (trade.getIterationsExecuted() >= Integer.parseInt(trade.getIterations()))
                        trade.setStatus("Not Running");
                }
            }

            if (sellOrder != null) {
                final TradeDetail tradeDetail = new TradeDetail();
                tradeDetail.setPlacedAt(Instant.now().getEpochSecond());
                tradeDetail.setOrderID(sellOrder.getClientOrderId());
                tradeDetail.setStatus(sellOrder.getStatus().name());
                tradeDetail.setOrderSide("SELL");
                tradeDetail.setOrderCondition(String.join(",", sellReasons));
                tradeDetail.setTrade(trade);
                trade.getTradeDetails().add(tradeDetail);
            }
            System.out.println("Sell Order Placed!");

        } catch (final Exception e) {
            System.out.println("Not Executed!");
            e.printStackTrace();
        }
    }

    public void currentBotTest(final Request request, final Response response) {
        request.addParam("BOUGHT", false);

        final Trade trade = (Trade) request.getParam(GlobalConstant.TRADE);
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
                        .multiply(BigDecimal.valueOf(1.05))
                        .compareTo(orderAmount
                                .divide(tradeSignalCache.getRecentClosingPriceMap()
                                        .get("MINUTE::" + trade.getSymbol()),
                                        MathContext.DECIMAL32)) > 0) {
            currentSellTest(request, response);
        }
    }

}
