package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.analysis.model.AssetDay;
import org.toasthub.stock.historicalanalysis.HistoricalAnalysisDao;
import org.toasthub.stock.model.HistoricalAnalysis;
import org.toasthub.stock.model.HistoricalDetail;
import org.toasthub.stock.model.Order;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;

@Service("HistoricalAnalyzingSvc")
public class HistoricalAnalyzingSvcImpl implements HistoricalAnalyzingSvc {

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected BuySignals buySignals;

    @Autowired
    protected HistoricalAnalysisDao historicalAnalysisDao;

    @Autowired
    protected HistoricalAnalyzingDao historicalAnalyzingDao;

    // Constructors
    public HistoricalAnalyzingSvcImpl() {
    }

    @Override
    public void process(Request request, Response response) {
        String action = (String) request.getParams().get("action");
        switch (action) {
            case "HISTORICALLY_ANALYZE_SWING_TRADE":
                historicallyAnalyzeSwingTrade(request, response);
                break;
            case "HISTORICALLY_ANALYZE_DAY_TRADE":
                historicallyAnalyzeDayTrade(request, response);
                break;
            default:
                break;
        }
    }

    public boolean evaluate(boolean alg1, boolean alg2, String operand) {
        boolean result = false;

        if (operand.equals(""))
            result = alg1;
        if (operand.equals("AND"))
            result = alg1 && alg2;
        if (operand.equals("OR"))
            result = alg1 || alg2;
        return result;
    }

    @SuppressWarnings("unchecked")
    private void historicallyAnalyzeSwingTrade(Request request, Response response) {
        try {
            Map<String, ?> map = (Map<String, ?>) request.getParam(GlobalConstant.ITEM);
            HistoricalAnalysis historicalAnalysis = new HistoricalAnalysis(map);
            int frequency = 0;
            if (((String) map.get("frequency")).equals("unlimited"))
                frequency = -1;
            else
                frequency = Integer.valueOf((String) map.get("frequency"));

            historicalAnalysis.setStartTime(
                    ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Long.valueOf((Integer) (map.get("startTime")))),
                            ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS).toEpochSecond());

            historicalAnalysis.setEndTime(
                    ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Long.valueOf((Integer) (map.get("endTime")))),
                            ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS).toEpochSecond());

            String algorithm = (String) map.get("algorithm");
            Set<HistoricalDetail> historicalDetails = new LinkedHashSet<HistoricalDetail>();

            String alg1 = algorithm;
            String operand = "";
            String alg2 = "";
            if (algorithm.contains(" ")) {
                alg1 = algorithm.substring(0, algorithm.indexOf(" "));
                operand = algorithm.substring(algorithm.indexOf(" ") + 1,
                        algorithm.indexOf((" "), algorithm.indexOf(" ") + 1));
                alg2 = algorithm.substring(algorithm.indexOf((" "), algorithm.indexOf(" ") + 1) + 1,
                        algorithm.length());
            }

            request.addParam(GlobalConstant.IDENTIFIER, "StockDay");
            historicalAnalyzingDao.items(request, response);
            List<AssetDay> stockDays = (List<AssetDay>) response.getParam(GlobalConstant.ITEMS);
            int startIndex = 0;
            int endIndex = 0;
            for (int i = 0; i < stockDays.size(); i++) {
                if (startIndex == 0) {
                    if (stockDays.get(i).getEpochSeconds() >= historicalAnalysis.getStartTime())
                        startIndex = i;
                }
                if (endIndex == 0) {
                    if (stockDays.get(i).getEpochSeconds() >= historicalAnalysis.getEndTime())
                        endIndex = i;
                }
            }
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal moneySpent = BigDecimal.ZERO;
            BigDecimal stockPrice = BigDecimal.ZERO;
            long currentTime;
            List<Order> orders = new ArrayList<Order>();
            request.addParam(GlobalConstant.SYMBOL, historicalAnalysis.getSymbol());

            int frequencyExecuted = 0;

            for (int i = startIndex; i < endIndex + 1; i++) {

                stockPrice = stockDays.get(i).getClose();
                currentTime = stockDays.get(i).getEpochSeconds();
                request.addParam("STOCKPRICE", stockPrice);
                request.addParam(GlobalConstant.EPOCHSECONDS, currentTime);

                if (evaluate(
                        buySignals.process(alg1, request, response),
                        buySignals.process(alg2, request, response),
                        operand)
                    && frequency != frequencyExecuted) {
                    frequencyExecuted++;
                    Order order = new Order(historicalAnalysis.getAmount(), stockPrice);
                    order.setTrailingStopPercent(historicalAnalysis.getTrailingStopPercent());
                    order.setTotalProfit(historicalAnalysis.getProfitLimit().multiply(stockPrice));
                    order.setBoughtAtTime(currentTime);
                    order.setHighPrice(stockPrice);
                    orders.add(order);
                    moneySpent = moneySpent.add(historicalAnalysis.getAmount());
                }

                for (int f = orders.size() - 1; f >= 0; f--) {
                    if (orders.get(f).getHighPrice().compareTo(stockPrice) < 0)
                        orders.get(f).setHighPrice(stockPrice);

                    if ((stockPrice.compareTo(orders.get(f).getTotalProfit())) >= 0) {
                        HistoricalDetail historicalDetail = new HistoricalDetail();
                        historicalDetail.setHistoricalAnalysis(historicalAnalysis);
                        historicalDetail.setBoughtAt(orders.get(f).getInitialPrice());
                        historicalDetail.setBoughtAtTime(orders.get(f).getBoughtAtTime());
                        historicalDetail.setHighPrice(orders.get(f).getHighPrice());
                        historicalDetail.setSoldAt(stockPrice);
                        historicalDetail.setSoldAtTime(currentTime);
                        historicalDetails.add(historicalDetail);
                        totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
                        orders.remove(f);
                    } else if ((stockPrice.divide(orders.get(f).getHighPrice(), MathContext.DECIMAL32))
                            .compareTo(orders.get(f).getTrailingStopPercent()) < 0) {
                        HistoricalDetail historicalDetail = new HistoricalDetail();
                        historicalDetail.setHistoricalAnalysis(historicalAnalysis);
                        historicalDetail.setBoughtAt(orders.get(f).getInitialPrice());
                        historicalDetail.setBoughtAtTime(orders.get(f).getBoughtAtTime());
                        historicalDetail.setHighPrice(orders.get(f).getHighPrice());
                        historicalDetail.setSoldAt(stockPrice);
                        historicalDetail.setSoldAtTime(currentTime);
                        historicalDetails.add(historicalDetail);
                        totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
                        orders.remove(f);
                    }
                }
            }
            for (int i = 0; i < orders.size(); i++) {
                totalValue = totalValue.add(orders.get(i).convertToDollars(stockPrice));
                HistoricalDetail historicalDetail = new HistoricalDetail();
                historicalDetail.setHistoricalAnalysis(historicalAnalysis);
                historicalDetail.setBoughtAtTime(orders.get(i).getBoughtAtTime());
                historicalDetail.setBoughtAt(orders.get(i).getInitialPrice());
                historicalDetail.setHighPrice(orders.get(i).getHighPrice());
                historicalDetails.add(historicalDetail);
            }
            historicalAnalysis.setHistoricalDetails(historicalDetails);
            historicalAnalysis.setHistoricalAnalysisType("Swing Trade");
            historicalAnalysis.setMoneySpent(moneySpent);
            historicalAnalysis.setTotalValue(totalValue);
            request.addParam(GlobalConstant.ITEM, historicalAnalysis);
            historicalAnalysisDao.save(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void historicallyAnalyzeDayTrade(Request request, Response response) {
        // Map<String, ?> map = (Map<String, ?>) request.getParam("ITEM");
        // HistoricalAnalysis historicalAnalysis = new HistoricalAnalysis();
        // String stockName = (String) map.get("stock");
        // String date = (String) map.get("startDate");
        // BigDecimal orderAmount = new BigDecimal((Integer) map.get("buyAmount"));
        // BigDecimal trailingStopPercent = new BigDecimal((Double)
        // map.get("trailingStopPercent"));
        // BigDecimal maxProfit = new BigDecimal((Double) map.get("profitLimit"));
        // historicalAnalysis.setStock((String) map.get("stock"));
        // String algorithm = (String) map.get("algorithm");
        // historicalAnalysis.setAlgorithm(algorithm);
        // historicalAnalysis.setStartDate((String) map.get("startDate"));
        // historicalAnalysis.setEndDate((String) map.get("endDate"));
        // historicalAnalysis.setBuyAmount(new BigDecimal((Integer)
        // map.get("buyAmount")));
        // historicalAnalysis.setSellAmount(new BigDecimal((Integer)
        // map.get("sellAmount")));
        // historicalAnalysis.setTrailingStopPercent(new BigDecimal((Double)
        // map.get("trailingStopPercent")));
        // historicalAnalysis.setProfitLimit(new BigDecimal((Double)
        // map.get("profitLimit")));
        // historicalAnalysis.setName((String) map.get("name"));

        // String alg1 = algorithm;
        // String operand = "";
        // String alg2 = "";
        // if (algorithm.contains(" ")) {
        // alg1 = algorithm.substring(0, algorithm.indexOf(" "));
        // operand = algorithm.substring(algorithm.indexOf(" ") + 1,
        // algorithm.indexOf((" "), algorithm.indexOf(" ") + 1));
        // alg2 = algorithm.substring(algorithm.indexOf((" "), algorithm.indexOf(" ") +
        // 1) + 1,
        // algorithm.length());
        // }

        // if ("".equals(stockName)) {
        // response.addParam("error", "Stock name is empty");
        // return;
        // }

        // List<StockBar> prestockBars = Functions.dayTradingBars(alpacaAPI, stockName,
        // date);
        // List<StockBar> overlapBars = Functions.dayTradingOverlapBars(alpacaAPI,
        // stockName, date);
        // List<StockBar> stockBars = new ArrayList<StockBar>(overlapBars);
        // stockBars.addAll(prestockBars);
        // BigDecimal totalValue = BigDecimal.ZERO;
        // BigDecimal moneySpent = BigDecimal.ZERO;
        // BigDecimal stockPrice = BigDecimal.ZERO;
        // List<Order> orders = new ArrayList<Order>(0);

        // for (int i = overlapBars.size(); i < stockBars.size(); i++) {

        // stockPrice = BigDecimal.valueOf(stockBars.get(i).getClose());
        // if (evaluate(buySignals.process(stockBars, i, alg1, stockName),
        // buySignals.process(stockBars, i, alg2, stockName),
        // operand)) {
        // orders.add(new Order(orderAmount, null, null, trailingStopPercent,
        // maxProfit.multiply(stockPrice), stockPrice));
        // moneySpent = moneySpent.add(orderAmount);

        // }

        // for (int f = orders.size() - 1; f >= 0; f--) {
        // if (orders.get(f).getHighPrice().compareTo(stockPrice) < 0)
        // orders.get(f).setHighPrice(stockPrice);

        // if ((stockPrice.compareTo(orders.get(f).getTotalProfit())) >= 0) {
        // totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
        // orders.remove(f);
        // } else if ((stockPrice.divide(orders.get(f).getHighPrice(),
        // MathContext.DECIMAL32))
        // .compareTo(orders.get(f).getTrailingStopPercent()) < 0) {
        // totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
        // orders.remove(f);
        // }
        // }
        // }
        // for (int i = 0; i < orders.size(); i++)
        // totalValue = totalValue.add(orders.get(i).convertToDollars(stockPrice));

        // historicalAnalysis.setType("Day Trade");
        // historicalAnalysis.setMoneySpent(moneySpent);
        // historicalAnalysis.setTotalValue(totalValue);
        // request.addParam(GlobalConstant.ITEM, historicalAnalysis);
        // try {
        // historicalAnalysisDao.save(request, response);
        // }catch(Exception e) {}

    }
}