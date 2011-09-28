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
package linqs.gaia.feature.derived.structural;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;

/**
 * A derived feature which returns the number of neighbors,
 * as defined by specified {@link Neighbor}.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Class of {@link Neighbor} implementation,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use when calculating
 * neighborhood.  Default is {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NeighborCount  extends DerivedNum {
	private boolean initialize = true;
	private String neighborclass = Adjacent.class.getCanonicalName();
	private Neighbor neighbor = null;
	
	private void initialize() {
		this.initialize = false;
		
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}

		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		if(initialize) {
			this.initialize();
		}
		
		this.neighbor = neighbor;
	}
	
	public FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for Graph Items: "
					+di.getClass().getCanonicalName());
		}
		
		return new NumValue(this.neighbor.numNeighbors((GraphItem) di));
	}

	public Feature copy() {
		DerivedFeature df = new NeighborCount();
		df.setCID(this.getCID());
		df.copyParameters(this);
		
		return df;
	}
}
