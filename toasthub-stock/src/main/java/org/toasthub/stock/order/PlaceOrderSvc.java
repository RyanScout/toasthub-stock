package org.toasthub.stock.order;

import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface PlaceOrderSvc {
    public void process(Request request, Response response);
    public void placeDefaultOrder(Request request, Response response);
    public void placeTrailingStopOrder(Request request, Response response);
}
