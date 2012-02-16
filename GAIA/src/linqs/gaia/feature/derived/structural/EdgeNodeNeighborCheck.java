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

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedCateg;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.derived.neighbor.NeighborWithOmmission;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.UnmodifiableList;

/**
 * Valid only for binary edges, this feature looks at
 * the two nodes adjacent to the edge and checks
 * to see whether they are neighbors, given some neighbor criterion.
 * This feature can capture whether the nodes are incident
 * to each other via another edge type or if they are transitively co-referent.
 * 
 * Required Parameters:
 * <UL>
 * <LI>neighborclass-Neighbor class to use for the node.
 * </UL
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>existfid-If specified, the feature first changes the feature
 * specified by this ID to non-existent.  This allows the neighbor computation
 * to occur regardless of whether or not this edge is predicted to exist or not. 
 * <LI>ignoreedge-If "yes", ignore this edge when doing this computation
 * (e.g., assume edge does not exist).  Default is "no".
 * <LI>ignoresid-If specified, ignore edges with this schema id and the same
 * object as the current edge.  Default is not to do anything.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeNeighborCheck extends DerivedCateg {
	UnmodifiableList<String> categs = new UnmodifiableList<String>(Constants.FALSETRUE);
	private Neighbor neighbor = null;
	private String existfid = null;
	
	private static CategValue truevalue = new CategValue(Constants.TRUE, new double[]{0,1});
	private static CategValue falsevalue = new CategValue(Constants.FALSE, new double[]{1,0});
	
	private boolean ignoreedge = false;
	private String ignoresid = null;
	
	protected void initialize() {
		String neighborclass = this.getStringParameter("neighborclass");
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		if(this.hasParameter("existfid")) {
			existfid = this.getStringParameter("existfid");
		}
		
		ignoreedge = this.getYesNoParameter("ignoreedge","no");
		ignoresid = this.getStringParameter("ignoresid",null);
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		// Only supports edges
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only defined for edges: "+di);
		}
		
		// Only supports binary edges
		if(((Edge) di).numNodes()!=2) {
			throw new UnsupportedTypeException("Feature only defined for binary edges: "+di
					+" has #nodes="+((Edge) di).numNodes());
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
			Edge e = (Edge) di;
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
		
		Node n1 = null;
		Node n2 = null;
		if(di instanceof DirectedEdge) {
			n1 = ((DirectedEdge) di).getSourceNodes().next();
			n2 = ((DirectedEdge) di).getTargetNodes().next();
		} else if(di instanceof UndirectedEdge) {
			Iterator<Node> nitr = ((UndirectedEdge) di).getAllNodes();
			n1 = nitr.next();
			n2 = nitr.next();
		} else {
			throw new UnsupportedTypeException("Unsupported Edge Type: "+di);
		}
		
		FeatureValue returnval = falsevalue;
		Iterable<GraphItem> n1neighbors = ignoreedge ?
				((NeighborWithOmmission) neighbor).getNeighbors(n1,ignoregi)
				: neighbor.getNeighbors(n1);
		for(GraphItem gi:n1neighbors) {
			if(gi.equals(n2)) {
				returnval = truevalue;
			}
		}
		
		// Do opposite direction for undirected edges
		// to handle case where the neighbor definition may not be symmetric
		if(di instanceof UndirectedEdge && !returnval.equals(truevalue)) {
			Iterable<GraphItem> n2neighbors = ignoreedge ?
					((NeighborWithOmmission) neighbor).getNeighbors(n2,ignoregi)
					: neighbor.getNeighbors(n2);
			for(GraphItem gi:n2neighbors) {
				if(gi.equals(n1)) {
					returnval = truevalue;
				}
			}
		}
		
		// Return previous state
		if(existfid!=null) {
			di.setFeatureValue(existfid, existval);
		}
		
		return returnval;
	}

	@Override
	public UnmodifiableList<String> getAllCategories() {
		return categs;
	}
}
