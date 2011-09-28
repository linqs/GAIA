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
package linqs.gaia.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.statistic.GraphStatisticUtils;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.KeyedList;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.OutputUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.UnmodifiableList;

/**
 * Collection of general utility functions for the graph and objects in it.
 * 
 * @author namatag
 *
 */
public class GraphUtils {
	/**
	 * Return a string, human readable representation of the Graph.
	 * This function is designed to give more information than the data toString() function.
	 * 
	 * @param graph Graph to print
	 * @return String representation of data
	 */
	public static String printFullData(Graph graph, boolean includeattrs) {
		String output = "";

		Iterator<Node> nodeitr = graph.getNodes();
		Log.INFO("Nodes: Total Num="+graph.numNodes());
		while(nodeitr.hasNext()){
			Log.INFO(GraphUtils.getNodeOverview(nodeitr.next(), includeattrs));
			Log.INFO("\n");
		}

		Iterator<Edge> eitr = graph.getEdges();
		Log.INFO("Print Edges: Total Num="+graph.numEdges());
		while(eitr.hasNext()){
			Log.INFO(GraphUtils.getEdgeOverview(eitr.next(), includeattrs));
			Log.INFO("\n");
		}

		return output;
	}

	/**
	 * Print general overview of the graph and its properties.
	 * The return string for this is much simpler than getGraphOverview
	 * and has just the counts of the nodes and edges in the graph.
	 * 
	 * @param g Graph
	 * @return String overview of graph
	 */
	public static String getSimpleGraphOverview(Graph g) {
		StringBuffer buf = new StringBuffer();
		
		Iterator<String> itr = g.getAllSchemaIDs();
		List<String> nsids = new ArrayList<String>();
		List<String> esids = new ArrayList<String>();
		while(itr.hasNext()) {
			String sid = itr.next();
			SchemaType stype = g.getSchemaType(sid);
			if(stype.equals(SchemaType.NODE)) {
				nsids.add(sid);
			} else if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
				esids.add(sid);
			}
		}

		buf.append("Number of nodes="+g.numNodes());
		buf.append(" (");
		for(int i=0; i<nsids.size(); i++) {
			String sid = nsids.get(i);

			if(i!=0) {
				buf.append(",");
			}

			buf.append("#"+sid+"="+g.numGraphItems(sid));
		}
		buf.append(")");

		buf.append(" Number of edges="+g.numEdges());
		buf.append(" (");
		for(int i=0; i<esids.size(); i++) {
			String sid = esids.get(i);

			if(i!=0) {
				buf.append(",");
			}

			buf.append("#"+sid+"="+g.numGraphItems(sid));
		}
		buf.append(")");

		return buf.toString();
	}

	/**
	 * Print general overview of the graph and its properties
	 * 
	 * @param g Graph
	 * @return String overview of graph
	 */
	public static String getGraphOverview(Graph g) {
		StringBuffer buf = new StringBuffer();
		
		String surroundchar = "#";
		String sepchar = "-";
		int sepnum = 60;

		// Get Graph Information
		buf.append("\n");
		buf.append(OutputUtils.separator(surroundchar, sepnum)+"\n");
		buf.append("Graph Overview\n");
		buf.append("Graph ID: "+g.getID()+"\n");
		buf.append("Graph Schema: "+g.getSchemaID()+" with "
					+g.getSchema(g.getSchemaID()).numFeatures()+" Features\n");

		// Get Node Information
		Iterator<Node> nodes = g.getNodes();
		KeyedList<String,Node> klnodes = new KeyedList<String,Node>();
		KeyedCount<String> kcnodes = new KeyedCount<String>();
		while(nodes.hasNext()){
			Node n = nodes.next();
			klnodes.addItem(n.getSchemaID(), n);
			kcnodes.increment(n.getSchemaID());
		}

		// Get Link Information
		Iterator<Edge> edges = g.getEdges();
		KeyedList<String,Edge> kledges = new KeyedList<String,Edge>();
		KeyedCount<String> kcedges = new KeyedCount<String>();
		while(edges.hasNext()){
			Edge e = edges.next();
			kledges.addItem(e.getSchemaID(), e);
			kcedges.increment(e.getSchemaID());
		}

		buf.append("Total Number of Nodes: "+g.numNodes()+"\n");
		buf.append(kcnodes.toString()+"\n");
		buf.append("Total Number of Edges: "+g.numEdges()+"\n");
		buf.append(kcedges.toString()+"\n");

		// Get Node Statistics
		// Get for all nodes
		Collection<GraphItem> allnodes = new LinkedList<GraphItem>();
		Iterator<Node> nodeitr = g.getNodes();
		while(nodeitr.hasNext()){
			allnodes.add(nodeitr.next());
		}

		buf.append(OutputUtils.separator(sepchar, sepnum)+"\n");
		buf.append("Statistics for All Nodes:\n");
		buf.append("All Degree\n");
		buf.append(getGraphItemsOverview(allnodes, null, "all", false)+"\n");
		buf.append("Undirected Degree\n");
		buf.append(getGraphItemsOverview(allnodes, null, "undirected", false)+"\n");
		buf.append("In Degree\n");
		buf.append(getGraphItemsOverview(allnodes, null, "in", false)+"\n");
		buf.append("Out Degree\n");
		buf.append(getGraphItemsOverview(allnodes, null, "out", false)+"\n");

		// Get for specific node schemas
		for(String sid: klnodes.getKeys()){
			Collection<GraphItem> coll = new LinkedList<GraphItem>();
			Iterator<GraphItem> itr = g.getGraphItems(sid);
			while(itr.hasNext()){
				coll.add(itr.next());
			}

			buf.append("Statistics for Node Schema: "+sid+"\n");
			buf.append(g.getSchema(sid).getSummaryString());
			buf.append("All Degree\n");
			buf.append(getGraphItemsOverview(coll, null, "all", false)+"\n");
			buf.append("Undirected Degree\n");
			buf.append(getGraphItemsOverview(coll, null, "undirected", false)+"\n");
			buf.append("In Degree\n");
			buf.append(getGraphItemsOverview(coll, null, "in",false)+"\n");
			buf.append("Out Degree\n");
			buf.append(getGraphItemsOverview(coll, null, "out",false)+"\n");
			
			// Apply to all possible edge schemas
			for(String esid: kledges.getKeys()){
				buf.append("All Degree with edge type "+esid+"\n");
				buf.append(getGraphItemsOverview(coll, esid, "all", false)+"\n");
				buf.append("Undirected Degree with edge type "+esid+"\n");
				buf.append(getGraphItemsOverview(coll, esid, "undirected", false)+"\n");
				buf.append("In Degree with edge type "+esid+"\n");
				buf.append(getGraphItemsOverview(coll, esid, "in",false)+"\n");
				buf.append("Out Degree with edge type "+esid+"\n");
				buf.append(getGraphItemsOverview(coll, esid, "out",false)+"\n");
			}

			buf.append("\n");
		}

		// Get Edge Statistics
		// Get for all edges
		buf.append(OutputUtils.separator(sepchar, sepnum)+"\n");
		buf.append("Statistics for All Edges:\n");
		Collection<GraphItem> alledges = new LinkedList<GraphItem>();
		Iterator<Edge> edgeitr = g.getEdges();
		while(edgeitr.hasNext()){
			alledges.add(edgeitr.next());
		}

		buf.append(getGraphItemsOverview(alledges, null, "undirected",false)+"\n");

		// Get for specific edge schemas
		for(String sid: kledges.getKeys()){
			Collection<GraphItem> coll = new LinkedList<GraphItem>();
			Iterator<GraphItem> itr = g.getGraphItems(sid);
			while(itr.hasNext()){
				coll.add(itr.next());
			}

			buf.append("Statistics for Edge Schema: "+sid+"\n");
			buf.append(g.getSchema(sid).getSummaryString());
			buf.append(getGraphItemsOverview(coll, null, "undirected",false));
			buf.append("\n");
		}

		buf.append(OutputUtils.separator(surroundchar, sepnum)+"\n");

		return buf.toString();
	}

	/**
	 * Get general overview of graph items
	 * 
	 * @param gitems List of graph items
	 * @param sid Schema ID
	 * @param degreetype Type of degree we're interested in @see degreeStats
	 * @param showdistribution If true, show the degree distribution.
	 * @return String with the overview of graph
	 */
	public static String getGraphItemsOverview(Collection<? extends GraphItem> gitems,
			String sid, String degreetype, boolean showdistribution){
		String output = "";

		output += "Number of items: "+gitems.size()+"\n";
		double dstat[] = GraphStatisticUtils.degreeStats(gitems, sid, degreetype);
		output += "Degree: ";
		output += "Min: "+dstat[0]+" ";
		output += "Max: "+dstat[1]+" ";
		output += "Mean: "+dstat[2]+" ";
		output += "StdDev: "+dstat[3]+"\n";
		if(showdistribution) {
			output += GraphStatisticUtils.degreeDistribution(gitems.iterator(), sid, degreetype);
		}

		return output;
	}

	/**
	 * Return overview of node
	 * 
	 * @param n Node
	 * @param includeattrs If True, provide overview of attributes.  False otherwise.
	 * @return Overview of nodes
	 */
	public static String getNodeOverview(Node n, boolean includeattrs){
		String output = "";

		output+="Ref ID: "+n.getID()+"\n";
		output+="Ref Schema: "+n.getSchemaID()+"\n";
		output+="# Edges:"
			+" # where source="+n.numEdgesWhereSource()
			+" # where target="+n.numEdgesWhereTarget()
			+" # where undirected="+n.numUndirEdges()+"\n";

		if(includeattrs){
			output+=getDecorableOverview(n.getGraph(), n);
		}

		return output;
	}

	/**
	 * Return overview of edge
	 * 
	 * @param e Edge
	 * @param includeattrs If True, provide overview of attributes.  False otherwise.
	 * @return Overview of edges
	 */
	public static String getEdgeOverview(Edge e, boolean includeattrs){
		StringBuffer buf = new StringBuffer();
		
		buf.append("Edge ID: "+e.getID()+"\n");
		buf.append("Edge Schema: "+e.getSchemaID()+"\n");
		buf.append("Edge Type: "+e.getClass().getCanonicalName()+"\n");
		if(e instanceof DirectedEdge){
			DirectedEdge de = (DirectedEdge) e;
			Iterator<Node> tempc = de.getSourceNodes();
			buf.append("Source Object IDs: ");
			boolean first = true;
			while(tempc.hasNext()){
				if(first){
					first = false;
				} else {
					buf.append(",");
				}

				buf.append(tempc.next().getID().getObjID());
			}
			buf.append("\n");

			tempc = de.getTargetNodes();
			buf.append("Target Object IDs: ");
			first = true;
			while(tempc.hasNext()){
				if(first){
					first = false;
				} else {
					buf.append(",");
				}

				buf.append(tempc.next().getID().getObjID());
			}
			
			buf.append("\n");
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			Iterator<Node> tempc = ue.getAllNodes();
			buf.append("Undirected IDs: ");
			boolean first = true;
			while(tempc.hasNext()){
				if(first){
					first = false;
				} else {
					buf.append(",");
				}

				buf.append(tempc.next().getID().getObjID());
			}

			buf.append("\n");
		}

		if(includeattrs){
			buf.append(getDecorableOverview(e.getGraph(), e));
		}

		return buf.toString();
	}

	/**
	 * Return overview of decorable items including the type of schema
	 * and the values for the features.
	 * 
	 * @param g Graph
	 * @param d Decorable item
	 * @return String overview of Decorable item
	 */
	public static String getDecorableOverview(Graph g, Decorable d){
		StringBuffer buf = new StringBuffer();

		Schema schema = g.getSchema(d.getSchemaID());
		Iterator<String> fids = schema.getFeatureIDs();
		
		buf.append("Features: Total Num="+schema.numFeatures()+"\n");
		while(fids.hasNext()){
			String fid = fids.next();
			buf.append(fid+"="+d.getFeatureValue(fid).getStringValue()+"\n");
		}

		return buf.toString();
	}

	/**
	 * Return True if the graph is homogeneous (exactly one node type and exactly one link type).
	 * False otherwise.
	 * 
	 * @param g Graph to check
	 * @return True if homogeneous and false otherwise
	 */
	public static boolean isHomogeneous(Graph g) {
		return (getNodeSids(g).size()==1 && getEdgeSids(g).size()==1);
	}
	
	/**
	 * Return True if the graph has self links
	 * (i.e., Undirected edge only has one node and at least one node
	 * is in the source and target of the same directed edge).
	 * False otherwise.
	 * 
	 * @param g Graph to check
	 * @return True if graph has self links and false otherwise
	 */
	public static boolean hasSelfLinks(Graph g) {
		Iterator<Edge> eitr = g.getEdges();
		
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			if(e instanceof UndirectedEdge) {
				// If the undirected edge has only one node, its a self link
				if(e.numNodes()==1) {
					return true;
				}
			} else if(e instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) e;
				Set<Node> sources = IteratorUtils.iterator2nodeset(de.getSourceNodes());
				Set<Node> targets = IteratorUtils.iterator2nodeset(de.getTargetNodes());
				sources.retainAll(targets);
				
				// If there is at least one node in the intersect of
				// the source and target nodes, its a self link
				if(!sources.isEmpty()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Returns true if the graph only contains binary edges
	 * (i.e., one source and one target for directed edges
	 * and one (for self links) or two nodes for undirected edges.
	 * 
	 * @param g Graph
	 * @return True if binary and false otherwise
	 */
	public static boolean isBinary(Graph g) {
		Iterator<Edge> eitr = g.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(e.numNodes()>2) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Return all the schema IDs defined for the edges in the graph
	 * 
	 * @param g Graph
	 * @return Set of schema IDs
	 */
	public static Set<String> getEdgeSids(Graph g){
		Set<String> sids = new HashSet<String>();

		Iterator<String> sitr = g.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			SchemaType stype = g.getSchemaType(sid);
			if(stype.equals(SchemaType.DIRECTED) || stype.equals(SchemaType.UNDIRECTED)) {
				sids.add(sid);
			}
		}

		return sids;
	}

	/**
	 * Return all the schema IDs defined for the nodes in the graph
	 * 
	 * @param g Graph
	 * @return Set of schema IDs
	 */
	public static Set<String> getNodeSids(Graph g){
		Set<String> sids = new HashSet<String>();

		Iterator<String> sitr = g.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			SchemaType stype = g.getSchemaType(sid);
			if(stype.equals(SchemaType.NODE)) {
				sids.add(sid);
			}
		}

		return sids;
	}

	/**
	 * Create a copy of all items with the given schema id.
	 * All copies will have the new schema specified with the
	 * object id of the original object.
	 * All schema features and values for those features will
	 * also be copied.
	 * For edges, the new edge will have the same set of nodes as the original edge.
	 * <p>
	 * Note:  This will not create copy edges for nodes (e.g., won't add new edges for nodes).
	 * 
	 * @param g Graph
	 * @param schemaid Schema ID
	 * @param copyschemaid Copy schema ID
	 */
	public static void copyGraphItems(Graph g, String schemaid, String copyschemaid) {
		// Copy schema of items
		Schema schema = g.getSchema(schemaid);
		g.addSchema(copyschemaid, schema);

		// Create new item based with the same set of nodes
		Iterator<GraphItem> gitems = g.getGraphItems(schemaid);
		while(gitems.hasNext()) {
			GraphItem gi = gitems.next();
			GraphItem copygi = null;
			GraphItemID copyid = new GraphItemID(g.getID(), copyschemaid, gi.getID().getObjID());

			// Copy item
			if(gi instanceof Node) {
				copygi = g.addNode(copyid);
			} else if(gi instanceof UndirectedEdge) {
				UndirectedEdge e = (UndirectedEdge) gi;
				copygi = g.addUndirectedEdge(copyid, e.getAllNodes());
			} else if(gi instanceof DirectedEdge) {
				DirectedEdge e = (DirectedEdge) gi;
				copygi = g.addDirectedEdge(copyid, e.getSourceNodes(), e.getTargetNodes());
			} else {
				throw new UnsupportedTypeException("Unsupported graph item type: "
						+gi.getClass().getCanonicalName());
			}

			// Copy features of item
			Iterator<SimplePair<String, Feature>> fitr = schema.getAllFeatures();
			while(fitr.hasNext()) {
				SimplePair<String, Feature> pair = fitr.next();

				String fid = pair.getFirst();
				Feature f = pair.getSecond();

				// Only copy values for explicit items
				if(f instanceof ExplicitFeature) {
					copygi.setFeatureValue(fid, gi.getFeatureValue(fid));
				}
			}
		}
	}
	
	/**
	 * Create a copy of the graph to the specified item
	 * 
	 * @param g Graph to copy from
	 * @param copyg Graph to copy to
	 */
	public static void copy(Graph g, Graph copyg) {
		// Copy schemas
		Iterator<String> sids = g.getAllSchemaIDs();
		while(sids.hasNext()) {
			String sid = sids.next();
			Schema schema = g.getSchema(sid);

			Schema copyschema = schema.copyWithFeatures();

			// Handle graph dependent features
			Iterator<SimplePair<String, Feature>> sitr = copyschema.getAllFeatures();
			while(sitr.hasNext()) {
				SimplePair<String, Feature> pair = sitr.next();
				Feature f = pair.getSecond();
				if(f instanceof GraphDependent) {
					((GraphDependent) f).setGraph(copyg);
				}
			}

			if(schema.getType().equals(SchemaType.GRAPH)) {
				copyg.replaceSchema(sid, copyschema);
			} else {
				copyg.addSchema(sid, copyschema);
			}
		}

		// Copy graph features
		GraphUtils.copyFeatures(g, copyg);

		// Copy Nodes
		Iterator<Node> nitr = g.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			Node copyn = copyg.addNode(new GraphItemID(copyg.getID(), n.getSchemaID(), n.getID().getObjID()));

			// Copy node features
			GraphUtils.copyFeatures(n, copyn);
		}

		// Copy Edges
		Iterator<Edge> eitr = g.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			GraphItemID copyeid = new GraphItemID(copyg.getID(), e.getSchemaID(), e.getID().getObjID());

			Edge copye = null;
			if(e instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) e;
				List<Node> snodes = new ArrayList<Node>();
				Iterator<Node> sitr = de.getSourceNodes();
				while(sitr.hasNext()) {
					Node n = sitr.next();
					snodes.add(copyg.getNode(new GraphItemID(copyg.getID(),
							n.getSchemaID(), n.getID().getObjID())));
				}

				List<Node> tnodes = new ArrayList<Node>();
				Iterator<Node> titr = de.getTargetNodes();
				while(titr.hasNext()) {
					Node n = titr.next();
					tnodes.add(copyg.getNode(new GraphItemID(copyg.getID(),
							n.getSchemaID(), n.getID().getObjID())));
				}

				copye = copyg.addDirectedEdge(copyeid, snodes, tnodes);
			} else if(e instanceof UndirectedEdge) {
				UndirectedEdge ue = (UndirectedEdge) e;
				List<Node> nodes = new ArrayList<Node>();
				Iterator<Node> enitr = ue.getAllNodes();
				while(enitr.hasNext()) {
					Node n = enitr.next();
					nodes.add(copyg.getNode(new GraphItemID(copyg.getID(),
							n.getSchemaID(), n.getID().getObjID())));
				}

				copye = copyg.addUndirectedEdge(copyeid, nodes);
			} else {
				throw new UnsupportedTypeException("Unsupported edge type: "
						+e.getClass().getCanonicalName());
			}

			// Copy edge features
			GraphUtils.copyFeatures(e, copye);
		}
	}
	
	/**
	 * Create an empty copy of the graph to the specified item
	 * 
	 * @param g Graph to copy from
	 * @param copyg Graph to copy to
	 */
	public static void copySchema(Graph g, Graph copyg) {
		// Copy schemas
		Iterator<String> sids = g.getAllSchemaIDs();
		while(sids.hasNext()) {
			String sid = sids.next();
			Schema schema = g.getSchema(sid);

			Schema copyschema = schema.copyWithFeatures();

			// Handle graph dependent features
			Iterator<SimplePair<String, Feature>> sitr = copyschema.getAllFeatures();
			while(sitr.hasNext()) {
				SimplePair<String, Feature> pair = sitr.next();
				Feature f = pair.getSecond();
				if(f instanceof GraphDependent) {
					((GraphDependent) f).setGraph(copyg);
				}
			}

			if(schema.getType().equals(SchemaType.GRAPH)) {
				copyg.replaceSchema(sid, copyschema);
			} else {
				copyg.addSchema(sid, copyschema);
			}
		}
	}
	
	/**
	 * Copy the values of explicit features from d1 to d2.
	 * Note: An exception is thrown if a feature is defined
	 * in d1 but not in d2.  An exception is also thrown if the types
	 * of the values do not match for a given feature.
	 * 
	 * @param d1 Decorable item to copy features from
	 * @param d2 Decorable item to copy features to
	 */
	public static void copyFeatures(Decorable d1, Decorable d2) {
		// Copy features of item
		Iterator<SimplePair<String, Feature>> fitr = d1.getSchema().getAllFeatures();
		while(fitr.hasNext()) {
			SimplePair<String, Feature> pair = fitr.next();

			String fid = pair.getFirst();
			Feature f = pair.getSecond();

			// Only copy values for explicit items
			if(f instanceof ExplicitFeature) {
				d2.setFeatureValue(fid, d1.getFeatureValue(fid));
			}
		}
	}

	/**
	 * Number of unknown feature values in the graph
	 * 
	 * @param g Graph
	 * @return Number of feature values whose value
	 * is not known through the full graph.
	 */
	public static int numUnknownValues(Graph g) {
		int numunknownvalue = 0;
		
		// Test all features of the graph
		Schema schema = g.getSchema();
		Iterator<String> fitr = schema.getFeatureIDs();
		while(fitr.hasNext()) {
			String fid = fitr.next();
			if(g.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)){
				numunknownvalue++;
			}
		}

		// Test all features of the nodes and edges
		Iterator<String> sitr = g.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			schema = g.getSchema(sid);
			if(schema.getType().equals(SchemaType.GRAPH)) {
				continue;
			}

			Iterator<GraphItem> gitr = g.getGraphItems(sid);
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();

				fitr = schema.getFeatureIDs();
				while(fitr.hasNext()) {
					String fid = fitr.next();
					if(gi.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)){
						numunknownvalue++;
					}
				}
			}
		}
		
		return numunknownvalue;
	}
	
	/**
	 * Remove self links from the graph where a self
	 * link occurs when an undirected edge only has one
	 * incident node or if a directed edge has
	 * at least one node in both its source and target node sets.
	 * 
	 * @param g Graph to remove self links from
	 */
	public static void removeSelfLinks(Graph g) {
		Iterator<Edge> eitr = g.getEdges();
		
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			if(e instanceof UndirectedEdge) {
				// If the undirected edge has only one node, its a self link
				if(e.numNodes()==1) {
					Log.DEBUG("Edge is a self link: "+e);
					g.removeEdge(e);
				}
			} else if(e instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) e;
				Set<Node> sources = IteratorUtils.iterator2nodeset(de.getSourceNodes());
				Set<Node> targets = IteratorUtils.iterator2nodeset(de.getTargetNodes());
				sources.retainAll(targets);
				
				// If there is at least one node in the intersect of
				// the source and target nodes, its a self link
				if(!sources.isEmpty()) {
					Log.DEBUG("Edge is a self link: "+de);
					g.removeEdge(de);
				}
			}
		}
	}
	
	/**
	 * Print an overview of the feature values of the features
	 * defined in the given schema.
	 * 
	 * @param g Graph to print overview for
	 * @param schemaid Schema ID of schema whose values we want an overview for
	 */
	public static void printFeatureValueOverview(Graph g, String schemaid) {
		Schema schema = g.getSchema(schemaid);
		Iterator<String> fitr = schema.getFeatureIDs();
		while(fitr.hasNext()) {
			String fid = fitr.next();
			Feature f = schema.getFeature(fid);
			
			if(f instanceof CompositeFeature) {
				// Handle composite features
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String,CVFeature>> cffeatures = cf.getFeatures();
				
				boolean cache = false;
				if(!cf.isCaching()) {
					cf.setCache(true);
					cache = true;
				}
				
				for(int i=0; i<cffeatures.size(); i++) {
					SimplePair<String,CVFeature> pair = cffeatures.get(i);
					String cfid = pair.getFirst();
					CVFeature cvf = pair.getSecond();
					
					Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
					double numunknown = 0;
					double numknown = 0;
					MinMax mm = new MinMax();
					int numzero = 0;
					KeyedCount<String> catvalues = new KeyedCount<String>();
					Set<String> values = new HashSet<String>();
					while(gitr.hasNext()) {
						GraphItem gi = gitr.next();
						FeatureValue fv = gi.getFeatureValue(fid);
						if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
							fv = ((CompositeValue) fv).getFeatureValues().get(i);
						}
						
						// Count number known or unknown value
						if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
							numunknown++;
							continue;
						} else {
							numknown++;
						}
						
						// Process based on type
						if(cvf instanceof NumFeature) {
							double value = ((NumValue) fv).getNumber();
							if(value==0) {
								numzero++;
							}
							
							mm.addValue(value);
							values.add(fv.getStringValue());
						} else if(cvf instanceof CategFeature) {
							catvalues.increment(((CategValue) fv).getCategory());
						} else if(cvf instanceof StringFeature) {
							values.add(fv.getStringValue());
						}
					}
					
					StringBuffer buf = new StringBuffer();
					buf.append(schemaid+"."+fid+"."+cfid+": #unknown="+numunknown+" #known="+numknown);
					if(cvf instanceof NumFeature) {
						buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
									+" #zero="+numzero+" #uniquevalues="+values.size());
					} else if(cvf instanceof CategFeature) {
						List<String> keys = new ArrayList<String>(catvalues.getKeys());
						Collections.sort(keys);
						for(String key:keys) {
							buf.append(" #"+key+"="+catvalues.getCount(key));
						}
					} else if(cvf instanceof StringFeature) {
						buf.append(" #uniquevalues="+values.size());
					}
					
					Log.INFO(buf.toString());
				}
				
				if(cache) {
					((DerivedFeature) f).resetCache();
					((DerivedFeature) f).setCache(false);
				}
			} else {
				// Handle non-composite features
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				double numunknown = 0;
				double numknown = 0;
				MinMax mm = new MinMax();
				int numzero = 0;
				KeyedCount<String> catvalues = new KeyedCount<String>();
				Set<String> values = new HashSet<String>();
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					FeatureValue fv = gi.getFeatureValue(fid);
					
					// Count number known or unknown value
					if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						numunknown++;
						continue;
					} else {
						numknown++;
					}
					
					// Process based on type
					if(f instanceof NumFeature) {
						double value = ((NumValue) fv).getNumber();
						if(value==0) {
							numzero++;
						}
						
						mm.addValue(value);
						values.add(fv.getStringValue());
					} else if(f instanceof CategFeature) {
						catvalues.increment(((CategValue) fv).getCategory());
					} else if(f instanceof StringFeature) {
						values.add(fv.getStringValue());
					}
				}
				
				StringBuffer buf = new StringBuffer();
				buf.append(schemaid+"."+fid+": #unknown="+numunknown+" #known="+numknown);
				if(f instanceof NumFeature) {
					buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
								+" #zero="+numzero+" #uniquevalues="+values.size());
				} else if(f instanceof CategFeature) {
					List<String> keys = new ArrayList<String>(catvalues.getKeys());
					buf.append(" #categories="+keys.size());
					Collections.sort(keys);
					for(String key:keys) {
						buf.append(" #"+key+"="+catvalues.getCount(key));
					}
				} else if(f instanceof StringFeature) {
					buf.append(" #uniquevalues="+values.size());
				}
				
				Log.INFO(buf.toString());
			}
		}
	}
	
	/**
	 * Print an overview of the feature values of the feature
	 * defined in the given schema.
	 * 
	 * @param g Graph to print overview for
	 * @param schemaid Schema ID of schema whose values we want an overview for
	 * @param fid Feature ID of the feature whose value we want an overview for
	 * 
	 */
	public static void printFeatureValueOverview(Graph g, String schemaid, String fid) {
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(fid);
		
		if(f instanceof CompositeFeature) {
			// Handle composite features
			CompositeFeature cf = (CompositeFeature) f;
			UnmodifiableList<SimplePair<String,CVFeature>> cffeatures = cf.getFeatures();
			
			boolean cache = false;
			if(!cf.isCaching()) {
				cf.setCache(true);
				cache = true;
			}
			
			for(int i=0; i<cffeatures.size(); i++) {
				SimplePair<String,CVFeature> pair = cffeatures.get(i);
				String cfid = pair.getFirst();
				CVFeature cvf = pair.getSecond();
				
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				double numunknown = 0;
				double numknown = 0;
				MinMax mm = new MinMax();
				int numzero = 0;
				KeyedCount<String> catvalues = new KeyedCount<String>();
				Set<String> values = new HashSet<String>();
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					FeatureValue fv = gi.getFeatureValue(fid);
					if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						fv = ((CompositeValue) fv).getFeatureValues().get(i);
					}
					
					// Count number known or unknown value
					if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						numunknown++;
						continue;
					} else {
						numknown++;
					}
					
					// Process based on type
					if(cvf instanceof NumFeature) {
						double value = ((NumValue) fv).getNumber();
						if(value==0) {
							numzero++;
						}
						
						mm.addValue(value);
						values.add(fv.getStringValue());
					} else if(cvf instanceof CategFeature) {
						catvalues.increment(((CategValue) fv).getCategory());
					} else if(cvf instanceof StringFeature) {
						values.add(fv.getStringValue());
					}
				}
				
				StringBuffer buf = new StringBuffer();
				buf.append(schemaid+"."+fid+"."+cfid+": #unknown="+numunknown+" #known="+numknown);
				if(cvf instanceof NumFeature) {
					buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
					+" #zero="+numzero+" #uniquevalues="+values.size());
				} else if(cvf instanceof CategFeature) {
					List<String> keys = new ArrayList<String>(catvalues.getKeys());
					Collections.sort(keys);
					for(String key:keys) {
						buf.append(" #"+key+"="+catvalues.getCount(key));
					}
				} else if(cvf instanceof StringFeature) {
					buf.append(" #uniquevalues="+values.size());
				}
				
				Log.INFO(buf.toString());
			}
			
			if(cache) {
				((DerivedFeature) f).resetCache();
				((DerivedFeature) f).setCache(false);
			}
		} else {
			// Handle non-composite features
			Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
			double numunknown = 0;
			double numknown = 0;
			MinMax mm = new MinMax();
			int numzero = 0;
			KeyedCount<String> catvalues = new KeyedCount<String>();
			Set<String> values = new HashSet<String>();
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();
				FeatureValue fv = gi.getFeatureValue(fid);
				
				// Count number known or unknown value
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					numunknown++;
					continue;
				} else {
					numknown++;
				}
				
				// Process based on type
				if(f instanceof NumFeature) {
					double value = ((NumValue) fv).getNumber();
					if(value==0) {
						numzero++;
					}
					
					mm.addValue(value);
					values.add(fv.getStringValue());
				} else if(f instanceof CategFeature) {
					catvalues.increment(((CategValue) fv).getCategory());
				} else if(f instanceof StringFeature) {
					values.add(fv.getStringValue());
				}
			}
			
			StringBuffer buf = new StringBuffer();
			buf.append(schemaid+"."+fid+": #unknown="+numunknown+" #known="+numknown);
			if(f instanceof NumFeature) {
				buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
							+" #zero="+numzero+" #uniquevalues="+values.size());
			} else if(f instanceof CategFeature) {
				List<String> keys = new ArrayList<String>(catvalues.getKeys());
				Collections.sort(keys);
				for(String key:keys) {
					buf.append(" #"+key+"="+catvalues.getCount(key));
				}
			} else if(f instanceof StringFeature) {
				buf.append(" #uniquevalues="+values.size());
			}
			
			Log.INFO(buf.toString());
		}
	}
	
	/**
	 * Print an overview of the feature values in the graph for all schemas.
	 * 
	 * @param g Graph to print overview for
	 */
	public static void printFeatureValueOverview(Graph g) {
		// Go through all schemas, sorted by type and alphabetically
		List<String> sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.NODE));
		Collections.sort(sids);
		for(String sid:sids) {
			printFeatureValueOverview(g, sid);
		}
		
		sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED));
		Collections.sort(sids);
		for(String sid:sids) {
			printFeatureValueOverview(g, sid);
		}
		
		sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED));
		Collections.sort(sids);
		for(String sid:sids) {
			printFeatureValueOverview(g, sid);
		}
	}
	
	/**
	 * For the given graph, get the feature value (i.e., compute the value)
	 * of all items with the specified schema.  This function then prints
	 * the amount of time it took to get all those valus for all those items.
	 * 
	 * @param g Graph
	 * @param schemaid Schema ID of items to compute the derived features of
	 */
	public static void printDerivedTime(Graph g, String schemaid) {
		Schema schema = g.getSchema(schemaid);
		Iterator<String> fitr = schema.getFeatureIDs();
		while(fitr.hasNext()) {
			String fid = fitr.next();
			Feature f = schema.getFeature(fid);
			
			if(f instanceof DerivedFeature) {
				// Compute for all items
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				
				MinMax mm = new MinMax();
				SimpleTimer totaltime = new SimpleTimer();
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					
					SimpleTimer currtime = new SimpleTimer();
					gi.getFeatureValue(fid);
					
					double indivmsec = currtime.msecLapse();
					mm.addValue(indivmsec);
				}
				
				double totalmsec = totaltime.msecLapse();
				
				String output = schemaid+"."+fid+":" 
				+" total(ms)="+totalmsec
				+" min(ms)="+mm.getMin()
				+" max(ms)="+mm.getMax()
				+" mean(ms)="+mm.getMean()
				+" total="+SimpleTimer.msec2string(totalmsec,false)
				+" min="+SimpleTimer.msec2string(mm.getMin(),false)
				+" max="+SimpleTimer.msec2string(mm.getMax(),false)
				+" mean="+SimpleTimer.msec2string(mm.getMean(),false);
				
				Log.INFO(output);
			}
		}
	}
	
	/**
	 * Prints the time to compute the derived values of all items in this graph,
	 * split by schema.
	 * 
	 * @param g Graph
	 */
	public static void printDerivedTime(Graph g) {
		// Go through all schemas, sorted by type and alphabetically
		List<String> sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.NODE));
		Collections.sort(sids);
		for(String sid:sids) {
			printDerivedTime(g, sid);
		}
		
		sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED));
		Collections.sort(sids);
		for(String sid:sids) {
			printDerivedTime(g, sid);
		}
		
		sids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED));
		Collections.sort(sids);
		for(String sid:sids) {
			printDerivedTime(g, sid);
		}
	}
	
	/**
	 * Compares the set of schemas and features for those schemas,
	 * of the two graphs.  Feature equality is verified by checking
	 * to see if the two features have the same class.
	 * Features with categorical values are also checked to
	 * ensure the have the same set of categories.
	 * 
	 * @param g1 First graph
	 * @param g2 Second graph
	 * @return True if the graphs have the same schemas, and false otherwise
	 */
	public static boolean hasSameSchemas(Graph g1, Graph g2) {
		// Check graph schema id
		if(!g1.getSchemaID().equals(g2.getSchemaID())) {
			return false;
		}
		
		Set<String> g1nsids = getNodeSids(g1);
		Set<String> g2nsids = getNodeSids(g2);
		Set<String> g1esids = getEdgeSids(g1);
		Set<String> g2esids = getEdgeSids(g2);
		
		// Verify same set of node and edge schema ids
		if(!g1nsids.equals(g2nsids) || !g1esids.equals(g2esids)) {
			return false;
		}
		
		// Check the features in the node and edge schemas
		List<String> sids = IteratorUtils.iterator2stringlist(g1.getAllSchemaIDs());
		for(String sid:sids) {
			Schema schema1 = g1.getSchema(sid);
			Schema schema2 = g2.getSchema(sid);
			Iterator<String> fitr = schema1.getFeatureIDs();
			while(fitr.hasNext()) {
				String fid = fitr.next();
				Feature f1 = schema1.getFeature(fid);
				Feature f2 = schema2.getFeature(fid);
				if(!f1.getClass().equals(f2.getClass())) {
					return false;
				}
				
				// Verify categories match
				if(f1 instanceof CategFeature) {
					UnmodifiableList<String> cats1 = ((CategFeature) f1).getAllCategories();
					UnmodifiableList<String> cats2 = ((CategFeature) f2).getAllCategories();
					if(!cats1.equals(cats2)) {
						return false;
					}
				} else if(f1 instanceof MultiCategFeature) {
					UnmodifiableList<String> cats1 = ((MultiCategFeature) f1).getAllCategories();
					UnmodifiableList<String> cats2 = ((MultiCategFeature) f2).getAllCategories();
					if(!cats1.equals(cats2)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Copy graph items from source graph to target graph.
	 * The items are added with the object id modified with the given prefix.
	 * An exception is thrown if the schemas or features do not
	 * match for source and target or if there is conflict in the ids.
	 * 
	 * @param source Source graph
	 * @param target Target graph
	 * @param prefix Prefix for object id in target graph.  Assumed to make
	 * all the ids not conflict to the ids already in the target graph.
	 */
	public static void copyGraphItems(Graph source, Graph target, String prefix) {
		Iterator<Node> nitr = source.getNodes();
		while(nitr.hasNext()) {
			Node gi = nitr.next();
			Node copygi;
			String gisid = gi.getSchemaID();
			String gioid = gi.getID().getObjID();
			
			copygi = target.addNode(new GraphItemID(gisid, prefix+gioid));
			FeatureUtils.copyFeatureValues(gi, copygi);
		}
		
		Iterator<Edge> eitr = source.getEdges();
		while(eitr.hasNext()) {
			Edge gi = (DirectedEdge) eitr.next();
			Edge copygi;
			String gisid = gi.getSchemaID();
			String gioid = gi.getID().getObjID();
			
			if(gi instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) gi;
				// Get sources
				List<Node> sources = new ArrayList<Node>();
				Iterator<Node> sitr = de.getSourceNodes();
				while(sitr.hasNext()) {
					Node n = sitr.next();
					sources.add(target.getNode(new GraphItemID(n.getSchemaID(),
							prefix+n.getID().getObjID())));
				}
				
				// Get targets
				List<Node> targets = new ArrayList<Node>();
				Iterator<Node> titr = de.getTargetNodes();
				while(titr.hasNext()) {
					Node n = titr.next();
					targets.add(target.getNode(new GraphItemID(n.getSchemaID(),
							prefix+n.getID().getObjID())));
				}
				
				// Add directed edges
				copygi = target.addDirectedEdge(
						new GraphItemID(gisid, prefix+gioid),
						sources,
						targets);
			} else if(gi instanceof UndirectedEdge) {
				UndirectedEdge ue = (UndirectedEdge) gi;
				// Get nodes
				List<Node> nodes = new ArrayList<Node>();
				Iterator<Node> titr = ue.getAllNodes();
				while(titr.hasNext()) {
					Node n = titr.next();
					nodes.add(target.getNode(new GraphItemID(n.getSchemaID(),
							prefix+n.getID().getObjID())));
				}
				
				// Add undirected edges
				copygi = target.addUndirectedEdge(new GraphItemID(gisid, prefix+gioid), nodes);
			} else {
				throw new UnsupportedTypeException("Unsupported graph item type: "
						+gi.getClass().getCanonicalName());
			}
			
			FeatureUtils.copyFeatureValues(gi, copygi);
		}
	}
}
