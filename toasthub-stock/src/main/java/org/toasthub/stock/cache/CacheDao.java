package org.toasthub.stock.cache;

import org.toasthub.common.BaseDao;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface CacheDao extends BaseDao {
    public void saveAll(Request request, Response response) throws Exception;
}
