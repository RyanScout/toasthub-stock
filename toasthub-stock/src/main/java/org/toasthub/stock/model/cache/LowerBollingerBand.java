package org.toasthub.stock.model.cache;

public class LowerBollingerBand {
    private boolean buyIndicator;
    private String LBBType;
    private double standardDeviationValue;

    public LowerBollingerBand(){
            setLBBType("20-day");
            setStandardDeviationValue(2);
        }

    public String getLBBType() {
        return LBBType;
    }

    public void setLBBType(String LBBType) {
        this.LBBType = LBBType;
    }

    public boolean isBuyIndicator() {
        return buyIndicator;
    }

    public void setBuyIndicator(boolean buyIndicator) {
        this.buyIndicator = buyIndicator;
    }

    public double geStandardDeviationValue(){
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue){
        this.standardDeviationValue = standardDeviationValue;
    }
}