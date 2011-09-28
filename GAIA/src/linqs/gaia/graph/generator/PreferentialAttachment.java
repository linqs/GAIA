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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.Dynamic;

/**
 * Graph generator using preferential attachment.
 * <p>
 * Barab‡si, Albert-L‡szl— and Albert, RŽka.
 * "Emergence of scaling in random networks".
 * Science, 286:509-512, October 15, 1999.
 * <p>
 * 
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
 * <LI> numinitial-Number of nodes to initially create.  Default is 10.
 * <LI> mindegree-Minimum degree to set per node.  Default is 5.
 * <LI> seed-Random generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class PreferentialAttachment extends BaseConfigurable implements Generator {
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
		
		String nodeschemaid = "panode";
		if(this.hasParameter("nodeschemaid")) {
			nodeschemaid = this.getStringParameter("nodeschemaid");
		}
		
		String edgeschemaid = "paedge";
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
		
		int numinitial = 10;
		if(this.hasParameter("numinitial")) {
			numinitial = this.getIntegerParameter("numinitial");
		}
		
		int mindegree = 5;
		if(this.hasParameter("mindegree")) {
			mindegree = this.getIntegerParameter("mindegree");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		Random rand = new Random(seed);
		
		// Create counters for creating object id
		int nodekeyid = 0;
		int edgekeyid = 0;
		
		// Create Graph
		GraphID gid = new GraphID(graphschemaid,graphobjid);
		String graphclass = PreferentialAttachment.DEFAULT_GRAPH_CLASS;
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
		
		// This list will contain an instance for each node
		// proportional to the degree of that node.
		List<Node> allnodes = new ArrayList<Node>(numnodes + (numnodes*mindegree));
		
		// Initialize some small number of nodes
		for(int i=0; i<numinitial; i++) {
			GraphItemID giid = new GraphItemID(gid, nodeschemaid, ""+(nodekeyid++));
			
			// Create node
			Node n = graph.addNode(giid);
			
			// Guarantee that the initial set of nodes has at least degree 1
			if(!allnodes.isEmpty()) {
				// Add edge
				Node target = allnodes.get(rand.nextInt(allnodes.size()));
				GraphItemID egiid = new GraphItemID((GraphID) graph.getID(),
						edgeschemaid,""+(edgekeyid++));
				if(isdirected) {
					graph.addDirectedEdge(egiid, n, target);
				} else {
					graph.addUndirectedEdge(egiid, n, target);
				}
				
				allnodes.add(target);
			}
			
			allnodes.add(n);
		}
		
		while(graph.numNodes() < numnodes) {
			GraphItemID giid = new GraphItemID(gid, nodeschemaid, ""+(nodekeyid++));
			
			// Create node
			Node n = graph.addNode(giid);
			
			// Create edges for node by randomly choosing a target node
			Set<Node> numtargets = new HashSet<Node>();
			while(numtargets.size() < mindegree) {
				Node target = allnodes.get(rand.nextInt(allnodes.size()));
				
				// Check to see if its already been a target
				// Note: We don't need to check self loops since the new
				// node isn't added to the allnodes list until the end.
				if(numtargets.contains(target)) {
					continue;
				}
				
				// Prevent indefinite loops
				if(numtargets.size()==allnodes.size()) {
					break;
				}
				
				// Add edge
				GraphItemID egiid = new GraphItemID((GraphID) graph.getID(),
						edgeschemaid,""+(edgekeyid++));
				if(isdirected) {
					graph.addDirectedEdge(egiid, n, target);
				} else {
					graph.addUndirectedEdge(egiid, n, target);
				}
				
				// Add randomly selected target node to previous targets
				numtargets.add(target);
				
				// Add randomly selected target node to all nodes
				// so that it has a greater likelihood of being
				// randomly selected later
				// (i.e., high degree nodes are more likely to have more edges)
				allnodes.add(target);
			}
			
			// For the added node, represent the outdegree count in all nodes.
			// Note: Previous versions which just added the node once only
			// the initial degree and did so evenly for all nodes.
			int currnodedegree = numtargets.size();
			for(int i=0; i<currnodedegree; i++) {
				allnodes.add(n);
			}
		}
		
		return graph;
	}
}
