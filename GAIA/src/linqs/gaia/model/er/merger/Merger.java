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
package linqs.gaia.model.er.merger;

import java.util.ArrayList;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.er.merger.feature.FeatureMerger;
import linqs.gaia.model.er.merger.node.IncidentEdgeMerger;

/**
 * Base class of all Merger implementations.
 * Mergers can merge information (e.g., feature value, edges) from multiple items
 * to the specified merge item.  One example is to have a Merger which
 * sets the label of the merge item to the majority label of the items.
 * This is for use with entity resolution with merging items which are deemed
 * to be the same entity.
 * <p>
 * Note:  Merges will merge information from multiple sources but will
 * not remove the sources (i.e., merged the features of nodes will not remove
 * the nodes themselves).  Sources, and other relevant information,
 * must be removed by themselves.
 * 
 * @author namatag
 *
 * @param <C> Class of the items being merged and the merged item
 */
public abstract class Merger<C> extends BaseConfigurable implements Configurable {
	/**
	 * Merge information from the specified items into the merged item.
	 * 
	 * @param items Iterable items to merge
	 * @param mergeditem Item to merge to
	 */
	public abstract void merge(Iterable<C> items, C mergeditem);
	
	/**
	 * Merge the specified nodes using the specified
	 * feature and edge merger.  Merging is done
	 * by creating a new node and setting the
	 * features and edges of that node with
	 * a merge of the features and edges of the merged items.
	 * <p>
	 * Note:  Removes the merged items.
	 * 
	 * @param items Iterable items to merge
	 */
	public static Node mergeNodes(Graph g, Iterable<Node> items,
			FeatureMerger fmerger, IncidentEdgeMerger iemerger) {
		
		// Create item to merge into
		GraphItemID gid = GraphItemID.generateGraphItemID(g, items.iterator().next().getSchemaID(), "m-");
		Node mergeditem = g.addNode(gid);
		
		// Convert list of nodes to list of decorable
		List<Decorable> itemlist = new ArrayList<Decorable>();
		for(Node currn : items) {
			itemlist.add(currn);
		}
		
		// Run feature and incident edge mergers
		if(fmerger!=null) {
			fmerger.merge(itemlist, mergeditem);
		}
		
		if(iemerger!=null) {
			iemerger.merge(items, mergeditem);
		}
		
		// Remove the nodes that were merged, as well as the edges for those nodes
		for(Node currn: items) {
			currn.removeIncidentEdges();
			g.removeNode(currn);
		}
		
		return mergeditem;
	}
}
