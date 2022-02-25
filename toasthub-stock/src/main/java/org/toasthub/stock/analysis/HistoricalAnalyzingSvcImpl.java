package org.toasthub.stock.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.stock.historicalanalysis.HistoricalAnalysisDao;
import org.toasthub.stock.model.HistoricalAnalysis;
import org.toasthub.stock.model.HistoricalDetail;
import org.toasthub.stock.model.Order;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;

@Service("HistoricalAnalyzingSvc")
public class HistoricalAnalyzingSvcImpl implements HistoricalAnalyzingSvc {

    @Autowired
    protected AlpacaAPI alpacaAPI;

    @Autowired
    protected BuySignals buySignals;

    @Autowired
    protected HistoricalAnalysisDao historicalAnalysisDao;

    // Constructors
    public HistoricalAnalyzingSvcImpl() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void process(Request request, Response response){
        String action = (String) request.getParams().get("action");
        switch (action) {
            case "SWING_TRADE_HISTORICAL_ANALYSIS":
                swingTradehistoricalAnalysis(request, response);
                break;
            case "DAY_TRADE_HISTORICAL_ANALYSIS":
                dayTradehistoricalAnalysis(request, response);
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
    private void swingTradehistoricalAnalysis(Request request, Response response){
        Map<String, ?> map = (Map<String, ?>) request.getParam("ITEM");
        HistoricalAnalysis historicalAnalysis = new HistoricalAnalysis(map);
        String algorithum = (String) map.get("algorithum");
        Set<HistoricalDetail> historicalDetails = new LinkedHashSet<HistoricalDetail>();

        String alg1 = algorithum;
        String operand = "";
        String alg2 = "";
        if (algorithum.contains(" ")) {
            alg1 = algorithum.substring(0, algorithum.indexOf(" "));
            operand = algorithum.substring(algorithum.indexOf(" ") + 1,
                    algorithum.indexOf((" "), algorithum.indexOf(" ") + 1));
            alg2 = algorithum.substring(algorithum.indexOf((" "), algorithum.indexOf(" ") + 1) + 1,
                    algorithum.length());
        }

        List<StockBar> prestockBars = Functions.swingTradingBars(alpacaAPI, historicalAnalysis.getStock(), historicalAnalysis.getStartDate(), historicalAnalysis.getEndDate());
        List<StockBar> overlapBars = Functions.swingTradingOverlapBars(alpacaAPI, historicalAnalysis.getStock(), historicalAnalysis.getStartDate());
        List<StockBar> stockBars = new ArrayList<StockBar>(overlapBars);
        stockBars.addAll(prestockBars);
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal moneySpent = BigDecimal.ZERO;
        BigDecimal stockPrice = BigDecimal.ZERO;
        long currentTime;
        List<Order> orders = new ArrayList<Order>();

        for (int i = overlapBars.size(); i < stockBars.size(); i++) {

            stockPrice = BigDecimal.valueOf(stockBars.get(i).getClose());
            currentTime = stockBars.get(i).getTimestamp().toEpochSecond();
            if (evaluate(
                buySignals.process(stockBars, i, alg1, historicalAnalysis.getStock()),
                buySignals.process(stockBars, i, alg2, historicalAnalysis.getStock()),
                operand)){
                Order order = new Order(historicalAnalysis.getBuyAmount() , stockPrice);
                order.setTrailingStopPercent(historicalAnalysis.getTrailingStopPercent());
                order.setTotalProfit(historicalAnalysis.getProfitLimit().multiply(stockPrice));
                order.setBoughtAtTime(currentTime);
                order.setHighPrice(stockPrice);
                orders.add(order);
                moneySpent = moneySpent.add(historicalAnalysis.getBuyAmount());
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
        historicalAnalysis.setType("Swing Trade");
        historicalAnalysis.setMoneySpent(moneySpent);
        historicalAnalysis.setTotalValue(totalValue);
        request.addParam(GlobalConstant.ITEM, historicalAnalysis);
        try {
        historicalAnalysisDao.save(request,response);
        }catch(Exception e){}
    }

    @SuppressWarnings("unchecked")
    private void dayTradehistoricalAnalysis(Request request, Response response){
        Map<String, ?> map = (Map<String, ?>) request.getParam("ITEM");
        HistoricalAnalysis historicalAnalysis = new HistoricalAnalysis();
        String stockName = (String) map.get("stock");
        String date = (String) map.get("startDate");
        BigDecimal orderAmount = new BigDecimal((Integer) map.get("buyAmount"));
        BigDecimal trailingStopPercent = new BigDecimal((Double) map.get("trailingStopPercent"));
        BigDecimal maxProfit = new BigDecimal((Double) map.get("profitLimit"));
        historicalAnalysis.setStock((String) map.get("stock"));
        String algorithum = (String) map.get("algorithum");
        historicalAnalysis.setAlgorithum(algorithum);
        historicalAnalysis.setStartDate((String) map.get("startDate"));
        historicalAnalysis.setEndDate((String) map.get("endDate"));
        historicalAnalysis.setBuyAmount(new BigDecimal((Integer) map.get("buyAmount")));
        historicalAnalysis.setSellAmount(new BigDecimal((Integer) map.get("sellAmount")));
        historicalAnalysis.setTrailingStopPercent(new BigDecimal((Double) map.get("trailingStopPercent")));
        historicalAnalysis.setProfitLimit(new BigDecimal((Double) map.get("profitLimit")));
        historicalAnalysis.setName((String) map.get("name"));

        String alg1 = algorithum;
        String operand = "";
        String alg2 = "";
        if (algorithum.contains(" ")) {
            alg1 = algorithum.substring(0, algorithum.indexOf(" "));
            operand = algorithum.substring(algorithum.indexOf(" ") + 1,
                    algorithum.indexOf((" "), algorithum.indexOf(" ") + 1));
            alg2 = algorithum.substring(algorithum.indexOf((" "), algorithum.indexOf(" ") + 1) + 1,
                    algorithum.length());
        }

        if ("".equals(stockName)) {
            response.addParam("error", "Stock name is empty");
            return;
        }

        List<StockBar> prestockBars = Functions.dayTradingBars(alpacaAPI, stockName, date);
        List<StockBar> overlapBars = Functions.dayTradingOverlapBars(alpacaAPI, stockName, date);
        List<StockBar> stockBars = new ArrayList<StockBar>(overlapBars);
        stockBars.addAll(prestockBars);
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal moneySpent = BigDecimal.ZERO;
        BigDecimal stockPrice = BigDecimal.ZERO;
        List<Order> orders = new ArrayList<Order>(0);

        for (int i = overlapBars.size(); i < stockBars.size(); i++) {

            stockPrice = BigDecimal.valueOf(stockBars.get(i).getClose());
            if (evaluate(buySignals.process(stockBars, i, alg1, stockName),
                    buySignals.process(stockBars, i, alg2, stockName),
                    operand)) {
                orders.add(new Order(orderAmount, null, null, trailingStopPercent,
                        maxProfit.multiply(stockPrice), stockPrice));
                moneySpent = moneySpent.add(orderAmount);

            }

            for (int f = orders.size() - 1; f >= 0; f--) {
                if (orders.get(f).getHighPrice().compareTo(stockPrice) < 0)
                    orders.get(f).setHighPrice(stockPrice);

                if ((stockPrice.compareTo(orders.get(f).getTotalProfit())) >= 0) {
                    totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
                    orders.remove(f);
                } else if ((stockPrice.divide(orders.get(f).getHighPrice(), MathContext.DECIMAL32))
                        .compareTo(orders.get(f).getTrailingStopPercent()) < 0) {
                    totalValue = totalValue.add(orders.get(f).convertToDollars(stockPrice));
                    orders.remove(f);
                }
            }
        }
        for (int i = 0; i < orders.size(); i++)
            totalValue = totalValue.add(orders.get(i).convertToDollars(stockPrice));

        historicalAnalysis.setType("Day Trade");
        historicalAnalysis.setMoneySpent(moneySpent);
        historicalAnalysis.setTotalValue(totalValue);
        request.addParam(GlobalConstant.ITEM, historicalAnalysis);
        try {
        historicalAnalysisDao.save(request, response);
        }catch(Exception e) {}

    }
}