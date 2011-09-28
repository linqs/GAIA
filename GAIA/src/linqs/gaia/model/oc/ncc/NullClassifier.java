/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.model.oc.ncc;

import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * This is a null classifier mainly used for debugging.
 * This classifier will just return {@link FeatureValue#UNKNOWN_VALUE}
 * as the predicted value of all items.
 * 
 * @author namatag
 *
 */
public class NullClassifier extends BaseVBClassifier implements VBClassifier  {
	private static final long serialVersionUID = 1L;

	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid,
			List<String> featureids) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		return FeatureValue.UNKNOWN_VALUE;
	}

	@Override
	public void loadVBOC(String directory) {
		// Do nothing
	}

	@Override
	public void saveVBOC(String directory) {
		// Do nothing
	}
}
