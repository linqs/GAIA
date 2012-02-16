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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.Dynamic;

/**
 * Given a binary edge,  consider the two nodes adjacent to that edge,
 * return the number of edges from one set of neighboring nodes to the other set of neighboring nodes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-{@link Neighbor} class to use for the node,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> normalize-If yes, return the number of edges normalized by the number of possible edges.
 * Default is to return the unnormalized number of edges.
 * <LI> edgesid-Schema id of edge where the neighboring nodes are adjacent by.
 * Default is to use all edges.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeNeighborAdjacency extends DerivedNum {
	private Neighbor neighbor = null;
	private boolean normalize = true;
	private String edgesid = null;
	
	protected void initialize() {
		String neighborclass = Adjacent.class.getCanonicalName();
		if(this.hasParameter("neighborclass")) {
			neighborclass = this.getStringParameter("neighborclass");
		}
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		if(this.hasParameter("normalize")) {
			normalize = this.getYesNoParameter("normalize");
		}
		
		if(this.hasParameter("edgesid")) {
			edgesid = this.getStringParameter("edgesid");
		}
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		this.initializeFeature();
		
		this.neighbor = neighbor;
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {	
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only valid for edges: "+
					di.getClass().getCanonicalName());
		}
		
		Edge e = (Edge) di;
		if(e.numNodes()!=2) {
			throw new UnsupportedTypeException("Only binary edges supported: "+
					e.numNodes());
		}
		
		// Get nodes
		Iterator<Node> nitr = e.getAllNodes();
		Node n1 = nitr.next();
		Node n2 = nitr.next();
		
		// Get neighbors for nodes
		Set<GraphItem> set1 = new HashSet<GraphItem>();
		Iterable<GraphItem> neighbors = neighbor.getNeighbors(n1);
		for(GraphItem gi:neighbors) {
			set1.add(gi);
		}
		
		Set<GraphItem> set2 = new HashSet<GraphItem>();
		neighbors = neighbor.getNeighbors(n2);
		for(GraphItem gi:neighbors) {
			set2.add(gi);
		}
		
		// Count the number of edges from the first set of neighbors to the second set
		double numedges = 0;
		for(GraphItem g1:set1) {
			for(GraphItem g2:set2) {
				if(edgesid==null) {
					if(g1.isAdjacent(g2)) {
						numedges++;
					}
				} else {
					if(g1.isAdjacent(g2, edgesid)) {
						numedges++;
					}
				}
			}
		}
		
		if(normalize) {
			// Normalize by the number of possible edges between the two sets
			if(set1.isEmpty() || set2.isEmpty()) {
				numedges = 0;
			} else {
				numedges = numedges/(set1.size()*set2.size());
			}
		}
		
		return new NumValue(numedges);
	}
}
