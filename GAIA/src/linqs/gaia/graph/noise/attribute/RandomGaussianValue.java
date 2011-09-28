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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;
import linqs.gaia.util.SimplePair;

/**
 * This feature sets the numeric value of all graph items
 * with the given schema id to a randomly chosen value
 * chosen from a Gaussian distribution where the original
 * value is the mean and the standard deviation is as defined.
 * 
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of objects whose features to change
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> featureids-Feature IDs of numeric values whose values should be changed.
 * Default is to modify all explicit numeric features.
 * <LI> stddev-Standard deviation to use.  Default is 1.
 * <LI> seed-Seed to use for the random number generator
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomGaussianValue extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid;
	private String[] featureids = null;
	private double stddev;
	private Random rand;
	
	private void initialize() {
		initialize = false;
		
		// Get the schema id
		schemaid = this.getStringParameter("schemaid");
		
		// Get the Feature IDs
		featureids = null;
		if(this.hasParameter("featureids")) {
			featureids = this.getStringParameter("featureids").split(",");
		}
		
		// Get the standard deviation to use
		stddev = 1.0;
		if(this.hasParameter("stddev")) {
			stddev = this.getDoubleParameter("stddev");
		}
		
		// Get random value
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
	}
	
	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		if(featureids==null) {
			// Choose all numeric features
			Schema schema = g.getSchema(schemaid);
			Iterator<SimplePair<String,Feature>> fitr = schema.getAllFeatures();
			List<String> enumfids = new ArrayList<String>();
			while(fitr.hasNext()) {
				SimplePair<String,Feature> pair = fitr.next();
				if(pair.getSecond() instanceof ExplicitNum) {
					enumfids.add(pair.getFirst());
				}
			}
			
			featureids = enumfids.toArray(new String[enumfids.size()]);
		}
		
		// Change to some random value from a Gaussian distribution
		// with the mean, as assigned in the
		// original value, and standard deviation specified.
		for(String fid:featureids) {
			Iterator<GraphItem> allrefs = g.getGraphItems(schemaid);
			while(allrefs.hasNext()) {
				GraphItem gi = allrefs.next();
				FeatureValue fv = gi.getFeatureValue(fid);
				if(!(fv instanceof NumValue)) {
					throw new ConfigurationException(
							"Noise can only be added for"
							+" known explicit numeric feature values: "
							+fv);
				}
				
				NumValue val = (NumValue) fv;
				double mean = val.getNumber();
				double normgauss = rand.nextGaussian();
				double gauss = (normgauss * stddev) + mean;
				
				gi.setFeatureValue(fid, new NumValue(gauss));
			}
		}
	}

	@Override
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		if(featureids==null) {
			// Choose all numeric features
			Schema schema = d.getSchema();
			Iterator<SimplePair<String,Feature>> fitr = schema.getAllFeatures();
			List<String> enumfids = new ArrayList<String>();
			while(fitr.hasNext()) {
				SimplePair<String,Feature> pair = fitr.next();
				if(pair.getSecond() instanceof ExplicitNum) {
					enumfids.add(pair.getFirst());
				}
			}
			
			featureids = enumfids.toArray(new String[enumfids.size()]);
		}
		
		// Change to some random value from a Gaussian distribution
		// with the mean, as assigned in the
		// original value, and standard deviation specified.
		for(String fid:featureids) {
			FeatureValue fv = d.getFeatureValue(fid);
			if(!(fv instanceof NumValue)) {
				throw new ConfigurationException(
						"Noise can only be added for"
						+" known explicit numeric feature values: "
						+fv);
			}
			
			NumValue val = (NumValue) fv;
			double mean = val.getNumber();
			double normgauss = rand.nextGaussian();
			double gauss = (normgauss * stddev) + mean;
			
			d.setFeatureValue(fid, new NumValue(gauss));
		}
	}

}
