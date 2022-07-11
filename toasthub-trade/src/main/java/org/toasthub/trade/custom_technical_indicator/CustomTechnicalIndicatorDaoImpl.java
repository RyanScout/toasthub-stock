package org.toasthub.trade.custom_technical_indicator;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.core.general.model.GlobalConstant;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.CustomTechnicalIndicator;

@Repository("TACustomTechnicalIndicatorDao")
@Transactional("TransactionManagerData")
public class CustomTechnicalIndicatorDaoImpl implements CustomTechnicalIndicatorDao {

    @Autowired
    private EntityManagerDataSvc entityManagerDataSvc;

    @Override
    public void delete(RestRequest request, RestResponse response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

            CustomTechnicalIndicator c = (CustomTechnicalIndicator) entityManagerDataSvc.getInstance().getReference(
                    CustomTechnicalIndicator.class,
                    Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            entityManagerDataSvc.getInstance().remove(c);
        }
    }

    @Override
    public void save(RestRequest request, RestResponse response) throws Exception {
    	entityManagerDataSvc.getInstance().merge((request.getParam(GlobalConstant.ITEM)));

    }

    @Override
    public void items(RestRequest request, RestResponse response) throws Exception {
        String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x";
        Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
        List<?> result = query.getResultList();

        response.addParam(GlobalConstant.ITEMS, result);
    }

    @Override
    public void itemCount(RestRequest request, RestResponse response) throws Exception {
        String queryStr = "SELECT COUNT(DISTINCT x) FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);
    }

    @Override
    public void item(RestRequest request, RestResponse response) throws Exception, NoResultException {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
            String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x WHERE x.id =:id";
            Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

            if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
                query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
                query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof String) {
                query.setParameter("id", Long.valueOf((String) request.getParam(GlobalConstant.ITEMID)));
            }

            CustomTechnicalIndicator c = CustomTechnicalIndicator.class.cast(query.getSingleResult());
            Hibernate.initialize(c.getSymbols());
            response.addParam(GlobalConstant.ITEM, c);
            return;
        }

        String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
        return;
    }

}
