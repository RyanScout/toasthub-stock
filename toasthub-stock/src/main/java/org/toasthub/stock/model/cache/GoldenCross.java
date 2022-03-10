package org.toasthub.stock.model.cache;


public class GoldenCross {
    private boolean buyIndicator;
    private String shortSMAType ;
    private String longSMAType;

    public GoldenCross(){
        setShortSMAType("15-day");
        setLongSMAType("50-day");
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
