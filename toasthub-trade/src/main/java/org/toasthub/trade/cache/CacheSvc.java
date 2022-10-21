package org.toasthub.trade.cache;

import org.toasthub.core.common.BaseSvc;
import org.toasthub.trade.model.CustomTechnicalIndicator;

public interface CacheSvc extends BaseSvc {
    public void save(CustomTechnicalIndicator c);
}
