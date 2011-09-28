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
package linqs.gaia.feature.explicit;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;

/**
 * Explicit implementation of a numeric feature
 * 
 * @author namatag
 *
 */
public class ExplicitNum extends BaseExplicit implements ExplicitFeature, NumFeature {
	public ExplicitNum() {
		super();
	}
	
	public ExplicitNum(FeatureValue closeddefault) {
		super(closeddefault);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		// Note: Check base first since we need to support UnknownValue
		return super.isValidValue(value) || value instanceof NumValue;
	}

	public Feature copy() {
		return new ExplicitNum(this.getClosedDefaultValue());
	}
}
