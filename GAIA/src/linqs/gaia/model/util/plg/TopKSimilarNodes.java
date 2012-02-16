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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.similarity.NodeSimilarity;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.util.BaseIterator;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.KeyedSet;
import linqs.gaia.util.TopK;

/**
 * Create potential links between a node and its K most similar nodes,
 * as defined by the specified node similarity measure.
 * <p>
 * Note: Assumes similarity is symmetric.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodesimclass-Normalized node similarity ({@link NormalizedNodeSimilarity}) measure to use,
 * instantiated using in {@link Dynamic#forConfigurableName}
 * <LI> nodeschemaid-Schema ID of nodes to add edges between
 * <LI> kvalue-K value to use
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> usenormalized-If "yes", use the normalized node similarity value.
 * An exception is throw if the specified node similarity measure to use is not of type
 * {@link NormalizedNodeSimilarity}.  Default is "no".
 * <LI> cachesimilarity-If "yes", cache the similarity values between applications
 * of this potential link generator.  Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class TopKSimilarNodes extends BaseConfigurable implements PotentialLinkGenerator {
	private boolean initialize = true;
	private String nodeschemaid;
	private NodeSimilarity nsim;
	private boolean usenormalized = false;
	private boolean cachesimilarity = false;
	private int kvalue = 0;
	protected Map<String,Double> key2sim = new HashMap<String,Double>();
	private String summaryfid = null;
	
	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		if(initialize) {
			this.initialize();
		}
		
		SchemaType type = g.getSchemaType(edgeschemaid);
		
		// Return existing edges matching some criterion
		if(type.equals(SchemaType.DIRECTED)) {
			return new DirectedIterator(g, edgeschemaid);
		} else if(type.equals(SchemaType.UNDIRECTED)) {
			return new UndirectedIterator(g, edgeschemaid);
		} else {
			throw new UnsupportedTypeException("Unsupported edge type: "+edgeschemaid+" is "+type);
		}
	}
	
	protected double getSimilarity(Node n1, Node n2) {
		double sim = 0;
		String key = null;
		if(cachesimilarity) {
			String id1 = n1.getID().getObjID();
			String id2 = n2.getID().getObjID();
			if(id2.compareTo(id1)>0) {
				String tmp = id1;
				id1 = id2;
				id2 = tmp;
			}
			
			key = id1+"."+id2;
		}
		
		if(cachesimilarity && key2sim.containsKey(key)) {
			sim = key2sim.get(key);
		} else {
			sim = usenormalized ? 
					((NormalizedNodeSimilarity) nsim).getNormalizedSimilarity(n1, n2) 
					: nsim.getSimilarity(n1, n2);
					
			if(cachesimilarity) {
				key2sim.put(key, sim);
			}
		}
		
		return sim;
	}
	
	private class DirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private String edgeschemaid;
		private Node currnode = null;
		private Iterator<Node> nitr1 = null;
		private LinkedList<Node> currtopknodes = new LinkedList<Node>();
		private KeyedSet<String,Node> word2nodes = null;
		
		public DirectedIterator(Graph g, String edgeschemaid) {
			this.g = g;
			this.edgeschemaid = edgeschemaid;
			
			nitr1 = this.g.getNodes(nodeschemaid);
			
			// Create inverted index and only compare those
			// which have at least one feature in common
			if(summaryfid!=null) {
				Iterator<Node> nitr = this.g.getNodes(nodeschemaid);
				while(nitr.hasNext()) {
					Node n = nitr.next();
					String[] words = n.getFeatureValue(summaryfid).getStringValue().split(",");
					for(String w:words) {
						word2nodes.addItem(w, n);
					}
				}
			}
		}
		
		@Override
		public Edge getNext() {
			while(true) {
				if(currtopknodes.isEmpty()) {
					if(nitr1.hasNext()) {
						currnode = nitr1.next();
						
						// Get topk most similar for current node
						TopK<Node> topksim = new TopK<Node>(kvalue);
						Iterator<Node> nitr = null;
						if(summaryfid==null) {
							nitr = this.g.getNodes(nodeschemaid);
						} else {
							// User inverted index to identify a smaller set of nodes to compare against
							Set<Node> compnodes = new HashSet<Node>();
							String[] words = currnode.getFeatureValue(summaryfid).getStringValue().split(",");
							for(String w:words) {
								compnodes.addAll(word2nodes.getCollection(w));
							}
							
							nitr = compnodes.iterator();
						}
						
						while(nitr.hasNext()) {
							Node n2 = nitr.next();
							if(currnode.equals(n2)) {
								continue;
							}
							
							double sim = getSimilarity(currnode, n2);
							topksim.add(sim, n2);
						}
						
						currtopknodes.addAll(topksim.getTopK());
					} else {
						return null;
					}
				}
				
				Node n2 = currtopknodes.poll();
				if(!currnode.isAdjacentTarget(n2,edgeschemaid)) {
					Edge e = g.addDirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid),
							currnode, n2);
					
					return e;
				}
			}
		}
	}
	
	private class UndirectedIterator extends BaseIterator<Edge> {
		private Graph g;
		private String edgeschemaid;
		private Node currnode = null;
		private Iterator<Node> nitr1 = null;
		private LinkedList<Node> currtopknodes = new LinkedList<Node>();
		
		public UndirectedIterator(Graph g, String edgeschemaid) {
			this.g = g;
			this.edgeschemaid = edgeschemaid;
			
			nitr1 = this.g.getNodes(nodeschemaid);
		}
		
		@Override
		public Edge getNext() {
			while(true) {
				if(currtopknodes.isEmpty()) {
					if(nitr1.hasNext()) {
						currnode = nitr1.next();
						
						// Get topk most similar for current node
						TopK<Node> topksim = new TopK<Node>(kvalue);
						Iterator<Node> nitr = this.g.getNodes(nodeschemaid);
						while(nitr.hasNext()) {
							Node n2 = nitr.next();
							if(currnode.equals(n2)) {
								continue;
							}
							
							double sim = getSimilarity(currnode, n2);
							topksim.add(sim, n2);
						}
						
						currtopknodes.addAll(topksim.getTopK());
					} else {
						return null;
					}
				}
				
				Node n2 = currtopknodes.poll();
				if(!currnode.isAdjacent(n2,edgeschemaid)) {
					Edge e = g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, edgeschemaid),
							currnode, n2);
					
					return e;
				}
			}
		}
	}
	
	private void initialize() {
		initialize = false;
		
		nsim = (NodeSimilarity) Dynamic.forConfigurableName(NodeSimilarity.class,
				this.getStringParameter("nodesimclass"),
				this);
		
		usenormalized = this.getYesNoParameter("usenormalized", "no");
		cachesimilarity = this.getYesNoParameter("cachesimilarity","no");
		
		nodeschemaid = this.getStringParameter("nodeschemaid");
		kvalue = this.getIntegerParameter("kvalue");
		
		summaryfid = this.getStringParameter("summaryfid",null);
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
