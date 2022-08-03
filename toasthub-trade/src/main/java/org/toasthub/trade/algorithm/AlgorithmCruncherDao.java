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

package org.toasthub.trade.algorithm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;

import org.toasthub.core.common.BaseDao;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;
import org.toasthub.trade.model.AssetDay;
import org.toasthub.trade.model.AssetMinute;
import org.toasthub.trade.model.LBB;
import org.toasthub.trade.model.SMA;
import org.toasthub.trade.model.UBB;

public interface AlgorithmCruncherDao extends BaseDao {
	public void item(RestRequest request, RestResponse response) throws NoResultException;

	public void saveObject(Object object);

	public void saveList(List<Object> list);

	public void initializedAssetDay(RestRequest request, RestResponse response) throws Exception;

	public void getEarliestAlgTime(RestRequest request, RestResponse response);

	public void getTechicalIndicator(RestRequest request, RestResponse response);

	public List<AssetDay> getAssetDays(String symbol, long startingEpochSeconds, long endingEpochSeconds);

	public List<AssetMinute> getAssetMinutes(String symbol, long startingEpochSeconds, long endingEpochSeconds);

	public Set<SMA> getSMAPrototypes();

	public Set<LBB> getLBBPrototypes();

	public Set<UBB> getUBBPrototypes();

	public long getSMAItemCount(final String symbol, final String evaluationPeriod , final int evaluationDuration , final long epochSeconds);
	public long getLBBItemCount(final String symbol, final String evaluationPeriod , final int evaluationDuration , final long epochSeconds, final BigDecimal standardDeviations);
	public long getUBBItemCount(final String symbol, final String evaluationPeriod , final int evaluationDuration , final long epochSeconds , final BigDecimal standardDeviations);
}
