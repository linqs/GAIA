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

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;

/**
 * A derived feature which returns the number of
 * incident items of the specified item are adjacent another way.
 * For example, this maybe used as an counter of how many other ways
 * the nodes of an edge are adjacent.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> schemaid-If specified, adjacency is checked for only this schema id
 * (i.e., two things are adjacent only if they are incident to the same item with
 * this schema id.)
 * </UL>
 * 
 * @author namatag
 *
 */
public class AdjacentCount  extends DerivedNum {
	public FeatureValue calcFeatureValue(Decorable di) {
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for Graph Items: "
					+di.getClass().getCanonicalName());
		}
		
		// This is currently only defined for items with two incident items
		// i.e., binary edge
		GraphItem gi = (GraphItem) di;
		if(gi.numIncidentGraphItems() != 2) {
			throw new UnsupportedTypeException("This feature only defined for items with" +
					" two adjacent graph items: "+gi+" has "+gi.numIncidentGraphItems());
		}
		
		// Get items incident to the specified item
		Iterator<GraphItem> gitr = gi.getIncidentGraphItems();
		GraphItem gi1 = gitr.next();
		GraphItem gi2 = gitr.next();
		
		gitr = null;
		if(this.hasParameter("schemaid")) {
			gitr = gi1.getIncidentGraphItems(this.getStringParameter("schemaid"));
		} else {
			gitr = gi1.getIncidentGraphItems();
		}
		
		int counter = 0;
		while(gitr.hasNext()){
			GraphItem currgi = gitr.next();
			if(currgi instanceof Edge && ((Edge) currgi).isIncident((Node) gi2)) {
				counter++;
			} else if(currgi instanceof Node && ((Node) currgi).isIncident((Edge) gi2)) {
				counter++;
			}
		}
		
		return new NumValue(counter);
	}

	public Feature copy() {
		DerivedFeature df = new AdjacentCount();
		df.setCID(this.getCID());
		df.copyParameters(this);
		
		return df;
	}
}
