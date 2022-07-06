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
    public void delete(final Request request, final Response response) {

    }

    @Override
    public void save(final Request request, final Response response) throws Exception {
        entityManager.merge(request.getParam(GlobalConstant.ITEM));
    }

    @Override
    public void saveAll(final Request request, final Response response) throws Exception {
        for (final Object o : ArrayList.class.cast(request.getParam(GlobalConstant.ITEMS))) {
            entityManager.merge(o);
        }
    }

    @Override
    public void items(final Request request, final Response response) {
        final String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator as x WHERE x.updating=:updating";

        final Query query = entityManager.createQuery(queryStr);

        query.setParameter("updating", false);

        final List<TechnicalIndicator> technicalIndicators = new ArrayList<TechnicalIndicator>();

        for (final Object o : ArrayList.class.cast(query.getResultList())) {
            final TechnicalIndicator t = TechnicalIndicator.class.cast(o);
            Hibernate.initialize(t.getDetails());
            technicalIndicators.add(t);
        }

        response.addParam(GlobalConstant.ITEMS, technicalIndicators);
    }

    @Override
    public void itemCount(final Request request, final Response response) throws Exception {
        final String queryStr = "SELECT COUNT(DISTINCT x) FROM TechnicalIndicator as x WHERE x.technicalIndicatorType =:technicalIndicatorType AND x.evaluationPeriod =:evaluationPeriod AND x.technicalIndicatorKey =:technicalIndicatorKey AND x.symbol =:symbol";
        final Query query = entityManager.createQuery(queryStr);
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
    public void item(final Request request, final Response response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID) != null)) {
            final String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator AS x WHERE x.id =:id";
            final Query query = entityManager.createQuery(queryStr);

            if (request.getParam(GlobalConstant.ITEMID) instanceof Integer) {
                query.setParameter("id", Long.valueOf((Integer) request.getParam(GlobalConstant.ITEMID)));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof Long) {
                query.setParameter("id", (Long) request.getParam(GlobalConstant.ITEMID));
            }

            if (request.getParam(GlobalConstant.ITEMID) instanceof String) {
                query.setParameter("id", Long.valueOf((String) request.getParam(GlobalConstant.ITEMID)));
            }

            final TechnicalIndicator t = TechnicalIndicator.class.cast(query.getSingleResult());

            Hibernate.initialize(t.getDetails());

            response.addParam(GlobalConstant.ITEM, t);

            return;
        }

        final String queryStr = "SELECT DISTINCT x FROM TechnicalIndicator as x WHERE x.technicalIndicatorType =:technicalIndicatorType AND x.evaluationPeriod =:evaluationPeriod AND x.technicalIndicatorKey =:technicalIndicatorKey";
        final Query query = entityManager.createQuery(queryStr);
        query.setParameter("technicalIndicatorType", (String) request.getParam("TECHNICAL_INDICATOR_TYPE"));
        query.setParameter("evaluationPeriod", (String) request.getParam("EVALUATION_PERIOD"));
        query.setParameter("technicalIndicatorKey", (String) request.getParam("TECHNICAL_INDICATOR_KEY"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getLatestAssetDay(final Request request, final Response response) throws NoResultException {
        final String x = (String) request.getParam(GlobalConstant.SYMBOL);

        if (!Arrays.asList(Symbol.SYMBOLS).contains(x)) {
            System.out.println("Symbol does not match symbols");
            return;
        }

        final String queryStr = "SELECT DISTINCT x FROM AssetDay x WHERE x.symbol =: symbol ORDER BY x.epochSeconds DESC";
        final Query query = entityManager.createQuery(queryStr)
                .setParameter("symbol", x)
                .setMaxResults(1);

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());

    }

    @Override
    public void getLatestAssetMinute(final Request request, final Response response) throws NoResultException {
        final String x = (String) request.getParam(GlobalConstant.SYMBOL);

        if (!Arrays.asList(Symbol.SYMBOLS).contains(x)) {
            System.out.println("Symbol does not match symbols");
            return;
        }

        final String queryStr = "SELECT DISTINCT x FROM AssetMinute x WHERE x.symbol =: symbol ORDER BY x.epochSeconds DESC";
        final Query query = entityManager.createQuery(queryStr)
                .setParameter("symbol", x)
                .setMaxResults(1);

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getAssetDays(final Request request, final Response response) {
        final String queryStr = "SELECT DISTINCT x FROM AssetDay AS x WHERE x.symbol =:symbol AND x.epochSeconds >=: startingEpochSeconds AND x.epochSeconds <=: endingEpochSeconds";

        final Query query = entityManager.createQuery(queryStr)
                .setParameter("symbol", request.getParam(GlobalConstant.SYMBOL))
                .setParameter("startingEpochSeconds", request.getParam("STARTING_EPOCH_SECONDS"))
                .setParameter("endingEpochSeconds", request.getParam("ENDING_EPOCH_SECONDS"));

        response.addParam(GlobalConstant.ITEMS, query.getResultList());
    }

    @Override
    public void getAssetMinutes(final Request request, final Response response) {
        final String queryStr = "SELECT DISTINCT x FROM AssetMinute AS x WHERE x.symbol =:symbol AND x.epochSeconds >=:startingEpochSeconds AND x.epochSeconds <=: endingEpochSeconds";

        final Query query = entityManager.createQuery(queryStr)
                .setParameter("symbol", request.getParam(GlobalConstant.SYMBOL))
                .setParameter("startingEpochSeconds", request.getParam("STARTING_EPOCH_SECONDS"))
                .setParameter("endingEpochSeconds", request.getParam("ENDING_EPOCH_SECONDS"));

        response.addParam(GlobalConstant.ITEMS, query.getResultList());
    }

    @Override
    public void getSMAValue(final Request request, final Response response) throws NoResultException {
        final String queryStr = "SELECT x.value FROM SMA x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol";

        final Query query = entityManager.createQuery(queryStr)
                .setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS))
                .setParameter("type", request.getParam(GlobalConstant.TYPE))
                .setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getLBB(final Request request, final Response response) throws NoResultException {
        final String queryStr = "SELECT DISTINCT x FROM LBB AS x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations";

        final Query query = entityManager.createQuery(queryStr);
        query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
        query.setParameter("type", request.getParam(GlobalConstant.TYPE));
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }

    @Override
    public void getUBB(final Request request, final Response response) throws NoResultException {
        final String queryStr = "SELECT DISTINCT x FROM UBB AS x WHERE x.epochSeconds =:epochSeconds AND x.type =: type AND x.symbol =:symbol AND x.standardDeviations =: standardDeviations";

        final Query query = entityManager.createQuery(queryStr);
        query.setParameter("epochSeconds", request.getParam(GlobalConstant.EPOCHSECONDS));
        query.setParameter("type", request.getParam(GlobalConstant.TYPE));
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("standardDeviations", request.getParam("STANDARD_DEVIATIONS"));

        response.addParam(GlobalConstant.ITEM, query.getSingleResult());
    }
}
