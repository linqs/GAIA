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
package linqs.gaia.model.er.bootstrap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;

/**
 * Implement of bootstrapping criterion as defined in
 * Section 5.4 of:
 * <p>
 * Bhattacharya, I. & Getoor, L.,
 * Collective Entity Resolution in Relational Data,
 * ACM Transactions on Knowledge Discovery from Data, 2007, 1, 1-36 
 * <p>
 * Given the "neighbors" of two items, return true
 * if at least "k" of the neighbors have the same value for a given feature.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> matchfeatureid-Feature which all neighbors must have and whose values must match.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> k-K value to use in bootstrapping.  Default is k=1.
 * <LI> neighborclass-{@link Neighbor} class to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class KExactMatch extends BaseConfigurable implements ERBootstrap {
	private static final long serialVersionUID = 1L;
	
	private boolean shouldInitialize = true;
	private Neighbor neighbor = null;
	private int k;
	private String matchfeatureid = null;
	
	/**
	 * Initialize the parameters, when first run
	 */
	private void initialize() {
		shouldInitialize = false;
		
		// Initialize neighbor information
		String neighborclass = Adjacent.class.getCanonicalName();
		if(this.hasParameter("neighborclass")) {
			neighborclass = this.getStringParameter("neighborclass");
		}
		
		k = 1;
		if(this.hasParameter("k")) {
			k = this.getIntegerParameter("k");
		}
		
		matchfeatureid = null;
		if(this.hasParameter("matchfeatureid")) {
			matchfeatureid = this.getStringParameter("matchfeatureid");
		}
		
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass);
		this.neighbor.copyParameters(this);
	}
	
	public boolean isSameEntity(GraphItem gi1, GraphItem gi2) {
		if(shouldInitialize) {
			this.initialize();
		}
		
		// Get neighbors of the two graph items
		Iterable<GraphItem> neigh1 = this.neighbor.getNeighbors(gi1);
		Iterable<GraphItem> neigh2 = this.neighbor.getNeighbors(gi2);
		
		// Get the values of people
		int nummatch = 0;
		for(GraphItem g1n:neigh1) {
			if(!g1n.getSchema().hasFeature(matchfeatureid)) {
				throw new ConfigurationException("Feature undefined in neighboring schema: "
						+matchfeatureid+" in "+g1n.getSchemaID());
			}
			
			for(GraphItem g2n:neigh2) {
				if(!g2n.getSchema().hasFeature(matchfeatureid)) {
					throw new ConfigurationException("Feature undefined in neighboring schema: "
							+matchfeatureid+" in "+g2n.getSchemaID());
				}
				
				FeatureValue g1nfv = g1n.getFeatureValue(matchfeatureid);
				FeatureValue g2nfv = g2n.getFeatureValue(matchfeatureid);
				
				if(g1nfv.equals(FeatureValue.UNKNOWN_VALUE) ||
					g2nfv.equals(FeatureValue.UNKNOWN_VALUE)) {
					continue;
				}
				
				// If they match, count as a match
				if(g1nfv.equals(g2nfv)) {
					nummatch++;
				}
			}
		}
			
		// Get the intersect of values		
		return nummatch > k ? true : false;
	}
}
