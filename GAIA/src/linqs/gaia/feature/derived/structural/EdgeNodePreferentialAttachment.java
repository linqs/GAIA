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
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.derived.neighbor.NeighborWithOmmission;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.UnmodifiableList;

/**
 * Compute the preferential attachment value for this edge
 * by returning the product of the degree of the edges,
 * as defined by the specified neighbor class.
 * The implementation is based on the score description in:
 * <p>
 * Liben-Nowell, D. & Kleinberg, J.<br>
 * The link prediction problem for social networks<br>
 * International Conference on Information and Knowledge Management, 2003
 * <p>
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
public class EdgeNodePreferentialAttachment extends DerivedNum {
	UnmodifiableList<String> categs = new UnmodifiableList<String>(Constants.FALSETRUE);
	private Neighbor neighbor = null;
	private String existfid = null;
	
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
		
		Edge e = (Edge) di;
		
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
				if(gi!=null) {
					ignoregi = gi;
				}
			} else {
				ignoregi = e;
			}
		}
		
		double product = 1;
		Iterator<Node> nitr = e.getAllNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			product *= ignoreedge ?
					((NeighborWithOmmission) neighbor).numNeighbors(n,ignoregi)
					: neighbor.numNeighbors(n);
		}
		
		// Return previous state
		if(existfid!=null) {
			di.setFeatureValue(existfid, existval);
		}
		
		return new NumValue(product);
	}
}
