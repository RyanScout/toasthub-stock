package org.toasthub.stock.model.cache;

public class UpperBollingerBand {
    private boolean sellIndicator;
    private String UBBType;
    private double standardDeviationValue;

    public UpperBollingerBand(){
            setUBBType("20-day");
            setStandardDeviationValue(2);
        }

    public String getUBBType() {
        return UBBType;
    }

    public void setUBBType(String UBBType) {
        this.UBBType = UBBType;
    }

    public boolean isSellIndicator() {
        return sellIndicator;
    }

    public void setSellIndicator(boolean sellIndicator) {
        this.sellIndicator = sellIndicator;
    }

    public double geStandardDeviationValue(){
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue){
        this.standardDeviationValue = standardDeviationValue;
    }
}
