package org.toasthub.stock.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.model.Symbol;
import org.toasthub.model.TechnicalIndicator;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("CacheDao")
@Transactional()
public class CacheDaoImpl implements CacheDao {

    @Autowired
    protected EntityManager entityManager;

    @Override
    public void delete(Request request, Response response) {

    }

    @Override
    public void save(Request request, Response response) throws Exception {
        entityManager.merge(request.getParam(GlobalConstant.ITEM));
    }

    @Override
    public void saveAll(Request request, Response response) throws Exception {
        for (Object o : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
            entityManager.merge(o);
        }
    }

    @Override
    public void items(Request request, Response response) {
        String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator as x WHERE x.updating=:updating";

        Query query = entityManager.createQuery(queryStr);

        query.setParameter("updating", false);

        List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

        for (Object o : ArrayList.class.cast(query.getResultList())) {
            TechnicalIndicator t = TechnicalIndicator.class.cast(o);
            Hibernate.initialize(t.getDetails());
            technicalIndicators.add(t);
        }

        response.addParam(GlobalConstant.ITEMS, technicalIndicators);
    }

    @Override
    public void itemCount(Request request, Response response) throws Exception {
        String queryStr = "SELECT COUNT(DISTINCT x) FROM TechnicalIndicator as x WHERE x.technicalIndicatorType =:technicalIndicatorType AND x.evaluationPeriod =:evaluationPeriod AND x.technicalIndicatorKey =:technicalIndicatorKey AND x.symbol =:symbol";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("technicalIndicatorType", (String) request.getParam("TECHNICAL_INDICATOR_TYPE"));
        query.setParameter("evaluationPeriod", (String) request.getParam("EVALUATION_PERIOD"));
        query.setParameter("technicalIndicatorKey", (String) request.getParam("TECHNICAL_INDICATOR_KEY"));
        query.setParameter("symbol", ((String) request.getParam(GlobalConstant.SYMBOL)));

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);
    }

    @Override
    public void item(Request request, Response response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
            String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator AS x WHERE x.id =:id";
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

            TechnicalIndicator t = TechnicalIndicator.class.cast(query.getSingleResult());
            Hibernate.initialize(t.getDetails());
            response.addParam(GlobalConstant.ITEM, t);
            return;
        }

        String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator as x WHERE x.technicalIndicatorType =:technicalIndicatorType AND x.evaluationPeriod =:evaluationPeriod AND x.technicalIndicatorKey =:technicalIndicatorKey";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("technicalIndicatorType", (String) request.getParam("TECHNICAL_INDICATOR_TYPE"));
        query.setParameter("evaluationPeriod", (String) request.getParam("EVALUATION_PERIOD"));
        query.setParameter("technicalIndicatorKey", (String) request.getParam("TECHNICAL_INDICATOR_KEY"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getLatestAssetDay(Request request, Response response) throws NoResultException {
        String x = (String) request.getParam(GlobalConstant.SYMBOL);

        if (!Arrays.asList(Symbol.SYMBOLS).contains(x)) {
            System.out.println("Symbol does not match symbols");
            return;
        }

        String queryStr = "SELECT DISTINCT x FROM AssetDay x WHERE (SELECT MAX(x.epochSeconds) FROM AssetDay x WHERE x.symbol = : symbol) = x.epochSeconds AND x.symbol =: symbol";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", x);
        response.addParam(GlobalConstant.ITEM, query.getSingleResult());

    }

    @Override
    public void getLatestAssetMinute(Request request, Response response) throws NoResultException {
        String x = (String) request.getParam(GlobalConstant.SYMBOL);

        if (!Arrays.asList(Symbol.SYMBOLS).contains(x)) {
            System.out.println("Symbol does not match symbols");
            return;
        }

        String queryStr = "SELECT DISTINCT x FROM AssetMinute x WHERE (SELECT MAX(x.epochSeconds) FROM AssetMinute x WHERE x.symbol = : symbol) = x.epochSeconds AND x.symbol =: symbol";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", x);
        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getSMAValue(Request request, Response response) throws NoResultException {
        String queryStr = "SELECT x.value FROM SMA x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol";

        Query query = entityManager.createQuery(queryStr)
                .setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS))
                .setParameter("type", request.getParam(GlobalConstant.TYPE))
                .setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getLBB(Request request, Response response) throws NoResultException {
        String queryStr = "SELECT DISTINCT x FROM LBB AS x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations";

        Query query = entityManager.createQuery(queryStr);
        query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
        query.setParameter("type", request.getParam(GlobalConstant.TYPE));
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getUBB(Request request, Response response) throws NoResultException {
        String queryStr = "SELECT DISTINCT x FROM UBB AS x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations";

        Query query = entityManager.createQuery(queryStr);
        query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
        query.setParameter("type", request.getParam(GlobalConstant.TYPE));
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }
}
