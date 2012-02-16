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
import linqs.gaia.feature.derived.neighbor.NeighborWithOmmission;
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
 * Given a binary edge, consider the two nodes adjacent to that edge,
 * return the specified set similarity of the neighbors of those two nodes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Neighbor class to use for the node.
 * Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> normalize-If yes, return the normalized similarity.
 * Default is to use the unnormalized similarity.
 * <LI> nssclass-Normalized set similarity class to mention.
 * Default is {@link linqs.gaia.similarity.set.CommonNeighbor}.
 * <LI> existfid-If specified, the edge existence, specified by this feature,
 * is set to non existing prior to computing the neighbors.
 * <LI> ignoreedge-If yes, assume the neighbor class is of type
 * {@link NeighborWithOmmission} and compute the neighborhood
 * with this edge ignored.
 * <LI> ignoresid-If ignoreedge is set to yes and this is specified,
 * aside from ignoring the edge, we also ignore another edge with
 * this as the schema id and with the same object id as this edge
 * (Note: useful if surrogate edges are created to improve feature computation).
 * </UL>
 * @author namatag
 *
 */
public class EdgeNodeNeighborSimilarity extends DerivedNum {
	private Neighbor neighbor = null;
	private boolean normalize = true;
	private NormalizedSetSimilarity nss = null;
	private String existfid = null;
	private boolean ignoreedge = false;
	private String ignoresid = null;
	
	protected void initialize() {
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
		
		if(this.hasParameter("existfid")) {
			existfid = this.getStringParameter("existfid");
		}
		
		ignoreedge = this.getYesNoParameter("ignoreedge","no");
		ignoresid = this.getStringParameter("ignoresid",null);
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

		// Handle case where feature is affected by the existence of this edge
		FeatureValue existval = null;
		if(existfid!=null) {
			existval = di.getFeatureValue(existfid);
			di.setFeatureValue(existfid, LinkPredictor.NOTEXISTVALUE);
		}
		
		// Get ignore set, if requested
		GraphItem ignoregi = null;
		if(ignoreedge) {
			if(ignoresid!=null) {
				GraphItemID gid = new GraphItemID(ignoresid, e.getID().getObjID());
				GraphItem gi = e.getGraph().getGraphItem(gid);
				if(gi==null) {
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
		Set<GraphItem> set1 = new HashSet<GraphItem>();
		Iterable<GraphItem> neighbors = ignoreedge ?
				((NeighborWithOmmission) neighbor).getNeighbors(n1,ignoregi)
				: neighbor.getNeighbors(n1);
		for(GraphItem gi:neighbors) {
			set1.add(gi);
		}

		Set<GraphItem> set2 = new HashSet<GraphItem>();
		neighbors = ignoreedge ?
				((NeighborWithOmmission) neighbor).getNeighbors(n2,ignoregi)
				: neighbor.getNeighbors(n2);
		for(GraphItem gi:neighbors) {
			set2.add(gi);
		}

		// Return previous state
		if(existfid!=null) {
			di.setFeatureValue(existfid, existval);
		}

		if(normalize) {
			return new NumValue(nss.getNormalizedSimilarity(set1, set2));
		} else {
			return new NumValue(nss.getSimilarity(set1, set2));
		}
	}
}
