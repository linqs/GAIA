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

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.StringFeature;

/**
 * String value returned by all {@link StringFeature} features.
 * 
 * @author namatag
 *
 */
public class StringValue implements FeatureValue {
	private String stringvalue = null;
	
	/**
	 * Constructor
	 * 
	 * @param value String value
	 */
	public StringValue(String value) {
		if(value == null){
			throw new InvalidAssignmentException("Cannot set null string." +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}
		
		this.stringvalue = value;
	}
	
	/**
	 * Return string value
	 * 
	 * @return String value
	 */
	public String getString() {
		return this.stringvalue;
	}
	
	public Object getRawValue() {
		return this.stringvalue;
	}

	public String getStringValue() {
		return this.stringvalue;
	}
	
	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY]
	 */
	public String toString() {
		return this.getClass().getCanonicalName()+"="+this.getStringValue();
	}
	
	public static StringValue parseString(String value) {
		value = value.trim();
		if(!value.startsWith(StringValue.class.getCanonicalName()+"=")) {
			throw new InvalidStateException("String not a valid string representation of this feature");
		}
		
		value = value.replace(StringValue.class.getCanonicalName()+"=", "");
		
		return new StringValue(value);
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (!(obj instanceof StringValue)) {
	      return false;
	    }
	    
	   StringValue value = (StringValue) obj;
	    
	    return this.stringvalue.equals(value.stringvalue);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.stringvalue.hashCode();
	    
	    return hash;
	}
}
