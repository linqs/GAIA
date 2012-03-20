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
package linqs.gaia.model.er;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.SystemDataManager;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

/**
 * For use as a baseline.  It's a "perfect" entity resolver.
 * The model is able to perfectly resolve nodes by loading the ground truth.
 * The ground truth is made available to this baseline in one of two ways.
 * The first way is by storing a value in the graph system data
 * (available through the interface {@link SystemDataManager})
 * keyed by the ID of the node.  All nodes with the same returned value
 * from system data is predicted as co-referent.
 * The alternate way to load the ground truth is to load a graph
 * which has the ground truth in the form of "refers-to"
 * edges from the references to an entity node.
 * References with "refers-to" edges to a node in common are predicted co-referent.
 * The graph is either loaded from the {@link GraphRegistry} keyed
 * by its Graph ID or via an {@link IO} object.
 * <p>
 * Note: In cases where a reference node is mapped to more than one entity,
 * if at least one entity matches given two references, they belong to the same entity.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> ersdkey-System data key for entity value.
 * The system data is assumed to exist for each node where the string
 * value is a comma delimited list of entity identifiers.
 * Either ersdkey, entitygraphid, or entitygraphio must be specified.
 * Order of precedence, if multiple are specified, is: ersdkey, entitygraphid, entitygraphio.
 * <LI> entitygraphid-String representation of the graph id of the graph where
 * the reference nodes are specified with refers-to edges to their entities.
 * If specified, a graph with this graph ID must be registered in {@link GraphRegistry} or
 * an exception will be thrown. Requires referstosid to be specified.
 * Either ersdkey, entitygraphid, or entitygraphio must be specified.
 * Order of precedence, if multiple are specified, is: ersdkey, entitygraphid, entitygraphio.
 * <LI> entitygraphio-Class for the {@link IO} to instantiate using
 * {@link Dynamic#forConfigurableName} for loading a graph with
 * entity resolution annotations.  Requires referstosid to be specified.
 * Either ersdkey, entitygraphid, or entitygraphio must be specified.
 * Order of precedence, if multiple are specified, is: ersdkey, entitygraphid, entitygraphio.
 * <LI> referstosid-The schema id of the "refers-to" edges in entity graph.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class EROracle extends BaseConfigurable implements EntityResolution {
	private static final long serialVersionUID = 1L;
	private String edgeschemaid;
	
	private String ersdkey;
	private GraphID entitygraphid;
	private String referstosid;
	private String refschemaid;
	private boolean initialize = true;
	private String entitygraphio;
	
	public void learn(Graph graph, String refschemaid, String edgeschemaid,
			PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
	}
	
	public void learn(Graph graph, String edgeschemaid, String entityschemaid, String refschemaid,
			String referstoschemaid, PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
	}
	
	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String refschemaid, String existfeature) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
	}
	
	private void initialize(String refschemaid, String edgeschemaid) {
		this.initialize = false;
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
		
		if(this.hasParameter("ersdkey")) {
			this.ersdkey = this.getStringParameter("ersdkey");
		} else if(this.hasParameter("entitygraphid")) {
			this.entitygraphid = GraphID.parseGraphID(this.getStringParameter("entitygraphid"));
			this.referstosid = this.getStringParameter("referstosid");
		} else if(this.hasParameter("entitygraphio")) {
			entitygraphio = this.getStringParameter("entitygraphio");
			this.referstosid = this.getStringParameter("referstosid");
		}
	}

	public void predictAsLink(Graph graph, PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		Graph entitygraph = null;
		if(entitygraphid!=null) {
			entitygraph = GraphRegistry.getGraph(entitygraphid);
		} else if(entitygraphio!=null) {
			IO io = (IO) Dynamic.forConfigurableName(IO.class,
					this.getStringParameter("entitygraphio"),
					this);
			entitygraph = io.loadGraph();
		}
		
		// Go through all positive edges.  Remove those which are not between the same entity.
		Iterator<Edge> eitr = generator.getLinksIteratively(graph, edgeschemaid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			Iterator<Node> nitr = e.getAllNodes();
			
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			// Check to see if one of the comma delimited entity strings
			// match for both nodes
			List<String> n1eids = this.getEntityIDs(graph, entitygraph, n1);
			List<String> n2eids = this.getEntityIDs(graph, entitygraph, n2);
			
			boolean issameentity = false;
			for(String n1eid:n1eids) {
				for(String n2eid:n2eids) {
					if(n1eid.equals(n2eid)) {
						issameentity = true;
						break;
					}
				}
				
				if(issameentity) {
					break;
				}
			}
			
			// If not the same entity, remove edge
			if(!issameentity) {
				graph.removeEdge(e);
			}
		}
	}
	
	public void predictAsLink(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		// Add schema, if not already defined
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Add existence feature, if not already defined
		Schema schema = graph.getSchema(this.edgeschemaid);
		if(!schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(this.edgeschemaid, schema);
		}
		
		Graph entitygraph = null;
		if(entitygraphid!=null) {
			entitygraph = GraphRegistry.getGraph(entitygraphid);
		} else if(entitygraphio!=null) {
			IO io = (IO) Dynamic.forConfigurableName(IO.class,
					this.getStringParameter("entitygraphio"),
					this);
			entitygraph = io.loadGraph();
		}
		
		// Go through all positive edges.  Remove those which are not between the same entity.
		Iterator<Edge> eitr = generator.getLinksIteratively(graph, edgeschemaid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			Iterator<Node> nitr = e.getAllNodes();
			
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			// Check to see if one of the comma delimited entity strings
			// match for both nodes
			List<String> n1eids = this.getEntityIDs(graph, entitygraph, n1);
			List<String> n2eids = this.getEntityIDs(graph, entitygraph, n2);
			
			boolean issameentity = false;
			for(String n1eid:n1eids) {
				for(String n2eid:n2eids) {
					if(n1eid.equals(n2eid)) {
						issameentity = true;
						break;
					}
				}
				
				if(issameentity) {
					break;
				}
			}
			
			// If not the same entity, remove edge
			if(issameentity) {
				e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
			} else {
				if(removenotexist) {
					graph.removeEdge(e);
				} else {
					e.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
				}
			}
		}
	}
	
	public void predictAsLink(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		// Add schema, if not already defined
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Add existence feature, if not already defined
		Schema schema = graph.getSchema(this.edgeschemaid);
		if(!schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(this.edgeschemaid, schema);
		}
		
		Graph entitygraph = null;
		if(entitygraphid!=null) {
			entitygraph = GraphRegistry.getGraph(entitygraphid);
		} else if(entitygraphio!=null) {
			IO io = (IO) Dynamic.forConfigurableName(IO.class,
					this.getStringParameter("entitygraphio"),
					this);
			entitygraph = io.loadGraph();
		}
		
		// Go through all positive edges.  Remove those which are not between the same entity.
		for(Edge e:unknownedges) {
			Iterator<Node> nitr = e.getAllNodes();
			
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			// Check to see if one of the comma delimited entity strings
			// match for both nodes
			List<String> n1eids = this.getEntityIDs(graph, entitygraph, n1);
			List<String> n2eids = this.getEntityIDs(graph, entitygraph, n2);
			
			boolean issameentity = false;
			for(String n1eid:n1eids) {
				for(String n2eid:n2eids) {
					if(n1eid.equals(n2eid)) {
						issameentity = true;
						break;
					}
				}
				
				if(issameentity) {
					break;
				}
			}
			
			// If not the same entity, remove edge
			if(issameentity) {
				e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
			} else {
				if(removenotexist) {
					graph.removeEdge(e);
				} else {
					e.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
				}
			}
		}		
	}
	
	private List<String> getEntityIDs(Graph graph, Graph entitygraph, Node n) {
		List<String> eids = null;
		if(ersdkey!=null) {
			// Assume that the string representation of the entity is in system data
			eids = Arrays.asList(graph.getSystemData(n.getID(), this.ersdkey).split(","));
		} else if(entitygraph!=null) {
			// Get the entity information from the entity graph
			Node equivn = (Node) entitygraph.getEquivalentGraphItem(n.getID());
			Iterator<Node> nitr = equivn.getAdjacentTargets(referstosid);
			eids = new ArrayList<String>();
			while(nitr.hasNext()) {
				eids.add(nitr.next().getID().toString());
			}
			
			if(eids.isEmpty()) {
				throw new InvalidStateException("All entities must have at least one reference");
			}
		} else {
			throw new ConfigurationException("Neither an ersdkey, entitygraphid, or entitygraphio was provided");
		}
		
		return eids;
	}
	
	public void predictAsNode(Graph graph, PotentialLinkGenerator generator,
			String entitysid, String referstosid) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		this.predictAsLink(graph, generator);
		ERUtils.addEntityNodesFromCoRef(graph, graph, edgeschemaid, entitysid, refschemaid, referstosid, null, true);
	}
	
	public void predictAsNode(Graph refgraph, Graph entitygraph, PotentialLinkGenerator generator,
			String entitysid, String reffeatureid) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		this.predictAsLink(refgraph, generator);
		ERUtils.addEntityNodesFromCoRef(refgraph, entitygraph, edgeschemaid, entitysid, refschemaid, null, reffeatureid, true);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		String refschemaid = this.getStringParameter("saved-refschemaid");
		this.initialize(refschemaid, edgeschemaid);
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.setParameter("saved-refschemaid", this.refschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
