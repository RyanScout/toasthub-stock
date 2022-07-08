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

package org.toasthub.stock.algorithm;


import javax.persistence.NoResultException;

import org.toasthub.core.common.BaseDao;
import org.toasthub.core.general.model.RestRequest;
import org.toasthub.core.general.model.RestResponse;


public interface AlgorithmCruncherDao extends BaseDao {
	public void item(RestRequest request, RestResponse response) throws NoResultException;
	public void saveAll(RestRequest request, RestResponse response);
	public void initializedAssetDay(RestRequest request, RestResponse response) throws Exception;
	public void getRecentAssetDay(RestRequest request, RestResponse response);
	public void getRecentAssetMinute(RestRequest request, RestResponse response);
	public void getRecentAssetMinutes(RestRequest request, RestResponse response);
	public void getEarliestAlgTime(RestRequest request, RestResponse response);
	public void getTechicalIndicator(RestRequest request, RestResponse response);
}
