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
package linqs.gaia.graph.converter.jung;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.converter.Converter;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * This export exports the structure of a GAIA graph to the the graph
 * format used by Jung v2beta1.
 * Note: This exporter requires the Jung Library to be in the classpath
 * (i.e., jung-api-2.0-beta1.jar).
 * <p>
 * Note:
 * <UL>
 * <LI> Jung does not support attributes over the graphs.
 * <LI> This implementation of the transformer only supports binary edges.
 * Jung has the potential to support hyperedges.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> For exporting graph:
 * <UL> 
 * <LI> exportnodesids-Comma delimited list of node schema ids.  If specified,
 * only nodes with the given schema ID will be exported.
 * <LI> exportedgesids-Comma delimited list of edge schema ids.  If specified,
 * only edges with the given schema ID will be exported.
 * <LI> asundirected-If "yes", export all edges as undirected regardless
 * of actual edge type.  Default is "no".
 * </UL>
 * 
 * <LI> For importing graph:
 * <UL>
 * <LI> graphclass-Full java class for the graph,
 * instantiated using {@link Dynamic#forConfigurableName}.
 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * <LI> graphsid-Schema ID of graph
 * <LI> graphobjid-Object ID of graph
 * <LI> nodesid-Schema ID of JUNG Nodes
 * <LI> dirsid-Schema ID of JUNG Directed Edges
 * <LI> undirsid-Schema ID of JUNG Undirected Edges
 * </UL>
 * 
 * @author namatag
 *
 */
public class JungConverter extends BaseConfigurable
	implements Converter<edu.uci.ics.jung.graph.Graph<Object,Object>> {
	
	/**
	 * Returns a JUNG graph where the identifier for each node
	 * and edge is the string representation of the corresponding
	 * Graph Item's ID.
	 */
	public edu.uci.ics.jung.graph.Graph<Object,Object> exportGraph(Graph g) {
		boolean asundirected = this.getYesNoParameter("asundirected","no");
		
		Set<String> nodesids = null;
		if(this.hasParameter("exportnodesids")) {
			String exportnodesids[] = this.getStringParameter("exportnodesids").split(",");
			nodesids = new HashSet<String>(Arrays.asList(exportnodesids));
		}
		
		Set<String> edgesids = null;
		if(this.hasParameter("exportedgesids")) {
			String exportedgesids[] = this.getStringParameter("exportedgesids").split(",");
			edgesids = new HashSet<String>(Arrays.asList(exportedgesids));
		}
		
		// Create jung graph
		edu.uci.ics.jung.graph.Graph<Object, Object> jungg
			= new SparseMultigraph<Object, Object>();
		
		// Add nodes to jung graph
		Iterator<Node> nitr = g.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			if(nodesids!=null && !nodesids.contains(n.getSchemaID())) {
				continue;
			}
			
			jungg.addVertex(n.getID().toString());
		}
		
		// Add edges to jung graph
		Iterator<Edge> eitr = g.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			if(edgesids!=null && !edgesids.contains(e.getSchemaID())) {
				continue;
			}
			
			if(e.numNodes() != 2) {
				Log.WARN("Skipping edge.  Only binary edges supported: "
						+e+" has "+e.numNodes()+" nodes");
				continue;
			}
			
			if(asundirected || e instanceof UndirectedEdge) {
				nitr = e.getAllNodes();
				Node n1 = nitr.next();
				Node n2 = nitr.next();
				jungg.addEdge(e.getID().toString(), n1.getID().toString(),
						n2.getID().toString(), EdgeType.UNDIRECTED);
			} else if(e instanceof DirectedEdge) {
				Node source = ((DirectedEdge) e).getSourceNodes().next();
				Node target = ((DirectedEdge) e).getTargetNodes().next();
				jungg.addEdge(e.getID().toString(), source.getID().toString(),
						target.getID().toString(), EdgeType.DIRECTED);
			} else {
				throw new UnsupportedTypeException("Unsupported edge type: "
						+e.getClass().getCanonicalName());
			}
		}
		
		return jungg;
	}
	
	/**
	 * Import the JUNG graph
	 * <p>
	 * Optional Parameters:
	 * <UL>
	 * <LI> graphclass-Full java class for the graph,
	 * instantiated using {@link Dynamic#forConfigurableName}.
	 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
	 * <LI> graphsid-Schema ID of graph
	 * <LI> graphobjid-Object ID of graph
	 * <LI> nodesid-Schema ID of JUNG Nodes
	 * <LI> dirsid-Schema ID of JUNG Directed Edges
	 * <LI> undirsid-Schema ID of JUNG Undirected Edges
	 * </UL>
	 */
	public Graph importGraph(edu.uci.ics.jung.graph.Graph<Object, Object> g) {
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
		String undirsid = this.hasParameter("undirsid")
						? this.getStringParameter("undirsid"):"jungundirected";
						
		Graph ourg = (Graph) Dynamic.forConfigurableName(Graph.class,
				graphclass, argsClass, argValues);
		ourg.copyParameters(this);
		
		Collection<Object> vertices = g.getVertices();
		for(Object v:vertices) {
			ourg.addNode(new GraphItemID(nodesid,v.toString()));
		}
		
		Collection<Object> edges = g.getEdges();
		for(Object e:edges) {
			EdgeType type = g.getEdgeType(e);
			if(type.equals(EdgeType.UNDIRECTED)) {
				Pair<Object> curre = g.getEndpoints(e);
				Node n1 = ourg.getNode(new GraphItemID(undirsid,curre.getFirst().toString()));
				Node n2 = ourg.getNode(new GraphItemID(undirsid,curre.getSecond().toString()));
				ourg.addUndirectedEdge(new GraphItemID(undirsid,e.toString()),n1,n2);
			} else if(type.equals(EdgeType.UNDIRECTED)) {
				Node n1 = ourg.getNode(new GraphItemID(undirsid,g.getSource(e).toString()));
				Node n2 = ourg.getNode(new GraphItemID(undirsid,g.getDest(e).toString()));
				ourg.addDirectedEdge(new GraphItemID(dirsid,e.toString()),n1,n2);
			} else {
				throw new UnsupportedTypeException("Unsupported JUNG EdgeType: "+type);
			}
		}
		
		return ourg;
	}
}
