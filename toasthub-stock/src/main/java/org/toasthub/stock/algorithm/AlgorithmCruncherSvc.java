package org.toasthub.stock.algorithm;


import org.toasthub.core.common.BaseSvc;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;


public interface AlgorithmCruncherSvc extends BaseSvc {
    public void loadStockData(final RestRequest request, final RestResponse response);
    public void loadCryptoData(final RestRequest request, final RestResponse response);
    public void loadAlgorithmData(final RestRequest request, final RestResponse response);
    public void backloadAlg(final RestRequest request, final RestResponse response);
    public void initializeDatabase();
}
