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
package linqs.gaia.graph.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.Numeric;

/**
 * Implements the Erdos-Renyi graph generation model.
 * This creates a graph of the specified number of nodes and edges
 * where edges are randomly added between the nodes.
 * <p>
 * Erdos, P.; Renyi, A. (1959). "On Random Graphs. I.". Publicationes Mathematicae 6: 290Ð297.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> graphclass-Full java class for the graph,
 * instantiated using {@link Dynamic#forConfigurableName}.
 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * <LI> graphobjid-Object id for graph.  Default is g1.
 * <LI> graphschemaid-Schema ID of a graph.  Default is pagraph.
 * <LI> nodeschemaid-Schema ID of the nodes.  Default is panode.
 * <LI> edgeschemaid-Schema ID of the edges.  Default is paedge.
 * <LI> isdirected-If yes, the generated edges are directed.
 * They edges are undirected otherwise.  Default is yes.
 * <LI> numnodes-Number of nodes to generate.  Default is 1000.
 * <LI> numedges-Number of edges to generate.  Default is 2000.
 * <LI> seed-Random generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class ErdosRenyi extends BaseConfigurable implements Generator {
	private final static String DEFAULT_GRAPH_CLASS = DataGraph.class.getCanonicalName();
	
	public Graph generateGraph() {
		return this.generateGraph(null);
	}

	public Graph generateGraph(String objid) {
		// Go through configurations
		String graphschemaid = "pagraph";
		if(this.hasParameter("graphschemaid")) {
			graphschemaid = this.getStringParameter("graphschemaid");
		}
		
		String graphobjid = "g1";
		if(objid!=null) {
			graphobjid = objid;
		} else if(this.hasParameter("graphobjid")) {
			graphobjid = this.getStringParameter("graphobjid");
		}
		
		String nodeschemaid = "ernode";
		if(this.hasParameter("nodeschemaid")) {
			nodeschemaid = this.getStringParameter("nodeschemaid");
		}
		
		String edgeschemaid = "eredge";
		if(this.hasParameter("edgeschemaid")) {
			edgeschemaid = this.getStringParameter("edgeschemaid");
		}
		
		boolean isdirected = true;
		if(this.hasParameter("isdirected")) {
			isdirected = this.getYesNoParameter("isdirected");
		}
		
		int numnodes = 1000;
		if(this.hasParameter("numnodes")) {
			numnodes = this.getIntegerParameter("numnodes");
		}
		
		int numedges = 2000;
		if(this.hasParameter("numedges")) {
			numnodes = this.getIntegerParameter("numedges");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		Random rand = new Random(seed);
		
		// Create Graph
		GraphID gid = new GraphID(graphschemaid,graphobjid);
		String graphclass = ErdosRenyi.DEFAULT_GRAPH_CLASS;
		if(this.hasParameter("graphclass")){
			graphclass = this.getStringParameter("graphclass");
		}

		Class<?>[] argsClass = new Class[]{GraphID.class};
		Object[] argValues = new Object[]{gid};
		Graph graph = (Graph) Dynamic.forName(Graph.class,
				graphclass,
				argsClass,
				argValues);
		
		graph.copyParameters(this);
		
		// Create node schema
		graph.addSchema(nodeschemaid, new Schema(SchemaType.NODE));
		
		// Create edge schema
		if(isdirected) {
			graph.addSchema(edgeschemaid, new Schema(SchemaType.DIRECTED));
		} else {
			graph.addSchema(edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Add nodes
		List<Node> allnodes = new ArrayList<Node>(numnodes);
		for(int i=0; i<numnodes; i++) {
			GraphItemID giid = new GraphItemID(gid, nodeschemaid, ""+(i));
			
			// Create nodes
			Node n = graph.addNode(giid);
			allnodes.add(n);
		}
		
		if(isdirected) {
			if((numnodes*(numnodes-1))<numedges) {
				throw new ConfigurationException("Impossible to create the specified number of edges: "+numedges
						+" with maximum possible, for given number of edges, "+(numnodes*(numnodes-1)));
			}
		} else {
			if(Numeric.combination(numnodes, 2)<numedges) {
				throw new ConfigurationException("Impossible to create the specified number of edges: "+numedges
						+" with maximum possible, for given number of edges, "+Numeric.combination(numnodes, 2));
			}
		}
		
		// Add edges
		while(graph.numEdges()<numedges) {
			Node source = allnodes.get(rand.nextInt(allnodes.size()));
			Node target = allnodes.get(rand.nextInt(allnodes.size()));
			
			if(source.equals(target)) {
				// Don't allow self links
				continue;
			} else if(isdirected && source.isAdjacentTarget(target, edgeschemaid)) {
				// Don't allow duplicated directed links
				continue;
			} else if(!isdirected && source.isAdjacent(target, edgeschemaid)) {
				// Don't allow duplicated undirected links
				continue;
			}
			
			// Add edges
			GraphItemID egiid = GraphItemID.generateGraphItemID(graph, edgeschemaid);
			if(isdirected) {
				graph.addDirectedEdge(egiid, source, target);
			} else {
				graph.addUndirectedEdge(egiid, source, target);
			}
		}
		
		return graph;
	}
}
