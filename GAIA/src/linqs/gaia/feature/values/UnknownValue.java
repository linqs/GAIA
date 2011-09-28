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

/**
 * Value to return whenever the value for a given feature is unknown.
 * A new instance of this should never be created.  Use the {@link FeatureValue#UNKNOWN_VALUE} instance.
 * 
 * @author namatag
 *
 */
public class UnknownValue implements FeatureValue {
	/**
	 * String to print for unknown values
	 */
	public static final String UNKNOWN_VALUE="UNKNOWN_VALUE";
	
	/**
	 * Constructor
	 */
	protected UnknownValue() {
		
	}
	
	public Object getRawValue() {
		return UNKNOWN_VALUE;
	}

	public String getStringValue() {
		return UNKNOWN_VALUE;
	}
	
	/**
	 * String representation of feature
	 */
	public String toString() {
		return UNKNOWN_VALUE;
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj || obj instanceof UnknownValue) {
			return true;
		}

		return false;
	}

	public int hashCode() {
		int hash = 1;

		return hash;
	}
}
