package org.toasthub.stock.model.cache;

public class LowerBollingerBand {
    private boolean buyIndicator;

    public boolean isBuyIndicator() {
        return buyIndicator;
    }

    public void setBuyIndicator(boolean buyIndicator) {
        this.buyIndicator = buyIndicator;
    }
}
