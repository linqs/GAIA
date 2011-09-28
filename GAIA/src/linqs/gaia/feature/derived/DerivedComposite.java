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
package linqs.gaia.feature.derived;

import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Abstract class for Categorical Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedComposite extends BaseDerived implements CompositeFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
	
	/**
	 * Abstract class to implement to return the list features defined in this composite feature
	 */
	abstract public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures();
	
	/**
	 * Return the number of features defined within this composite feature
	 */
	public int numFeatures() {
		return this.getFeatures().size();
	}
}
