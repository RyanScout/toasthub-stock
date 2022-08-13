package org.toasthub.trade.custom_technical_indicator;

import java.util.ArrayList;
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
    public void delete(final RestRequest request, final RestResponse response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && !"".equals(request.getParam(GlobalConstant.ITEMID))) {

            final CustomTechnicalIndicator c = (CustomTechnicalIndicator) entityManagerDataSvc.getInstance()
                    .getReference(
                            CustomTechnicalIndicator.class,
                            Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            entityManagerDataSvc.getInstance().remove(c);
        }
    }

    @Override
    public void save(final RestRequest request, final RestResponse response) throws Exception {
        entityManagerDataSvc.getInstance().merge((request.getParam(GlobalConstant.ITEM)));
    }

    @Override
    public void saveItem(final Object o) {
        entityManagerDataSvc.getInstance().merge(o);
    }

    @Override
    public CustomTechnicalIndicator getReference(final long id) {
        return entityManagerDataSvc.getInstance().getReference(CustomTechnicalIndicator.class, id);
    }

    @Override
    public CustomTechnicalIndicator findById(final long id) {
        return entityManagerDataSvc.getInstance().find(CustomTechnicalIndicator.class, id);
    }

    @Override
    public long countByName(final String name) {
        final String queryStr = "SELECT COUNT(DISTINCT x) FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        query.setParameter("name", name);

        return Long.class.cast(query.getSingleResult());
    }

    @Override
    public CustomTechnicalIndicator findByName(final String name) throws NoResultException {
        final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x JOIN FETCH x.symbols AS s WHERE x.name =: name";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr).setParameter("name", name);

        return CustomTechnicalIndicator.class.cast(query.getSingleResult());
    }

    @Override
    public void items(final RestRequest request, final RestResponse response) throws Exception {
        final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x JOIN FETCH x.symbols";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);
        final List<?> result = query.getResultList();

        response.addParam(GlobalConstant.ITEMS, result);
    }

    @Override
    public List<CustomTechnicalIndicator> getCustomTechnicalIndicators() {
        final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x LEFT OUTER JOIN FETCH x.symbols";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        final List<CustomTechnicalIndicator> list = new ArrayList<CustomTechnicalIndicator>();

        for (final Object o : query.getResultList()) {
            list.add(CustomTechnicalIndicator.class.cast(o));
        }

        return list;
    }

    @Override
    public void itemCount(final RestRequest request, final RestResponse response) throws Exception {
        final String queryStr = "SELECT COUNT(DISTINCT x) FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);
    }

    @Override
    public void item(final RestRequest request, final RestResponse response) throws Exception, NoResultException {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
            final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x WHERE x.id =:id";
            final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

            if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
                query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
                query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof String) {
                query.setParameter("id", Long.valueOf((String) request.getParam(GlobalConstant.ITEMID)));
            }

            final CustomTechnicalIndicator c = CustomTechnicalIndicator.class.cast(query.getSingleResult());
            Hibernate.initialize(c.getSymbols());
            response.addParam(GlobalConstant.ITEM, c);
            return;
        }

        final String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator as x WHERE x.name =:name";
        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        query.setParameter("name", request.getParam("NAME"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
        return;
    }

}
