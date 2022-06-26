package org.toasthub.stock.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

@Service("PlaceOrderSvc")
public class PlaceOrderSvcImpl implements PlaceOrderSvc {

    @Autowired
	protected AlpacaAPI alpacaAPI = null;

    // Constructors
	public PlaceOrderSvcImpl() {
	}

    @Override
	public void process(Request request, Response response) {
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
    public void placeDefaultOrder(Request request, Response response) {
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
    public void placeTrailingStopOrder(Request request, Response response){
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
