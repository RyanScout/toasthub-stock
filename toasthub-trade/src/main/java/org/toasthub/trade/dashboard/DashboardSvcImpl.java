package org.toasthub.trade.dashboard;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.core.general.handler.ServiceProcessor;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.account.Account;
import net.jacobpeterson.alpaca.model.endpoint.clock.Clock;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

@Service("TADashboardSvc")
public class DashboardSvcImpl implements ServiceProcessor, DashboardSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;
	
	// Constructors
	public DashboardSvcImpl() {
	}

	@Override
	public void process(RestRequest request, RestResponse response) {
		String action = (String) request.getParams().get("action");
		
		switch (action) {

		case "DASHBOARD":
			getData(request, response);
			break;
		case "TEST":
			
			break;
		
		default:
			break;
		}
		
	}
	
	@Override
	public void getData(RestRequest request, RestResponse response) {
		
		try {
			Clock clock = alpacaAPI.clock().get();
		    response.addParam("CLOCK", clock);
		    
		    Account account = alpacaAPI.account().get();
		    response.addParam("ACCOUNT", account);
		    
		} catch (AlpacaClientException exception) {
		    exception.printStackTrace();
		}
	}
}
