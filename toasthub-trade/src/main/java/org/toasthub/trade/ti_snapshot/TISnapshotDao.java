package org.toasthub.trade.ti_snapshot;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.toasthub.core.common.EntityManagerDataSvc;
import org.toasthub.trade.model.TISnapshot;

@Repository("TATISnapshotDao")
@Transactional("TransactionManagerData")
public class TISnapshotDao {

    @Autowired
    protected EntityManagerDataSvc entityManagerDataSvc;

    public TISnapshot save(final TISnapshot snapshot) {
        return entityManagerDataSvc.getInstance().merge(snapshot);
    }

    public List<TISnapshot> getSnapshots() {
        final List<TISnapshot> snapshots = new ArrayList<TISnapshot>();

        final String queryStr = "SELECT x FROM TISnapShot as snapshot ";

        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr);

        for (final Object o : query.getResultList()) {
            snapshots.add(TISnapshot.class.cast(o));
        }

        return snapshots;
    }

    public List<TISnapshot> getSnapshotsWithProperties(final String evaluationPeriod,
            final String technicalIndicatorKey, final String technicalIndicatorType) {
        final List<TISnapshot> snapshots = new ArrayList<TISnapshot>();

        final String queryStr = "SELECT snapshot FROM TISnapshot as snapshot"
                + " WHERE snapshot.technicalIndicatorKey = :technicalIndicatorKey"
                + " AND snapshot.technicalIndicatorType = :technicalIndicatorType"
                + " AND snapshot.evaluationPeriod = :evaluationPeriod";

        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
                .setParameter("evaluationPeriod", evaluationPeriod)
                .setParameter("technicalIndicatorType", technicalIndicatorType)
                .setParameter("technicalIndicatorKey", technicalIndicatorKey);

        for (final Object o : query.getResultList()) {
            snapshots.add(TISnapshot.class.cast(o));
        }

        return snapshots;
    }

    public long snapshotCountWithProperties(final String symbol, final String evaluationPeriod,
            final String technicalIndicatorKey, final String technicalIndicatorType) {

        final String queryStr = "SELECT COUNT(snapshot) FROM TISnapshot as snapshot"
                + " WHERE snapshot.symbol = :symbol"
                + " AND snapshot.technicalIndicatorKey = :technicalIndicatorKey"
                + " AND snapshot.technicalIndicatorType = :technicalIndicatorType"
                + " AND snapshot.evaluationPeriod = :evaluationPeriod";

        final Query query = entityManagerDataSvc.getInstance().createQuery(queryStr)
                .setParameter("symbol", symbol)
                .setParameter("evaluationPeriod", evaluationPeriod)
                .setParameter("technicalIndicatorType", technicalIndicatorType)
                .setParameter("technicalIndicatorKey", technicalIndicatorKey);

        return Long.class.cast(query.getSingleResult());
    }
}
