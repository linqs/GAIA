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
package linqs.gaia.graph.noise.attribute;

import java.util.Iterator;
import java.util.Random;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;

/**
 * Randomly remove explicit valued features for certain graph items.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>schemaid-Schema id of schema and graph items whose values will be removed
 * <LI>featureids-Comma delimited list of feature ids of explicit feature to randomly remove
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>probremove-Probability of removing an attribute.  Default is .25.
 * <LI>seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomRemoveLabels extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid = null;
	private String[] featureids = null;
	private Random rand = null;
	private double probremove = .25;
	
	private void initialize() {
		initialize = false;
		
		// Get parameters
		schemaid = this.getStringParameter("schemaid");
		featureids = this.getStringParameter("featureids").split(",");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
		
		probremove = .25;
		if(this.hasParameter("probremove")) {
			probremove = this.getDoubleParameter("probremove");
		}
	}
	
	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		for(String fid:featureids) {
			this.addNoise(g, fid);
		}
	}
	
	private void addNoise(Graph g, String featureid) {
		// Verify that the feature is a valid explicit feature
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(featureid);
		
		if(!(f instanceof ExplicitFeature)) {
			throw new ConfigurationException("Only explicit features supported:"
					+" Schema ID="+schemaid
					+" Feature ID="+featureid
					+" is "+f.getClass().getCanonicalName());
		}
		
		// With a given probability, remove the value of the feature for a
		// given graph item
		Iterator<GraphItem> itr = g.getGraphItems(schemaid);
		while(itr.hasNext()) {
			GraphItem gi = itr.next();
			
			if(rand.nextDouble() < probremove) {
				gi.setFeatureValue(featureid, FeatureValue.UNKNOWN_VALUE);
			}
		}
	}

	@Override
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		for(String fid:featureids) {
			if(rand.nextDouble() < probremove) {
				d.setFeatureValue(fid, FeatureValue.UNKNOWN_VALUE);
			}
		}
	}
}
