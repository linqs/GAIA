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
package linqs.gaia.model.util.plg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.util.plg.PLGUtils;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.BaseIterator;
import linqs.gaia.util.Dynamic;

/**
 * Generator potential links between a node and its neighbors,
 * as defined by the {@link Neighbor} class.
 * For example, we can consider potential links
 * between nodes with a path of at least length two between them.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodeschemaid-Schema ID of nodes incident on the links to predict.
 * Note:  We this generator currently only support links between nodes of the same schema.
 * <LI> neighborclass-{@link Neighbor} class,
 * instantiated using in {@link Dynamic#forConfigurableName},
 * to specify which nodes are considered neighbors.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class NeighborPLG extends BaseConfigurable implements PotentialLinkGenerator {
	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		String nodeschemaid = this.getStringParameter("nodeschemaid");
		String neighborclass = this.getStringParameter("neighborclass");
		
		SchemaType type = g.getSchemaType(edgeschemaid);
		if(type.equals(SchemaType.UNDIRECTED)) {
			return new UndirectedIterator(g, nodeschemaid, neighborclass, edgeschemaid, this);
		} else if(type.equals(SchemaType.DIRECTED)) {
			return new DirectedIterator(g, nodeschemaid, neighborclass, edgeschemaid, this);
		} else {
			throw new ConfigurationException("Unsupported edge type: "+edgeschemaid+" of type "+type);
		}
	}
	
	public static class UndirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private Iterator<Node> nitr = null;
		private String edgeschemaid;
		private Neighbor neighbor;
		private Set<Node> unprocneighbors = new HashSet<Node>();
		private Set<Node> processed = new HashSet<Node>();
		private Node currn;
		
		private UndirectedIterator(Graph g, String nodeschemaid, String neighborclass,
				String edgeschemaid, Configurable conf) {
			this.g = g;
			nitr = g.getNodes(nodeschemaid);
			this.edgeschemaid = edgeschemaid;
			
			neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, conf);
		}
		
		@Override
		public Edge getNext() {
			// Get the neighbors for the current node
			while(unprocneighbors.isEmpty()) {
				// Store previous node as completely processed
				if(currn!=null) {
					processed.add(currn);
				}
				
				if(!nitr.hasNext()) {
					return null;
				}
				
				// Get next node whose neighbors we want to process
				currn = nitr.next();
				
				// Go through the neighbors of the node
				Iterator<GraphItem> neighitr = neighbor.getNeighbors(currn).iterator();
				while(neighitr.hasNext()) {
					Node currneighbor = (Node) neighitr.next();
					
					// Since this is undirected,
					// we only need to add an edge once
					// Note: We can allow duplicate edges
					// by removing this check.
					// Don't add a duplicate edge
					if(!processed.contains(currneighbor)
						&& !currneighbor.isAdjacent(currn, edgeschemaid)) {
						unprocneighbors.add(currneighbor);
					}
				}
			}
			
			// Get the next of the unprocessed neighbors
			Node neighbor = unprocneighbors.iterator().next();
			unprocneighbors.remove(neighbor);
			
			GraphItemID id = GraphItemID.generateGraphItemID(g, edgeschemaid);
			
			return g.addUndirectedEdge(id, currn, neighbor);
		}
	}
	
	public static class DirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private Iterator<Node> nitr = null;
		private String edgeschemaid;
		private Neighbor neighbor;
		private Set<Node> unprocneighbors = new HashSet<Node>();
		private Node currn;
		
		private DirectedIterator(Graph g, String nodeschemaid, String neighborclass,
				String edgeschemaid, Configurable conf) {
			this.g = g;
			nitr = g.getNodes(nodeschemaid);
			this.edgeschemaid = edgeschemaid;
			
			neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, conf);
		}
		
		@Override
		public Edge getNext() {
			// Get the neighbors for the current node
			while(unprocneighbors.isEmpty()) {
				if(!nitr.hasNext()) {
					return null;
				}
				
				// Get next node whose neighbors we want to process
				currn = nitr.next();
				
				// Go through the neighbors of the node
				Iterator<GraphItem> neighitr = neighbor.getNeighbors(currn).iterator();
				while(neighitr.hasNext()) {
					Node currneighbor = (Node) neighitr.next();
					
					// Don't add a duplicate edge
					if(!currneighbor.isAdjacentSource(currn, edgeschemaid)) {
						unprocneighbors.add(currneighbor);
					}
				}
			}
			
			// Get the next of the unprocessed neighbors
			Node currneighbor = unprocneighbors.iterator().next();
			unprocneighbors.remove(currneighbor);
			
			// Make sure duplicate edges are inadvertently made
			if(currneighbor.isAdjacentSource(currn, edgeschemaid)) {
				throw new InvalidStateException("Attempting to add a duplicate edge");
			}
			
			GraphItemID id = GraphItemID.generateGraphItemID(g, edgeschemaid);
			return g.addDirectedEdge(id, currn, currneighbor);
		}
	}
	
	public void addAllLinks(Graph g, String edgeschemaid) {
		PLGUtils.addAllLinks(g, edgeschemaid, this.getLinksIteratively(g, edgeschemaid));
	}

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature,
			boolean setasnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature,
				this.getLinksIteratively(g, edgeschemaid), setasnotexist);
	}
}
