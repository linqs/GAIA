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
package linqs.gaia.model.er.merger.node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

/**
 * Create edges for the merged item based on the edges of the items being merged.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>selfloop-If yes, allow nodes to link to itself.  If no, edges causing
 * a self loop will not be added to the merged item.  Default is false.
 * <LI>allowdups-If yes, allow node to link to an item it also shares a link with.
 * If no, edges causing a duplicate will not be added to the merged item.
 * Default is false.
 * </UL>
 * 
 * @author namatag
 *
 */
public class UnionEdges extends IncidentEdgeMerger {
	private static final long serialVersionUID = 1L;

	public void merge(Iterable<Node> items, Node mergeditem) {
		Graph g = mergeditem.getGraph();
		List<Node> itemlist = IteratorUtils.iterator2nodelist(items.iterator());
		Set<Edge> processed = new HashSet<Edge>();
		
		boolean selfloop = false;
		if(this.hasParameter("selfloop")) {
			selfloop = this.getYesNoParameter("selfloop");
		}
		
		boolean allowdups = false;
		if(this.hasParameter("allowdups")) {
			allowdups = this.getYesNoParameter("allowdups");
		}
		
		for(Node n:items) {
			Iterator<Edge> eitr = n.getAllEdges();
			
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				// Only process each edge once
				// for cases when an edge maybe adjacent
				// to multiple items in the list.
				if(processed.contains(e)) {
					continue;
				}
				
				Edge copye = null;
				if(e instanceof DirectedEdge) {
					DirectedEdge de = (DirectedEdge) e;
					List<Node> sources = IteratorUtils.iterator2nodelist(de.getSourceNodes());
					List<Node> targets = IteratorUtils.iterator2nodelist(de.getTargetNodes());
					
					Set<Node> isect = new HashSet<Node>(sources);
					isect.retainAll(itemlist);
					if(!isect.isEmpty()) {
						sources.removeAll(itemlist);
						sources.add(mergeditem);
					}
					
					isect = new HashSet<Node>(targets);
					isect.retainAll(itemlist);
					if(!isect.isEmpty()) {
						targets.removeAll(itemlist);
						targets.add(mergeditem);
					}
					
					// Handle self loops
					if(!selfloop) {
						isect = new HashSet<Node>(sources);
						isect.retainAll(targets);
						
						if(!isect.isEmpty()) {
							processed.add(e);
							continue;
						}
					}
					
					// Create a copy of the edge with the appropriate values replaced
					GraphItemID gid = GraphItemID.generateGraphItemID(g, e.getSchemaID(), "ued-");
					copye = g.addDirectedEdge(gid, sources.iterator(), targets.iterator());
				} else if(e instanceof UndirectedEdge) {
					UndirectedEdge ue = (UndirectedEdge) e;
					List<Node> nodes = IteratorUtils.iterator2nodelist(ue.getAllNodes());
					
					// At least node n should be in the list so
					// remove all items and add the single merged item
					nodes.removeAll(itemlist);
					nodes.add(mergeditem);
					
					// Handle self loops
					if(!selfloop && nodes.size() == 1) {
						processed.add(e);
						continue;
					}
					
					// Handle duplicates
					if(!allowdups) {
						for(Node currn:nodes) {
							if(!currn.equals(mergeditem) && mergeditem.isAdjacent(n, ue.getSchemaID())) {
								processed.add(e);
								continue;
							}
						}
					}
					
					// Create a copy of the edge with the appropriate values replaced
					GraphItemID gid = GraphItemID.generateGraphItemID(g, e.getSchemaID(), "ueu-");
					copye = g.addUndirectedEdge(gid, nodes.iterator());
				} else {
					throw new UnsupportedTypeException("Unexpected edge type: "
							+e.getClass().getCanonicalName());
				}
				
				// Handle duplicates
				if(!allowdups) {
					Iterator<Edge> curreitr = mergeditem.getAllEdges(e.getSchemaID());
					
					// Remove the previously added edge if it has the same
					// nodes as another added edge
					while(curreitr.hasNext()) {
						Edge curre = (Edge) curreitr.next();
						
						if(!curre.equals(copye) && curre.hasSameNodes(copye)) {
							g.removeEdge(copye);
							break;
						}
					}
				}
				
				processed.add(e);
			}
		}
	}
}
