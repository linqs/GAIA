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
package linqs.gaia.graph.converter.junto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.UnmodifiableList;
import upenn.junto.graph.Vertex;

/**
 * Graph converter for converting to and from the graph
 * object of the Junto Library.
 * <p>
 * Note: This exporter requires the Junto 1.2.2 Library to be in the classpath
 * (i.e., junto.jar).
 * </p>
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> For exporting:
 * <UL>
 * <LI> targetschemaID-Schema ID of nodes to export to Junto graph.
 * Note: This implementation only supports exporting of a single node type.
 * <LI> targetfeatureID-Feature ID of the feature whose value we want
 * to export to Junto (e.g., for prediction)
 * </UL>
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> For exporting:
 * <UL>
 * <LI> edgesid-Schema ID of edges to export.  If not specified all edges will
 * be exported, with an exception throw if an edge is encountered incident
 * to a node with a different schema than the specified target schema id.
 * <LI> undirweight-Weight to add, in both direction, for an undirected edge. Default is 1.
 * <LI> sourcetargetweight-Weight to add, from source to target, for a directed edge.  Default is 1.
 * <LI> targetsourceweight-Weight to add, from target to source, for a directed edge.  Default is 1.
 * </UL>
 * 
 * <LI> For importing:
 * <UL>
 * <LI> graphclass-Full java class for the graph,
 * instantiated using {@link Dynamic#forConfigurableName}.
 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * <LI> graphsid-Schema ID of resulting GAIA graph.  Default is "juntograph".
 * <LI> graphobjid-Object ID of resulting GAIA graph.  Default is "junto1".
 * <LI> nodesid-Schema ID of imported nodes.  Default is "juntonode".
 * <LI> edgesid-Schema ID of imported directed edges.  Default is "juntoedge".
 * <LI> labelfid-Feature ID of feature to load.  Default is not to load a feature.
 * </UL>
 * 
 * </UL>
 *
 * @author jvalentine
 * @author namatag
 *
 */
public class JuntoConverter extends BaseConfigurable {
	
	public upenn.junto.graph.Graph exportGraph(Graph graph){
		// Get features
		String targetfeatureID = this.getStringParameter("targetfeatureID");
		String targetschemaID = this.getStringParameter("targetschemaID");
		
		return this.exportGraph(graph, targetschemaID, targetfeatureID);
	}

	public upenn.junto.graph.Graph exportGraph(Graph graph,
			String targetschemaID, String targetfeatureID){
		String edgesid = this.getStringParameter("edgesid",null);
		double undirweight = this.getDoubleParameter("undirweight", 1.0);
		double sourcetargetweight = this.getDoubleParameter("sourcetargetweight", 1.0);
		double targetsourceweight = this.getDoubleParameter("targetsourceweight", 1.0);
		
		// Initialize a Junto style graph object
		upenn.junto.graph.Graph junto_g = new upenn.junto.graph.Graph();

		// Add all of the GAIA graph nodes to the Junto graph
		Iterator<Node> node_it = graph.getNodes(targetschemaID);
		Node node;
		Vertex vertex = null;
		while(node_it.hasNext()){
			node = node_it.next();	
			vertex = junto_g.AddVertex(node.getID().toString(), null);
			FeatureValue feat = node.getFeatureValue(targetfeatureID);
			
			if(!feat.equals(FeatureValue.UNKNOWN_VALUE)){
				vertex.SetGoldLabel(feat.getStringValue(), 1.0);
				vertex.SetInjectedLabelScore(feat.getStringValue(), 1.0);
				vertex.SetSeedNode();
			}
		}

		// Add all of the GAIA graph edges to the Junto graph
		Iterator<Edge> edge_it = edgesid==null ? graph.getEdges() : graph.getEdges(edgesid);
		Edge edge = null;
		while(edge_it.hasNext()){
			edge = edge_it.next();
			if (edge.numNodes() > 2){
				throw new UnsupportedTypeException("Only binary edges supported: "+edge
						+" has "+edge.numNodes()+" nodes");
			}
			
			if(edge instanceof UndirectedEdge){
				node_it = edge.getAllNodes();
				Node n1 = node_it.next();
				Node n2 = node_it.hasNext() ? node_it.next() : n1;
				
				if(!n1.getSchemaID().equals(targetschemaID) || !n2.getSchemaID().equals(targetschemaID)) {
					throw new InvalidStateException("Edge has nodes which are not of the specified schema: "+
							edge+" has nodes "+n1+" and "+n2);
				}

				junto_g.GetVertex(n1.getID().toString()).AddNeighbor(n2.getID().toString(), undirweight);
				junto_g.GetVertex(n2.getID().toString()).AddNeighbor(n1.getID().toString(), undirweight);
			} else if(edge instanceof DirectedEdge){
				Node source = ((DirectedEdge) edge).getSourceNodes().next();
				Node target = ((DirectedEdge) edge).getTargetNodes().next();
				
				if(!source.getSchemaID().equals(targetschemaID) || !target.getSchemaID().equals(targetschemaID)) {
					throw new InvalidStateException("Edge has nodes which are not of the specified schema: "+
							edge+" has nodes "+source+" and "+target);
				}
				
				junto_g.GetVertex(source.getID().toString()).AddNeighbor(target.getID().toString(), sourcetargetweight);
				junto_g.GetVertex(target.getID().toString()).AddNeighbor(source.getID().toString(), targetsourceweight);
			} else {
				throw new UnsupportedTypeException("Unsupported edge type: "
						+edge.getClass().getCanonicalName());
			}
		}

		return junto_g;
	}

	public Graph importGraph(upenn.junto.graph.Graph g) {
		String graphclass = DataGraph.class.getCanonicalName();
		if(this.hasParameter("graphclass")){
			graphclass = this.getStringParameter("graphclass");
		}

		// Create Graph
		String schemaID = this.getStringParameter("graphsid", "juntograph");
		String objID = this.getStringParameter("graphobjid","junto1");
		GraphID id = new GraphID(schemaID, objID);
		Class<?>[] argsClass = new Class[]{GraphID.class};
		Object[] argValues = new Object[]{id};
		
		Graph ourg = (Graph) Dynamic.forConfigurableName(Graph.class,
				graphclass, argsClass, argValues);
		ourg.copyParameters(this);
		
		String nodesid = this.getStringParameter("nodesid","juntonode");
		String edgesid = this.getStringParameter("edgesid","juntoedge");
		String labelfid = this.getStringParameter("labelfid",null);

		ourg.addSchema(nodesid, new Schema(SchemaType.NODE));
		ourg.addSchema(edgesid, new Schema(SchemaType.DIRECTED));
		
		List<String> categories = null;
		Collection<Vertex> vertices = g._vertices.values();
		for(Vertex v : vertices) {
			Node node = ourg.addNode(new GraphItemID(nodesid, v.GetName()));
			
			// Add label from Junto, if requested
			if(labelfid!=null) {
				if(categories==null) {
					String[] cats = v.GetEstimatedLabelScores().keys(new String[0]);
					categories = new ArrayList<String>(cats.length);
					for(String cat:cats) {
						categories.add(cat);
					}
					
					// Add categorical feature
					Schema schema = ourg.getSchema(nodesid);
					schema.addFeature(labelfid, new ExplicitCateg(categories));
					ourg.updateSchema(nodesid, schema);
				}
				
				// Get probabilities from Junto
				double[] probs = new double[categories.size()];
				for(int i=0; i<categories.size(); i++){
					probs[i] = v.GetEstimatedLabelScore(categories.get(i));
				}
				
				FeatureValue fv = new CategValue(categories.get(ArrayUtils.maxValueIndex(probs)), probs);
				node.setFeatureValue(labelfid, fv);
			}
		}

		for(Vertex v : vertices) {
			String[] neighbors = (String[]) v.GetNeighborNames();
			for(int i = 0; i < neighbors.length; i++){
				Vertex n_vertex = g._vertices.get(neighbors[i]);
				
				// Directed Edge needs to be added
				Node n1 = ourg.getNode(new GraphItemID(nodesid,v.GetName()));
				Node n2 = ourg.getNode(new GraphItemID(nodesid,n_vertex.GetName()));
				String edge_id = v.GetName()+n_vertex.GetName()+n_vertex.GetName()+v.GetName();
				ourg.addDirectedEdge(new GraphItemID(edgesid, edge_id), n1, n2);
			}
		}

		return ourg;
	}
	
	/**
	 * Set labels onto Graph g from Junto golden labels.
	 * Assumes the Junto graph was exported from the specified
	 * GAIA graph using {@link #exportGraph(Graph)}
	 * 
	 * @param g GAIA Graph object to copy labels to
	 * @param junto Junto Graph object to copy labels from
	 * @param targetschemaID Schema ID of nodes
	 * @param targetfeatureID Feature ID of nodes
	 */
	public static void overwriteGraph(Graph g, upenn.junto.graph.Graph junto,
			String targetschemaID, String targetfeatureID){
		
		Schema schema = g.getSchema(targetschemaID);
		CategFeature cf = (CategFeature) schema.getFeature(targetfeatureID);
		UnmodifiableList<String> categories = cf.getAllCategories();
		int numcats = categories.size();

		// Iterate through all GAIA graph nodes and apply new labels from Junto graph vertexes
		Iterator<Node> node_it = g.getNodes(targetschemaID);
		Node node;
		Vertex vertex = null;
		while(node_it.hasNext()){
			node = node_it.next();
			vertex = junto.GetVertex(node.getID().toString());
			if(node.getSchemaID().equals(targetschemaID)
					&& node.getFeatureValue(targetfeatureID).equals(FeatureValue.UNKNOWN_VALUE)){
				
				// Get probabilities from Junto
				double[] probs = new double[categories.size()];
				for(int i=0; i<numcats; i++){
					probs[i] = vertex.GetEstimatedLabelScore(categories.get(i));
				}
				
				FeatureValue fv = new CategValue(categories.get(ArrayUtils.maxValueIndex(probs)), probs);
				
				node.setFeatureValue(targetfeatureID, fv);
			}
		}
	}
}