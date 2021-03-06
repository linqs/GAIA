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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.derived.neighbor.NeighborWithOmmission;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.similarity.set.CommonNeighbor;
import linqs.gaia.util.Dynamic;

/**
 * Given a binary edge, consider the two nodes adjacent to that edge
 * and the co-references of these two nodes,
 * return the specified set similarity of the coreferences of their neighbors.
 * This is similar to {@link EdgeNodeNeighborSimilarity} but is optimized
 * for the case when there are co-reference relationships and the existence
 * of the edge, itself, may affect the value of the feature.
 * More specifically, the feature proceeds as follows:
 * <OL>
 * <LI> Let A and B to be the two nodes incident on the binary edge.
 * <LI> Let coref(A) define the set of nodes that are co-referent (by transitive closure)
 * to node A (including the node A itself).  Define coref(B) similarly.
 * <LI> Let corefneighbors(A) be the union of the set of neighbors of each node in coref(A).
 * Define corefneighbors(B) similarly.
 * <LI> Let ccn(A) be the union of the set of nodes that are co-referent (by transitive closure)
 * to each node in corefneighbors(A) (including the nodes themselves).
 * Define ccn(B) similarly.
 * <LI> Return the set similarity of sets ccn(A) and ccn(B).
 * </OL>
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> corefsid-Schema ID of edges which exists between co-referent nodes.
 * <LI> coreffid-Feature ID of feature which specifies whether the co-reference edge exists or not.
 * <LI> referstosid-Schema ID of directed edges between a reference node and the entity it refers to
 * (i.e., all co-referent nodes have a directed edge to the same node).
 * <LI> existfid-Feature ID of existence feature for the edges this feature is defined over.
 * This should be specified in cases where the value of this existence feature for this
 * node may incorrectly affect the value of this feature.  If specified, whenever the value
 * is set to existing, the feature value is first set to non-existing prior to computing the feature value.
 * The feature value is then restored after computing the feature.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Neighbor class to use for the node.
 * Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> nssclass-nssclass-Normalized set similarity class to mention.
 * Default is {@link linqs.gaia.similarity.set.CommonNeighbor}.
 * <LI> normalize-If "yes", use the normalized set similarity value, and if "no"
 * use the unnormalized value.  Default is yes.
 * <LI> cachecoref-If "yes", create a static cache of co-referent nodes
 * for each node where the cache is only cleared using {@link #clearCorefCache()}.
 * Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeNeighborCorefSimilarity extends DerivedNum {
	private String corefsid = null;
	private String coreffid = null;
	private String referstosid = null;
	private String existfid = null;
	
	private Neighbor neighbor = null;
	private boolean normalize = true;
	private NormalizedSetSimilarity nss = null;
	
	private boolean cachecoref = false;
	private static Map<String, Set<GraphItem>> crcache = new HashMap<String,Set<GraphItem>>();
	
	private boolean ignoreedge = false;
	private String ignoresid = null;
	
	public void cacheNeighbor() {
		this.initializeFeature();
		
		this.neighbor.setCache(true);
	}
	
	public void clearNeighborCache() {
		this.initializeFeature();
		
		this.neighbor.resetAll();
	}
	
	protected void initialize() {		
		corefsid = this.getStringParameter("corefsid");
		coreffid = this.getStringParameter("coreffid");
		referstosid = this.getStringParameter("referstosid");
		existfid = this.getStringParameter("existfid");
		
		String neighborclass = Adjacent.class.getCanonicalName();
		if(this.hasParameter("neighborclass")) {
			neighborclass = this.getStringParameter("neighborclass");
		}
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		if(this.hasParameter("normalize")) {
			normalize = this.getYesNoParameter("normalize");
		}
		
		String nssclass = CommonNeighbor.class.getCanonicalName();
		if(this.hasParameter("nssclass")) {
			nssclass = this.getStringParameter("nssclass");
		}
		nss = (NormalizedSetSimilarity)
			Dynamic.forConfigurableName(NormalizedSetSimilarity.class, nssclass, this);
		
		ignoreedge = this.getYesNoParameter("ignoreedge","no");
		ignoresid = this.getStringParameter("ignoresid",null);
		
		cachecoref = this.getYesNoParameter("cachecoref","no");
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
		
		// Handle case where feature is affected by the existence of this edge
		// In this case, set the edge as non-existing prior to computing the value of this feature.
		// Note:  The current value will be return at the end of the computation.
		CategValue existval = null;
		boolean isremoved = false;
		if(existfid!=null) {
			existval = (CategValue) di.getFeatureValue(existfid);
			if(existval.hasCateg(LinkPredictor.EXIST)) {
				di.setFeatureValue(existfid, LinkPredictor.NOTEXISTVALUE);
				isremoved = true;
			}
		}
		
		// Get ignore set, if requested
		GraphItem ignoregi = null;
		if(ignoreedge) {
			if(ignoresid!=null) {
				GraphItemID gid = new GraphItemID(ignoresid, e.getID().getObjID());
				GraphItem gi = e.getGraph().getGraphItem(gid);
				if(gi!=null) {
					ignoregi = gi;
				}
			} else {
				ignoregi = e;
			}
		}
		
		// Get nodes
		Iterator<Node> nitr = e.getAllNodes();
		Node n1 = nitr.next();
		Node n2 = nitr.next();
		
		// Get neighbors for nodes
		Set<GraphItem> set1 = this.getCACNeighbor(n1, e, isremoved, ignoregi);
		// If n1 and n2 are coreferent, they're going to return the same set of neighbors
		// so no need to recompute
		String crcachekey = e.getID().toString()+"-"+n1.getID().toString();
		Set<GraphItem> set2 = (crcache.containsKey(crcachekey) && crcache.get(crcachekey).contains(n2))
			? set1 : this.getCACNeighbor(n2, e, isremoved, ignoregi);	
		
		// Return previous state
		if(isremoved) {
			di.setFeatureValue(existfid, existval);
		}
		
		if(normalize) {
			return new NumValue(nss.getNormalizedSimilarity(set1, set2));
		} else {
			return new NumValue(nss.getSimilarity(set1, set2));
		}
	}
	
	private Set<GraphItem> getCACNeighbor(Node n, Edge e,
			boolean isremoved, GraphItem ignoregi) {
		// Get corefs
		Set<GraphItem> nodecorefs = this.getCorefences(n, e, isremoved, ignoregi);
		
		// Get neighbors of corefs
		Set<GraphItem> neighbors = new HashSet<GraphItem>();
		for(GraphItem corefn:nodecorefs) {
			Iterable<GraphItem> nitrbl = ignoreedge ?
					((NeighborWithOmmission) neighbor).getNeighbors(corefn, ignoregi)
					: this.neighbor.getNeighbors(corefn);
			for(GraphItem crneighbor:nitrbl) {
				neighbors.add(crneighbor);
			}
		}
		
		// Get corefs of neighbors
		Set<GraphItem> neighborcorefs = new HashSet<GraphItem>();
		for(GraphItem currn:neighbors) {
			// Given the transitive closure of corefs,
			// no need to recompute node neighbor if its already added
			if(neighborcorefs.contains(currn)) {
				continue;
			}
			
			neighborcorefs.addAll(this.getCorefences(n, e, isremoved, ignoregi));
		}
		
		return neighborcorefs;
	}
	
	private Set<GraphItem> getCorefences(Node n, Edge e,
			boolean isremoved, GraphItem ignoregi) {
		// Return cached value, if available
		if(cachecoref) {
			String crkey = e.getID().toString()+"-"+n.getID().toString();
			if(crcache.containsKey(crkey)) {
				return crcache.get(crkey);
			}
		}
		
		Set<GraphItem> coreferences = new HashSet<GraphItem>();
		if(isremoved) {
			// Compute using coref edges
			Set<Node> unexplored = new HashSet<Node>();
			unexplored.add(n);
			while(!unexplored.isEmpty()) {
				Node currn = unexplored.iterator().next();
				unexplored.remove(currn);
				
				Iterator<Edge> eitr = currn.getAllEdges(corefsid);
				while(eitr.hasNext()) {
					Edge curre = eitr.next();
					
					if(ignoreedge && ignoregi.equals(curre)) {
						continue;
					}
					
					CategValue existfid = (CategValue) curre.getFeatureValue(coreffid);
					if(existfid.hasCateg(LinkPredictor.NOTEXIST)) {
						continue;
					}
					
					Iterator<Node> nitr = curre.getAllNodes();
					while(nitr.hasNext()) {
						Node edgen = nitr.next();
						if(!coreferences.contains(edgen)) {
							unexplored.add(edgen);
						}
					}
				}
				
				coreferences.add(currn);
			}
		} else {
			// Compute using refers to edges
			// Note: Assumes a refers to edge has been added which
			// is still valid given that the existence of this edge at
			// the previous time step is non-existent
			// i.e., nodes computed this way are coreferent whether or not
			// his particular edge exists.
			Iterator<Edge> rtitr = n.getAllEdges(referstosid);
			Set<Node> entities = new HashSet<Node>();
			while(rtitr.hasNext()) {
				Edge rte = rtitr.next();
				
				if(ignoreedge && ignoregi.equals(rte)) {
					continue;
				}
				
				Iterator<Node> nitr = rte.getAllNodes();
				while(nitr.hasNext()) {
					Node rtn = nitr.next();
					if(!rtn.equals(n)) {
						entities.add(rtn);
					}
				}
			}
			
			for(Node entity:entities) {
				Iterator<Edge> eitr = entity.getAllEdges(referstosid);
				while(eitr.hasNext()) {
					Edge rte = eitr.next();
					
					if(ignoreedge && ignoregi.equals(rte)) {
						continue;
					}
					
					Iterator<Node> nitr = rte.getAllNodes();
					while(nitr.hasNext()) {
						Node rtn = nitr.next();
						if(!rtn.equals(entity) && !rtn.equals(n)) {
							coreferences.add(rtn);
						}
					}
				}
			}
		}
		
		// Add that a node is coreferent to itself
		coreferences.add(n);
		
		// Cache coreferences
		if(cachecoref) {
			for(GraphItem cr:coreferences) {
				crcache.put(e.getID().toString()+"-"+cr.getID().toString(), coreferences);
			}
		}
		
		return coreferences;
	}
	
	/*
	 * Clear the static Co-Reference cache used by this feature
	 */
	public static void clearCorefCache() {
		crcache.clear();
	}
}
