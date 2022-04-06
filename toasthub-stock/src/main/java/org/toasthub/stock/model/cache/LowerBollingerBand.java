package org.toasthub.stock.model.cache;

public class LowerBollingerBand {
    private boolean buyIndicator;
    private String LBBType;
    private double standardDeviationValue;
    public static final String DEFAULT_LBB_TYPE_DAY = "20-day";
    public static final String DEFAULT_LBB_TYPE_MINUTE = "20-minute";
    public static final double DEFAULT_STANDARD_DEVIATION_VALUE = 2.0;

    public LowerBollingerBand() {
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

    public double geStandardDeviationValue() {
        return standardDeviationValue;
    }

    public void setStandardDeviationValue(double standardDeviationValue) {
        this.standardDeviationValue = standardDeviationValue;
    }
}