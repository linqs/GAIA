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

import java.util.HashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;

public class DGExplicitString extends ExplicitString implements StringFeature, DGExplicitFeature {
	private ExplicitString es = null;
	private HashMap<Integer,String> id2value;

	public DGExplicitString(ExplicitString es) {
		this.id2value = new HashMap<Integer, String>();
		this.es = es;
	}

	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			return new StringValue(id2value.get(di));
		}

		// Handle unknown value
		if(es.isClosed()){
			return es.getClosedDefaultValue();
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
		} else if(this.isClosed() && value.equals(es.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			this.id2value.put(di, ((StringValue) value).getString());
		}
	}

	public FeatureValue getClosedDefaultValue() {
		return es.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return es.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return es.isValidValue(value);
	}

	public Feature copy() {
		return this.es.copy();
	}

	public ExplicitFeature getOrigFeature() {
		return this.es;
	}
}
