package org.toasthub.stock.custom_technical_indicator;

import javax.persistence.NoResultException;

import org.toasthub.common.BaseDao;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

public interface CustomTechnicalIndicatorDao extends BaseDao {
    public void item(Request request, Response response)throws NoResultException;
}
