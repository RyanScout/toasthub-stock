package org.toasthub.stock.cache;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.common.Symbol;
import org.toasthub.stock.model.cache.GoldenCross;
import org.toasthub.stock.model.cache.LowerBollingerBand;
import org.toasthub.stock.model.cache.UpperBollingerBand;
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
        entityManager.merge((Object) request.getParam(GlobalConstant.ITEM));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveAll(Request request, Response response) throws Exception {
        for (Object obj : (List<Object>) request.getParam(GlobalConstant.ITEMS)) {
            entityManager.merge(obj);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void items(Request request, Response response) throws Exception {
        String x = "";
        switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
            case "GoldenCross":
                x = "GoldenCross";
                break;
            case "LowerBollingerBand":
                x = "LowerBollingerBand";
                break;
            case "UpperBollingerBand":
                x = "UpperBollingerBand";
                break;
            default:
                break;
        }

        String queryStr = "SELECT DISTINCT x FROM "
                + x
                + " AS x WHERE x.symbol =:symbol AND x.evalPeriod =:evalPeriod";

        Query query = entityManager.createQuery(queryStr);

        if (Arrays.asList(Symbol.SYMBOLS).contains((String) request.getParam(GlobalConstant.SYMBOL))) {
            query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
        }
        switch ((String) request.getParam("EVAL_PERIOD")) {
            case "DAY":
                query.setParameter("evalPeriod", (String) request.getParam("EVAL_PERIOD"));
                break;
            case "MINUTE":
                query.setParameter("evalPeriod", (String) request.getParam("EVAL_PERIOD"));
                break;
            default:
                break;
        }

        List<?> items = (List<?>) query.getResultList();
        switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
            case "GoldenCross":
                List<GoldenCross> goldenCrosses = (List<GoldenCross>) items;
                for (GoldenCross goldenCross : goldenCrosses) {
                    Hibernate.initialize(goldenCross.getGoldenCrossDetails());
                }
                break;
            case "LowerBollingerBand":
                List<LowerBollingerBand> lowerBollingerBands = (List<LowerBollingerBand>) items;
                for (LowerBollingerBand lowerBollingerBand : lowerBollingerBands) {
                    Hibernate.initialize(lowerBollingerBand.getLowerBollingerBandDetails());
                }
                break;
            case "UpperBollingerBand":
                List<UpperBollingerBand> upperBollingerBands = (List<UpperBollingerBand>) items;
                for (UpperBollingerBand upperBollingerBand : upperBollingerBands) {
                    Hibernate.initialize(upperBollingerBand.getUpperBollingerBandDetails());
                }
                break;
            default:
                break;
        }

        response.addParam(GlobalConstant.ITEMS, items);
    }

    @Override
    public void itemCount(Request request, Response response) throws Exception {
        switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
            case "GoldenCross":
                goldenCrossCount(request, response);
                break;
            case "LowerBollingerBand":
                lowerBollingerBandCount(request, response);
                break;
            case "UpperBollingerBand":
                upperBollingerBandCount(request, response);
                break;
            default:
                break;
        }
    }

    public void goldenCrossCount(Request request, Response response) {

        String queryStr = "SELECT COUNT(DISTINCT x) FROM GoldenCross as x ";

        boolean and = false;
        if (request.containsParam(GlobalConstant.SYMBOL)) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.symbol =:symbol ";
            and = true;
        }
        if (request.containsParam("SHORT_SMA_TYPE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.shortSMAType =:shortSMAType ";
            and = true;
        }
        if (request.containsParam("LONG_SMA_TYPE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.longSMAType =:longSMAType ";
            and = true;
        }

        Query query = entityManager.createQuery(queryStr);

        if (request.containsParam(GlobalConstant.SYMBOL)) {
            query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
        }

        if (request.containsParam("SHORT_SMA_TYPE")) {
            query.setParameter("shortSMAType", (String) request.getParam("SHORT_SMA_TYPE"));
        }

        if (request.containsParam("LONG_SMA_TYPE")) {
            query.setParameter("longSMAType", (String) request.getParam("LONG_SMA_TYPE"));
        }

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);

    }

    public void lowerBollingerBandCount(Request request, Response response) {
        String queryStr = "SELECT COUNT(DISTINCT x) FROM LowerBollingerBand as x ";

        boolean and = false;
        if (request.containsParam(GlobalConstant.SYMBOL)) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.symbol =:symbol ";
            and = true;
        }

        if (request.containsParam("LBB_TYPE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.LBBType =:LBBType ";
            and = true;
        }
        if (request.containsParam("STANDARD_DEVIATION_VALUE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.standardDeviationValue =:standardDeviationValue";
            and = true;
        }
        Query query = entityManager.createQuery(queryStr);

        if (request.containsParam(GlobalConstant.SYMBOL)) {
            query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
        }

        if (request.containsParam("LBB_TYPE")) {
            query.setParameter("LBBType", (String) request.getParam("LBB_TYPE"));
        }

        if (request.containsParam("STANDARD_DEVIATION_VALUE")) {
            query.setParameter("standardDeviationValue", (double) request.getParam("STANDARD_DEVIATION_VALUE"));
        }

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);

    }

    public void upperBollingerBandCount(Request request, Response response) {
        String queryStr = "SELECT COUNT(DISTINCT x) FROM UpperBollingerBand as x ";

        boolean and = false;
        if (request.containsParam(GlobalConstant.SYMBOL)) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.symbol =:symbol ";
            and = true;
        }

        if (request.containsParam("UBB_TYPE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.UBBType =:UBBType ";
            and = true;
        }
        if (request.containsParam("STANDARD_DEVIATION_VALUE")) {
            if (!and)
                queryStr += " WHERE ";
            else
                queryStr += " AND ";

            queryStr += "x.standardDeviationValue =:standardDeviationValue";
            and = true;
        }

        Query query = entityManager.createQuery(queryStr);

        if (request.containsParam(GlobalConstant.SYMBOL)) {
            query.setParameter("symbol", (String) request.getParam(GlobalConstant.SYMBOL));
        }

        if (request.containsParam("UBB_TYPE")) {
            query.setParameter("UBBType", (String) request.getParam("UBB_TYPE"));
        }

        if (request.containsParam("STANDARD_DEVIATION_VALUE")) {
            query.setParameter("standardDeviationValue", (double) request.getParam("STANDARD_DEVIATION_VALUE"));
        }

        Long count = (Long) query.getSingleResult();
        if (count == null) {
            count = 0l;
        }
        response.addParam(GlobalConstant.ITEMCOUNT, count);
    }

    @Override
    public void item(Request request, Response response) throws Exception {
        switch ((String) request.getParam(GlobalConstant.IDENTIFIER)) {
            case "GoldenCross":
                goldenCross(request, response);
                break;
            case "LowerBollingerBand":
                lowerBollingerBand(request, response);
                break;
            case "UpperBollingerBand":
                upperBollingerBand(request, response);
                break;
            default:
                break;
        }
    }

    // objects are fully initialized as all children are needed when stores in cache
    public void goldenCross(Request request, Response response) {

        String queryStr = "SELECT DISTINCT x FROM GoldenCross AS x WHERE x.symbol =:symbol AND x.shortSMAType =:shortSMAType AND x.longSMAType =: longSMAType";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("shortSMAType", request.getParam("SHORT_SMA_TYPE"));
        query.setParameter("longSMAType", request.getParam("LONG_SMA_TYPE"));
        GoldenCross result = (GoldenCross) query.getSingleResult();

        Hibernate.initialize(result.getGoldenCrossDetails());

        response.addParam(GlobalConstant.ITEM, result);
    }

    public void lowerBollingerBand(Request request, Response response) {
        String queryStr = "SELECT DISTINCT x FROM LowerBollingerBand AS x WHERE x.symbol =:symbol AND x.LBBType =:LBBType AND x.standardDeviationValue =: standardDeviationValue";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("LBBType", request.getParam("LBB_TYPE"));
        query.setParameter("standardDeviationValue", request.getParam("STANDARD_DEVIATION_VALUE"));
        LowerBollingerBand result = (LowerBollingerBand) query.getSingleResult();

        Hibernate.initialize(result.getLowerBollingerBandDetails());

        response.addParam(GlobalConstant.ITEM, result);
    }

    public void upperBollingerBand(Request request, Response response) {
        String queryStr = "SELECT DISTINCT x FROM UpperBollingerBand AS x WHERE x.symbol =:symbol AND x.UBBType =:UBBType AND x.standardDeviationValue =: standardDeviationValue";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("UBBType", request.getParam("UBB_TYPE"));
        query.setParameter("standardDeviationValue", request.getParam("STANDARD_DEVIATION_VALUE"));
        UpperBollingerBand result = (UpperBollingerBand) query.getSingleResult();

        Hibernate.initialize(result.getUpperBollingerBandDetails());

        response.addParam(GlobalConstant.ITEM, result);
    }
}
