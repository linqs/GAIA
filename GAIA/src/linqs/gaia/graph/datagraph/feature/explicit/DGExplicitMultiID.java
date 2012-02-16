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

import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.UnmodifiableSet;

public class DGExplicitMultiID extends ExplicitMultiID implements MultiIDFeature, DGExplicitFeature {
	private ExplicitMultiID emid = null;
	private ConcurrentHashMap<Integer,UnmodifiableSet<ID>> id2value;
	
	public DGExplicitMultiID(ExplicitMultiID es) {
		this.id2value = new ConcurrentHashMap<Integer, UnmodifiableSet<ID>>();
		this.emid = es;
	}
	
	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			return new MultiIDValue(id2value.get(di));
		}
		
		// Handle unknown value
		if(emid.isClosed()){
			return emid.getClosedDefaultValue();
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
		} else if(this.isClosed() && value.equals(emid.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			this.id2value.put(di, ((MultiIDValue) value).getIDs());
		}
	}

	public FeatureValue getClosedDefaultValue() {
		return emid.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return emid.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return emid.isValidValue(value);
	}

	public Feature copy() {
		return this.emid.copy();
	}
	
	public ExplicitFeature getOrigFeature() {
		return this.emid;
	}
}
