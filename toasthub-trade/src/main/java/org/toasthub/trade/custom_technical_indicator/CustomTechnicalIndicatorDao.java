package org.toasthub.trade.custom_technical_indicator;

import org.toasthub.core.common.BaseDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;

public interface CustomTechnicalIndicatorDao extends BaseDao {
    public void saveItem(Object o);

    public CustomTechnicalIndicator getReference(long id);

    public CustomTechnicalIndicator find(long id);
}
