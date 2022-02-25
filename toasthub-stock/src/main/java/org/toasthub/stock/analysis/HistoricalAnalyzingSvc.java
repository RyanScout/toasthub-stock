package org.toasthub.stock.analysis;

import org.toasthub.utils.Request;
import org.toasthub.utils.Response;


public interface HistoricalAnalyzingSvc {
    public void process(Request request, Response response);
}
