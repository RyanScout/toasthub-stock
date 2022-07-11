package org.toasthub.stock.dashboard;

import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;

public interface DashboardSvc {
	public void process(RestRequest request, RestResponse response);
	public void getData(RestRequest request, RestResponse response);
}
