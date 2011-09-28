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
package linqs.gaia.feature.derived.neighbor;

import java.util.ArrayList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Return the set of graph items uniquely identified by the ids
 * stored in the specified multi-id feature.
 * The id is used with the GraphRegistry to get the graph item,
 * if its registered (allowing for a neighbor in other registered graphs),
 * or the id is used on the graph containing the specified graph item.
 * An empty list is returned if the value of the multi-id feature
 * is unknown.  An exception is thrown if an ID of the feature
 * is not a graph item id or if the item cannot be located.
 * 
 * Required Parameters:
 * <UL>
 * <LI> midfid-Feature ID of the multi-id feature where the ids are stored
 * </UL>
 * 
 * @author namatag
 *
 */
public class MultiIDNeighbor extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		String midfid = this.getStringParameter("midfid");
		
		List<GraphItem> neighbors = new ArrayList<GraphItem>();
		FeatureValue fv = gi.getFeatureValue(midfid);
		
		if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
			// If none are defined, return an empty list
			return neighbors;
		}
		
		if(!(fv instanceof MultiIDValue)) {
			throw new ConfigurationException("MultiID feature expected: "
				+midfid+" is of type "+gi.getSchema().getFeature(midfid).getClass().getCanonicalName());
		}
		
		MultiIDValue midv = (MultiIDValue) fv;
		UnmodifiableSet<ID> ids = midv.getIDs();
		for(ID id:ids) {
			if(!(id instanceof GraphItemID)) {
				throw new UnsupportedTypeException("Only graph item ids supported");
			}
			
			GraphItemID giid = (GraphItemID) id;
			if(GraphRegistry.isRegistered(giid)) {
				neighbors.add(GraphRegistry.getGraphItem(giid));
			} else if(gi.getGraph().hasGraphItem(giid)) {
				neighbors.add(gi.getGraph().getGraphItem(giid));
			} else {
				throw new InvalidStateException("ID cannot be resolved to an item" +
						" in a registered graph or in the graph of the object");
			}
		}
		
		return neighbors;
	}

}
