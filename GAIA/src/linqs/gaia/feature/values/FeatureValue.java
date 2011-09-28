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
package linqs.gaia.feature.values;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Base interface for the value of a feature corresponding to
 * a specific {@link Decorable} object.
 * <p>
 * Note:
 * <UL>
 * <LI>A {@link FeatureValue} can only be set during construction.
 * <LI>{@link FeatureValue#UNKNOWN_VALUE} should be returned for items where a feature is not defined.
 * </UL>
 * 
 * @author namatag
 *
 */
public interface FeatureValue {
	
	public static FeatureValue UNKNOWN_VALUE = new UnknownValue();
	
	/**
	 * Return the object value of the feature.
	 * For use when you want the value of feature for an object
	 * for things like equality checking or printing
	 * where you don't necessarily need to know the specific
	 * value type.
	 * 
	 * @return Object representation of the value
	 */
	Object getRawValue();
	
	/**
	 * Return a string representation of the value
	 * 
	 * @return String representation of the value
	 */
	String getStringValue();
}
