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

package org.toasthub.trade.trade;

import java.util.List;

import org.toasthub.core.common.BaseDao;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.Trade;

public interface TradeDao extends BaseDao {
    public List<Trade> getRunningTrades();

    public List<Trade> getAllRunningTrades();

    public List<Trade> getRunningDayTrades();

    public void resetTrade(RestRequest request, RestResponse response);

    public void getSymbolData(RestRequest request, RestResponse response);
}
