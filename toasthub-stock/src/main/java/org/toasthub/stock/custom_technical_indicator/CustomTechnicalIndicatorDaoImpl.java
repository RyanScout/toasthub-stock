package org.toasthub.stock.custom_technical_indicator;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.toasthub.core.model.CustomTechnicalIndicator;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("CustomTechnicalIndicatorDao")
@Transactional()
public class CustomTechnicalIndicatorDaoImpl implements CustomTechnicalIndicatorDao {

    @Autowired
    private EntityManager entityManager;

    @Override
    public void delete(Request request, Response response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

            CustomTechnicalIndicator c = (CustomTechnicalIndicator) entityManager.getReference(
                    CustomTechnicalIndicator.class,
                    Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            entityManager.remove(c);
        }
    }

    @Override
    public void save(Request request, Response response) throws Exception {
        entityManager.merge((request.getParam(GlobalConstant.ITEM)));

    }

    @Override
    public void items(Request request, Response response) throws Exception {
        String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x";
        Query query = entityManager.createQuery(queryStr);
        List<?> result = query.getResultList();

        response.addParam(GlobalConstant.ITEMS, result);
    }

    @Override
    public void itemCount(Request request, Response response) throws Exception {
        String queryStr = "SELECT COUNT(DISTINCT x) FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        Query query = entityManager.createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);
    }

    @Override
    public void item(Request request, Response response) throws NoResultException {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
            String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x WHERE x.id =:id";
            Query query = entityManager.createQuery(queryStr);

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
        Query query = entityManager.createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
        return;
    }

}
