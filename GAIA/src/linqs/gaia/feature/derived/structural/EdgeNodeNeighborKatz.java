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

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.adjmatrix.AdjacencyMatrix;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.SimplePair;

/**
 * Given a binary edge, consider the two nodes adjacent to that edge,
 * return the min, max, or mean Katz score between the neighbors
 * of one node and the neighbors of the other.  This feature captures
 * how close the neighbor of the nodes incident the specified edge are
 * to each other.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Neighbor class to use for the node.
 * Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> adjmatrixclass-Adjacency Matrix exporter to use to create the the adjaceny matrix.
 * By default, use an Adjacency Matrix exporter with the default settings.
 * <LI> beta-Beta parameter to use in Katz score.  If null,
 * beta is set to 1.5 times the largest eigenvalue of the adjacency matrix.
 * Note that 1/beta must be larger than the largest eigenvalue of the adjacency matrix.
 * <LI> numvaluetype-If specified, instead of returning the most common
 * numeric value for numeric feature, return an aggregate on the specified type. Option are:
 *   <UL>
 *   <LI> min-Mininum value of numeric feature
 *   <LI> max-Maximum value of numeric feature
 *   <LI> mean-Mean value of numeric feature
 *   </UL>
 * </UL>
 * @author namatag
 *
 */
public class EdgeNodeNeighborKatz extends DerivedNum implements GraphDependent {
	private Graph g = null;
	private Neighbor neighbor = null;
	private boolean initialize = true;
	private Map<ID,Integer> nodeindex = new HashMap<ID,Integer>();
	private double[][] katzmatrix = null;
	private String numvaluetype = null;
	
	private void initialize() {
		initialize = false;
		
		String neighborclass = Adjacent.class.getCanonicalName();
		if(this.hasParameter("neighborclass")) {
			neighborclass = this.getStringParameter("neighborclass");
		}
		neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		
		numvaluetype = "mean";
		if(this.hasParameter("numvaluetype")) {
			numvaluetype = this.getCaseParameter("numvaluetype", new String[]{"min","max","mean"});
		}
		
		AdjacencyMatrix exporter = null;
		String adjmatrixclass = AdjacencyMatrix.class.getCanonicalName();
		if(this.hasParameter("adjmatrixclass")) {
			adjmatrixclass = this.getStringParameter("adjmatrixclass");
		}
		
		Double beta = null;
		if(this.hasParameter("beta")) {
			beta = this.getDoubleParameter("beta");
		}
		
		exporter = (AdjacencyMatrix) Dynamic.forConfigurableName(AdjacencyMatrix.class, adjmatrixclass, this);
		
		SimplePair<Map<ID,Integer>, double[][]> pair =  exporter.exportGraphWithNodeMap(g);
		this.nodeindex = pair.getFirst();
		
		double[][] adjacencymatrix = pair.getSecond();
		katzmatrix = EdgeNodeKatz.computeKatzMatrix(adjacencymatrix, beta);
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		this.initialize();
		this.neighbor = neighbor;
	}
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(initialize) {
			this.initialize();
		}
		
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
		MinMax mm = new MinMax();
		for(GraphItem g1:set1) {
			int index1 = nodeindex.get(g1.getID());
			for(GraphItem g2:set2) {
				int index2 = nodeindex.get(g2.getID());
				double currkatz = this.katzmatrix[index1][index2];
				mm.addValue(currkatz);
			}
		}
		
		Double katz = 0.0;
		if(numvaluetype.equals("mean")) {
			katz = mm.getMean();
		} else if(numvaluetype.equals("min")) {
			katz = mm.getMin();
		} else if(numvaluetype.equals("max")) {
			katz = mm.getMax();
		} else {
			throw new ConfigurationException("Unsupported numvaluetype: "+numvaluetype);
		}
		
		// Handle special case
		if(katz==null || katz.isNaN() || katz.isInfinite()) {
			katz = 0.0;
		}
		
		return new NumValue(katz);
	}
	
	public void setGraph(Graph g) {
		this.g = g;
	}
}
