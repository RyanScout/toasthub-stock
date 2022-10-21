package org.toasthub.trade.historical_analysis;

import java.util.List;

import org.toasthub.core.common.BaseSvc;
import org.toasthub.trade.model.Trade;

public interface HistoricalAnalysisSvc extends BaseSvc {
    public void historicalAnalysis(final Trade initTrade, final long startTime, final long endTime) throws Exception;
    public List<Trade> getHistoricalAnalyses()throws Exception;
}
