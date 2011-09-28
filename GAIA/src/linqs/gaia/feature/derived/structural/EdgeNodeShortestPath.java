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
import java.util.Iterator;
import java.util.Map;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.adjmatrix.AdjacencyMatrix;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimplePair;

/**
 * Get the length of the shortest path of a given pair of nodes.
 * To compute it, we use the Floyd–Warshall algorithm over the
 * adjacency matrix.
 * <p>
 * Note: All shortest paths lengths are computed during the first call to this feature.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> adjmatrixclass-Adjacency Matrix exporter to use to create the the adjaceny matrix.
 * By default, use an Adjacency Matrix exporter with the default settings.
 * <LI> beta-Beta parameter to use in Katz score.  If null,
 * beta is set to 1.5 times the largest eigenvalue of the adjacency matrix.
 * Note that 1/beta must be larger than the largest eigenvalue of the adjacency matrix.
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeShortestPath extends DerivedNum implements GraphDependent {
	private Graph g = null;
	private boolean initialize = true;
	private Map<ID,Integer> nodeindex = new HashMap<ID,Integer>();
	private double[][] shortpathlengths = null;
	
	private void initialize() {
		initialize = false;
		
		AdjacencyMatrix exporter = null;
		String adjmatrixclass = AdjacencyMatrix.class.getCanonicalName();
		if(this.hasParameter(adjmatrixclass)) {
			adjmatrixclass = this.getStringParameter("adjmatrixclass");
		}
		
		exporter = (AdjacencyMatrix) Dynamic.forConfigurableName(AdjacencyMatrix.class, adjmatrixclass, this);
		
		SimplePair<Map<ID,Integer>, double[][]> pair =  exporter.exportGraphWithNodeMap(g);
		this.nodeindex = pair.getFirst();
		double[][] adjacencymatrix = pair.getSecond();
		shortpathlengths = EdgeNodeShortestPath.computeShortestPathLengths(adjacencymatrix);
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
		
		return new NumValue(this.getShortestPathLength(n1, n2));
	}
	
	private double getShortestPathLength(Node n1, Node n2) {
		int index1 = nodeindex.get(n1.getID());
		int index2 = nodeindex.get(n2.getID());
		
		if(index1==-1) {
			throw new InvalidStateException("Node not in nodes list: "+n1);
		}
		
		if(index2==-1) {
			throw new InvalidStateException("Node not in nodes list: "+n2);
		}
		
		return this.shortpathlengths[index1][index2];
	}
	
	/**
	 * Get the shortest path matrix given the adjacency matrix.
	 * The returned matrix has the shortest path length of a node i and j as matrix[i][j].
	 * We compute the shortest path using the Floyd–Warshall algorithm.
	 * 
	 * @param adjacencymatrix Adjacency matrix of graph
	 * @return Matrix of Shortest Paths
	 */
	public static double[][] computeShortestPathLengths(double[][] adjacencymatrix) {
		double[][] path = EdgeNodeShortestPath.setNoEdgeToInfinity(adjacencymatrix);
		
		int size = adjacencymatrix.length;
		for(int k=0; k<size; k++) {
			for(int i=0; i<size; i++) {
				for(int j=0; j<size; j++) {
					double p1 = path[i][j];
					double p2 = path[i][k]+path[k][j];
					path[i][j] = p1<p2 ? p1:p2;
				}
			}
		}
		
		return path;
	}
	
	/**
	 * Set entries in the adjacency matrix where the value is 0 to positive infinity.
	 * 
	 * @param adjacencymatrix Adjacency matrix
	 * @return Modified adjacency matrix
	 */
	public static double[][] setNoEdgeToInfinity(double[][] adjacencymatrix) {
		for(int i=0; i<adjacencymatrix.length; i++) {
			for(int j=0; j<adjacencymatrix.length; j++) {
				if(adjacencymatrix[i][j]==0) {
					adjacencymatrix[i][j] = Double.POSITIVE_INFINITY;
				}
			}
		}
		
		return adjacencymatrix;
	}

	public void setGraph(Graph g) {
		this.g = g;
	}
}
