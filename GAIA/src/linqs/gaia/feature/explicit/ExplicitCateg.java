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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.UnmodifiableList;

/**
 * Explicit implementation of a categorical feature
 * <p>
 * Note: This feature assumes that the categories can be stored in memory.
 * 
 * @author namatag
 *
 */
public class ExplicitCateg extends BaseExplicit implements ExplicitFeature, CategFeature {
	private UnmodifiableList<String> categories;
	
	public ExplicitCateg(UnmodifiableList<String> categories, FeatureValue closeddefault){
		if(categories.isEmpty()){
			throw new InvalidAssignmentException("No categories defined");
		}
		
		Set<String> categoriesset = new HashSet<String>(categories.copyAsList());
		if(categories.size()!=categoriesset.size()) {
			throw new InvalidAssignmentException("Duplicates cannot exist in categories: "+categories);
		}
		
		this.categories = categories;
		this.initialize(closeddefault);
	}
	
	public ExplicitCateg(UnmodifiableList<String> categories){
		this(categories, null);
	}
	
	public ExplicitCateg(List<String> categories, FeatureValue closeddefault){
		this(new UnmodifiableList<String>(categories), closeddefault);
	}
	
	public ExplicitCateg(List<String> categories){
		this(new UnmodifiableList<String>(categories), null);
	}
	
	public ExplicitCateg(String[] categories, FeatureValue closeddefault){
		this(new UnmodifiableList<String>(categories), closeddefault);
	}
	
	public ExplicitCateg(String[] categories){
		this(new UnmodifiableList<String>(categories), null);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		if(super.isValidValue(value)) {
			return true;
		}
		
		// If value is of the wrong type, return false
		if(value==null || !(value instanceof CategValue)) {
			return false;
		}
		
		// Return true if its one of the valid categories.  False otherwise.
		CategValue cvalue = (CategValue) value;
		if(!this.categories.contains(cvalue.getCategory().intern())) {
			return false;
		}
		
		double[] probs = cvalue.getProbs();
		if(probs!=null && probs.length!=categories.size()) {
			return false;
		}
		
		return true;
	}
	
	public UnmodifiableList<String> getAllCategories() {
		return this.categories;
	}

	public Feature copy() {
		return new ExplicitCateg(this.getAllCategories(), this.getClosedDefaultValue());
	}

	public int numCategories() {
		return this.categories.size();
	}
}
