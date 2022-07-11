package org.toasthub.stock.cache;

import javax.persistence.NoResultException;

import org.toasthub.core.common.BaseDao;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;


public interface CacheDao extends BaseDao {
    public void saveAll(RestRequest request, RestResponse response) throws Exception;
    public void getLatestAssetDay(RestRequest request, RestResponse response) throws NoResultException;
	public void getLatestAssetMinute(RestRequest request, RestResponse response) throws NoResultException;
    public void getSMAValue(RestRequest request, RestResponse response) throws NoResultException;
    public void getLBB(RestRequest request, RestResponse response) throws NoResultException;
    public void getUBB(RestRequest request, RestResponse response) throws NoResultException;
    public void getAssetDays(RestRequest request, RestResponse response);
    public void getAssetMinutes(RestRequest request, RestResponse response);
    public void refresh(RestRequest request, RestResponse response);
    public void getEarliestAlgTime(final RestRequest request, final RestResponse response) throws NoResultException;
}
