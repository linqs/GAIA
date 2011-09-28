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
package linqs.gaia.feature.derived.aggregate;

import java.util.ArrayList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.BaseDerived;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NodeSimilarity;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.MinMax;

/**
 * Return, as feature, the similarity of neighboring nodes
 * for the given decorable item.  For example, if the decorable
 * item is an edge where the neighbors are defined as the incident nodes,
 * this will compute the similarity for all unordered pairs of nodes
 * and return the min, max, or mean similarity (as specified in the parameters)
 * of pairs of these nodes.
 * <p>
 * Note: This feature is limited to graph items and the set of neighbors must
 * consists only of nodes.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodesimclass-Node similarity measure to use to calculate similarity.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-The neighbor object used to compute the neighbors
 * of the nodes.  Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> aggtype-Parameter can be set to min, max or mean when a delimiter is specified.
 * When a delimiter is defined, either the min, max, or mean
 * values for all pairwise comparisons between the the delimited
 * list of string will be used.  Default is max.
 * <LI> normalize-If yes, use the normalized value.  This assumes
 * that the nodesimclass specified is also a normalized node similarity.
 * Default is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NeighboringNodeSimilarity extends BaseDerived implements 
	DerivedFeature, NumFeature {
	
	private String neighborclass = Adjacent.class.getCanonicalName();
	private Neighbor neighbor = null;
	private NodeSimilarity nodesim;
	private String aggtype = "max";
	private boolean normalize = false;
	
	/**
	 * Initialize information required by the feature
	 */
	protected void initialize() {
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}
		
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
		// Get node similarity measure
		String nodesimclass = this.getStringParameter("nodesimclass");
		nodesim = (NodeSimilarity) 
			Dynamic.forConfigurableName(NodeSimilarity.class,
			nodesimclass, this);
		
		// Specify how to aggregate when there are more than two nodes
		if(this.hasParameter("aggtype")) {
			this.aggtype = this.getCaseParameter("aggtype", new String[]{"min","max","mean"});
		}
		
		// Specify whether or not to normalize
		if(this.hasParameter("normalize")) {
			this.normalize = this.getYesNoParameter("normalize");
			
			// Verify that we are able to normalize
			if(normalize && !(nodesim instanceof NormalizedNodeSimilarity)) {
				throw new ConfigurationException("Node similarity class is not a normalized measure: "+
						nodesim.getClass().getCanonicalName());
			}
		}
	}

	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(neighbor == null) {
			this.initialize();
		}
		
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items");
		}
		
		GraphItem gi = (GraphItem) di;
		Iterable<GraphItem> neighbors = neighbor.getNeighbors(gi);
		
		// Verify valid nodes
		List<Node> nodeneighbors = new ArrayList<Node>();
		for(GraphItem ngi:neighbors) {
			if(!(ngi instanceof Node)) {
				throw new UnsupportedTypeException("Feature only supports node neighbors: "+ngi);
			}
			
			nodeneighbors.add((Node) ngi);
		}
		
		// Get the similarity for all pairs of nodes
		MinMax mm = new MinMax();
		for(int i=0; i<nodeneighbors.size(); i++) {
			for(int j=i+1; j<nodeneighbors.size(); j++) {
				if(normalize) {
					mm.addValue(((NormalizedNodeSimilarity) nodesim).getNormalizedSimilarity(
							nodeneighbors.get(i),
							nodeneighbors.get(j)));
				} else {
					mm.addValue(nodesim.getSimilarity(
						nodeneighbors.get(i),
						nodeneighbors.get(j)));
				}
			}
		}
		
		// Aggregate the similarities somehow
		if(this.aggtype.equals("max")) {
			return new NumValue(mm.getMax());
		} else if(this.aggtype.equals("min")) {
			return new NumValue(mm.getMin());
		} else if(this.aggtype.equals("mean")) {
			return new NumValue(mm.getMean());
		} else {
			throw new ConfigurationException("Invalid aggtype: "+this.aggtype);
		}
	}
}
