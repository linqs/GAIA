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
package linqs.gaia.feature;

import linqs.gaia.util.UnmodifiableList;


/**
 * Categorical feature interface.  Categorical features are string valued features
 * where the value of the string must be a from a pre-specified set of strings.
 * <p>
 * Categorical features must return a CategValue object.
 * 
 * @see linqs.gaia.feature.values.CategValue
 * 
 * @author namatag
 *
 */
public interface CategFeature extends Feature {
	/**
	 * Categories valid for feature
	 * 
	 * @return List of categories
	 */
	UnmodifiableList<String> getAllCategories();
	
	/**
	 * Number of categories valid for feature
	 * 
	 * @return Number of categories
	 */
	int numCategories();
}
