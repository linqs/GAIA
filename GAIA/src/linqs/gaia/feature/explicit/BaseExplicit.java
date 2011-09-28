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

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Base implementation of explicit class to simplify implementations
 * of explicit features.
 * 
 * @author namatag
 *
 */
public abstract class BaseExplicit implements ExplicitFeature {
	private FeatureValue closeddefault;
	
	/**
	 * Default is an open explicit feature
	 */
	protected BaseExplicit(){
		this(null);
	}
	
	protected BaseExplicit(FeatureValue closeddefault){
		initialize(closeddefault);
	}
	
	/**
	 * Initialize base explicit.  Called from base constructor and is done
	 * this way since for it to work for Explicit Categorical values,
	 * the category must be assigned prior to initializing.  Otherwise,
	 * an exception is thrown when we check if the value is valid.
	 * 
	 * @param closeddefault
	 */
	protected void initialize(FeatureValue closeddefault) {
		if(closeddefault != null && !this.isValidValue(closeddefault)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+closeddefault);
		}
		
		this.closeddefault = closeddefault;
	}
	
	/**
	 * Unknown value is a valid value for all categorical features
	 */
	public boolean isValidValue(FeatureValue value) {
		return value == FeatureValue.UNKNOWN_VALUE;
	}
	
	public FeatureValue getClosedDefaultValue() {
		return this.closeddefault;
	}

	public boolean isClosed() {
		return closeddefault != null;
	}
}
