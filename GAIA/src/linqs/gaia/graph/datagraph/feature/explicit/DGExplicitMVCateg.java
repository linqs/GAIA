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
package linqs.gaia.graph.datagraph.feature.explicit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

public class DGExplicitMVCateg extends ExplicitMultiCateg implements MultiCategFeature, DGExplicitFeature {
	private ExplicitMultiCateg emvc = null;
	private ConcurrentHashMap<Integer,SimplePair<Set<String>,double[]>> id2value;
	
	public DGExplicitMVCateg(ExplicitMultiCateg emvc) {
		super(emvc.getAllCategories());
		
		this.id2value = new ConcurrentHashMap<Integer,SimplePair<Set<String>,double[]>>();
		this.emvc = emvc;
	}

	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			SimplePair<Set<String>,double[]> pair = id2value.get(di);
			return new MultiCategValue(pair.getFirst(), pair.getSecond());
		}
		
		// Handle unknown value
		if(emvc.isClosed()){
			return emvc.getClosedDefaultValue();
		} else {
			return FeatureValue.UNKNOWN_VALUE;
		}
	}

	public void setFeatureValue(Integer di, FeatureValue value) {
		if(!this.isValidValue(value)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+value);
		}
		
		// If the value is set as unknown value, don't store it in map.
		if(value.equals(FeatureValue.UNKNOWN_VALUE)){
			if(this.id2value.containsKey(di)) {
				this.id2value.remove(di);
			}
		} else if(this.isClosed() && value.equals(emvc.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			MultiCategValue mvc = (MultiCategValue) value;
			Set<String> cats = mvc.getCategories().copyAsSet();
			double[]  probs = mvc.getProbs();
			
			// Handle categorical values where the prob is set to null
			if(probs==null) {
				probs = new double[this.getAllCategories().size()];
				for(int i=0; i<probs.length; i++) {
					probs[i] = 1;
				}
			}
			
			SimplePair<Set<String>,double[]> pair
				= new SimplePair<Set<String>,double[]>(cats, probs);
			id2value.put(di, pair);
		}
	}

	public UnmodifiableList<String> getAllCategories() {
		return emvc.getAllCategories();
	}

	public FeatureValue getClosedDefaultValue() {
		return emvc.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return emvc.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return emvc.isValidValue(value);
	}

	public Feature copy() {
		return this.emvc.copy();
	}
	
	public ExplicitFeature getOrigFeature() {
		return this.emvc;
	}
}
