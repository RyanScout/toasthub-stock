package org.toasthub.trade.algorithm;

import org.toasthub.core.common.BaseSvc;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

public interface AlgorithmCruncherSvc extends BaseSvc {
    public void loadStockData(final RestRequest request, final RestResponse response);

    public void loadCryptoData(final RestRequest request, final RestResponse response);

    public void loadAlgorithmData(final RestRequest request, final RestResponse response);

    public void backloadAlgorithm(final long itemId, final long startTime, final long endTime) throws Exception;

    public void initializeDatabase();
}
