package org.toasthub.stock.model.cache;

import org.toasthub.common.BaseDao;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;


public interface CacheDao extends BaseDao {
    public void getUnfilledGoldenCrossDetails(Request request, Response response);
    public void saveGoldenCross(Request request, Response response) throws Exception;
}
