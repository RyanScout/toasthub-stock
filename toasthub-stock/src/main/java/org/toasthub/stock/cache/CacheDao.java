package org.toasthub.stock.cache;

import javax.persistence.NoResultException;

import org.toasthub.common.BaseDao;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface CacheDao extends BaseDao {
    public void saveAll(Request request, Response response) throws Exception;
    public void getLatestAssetDay(Request request, Response response) throws NoResultException;
	public void getLatestAssetMinute(Request request, Response response) throws NoResultException;
    public void getSMAValue(Request request, Response response) throws NoResultException;
    public void getLBB(Request request, Response response) throws NoResultException;
    public void getUBB(Request request, Response response) throws NoResultException;
    public void getAssetDays(Request request, Response response);
    public void getAssetMinutes(Request request, Response response);
    public void refresh(Request request, Response response);
    public void getEarliestAlgTime(final Request request, final Response response) throws NoResultException;
}
