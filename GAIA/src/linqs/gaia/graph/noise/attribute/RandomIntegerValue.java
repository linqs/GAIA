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

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;

/**
 * Create an integer valued feature whose
 * value is randomly set.
 * 
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of objects whose features to change
 * <LI> featureid-Feature ID of attribute to add
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> maxvalue-Maximum integer value.  The value is set
 * between 0 (inclusive) and this value (exclusive).
 * <LI> seed-Seed to use for the random number generator
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomIntegerValue extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid;
	private String featureid;
	private Integer maxvalue;
	private Random rand;
	
	private void initialize() {
		initialize = false;
		
		schemaid = this.getStringParameter("schemaid");
		featureid = this.getStringParameter("featureid");
		
		maxvalue = null;
		if(this.hasParameter("maxvalue")) {
			maxvalue = this.getIntegerParameter("maxvalue");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		rand = new Random(seed);
	}

	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		// Add feature if not already defined
		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(featureid)) {
			schema.addFeature(featureid, new ExplicitNum());
			g.updateSchema(schemaid, schema);
		}
		
		// Set graph item values
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			int value = 0;
			if(maxvalue!=null) {
				value = rand.nextInt(maxvalue);
			} else {
				value = rand.nextInt();
			}
			
			gi.setFeatureValue(featureid, new NumValue(value));
		}
	}

	@Override
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		// Add feature if not already defined
		Schema schema = d.getSchema();
		if(!schema.hasFeature(featureid)) {
			schema.addFeature(featureid, new ExplicitNum());
			Graph g = d instanceof Graph ? (Graph) d : ((GraphItem) d).getGraph();
			g.updateSchema(schemaid, schema);
		}
		
		// Set graph item values
		int value = 0;
		if(maxvalue!=null) {
			value = rand.nextInt(maxvalue);
		} else {
			value = rand.nextInt();
		}
		
		d.setFeatureValue(featureid, new NumValue(value));
	}
}
