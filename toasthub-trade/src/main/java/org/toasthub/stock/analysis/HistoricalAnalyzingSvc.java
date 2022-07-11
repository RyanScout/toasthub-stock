package org.toasthub.stock.analysis;

import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

public interface HistoricalAnalyzingSvc {
    public void process(RestRequest request, RestResponse response);
}
