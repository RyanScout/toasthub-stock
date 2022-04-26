package org.toasthub.stock.model.cache;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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
                + " AS x ";

        Query query = entityManager.createQuery(queryStr);
        List<?> items = query.getResultList();

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

    // attach and initialize only children which are used in calculation
    // done as two queries because join fetch not working as expected
    @SuppressWarnings("unchecked")
    public void goldenCross(Request request, Response response) {

        String queryStr = "SELECT DISTINCT x FROM GoldenCross AS x WHERE x.symbol =:symbol AND x.shortSMAType =:shortSMAType AND x.longSMAType =: longSMAType";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("shortSMAType", request.getParam("SHORT_SMA_TYPE"));
        query.setParameter("longSMAType", request.getParam("LONG_SMA_TYPE"));
        GoldenCross result = (GoldenCross) query.getSingleResult();

        queryStr = "SELECT DISTINCT x FROM GoldenCrossDetail AS x WHERE x.goldenCross =:goldenCross AND x.success =:success AND x.checked <: checked";
        query = entityManager.createQuery(queryStr);
        query.setParameter("goldenCross", result);
        query.setParameter("success", false);
        query.setParameter("checked", 20);

        result.setGoldenCrossDetails((Set<GoldenCrossDetail>) query.getResultStream().collect(Collectors.toSet()));

        response.addParam(GlobalConstant.ITEM, result);
    }

    // attach and initialize only children which are used in calculation
    // done as two queries because join fetch not working as expected
    @SuppressWarnings("unchecked")
    public void lowerBollingerBand(Request request, Response response) {
        String queryStr = "SELECT DISTINCT x FROM LowerBollingerBand AS x WHERE x.symbol =:symbol AND x.LBBType =:LBBType AND x.standardDeviationValue =: standardDeviationValue";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("LBBType", request.getParam("LBB_TYPE"));
        query.setParameter("standardDeviationValue", request.getParam("STANDARD_DEVIATION_VALUE"));
        LowerBollingerBand result = (LowerBollingerBand) query.getSingleResult();

        queryStr = "SELECT DISTINCT x FROM LowerBollingerBandDetail AS x WHERE x.lowerBollingerBand =:lowerBollingerBand AND x.success =:success AND x.checked <: checked";
        query = entityManager.createQuery(queryStr);
        query.setParameter("lowerBollingerBand", result);
        query.setParameter("success", false);
        query.setParameter("checked", 20);

        result.setLowerBollingerBandDetails(
                (Set<LowerBollingerBandDetail>) query.getResultStream().collect(Collectors.toSet()));

        response.addParam(GlobalConstant.ITEM, result);
    }

    // attach and initialize only children which are used in calculation
    // done as two queries because join fetch not working as expected
    @SuppressWarnings("unchecked")
    public void upperBollingerBand(Request request, Response response) {
        String queryStr = "SELECT DISTINCT x FROM UpperBollingerBand AS x WHERE x.symbol =:symbol AND x.UBBType =:UBBType AND x.standardDeviationValue =: standardDeviationValue";
        Query query = entityManager.createQuery(queryStr);
        query.setParameter("symbol", request.getParam(GlobalConstant.SYMBOL));
        query.setParameter("UBBType", request.getParam("UBB_TYPE"));
        query.setParameter("standardDeviationValue", request.getParam("STANDARD_DEVIATION_VALUE"));
        UpperBollingerBand result = (UpperBollingerBand) query.getSingleResult();

        queryStr = "SELECT DISTINCT x FROM UpperBollingerBandDetail AS x WHERE x.upperBollingerBand =:upperBollingerBand AND x.success =:success AND x.checked <: checked";
        query = entityManager.createQuery(queryStr);
        query.setParameter("upperBollingerBand", result);
        query.setParameter("success", false);
        query.setParameter("checked", 20);

        result.setUpperBollingerBandDetails(
                (Set<UpperBollingerBandDetail>) query.getResultStream().collect(Collectors.toSet()));

        response.addParam(GlobalConstant.ITEM, result);
    }
}
