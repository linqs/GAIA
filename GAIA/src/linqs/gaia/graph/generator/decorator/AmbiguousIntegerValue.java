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
package linqs.gaia.graph.generator.decorator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;

/**
 * Create an integer valued feature whose
 * value is set according to the synthetic
 * data generator defined in:
 * 
 * The decorator works by going through each item of the specified schema.
 * For each item, with the given probability, select an integer previously
 * applied to another item.  Otherwise, select a value
 * (between 0, inclusive, and maxvalue, exclusive)
 * that wasn't previously selected
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of objects whose features to change
 * <LI> featureid-Feature ID of attribute to add
 * <LI> probamb-Probability of using a previously given value instead
 * of a new unique one.
 * <LI> maxvalue-Maximum integer value to assign.  If maxvalue is less than
 * the number of instances, a previously assigned value will be used.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> seed-Seed to use for the random number generator
 * </UL>
 * 
 * @author namatag
 *
 */
public class AmbiguousIntegerValue extends BaseConfigurable implements Decorator {
	private boolean initialize = true;
	private String schemaid;
	private String featureid;
	private double probamb;
	private Random rand;
	private int maxvalue;
	
	private void initialize() {
		initialize = false;
		
		schemaid = this.getStringParameter("schemaid");
		featureid = this.getStringParameter("featureid");
		probamb = this.getDoubleParameter("probamb");
		if(probamb<0 || probamb>1) {
			throw new ConfigurationException("Probability must be between 0 and 1, inclusive");
		}
		
		maxvalue = this.getIntegerParameter("maxvalue");
		if(maxvalue<=0) {
			throw new ConfigurationException("Parameter maxvalue must be greater than 0");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		rand = new Random(seed);
	}
	
	public void decorate(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		// Add feature if not already defined
		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(featureid)) {
			schema.addFeature(featureid, new ExplicitNum());
			g.updateSchema(schemaid, schema);
		}
		
		// Initialize available free values
		List<Integer> freevalues = new ArrayList<Integer>();
		for(int i=0; i<maxvalue; i++) {
			freevalues.add(i);
		}
		
		// Set graph item values
		List<Integer> ambvalues = new ArrayList<Integer>();
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			int value = 0;
			
			if(freevalues.isEmpty() || (!ambvalues.isEmpty() && rand.nextDouble()<probamb)) {
				// Set to a previously defined value
				value = ambvalues.get(rand.nextInt(ambvalues.size()));
			} else {
				// Set to a randomly selected new value
				value = freevalues.remove(rand.nextInt(freevalues.size()));
				ambvalues.add(value);
			}
			
			gi.setFeatureValue(featureid, new NumValue(value));
		}
	}
}
