/*
 * Copyright (C) 2020 The ToastHub Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.toasthub.trade.historical_analysis;

import java.util.List;

import org.toasthub.core.common.BaseDao;
import org.toasthub.trade.model.CustomTechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicator;
import org.toasthub.trade.model.TechnicalIndicatorDetail;
import org.toasthub.trade.model.Trade;

public interface HistoricalAnalysisDao extends BaseDao {
        public List<TechnicalIndicatorDetail> getTechnicalIndicatorDetails(TechnicalIndicator technicalIndicator,
                        long startDate,
                        long endDate);

        public TechnicalIndicator findTechnicalIndicatorById(long id);

        public Trade findTradeById(long id);

        public void saveItem(Object o);

        public CustomTechnicalIndicator getCustomTechnicalIndicatorById(long id);

        public TechnicalIndicator getTechnicalIndicatorByProperties(String symbol, String evaluationPeriod,
                        String technicalIndicatorKey);

        public TechnicalIndicator getTechnicalIndicatorFromChild(TechnicalIndicatorDetail child);
}
