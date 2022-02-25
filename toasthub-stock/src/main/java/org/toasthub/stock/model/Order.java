package org.toasthub.stock.model;

import java.math.BigDecimal;
import java.math.MathContext;

public class Order {
    private BigDecimal dollarAmount;
    private BigDecimal stockAmount;
    private BigDecimal highPrice;
    private BigDecimal trailingStopPrice;
    private BigDecimal trailingStopPercent;
    private BigDecimal totalProfit;
    private BigDecimal initialPrice;
    private long boughtAtTime;
    
    public Order(BigDecimal dollarAmount, BigDecimal stockAmount, BigDecimal trailingStopPrice, 
                 BigDecimal trailingStopPercent, BigDecimal totalProfit, BigDecimal initialStockPrice){

        if(dollarAmount != null){
            this.dollarAmount = dollarAmount;
            convertToShares(initialStockPrice);
        }
        else{
            this.stockAmount = stockAmount;
            convertToDollars(initialStockPrice);
        }

        if(trailingStopPercent != null)
        this.trailingStopPercent = trailingStopPercent;

        if (trailingStopPrice != null)
        this.trailingStopPrice = trailingStopPrice;

        if(totalProfit != null)
        this.totalProfit = totalProfit;

        if(initialStockPrice != null){
        this.highPrice = initialStockPrice;
        this.initialPrice = initialStockPrice;
        }
    }

    public long getBoughtAtTime() {
        return boughtAtTime;
    }

    public void setBoughtAtTime(long boughtAtTime) {
        this.boughtAtTime = boughtAtTime;
    }

    public Order(BigDecimal dollarAmount , BigDecimal stockPrice){
        this.dollarAmount = dollarAmount;
        this.stockAmount = convertToShares(stockPrice);
        this.initialPrice = stockPrice;
        this.highPrice = stockPrice;
    }

    public Order(int shareAmount , BigDecimal stockPrice){
        BigDecimal stockAmount = new BigDecimal(shareAmount);
        this.stockAmount = stockAmount;
        this.dollarAmount = convertToDollars(stockPrice);
        this.initialPrice = stockPrice;
        this.highPrice = stockPrice;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getStockAmount() {
        return stockAmount;
    }

    public void setStockAmount(BigDecimal stockAmount) {
        this.stockAmount = stockAmount;
    }

    public BigDecimal getDollarAmount() {
        return dollarAmount;
    }

    public void setDollarAmount(BigDecimal dollarAmount) {
        this.dollarAmount = dollarAmount;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }
    public BigDecimal getTrailingStopPercent() {
        return trailingStopPercent;
    }
    public void setTrailingStopPercent(BigDecimal trailingStopPercent){
        this.trailingStopPercent = trailingStopPercent;
    }
    public BigDecimal getTrailingStopPrice() {
        return trailingStopPrice;
    }
    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }
    public BigDecimal convertToShares(BigDecimal stockPrice){
        this.stockAmount = this.dollarAmount.divide(stockPrice, MathContext.DECIMAL32);
        return this.stockAmount;
    }
    public BigDecimal convertToDollars(BigDecimal stockPrice){
        this.dollarAmount  = this.stockAmount.multiply(stockPrice, MathContext.DECIMAL32);
        return this.dollarAmount;
    }

    public BigDecimal getInitialPrice(){
        return initialPrice;
    }

    

}
