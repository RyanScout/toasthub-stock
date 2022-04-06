package org.toasthub.stock.model.cache;

public class UpperBollingerBand {
    private boolean sellIndicator;
    private String UBBType;
    private double standardDeviationValue;
    public static final String DEFAULT_UBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_UBB_TYPE_MINUTE = "20-minute";
    public static final double DEFAULT_STANDARD_DEVIATION_VALUE = 2.0;

    public UpperBollingerBand() {
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

    public double geStandardDeviationValue() {
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue) {
        this.standardDeviationValue = standardDeviationValue;
    }
}
