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

import java.io.Serializable;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.NumFeature;

/**
 * Numeric value returned by all {@link NumFeature} features.
 * 
 * @author namatag
 *
 */
public class NumValue implements FeatureValue, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Double value = null;
	
	/**
	 * Constructor
	 * 
	 * @param value Numeric value
	 */
	public NumValue(Double value) {
		if(value == null || value.equals(Double.NaN)){
			throw new InvalidAssignmentException("Cannot set null or NaN number.  " +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.  Value="+value);
		}
		
		this.value = value;
	}
	
	/**
	 * Constructor
	 * 
	 * @param value Numeric value
	 */
	public NumValue(Integer value) {
		if(value == null){
			throw new InvalidAssignmentException("Cannot set null or NaN number.  " +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.  Value="+value);
		}
		
		this.value = 0.0+value;
	}
	
	/**
	 * Return numeric value
	 * 
	 * @return Numeric value
	 */
	public Double getNumber() {
		return this.value;
	}

	public Object getRawValue() {
		return this.value;
	}

	public String getStringValue() {
		return this.value.toString();
	}

	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY]
	 */
	public String toString() {
		return this.getClass().getCanonicalName()+"="+this.getNumber();
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (!(obj instanceof NumValue)) {
	      return false;
	    }
	    
	    NumValue value = (NumValue) obj;
	    
	    return this.value.equals(value.value);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.value.hashCode();
	    
	    return hash;
	}
}
