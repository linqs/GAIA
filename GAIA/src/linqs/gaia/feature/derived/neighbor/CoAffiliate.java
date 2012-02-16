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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;

/**
 * Identify a co-affiliates of a graph item.
 * An affiliate is defined over bipartite graphs where
 * if A is adjacent to C and B is adjacent to C
 * (given incident items of the specified schema id)
 * then A and B are co-affiliates.
 * For nodes adjacent given a directed edge,
 * A and B are adjacent if they have same direction of
 * relationship with C.
 * This means that if a directed edge from A to C exists
 * and B to C exists, then A and B are co-affiliates.
 * However, if the edge is from C to B, then A and C are not affiliates.
 * <p>
 * Note: Graph Items which are co-affiliates in multiple ways
 * are still only returned once.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> incidentsid-Schema ID of incident graph items
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> adjacentsid-Schema ID of items adjacent to this item
 * <LI> affiliatesid-Schema ID of co-affiliates
 * <LI> includeself-If yes, include self as one of the neighbors.
 * Default is no.
 * <LI>dirtype-If the connecting sid is a directed edge, this specifies the
 * types of nodes to include.  Options include:
 * <UL>
 * <LI>commontarget-Return only nodes which share a common target to this node
 * (i.e., For item A, if an edge from A to C exists and B to C exists, then B is a co-affiliate).
 * <LI>commonsource-Return only nodes which share a common source to this node
 * (i.e., For item A, if an edge from C to A exists and C to B exists, then B is a co-affiliate).
 * <LI>all-Return all nodes with a common target or source (Default)
 * </UL>
 * </UL>
 * 
 * @author namatag
 *
 */
public class CoAffiliate extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	private String incidentsid = null;
	private String adjacentsid = null;
	private String affiliatesid = null;
	private String dirtype = null;
	private boolean includeself = false;
	
	public CoAffiliate() {
		// Do nothing.  Assume the appropriate parameters will be set.
	}
	
	public CoAffiliate(String incidentsid, String adjacentsid, String affiliatesid) {
		this.setParameter("incidentsid", incidentsid);
		this.setParameter("adjacentsid", adjacentsid);
		this.setParameter("affiliatesid", affiliatesid);
	}
	
	protected void initialize() {
		incidentsid = this.getStringParameter("incidentsid");
		
		if(this.hasParameter("adjacentsid")) {
			adjacentsid = this.getStringParameter("adjacentsid");
		}
		
		if(this.hasParameter("affiliatesid")) {
			affiliatesid = this.getStringParameter("affiliatesid");
		}
		
		includeself = this.hasYesNoParameter("includeself", "yes");
		dirtype = this.getCaseParameter("dirtype", new String[]{"commonsource","commontarget","all"}, "all");
	}

	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		Set<GraphItem> coaffiliates = new HashSet<GraphItem>();
		
		SchemaType inctype = gi.getGraph().getSchemaType(incidentsid);
		if(gi instanceof Node && inctype.equals(SchemaType.DIRECTED)) {
			Node n = (Node) gi;
			
			if(dirtype.equals("all") || dirtype.equals("commontarget")) {
				// Handle case where A->C and B->C so A and B are affiliates
				Iterator<Node> adjitr = n.getAdjacentSources(incidentsid);
				while(adjitr.hasNext()) {
					Node adj = adjitr.next();
					if(adjacentsid != null && !adj.getSchemaID().equals(adjacentsid)) {
						continue;
					}
					
					Iterator<Node> affitr = adj.getAdjacentTargets(incidentsid);
					while(affitr.hasNext()) {
						GraphItem aff = affitr.next();
						if(affiliatesid!=null && !aff.getSchemaID().equals(affiliatesid)) {
							continue;
						}
						
						if(aff.equals(gi)) {
							continue;
						}
						
						coaffiliates.add(aff);
					}
				}
			}
			
			if(dirtype.equals("all") || dirtype.equals("commontarget")) {
				// Handle case where C->A and C->B so A and B are affiliates
				Iterator<Node> adjitr = n.getAdjacentTargets(incidentsid);
				while(adjitr.hasNext()) {
					Node adj = adjitr.next();
					if(adjacentsid != null && !adj.getSchemaID().equals(adjacentsid)) {
						continue;
					}
					
					Iterator<Node> affitr = adj.getAdjacentSources(incidentsid);
					while(affitr.hasNext()) {
						GraphItem aff = affitr.next();
						if(affiliatesid!=null && !aff.getSchemaID().equals(affiliatesid)) {
							continue;
						}
						
						if(aff.equals(gi)) {
							continue;
						}
						
						coaffiliates.add(aff);
					}
				}
			}
		} else {
			// Get adjacent graph items of gi
			Iterator<GraphItem> adjitr = gi.getAdjacentGraphItems(incidentsid);
			while(adjitr.hasNext()) {
				// Get items adjacent to the adjacent item
				GraphItem adj = adjitr.next();
				if(adjacentsid != null && !adj.getSchemaID().equals(adjacentsid)) {
					continue;
				}
				
				Iterator<GraphItem> affitr = adj.getAdjacentGraphItems(incidentsid);
				while(affitr.hasNext()) {
					GraphItem aff = affitr.next();
					
					if(affiliatesid!=null && !aff.getSchemaID().equals(affiliatesid)) {
						continue;
					}
					
					if(aff.equals(gi)) {
						continue;
					}
					
					coaffiliates.add(aff);
				}
			}
		}
		
		if(includeself) {
			coaffiliates.add(gi);
		}
		
		return coaffiliates;
	}
}
