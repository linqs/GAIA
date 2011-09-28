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

import java.util.List;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Values returned by all {@link CompositeFeature} features.
 * 
 * @author namatag
 *
 */
public class CompositeValue implements FeatureValue {
	private UnmodifiableList<FeatureValue> values;

	/**
	 * Constructor
	 * 
	 * @param values List of Feature values
	 */
	public CompositeValue(List<FeatureValue> values) {
		if(values == null){
			throw new InvalidAssignmentException("Cannot set null values." +
			"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}

		this.values = new UnmodifiableList<FeatureValue>(values);
	}

	/**
	 * Constructor
	 * 
	 * @param values Unmodifiable List of Feature values
	 */
	public CompositeValue(UnmodifiableList<FeatureValue> values) {
		if(values == null){
			throw new InvalidAssignmentException("Cannot set null values." +
			"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}

		this.values = values;
	}

	/**
	 * Return the list of included feature values
	 * 
	 * @return Unmodifiable list of feature values
	 */
	public UnmodifiableList<FeatureValue> getFeatureValues() {
		return values;
	}

	public Object getRawValue() {
		return this.values;
	}

	public String getStringValue() {
		return IteratorUtils.iterator2string(this.values.iterator(), ",");
	}
}
