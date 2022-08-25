package org.toasthub.trade.technical_indicator;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.trade.model.TechnicalIndicator;

@Repository("TATechnicalIndicatorDao")
@Transactional("TransactionManagerData")
public class TechnicalIndicatorDao {

    @Autowired
    protected EntityManagerDataSvc entityManagerDataSvc;

    public TechnicalIndicator getTechnicalIndicator(final String symbol, final String evaluationPeriod,
            final String technicalIndicatorKey, final String technicalIndicatorType) {

        final String queryStr = "SELECT DISTINCT technicalIndicator FROM TechnicalIndicator AS technicalIndicator"
                + " WHERE technicalIndicator.technicalIndicatorKey = :technicalIndicatorKey"
                + " AND technicalIndicator.symbol = :symbol"
                + " AND technicalIndicator.technicalIndicatorType = :technicalIndicatorType"
                + " AND technicalIndicator.evaluationPeriod = :evaluationPeriod";

        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
                .setParameter("symbol", symbol)
                .setParameter("evaluationPeriod", evaluationPeriod)
                .setParameter("technicalIndicatorType", technicalIndicatorType)
                .setParameter("technicalIndicatorKey", technicalIndicatorKey);

        return TechnicalIndicator.class.cast(query.getSingleResult());
    }

    public TechnicalIndicator findById(final long id) {
        return entityManagerDataSvc.getInstance().find(TechnicalIndicator.class, id);
    }
}
