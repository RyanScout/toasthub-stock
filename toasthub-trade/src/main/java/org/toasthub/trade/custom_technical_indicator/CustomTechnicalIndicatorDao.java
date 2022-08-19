package org.toasthub.trade.custom_technical_indicator;

import java.util.List;

import org.toasthub.core.common.BaseDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.Symbol;

public interface CustomTechnicalIndicatorDao extends BaseDao {
    public void saveItem(Object o);

    public CustomTechnicalIndicator getReference(long id);

    public CustomTechnicalIndicator findById(long id);

    public List<CustomTechnicalIndicator> getCustomTechnicalIndicators();

    public long countByName(String name);

    public CustomTechnicalIndicator findByName(String name);

    public List<Symbol> getCustomTechnicalIndicatorSymbols(CustomTechnicalIndicator customTechnicalIndicator);
}
