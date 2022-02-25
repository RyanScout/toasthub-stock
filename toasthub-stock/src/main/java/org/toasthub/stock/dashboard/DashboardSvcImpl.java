package org.toasthub.stock.dashboard;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.account.Account;
import net.jacobpeterson.alpaca.model.endpoint.clock.Clock;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

@Service("DashboardSvc")
public class DashboardSvcImpl implements DashboardSvc {

	@Autowired
	protected AlpacaAPI alpacaAPI;
	
	// Constructors
	public DashboardSvcImpl() {
	}
	
		
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}


	@Override
	public void process(Request request, Response response) {
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
	public void getData(Request request, Response response) {
		
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
