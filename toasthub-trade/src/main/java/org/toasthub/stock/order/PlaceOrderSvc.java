package org.toasthub.stock.order;

import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

public interface PlaceOrderSvc {
    public void process(RestRequest request, RestResponse response);
    public void placeDefaultOrder(RestRequest request, RestResponse response);
    public void placeTrailingStopOrder(RestRequest request, RestResponse response);
}
