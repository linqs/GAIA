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
package linqs.gaia.graph.converter.adjmatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.Converter;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SimplePair;

/**
 * Export the graph to list of nodes and a two dimensional double adjacency matrix.
 * An edge exists from i to j if matrix[i][j] is non-zero.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Neighbor class to use.  If specified, add an edge
 * between a node i with all neighbor nodes j.
 * <LI> nodesids-Schema IDs of nodes to include in the adjacency matrix.
 * If not specified, all nodes are used.
 * <LI> edgesids-Schema IDs of edges to include in the adjacency matrix.
 * If not specified, all edges are used.  Only applicable when neighborclass is NOT specified.
 * <LI> asundirected-If true, always set as if the edge is undirected
 * (e.g., add edge i>j and j>i).  Only applicable when neighborclass is NOT specified.
 * <LI> weighted-If true, matrix[i][j] is the number of edges from i to j.
 * If false, matrix[i][j] is set to 1.  Default is fase
 * </UL>
 * 
 * @author namatag
 *
 */
public class AdjacencyMatrix extends BaseConfigurable implements Converter<double[][]>{

	public double[][] exportGraph(Graph g) {
		return this.exportGraphWithNodeMap(g).getSecond();
	}
	
	public SimplePair<Map<ID,Integer>, double[][]> exportGraphWithNodeMap(Graph g) {
		// Get neighbor class
		Neighbor neighbor = null;
		if(this.hasParameter("neighborclass")) {
			String neighborclass = this.getStringParameter("neighborclass");
			neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, neighborclass, this);
		}
		
		List<Node> nodes = new ArrayList<Node>();
		// Get node schema ids
		List<String> nsids = null;
		if(this.hasParameter("nodesids")) {
			String nodesids = this.getStringParameter("nodesids");
			nsids = Arrays.asList(nodesids.split(","));
		} else {
			nsids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.NODE));
		}
		
		// Add all nodes with that schema
		for(String nsid:nsids) {
			nodes.addAll(IteratorUtils.iterator2nodelist(g.getNodes(nsid)));
		}
		
		String edgesids = null;
		if(this.hasParameter("edgesids")) {
			edgesids = this.getStringParameter("edgesids");
		}
		
		boolean asundirected = this.hasYesNoParameter("asundirected", "yes");
		boolean weighted = this.hasYesNoParameter("weighted", "yes");
		
		// Get adjacency matrix
		double [][] adjmatrix = null;
		if(neighbor == null) {
			adjmatrix = AdjacencyMatrix.getAdjacencyMatrix(g, nodes, edgesids, weighted, asundirected);
		} else {
			adjmatrix = AdjacencyMatrix.getAdjacencyMatrix(g, nodes, neighbor, weighted);
		}
		
		// Compute node index
		Map<ID,Integer> nodeindex = new HashMap<ID,Integer>();
		for(int i=0; i<nodes.size(); i++) {
			nodeindex.put(nodes.get(i).getID(), i);
		}
		
		return new SimplePair<Map<ID,Integer>,double[][]>(nodeindex, adjmatrix);
	}
	
	/**
	 * Get adjacency matrix representation of a given graph
	 * where the adjacency matrix is a two dimensional array of doubles.
	 * 
	 * @param g Graph
	 * @param nodes List of nodes
	 * @param edgesids Comma delimited list of Schema IDs of edge to consider.  If null, use all edges.
	 * @param weighted If true, matrix[i][j] is the number of edges from i to j.
	 * If false, matrix[i][j] is set to 1.
	 * @param asundirected If true, always set as if the edge is undirected
	 * (e.g., add edge i>j and j>i)
	 * @return Two dimensional array representing the adjacency matrix of a graph
	 */
	public static double[][] getAdjacencyMatrix(Graph g, List<Node> nodes,
			String edgesids, boolean weighted, boolean asundirected) {
		double[][] adjacency = new double[nodes.size()][nodes.size()];
		Map<Node,Integer> index = new HashMap<Node,Integer>(nodes.size());
		int counter = 0;
		for(Node n:nodes) {
			index.put(n, counter);
			counter++;
		}
		
		List<String> esids = new ArrayList<String>();
		if(edgesids==null) {
			// Go through all edge types
			esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED)));
			esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED)));
		} else{
			// Go through only the specified edge type
			esids.addAll(Arrays.asList(edgesids.split(",")));
		}
		
		for(String curresid:esids) {
			// Verify directionality of edge
			SchemaType type = g.getSchemaType(curresid);
			boolean isdirected = false;
			if(asundirected) {
				isdirected = false;
			} else if(type.equals(SchemaType.DIRECTED)) {
				isdirected = true;
			} else if(type.equals(SchemaType.UNDIRECTED)) {
				isdirected = false;
			} else {
				throw new UnsupportedTypeException("Unsupported schema type: "+type);
			}
			
			// Go over all edges of the given type
			Iterator<Edge> eitr = g.getEdges(curresid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				// Support only binary edges or self loops
				if(e.numNodes()>2) {
					throw new UnsupportedTypeException("Only binary edges supported: "+e+" has "+e.numNodes());
				}
				
				if(isdirected) {
					// Handle directed edges
					DirectedEdge de = (DirectedEdge) e;
					Node n1 = de.getSourceNodes().next();
					Node n2 = de.getTargetNodes().next();
					
					int index1 = index.get(n1);
					int index2 = index.get(n2);
					
					if(index1==-1) {
						throw new InvalidStateException("Node not in nodes list: "+n1);
					}
					
					if(index2==-1) {
						throw new InvalidStateException("Node not in nodes list: "+n2);
					}
					
					if(weighted) {
						adjacency[index1][index2]++;
					} else {
						adjacency[index1][index2]=1;
					}
				} else {
					// Handle undirected edges
					Iterator<Node> nitr = e.getAllNodes();
					Node n1 = nitr.next();
					Node n2 = n1;
					if(nitr.hasNext()) {
						n2 = nitr.next();
					}
					
					int index1 = index.get(n1);
					int index2 = index.get(n2);
					
					if(index1==-1) {
						throw new InvalidStateException("Node not in nodes list: "+n1);
					}
					
					if(index2==-1) {
						throw new InvalidStateException("Node not in nodes list: "+n2);
					}
					
					if(weighted) {
						adjacency[index1][index2]++;
						adjacency[index2][index1]++;
					} else {
						adjacency[index1][index2]=1;
						adjacency[index2][index1]=1;
					}
				}
			}
		}
		
		return adjacency;
	}
	
	/**
	 * Get adjacency matrix representation of a given graph
	 * where the adjacency matrix is a two dimensional array of doubles.
	 * 
	 * @param g Graph
	 * @param nodes List of nodes
	 * @param neighbor Neighbor definition where an edge is counted
	 * between two nodes if they are neighbors
	 * @param weighted If true, matrix[i][j] is the number of edges from i to j.
	 * If false, matrix[i][j] is set to 1.
	 * @return Two dimensional array representing the adjacency matrix of a graph
	 */
	public static double[][] getAdjacencyMatrix(Graph g, List<Node> nodes,
			Neighbor neighbor, boolean weighted) {
		
		double[][] adjacency = new double[nodes.size()][nodes.size()];
		Map<Node,Integer> index = new HashMap<Node,Integer>(nodes.size());
		int counter = 0;
		for(Node n:nodes) {
			index.put(n, counter);
			counter++;
		}
		
		// Go over all nodes
		for(Node n1:nodes) {
			// Get neighbors of each node
			Iterable<GraphItem> neighbors = neighbor.getNeighbors(n1);
			for(GraphItem n2:neighbors) {
				int index1 = index.get(n1);
				int index2 = index.get(n2);
				
				if(index2==-1) {
					throw new InvalidStateException("Neighbor not in nodes list: "+n2);
				}
				
				// Add an edge between a node and its neighbors
				if(weighted) {
					adjacency[index1][index2]++;
				} else {
					adjacency[index1][index2]=1;
				}
			}
		}
			
		return adjacency;
	}
	
	/**
	 * Import the an adjacency matrix as a graph where
	 * we assume all the edges are directed.
	 * <p>
	 * Optional Parameters:
	 * <UL>
	 * <LI> graphclass-Full java class for the graph,
	 * instantiated using {@link Dynamic#forConfigurableName}.
	 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
	 * <LI> graphsid-Schema ID of graph
	 * <LI> graphobjid-Object ID of graph
	 * <LI> nodesid-Schema ID of Nodes
	 * <LI> dirsid-Schema ID of Directed Edges
	 * <LI> unweighted-If yes, add only one edge for each
	 * non-zero entry in the adjacency matrix.  By default,
	 * we add the floor of the entry.
	 * </UL>
	 */
	public Graph importGraph(double[][] g) {
		String graphclass = DataGraph.class.getCanonicalName();
		if(this.hasParameter("graphclass")){
			graphclass = this.getStringParameter("graphclass");
		}
		
		// Create Graph
		String schemaID = this.hasParameter("graphsid")
						? this.getStringParameter("graphsid"):"junggraph";
		String objID = this.hasParameter("graphobjid")
						? this.getStringParameter("graphobjid"):"jg1";
		GraphID id = new GraphID(schemaID, objID);
		Class<?>[] argsClass = new Class[]{GraphID.class};
		Object[] argValues = new Object[]{id};
		
		String nodesid = this.hasParameter("nodesid")
						? this.getStringParameter("nodesid"):"jungnode";
		String dirsid = this.hasParameter("dirsid")
						? this.getStringParameter("dirsid"):"jungdirected";
		boolean unweighted = this.hasYesNoParameter("unweighted","yes");
						
		Graph ourg = (Graph) Dynamic.forConfigurableName(Graph.class,
				graphclass, argsClass, argValues);
		ourg.copyParameters(this);
		
		int numnodes = g.length;
		for(int i=0; i<numnodes; i++) {
			ourg.addNode(new GraphItemID(nodesid,""+i));
		}
		
		for(int i=0; i<numnodes; i++) {
			for(int j=0; j<numnodes; j++) {
				double numedges = g[i][j];
				numedges = (unweighted && numedges > 0) ? 1:numedges; 
				
				for(int k=0; k<numedges; k++) {
					Node source = ourg.getNode(new GraphItemID(nodesid, ""+i));
					Node target = ourg.getNode(new GraphItemID(nodesid, ""+j));
					ourg.addDirectedEdge(new GraphItemID(dirsid,i+"-"+j+"-"+k),
							source, target);
				}
			}
		}
		
		return ourg;
	}
}
