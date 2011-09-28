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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.similarity.NodeSimilarity;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.util.BaseIterator;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.KeyedSet;

/**
 * Implementation of the Canopy approach in:
 * <p>
 * Efficient Clustering of High-Dimensional Data Sets with Application to Reference Matching,"
 * Andrew McCallum, Kamal Nigam, and Lyle Ungar.
 * Proceedings of the ACM SIGKDD International Conference On Knowledge Discovery And Data, 2000.
 * <p>
 * 
 * Required Parameters:
 * <UL>
 * <LI>nodesid-Schema ID of nodes to add edges between
 * <LI>threshold1-Loose threshold value to use (t1 in the paper).
 * All nodes similar above this value are added to a given canopy.
 * <LI>threshold2-Tight threshold value to use (t2 in the paper).
 * All nodes similar above this value are removed from the node list
 * and assumed to only belong to a specific canopy.
 * <LI>nodesimclass-Normalized node similarity measure to use.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>summaryfid-This contains a string value which summarizes
 * sparse features as a comma delimited list of feature ids.  When specified,
 * only nodes which match at least one feature id in common are compared.
 * <LI>usenormalized-If "yes", used the normalized node similarity.  Otherwise,
 * just the node similarity.  If "yes", an exception is thrown if the Node similarity
 * measure used is not a normalized node similarity measure.  Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class Canopy extends BaseConfigurable implements PotentialLinkGenerator, GraphDependent {
	private boolean initialize = true;
	private double threshold1 = 0;
	private double threshold2 = Double.POSITIVE_INFINITY;
	private NodeSimilarity nsim;
	private String nodesid = null;
	private String summaryfid = null;
	private boolean usenormalized = false;
	
	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		SchemaType type = g.getSchemaType(edgeschemaid);
		if(!type.equals(SchemaType.UNDIRECTED)) {
			throw new UnsupportedTypeException("Only undirected edges are supported: "+type);
		}
		
		return new UndirectedIterator(g, edgeschemaid);
	}
	
	private class UndirectedIterator extends BaseIterator<Edge> {
		Set<Node> allnodes = null;
		List<List<Node>> canopies = null;
		List<Node> currcanopy = null;
		String edgeschemaid = null;
		Graph g = null;
		Node currnode = null;
		int index = 0;
		
		public UndirectedIterator(Graph g, String edgeschemaid) {
			this.edgeschemaid = edgeschemaid;
			this.g = g;
			
			KeyedSet<String,Node> word2nodes = new KeyedSet<String,Node>();
			Iterator<Node> nitr = g.getNodes(nodesid);
			allnodes = new HashSet<Node>();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				allnodes.add(n);
				
				// Create inverted index and only compare those
				// which have at least one feature in common
				if(summaryfid!=null) {
					String[] words = n.getFeatureValue(summaryfid).getStringValue().split(",");
					for(String w:words) {
						word2nodes.addItem(w, n);
					}
				}
			}
			
			// Compute all the canopies
			canopies = new ArrayList<List<Node>>();
			
			Set<Node> removed = new HashSet<Node>();
			while(!allnodes.isEmpty()) {
				// Select canopy node
				Node currcanopynode = allnodes.iterator().next();
				allnodes.remove(currcanopynode);
				
				// Get nodes to consider relative to current canopy node
				Set<Node> compnodes = null;
				if(summaryfid!=null) {
					compnodes = new HashSet<Node>();
					String[] words = currcanopynode.getFeatureValue(summaryfid).getStringValue().split(",");
					for(String w:words) {
						compnodes.addAll(word2nodes.getCollection(w));
					}
				} else {
					compnodes = new HashSet<Node>(allnodes);
				}
				
				// Clear compnodes
				compnodes.removeAll(removed);
				
				// Initialize canopy
				List<Node> canopy = new ArrayList<Node>();
				canopy.add(currcanopynode);
				
				// Go through the appropriate nodes and add if needed
				for(Node indexnode:compnodes) {
					double sim = usenormalized ?
							((NormalizedNodeSimilarity) nsim).getNormalizedSimilarity(currcanopynode, indexnode)
							: nsim.getSimilarity(currcanopynode, indexnode);
					
					if(sim>threshold1) {
						canopy.add(indexnode);
						
						if(sim>threshold2) {
							allnodes.remove(index);
							removed.add(indexnode);
						}
					}
				}
				
				// Add current canopy
				canopies.add(canopy);
			}
		}
		
		@Override
		public UndirectedEdge getNext() {
			while(true) {
				if(currcanopy==null) {
					if(canopies.isEmpty()) {
						return null;
					} else {
						currcanopy = canopies.remove(0);
						if(currcanopy.isEmpty()) {
							currcanopy = null;
							continue;
						}
						
						currnode = currcanopy.remove(0);
						index = 0;
					}
				}
				
				if(index>=currcanopy.size()) {
					currcanopy = null;
					continue;
				}
				
				Node indexnode = currcanopy.get(index);
				if(!currnode.equals(indexnode) && !currnode.isAdjacent(indexnode, edgeschemaid)) {
					return g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid),
							currnode, indexnode);
				} else {
					index++;
				}
			}
		}
	}
	
	private void initialize() {
		this.initialize = false;
		
		nodesid = this.getStringParameter("nodesid");
		threshold1 = this.getDoubleParameter("threshold1");
		if(this.hasParameter("threshold2")) {
			threshold2 = this.getDoubleParameter("threshold2");
		}
		
		if(this.hasParameter("summaryfid")) {
			summaryfid = this.getStringParameter("summaryfid");
		}
		
		nsim = (NodeSimilarity) Dynamic.forConfigurableName(NodeSimilarity.class,
				this.getStringParameter("nodesimclass"),
				this);
		
		usenormalized = this.getYesNoParameter("usenormalized", "no");
	}

	public void addAllLinks(Graph g, String edgeschemaid) {
		PLGUtils.addAllLinks(g, edgeschemaid, this.getLinksIteratively(g, edgeschemaid));
	}

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature,
			boolean setasnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature, this.getLinksIteratively(g, edgeschemaid), setasnotexist);
	}

	public void setGraph(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		if(nsim instanceof GraphDependent) {
			((GraphDependent) nsim).setGraph(g);
		}
	}
}
