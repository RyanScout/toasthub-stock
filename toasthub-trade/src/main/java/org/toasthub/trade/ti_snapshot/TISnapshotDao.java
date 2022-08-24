package org.toasthub.trade.ti_snapshot;

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
}
