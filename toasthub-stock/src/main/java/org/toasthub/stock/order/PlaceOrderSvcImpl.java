package org.toasthub.stock.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

@Service("TAPlaceOrderSvc")
public class PlaceOrderSvcImpl implements PlaceOrderSvc {

    @Autowired
	protected AlpacaAPI alpacaAPI = null;

    // Constructors
	public PlaceOrderSvcImpl() {
	}

    @Override
	public void process(RestRequest request, RestResponse response) {
		String action = (String) request.getParams().get("action");
		
		switch (action) {
		case "DEFAULT_ORDER":
			placeDefaultOrder(request, response);
			break;
		case "TRAILING_STOP_ORDER":
			placeTrailingStopOrder(request, response);
			break;
		
		default:
			break;
		}
	}

    @Override
    public void placeDefaultOrder(RestRequest request, RestResponse response) {
        String stockName = (String) request.getParams().get("stockName");
        Double orderAmount = Double.parseDouble((String)request.getParams().get("orderAmount"));

        if ("".equals(stockName)) {
			response.addParam("error", "Stock name is empty");
			return;
        }
        try {
            alpacaAPI.orders().requestNotionalMarketOrder(stockName, orderAmount, OrderSide.BUY);
            response.addParam("success", "Order has been placed");
        } catch (AlpacaClientException exception) {
		    exception.printStackTrace();
        }
        
    }

    @Override
    public void placeTrailingStopOrder(RestRequest request, RestResponse response){
        String stockName = (String) request.getParams().get("stockName");

        if ("".equals(stockName)) {
			response.addParam("error", "Stock name is empty");
			return;
        }
        try 
        {
            alpacaAPI.orders().requestTrailingStopPercentOrder
            (stockName, 1, OrderSide.BUY,OrderTimeInForce.DAY, 10.0, false);
            response.addParam("success", "Limit order has been placed");
        }
        catch (AlpacaClientException exception) {
		    exception.printStackTrace();
        }
    }

    
}
