package org.toasthub.stock.model.cache;


public class GoldenCross {
    private boolean buyIndicator;
    private String shortSMAType;
    private String longSMAType;
    public static final String DEFAULT_SHORT_SMA_TYPE_DAY = "15-day";
    public static final String DEFAULT_LONG_SMA_TYPE_DAY = "50-day";
    public static final String DEFAULT_SHORT_SMA_TYPE_MINUTE = "15-minute";
    public static final String DEFAULT_LONG_SMA_TYPE_MINUTE = "50-minute";

    public GoldenCross(){
    }

    public String getLongSMAType() {
        return longSMAType;
    }

    public void setLongSMAType(String longSMAType) {
        this.longSMAType = longSMAType;
    }

    public String getShortSMAType() {
        return shortSMAType;
    }

    public void setShortSMAType(String shortSMAType) {
        this.shortSMAType = shortSMAType;
    }


    public boolean isBuyIndicator() {
        return buyIndicator;
    }

    public void setBuyIndicator(boolean buyIndicator) {
        this.buyIndicator = buyIndicator;
    }
}
