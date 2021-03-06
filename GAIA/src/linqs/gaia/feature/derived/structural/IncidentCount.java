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
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Node;

/**
 * A derived feature which returns the degree of a decorable item.
 * This feature is only defined over nodes and edges.
 * For nodes, this will return the number of unique edges
 * it participates in.
 * For edges, this will return the number of unique nodes
 * the edge contains.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> schemaid-If specified, only incident nodes with this schema ID are counted.
 * </UL>
 * 
 * @author namatag
 *
 */
public class IncidentCount  extends DerivedNum {
	String schemaid = null;
	protected void initialize() {
		if(this.hasParameter("schemaid")) {
			schemaid = this.getStringParameter("schemaid");
		}
	}
	
	public FeatureValue calcFeatureValue(Decorable di) {		
		int degree = -1;
		if(di instanceof Node) {
			degree = schemaid==null ? ((Node) di).numEdges() : ((Node) di).numIncidentGraphItems(schemaid);
		} else if(di instanceof Edge) {
			degree = schemaid==null ? ((Edge) di).numNodes() : ((Edge) di).numIncidentGraphItems(schemaid);
		} else {
			throw new UnsupportedTypeException("Unsupported decorable item type: "
					+di.getClass().getCanonicalName());
		}
		
		return new NumValue(0.0+degree);
	}

	public Feature copy() {
		DerivedFeature df = new IncidentCount();
		df.setCID(this.getCID());
		df.copyParameters(this);
		
		return df;
	}
}
