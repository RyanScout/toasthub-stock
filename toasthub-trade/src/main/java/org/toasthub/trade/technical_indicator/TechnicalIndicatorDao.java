package org.toasthub.trade.technical_indicator;

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

    public TechnicalIndicator findById(final long id) {
        return entityManagerDataSvc.getInstance().find(TechnicalIndicator.class, id);
    }
}
