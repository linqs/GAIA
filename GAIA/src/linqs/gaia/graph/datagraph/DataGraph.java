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
package linqs.gaia.graph.datagraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitFeature;
import linqs.gaia.graph.event.EdgeAddedEvent;
import linqs.gaia.graph.event.EdgeRemovedEvent;
import linqs.gaia.graph.event.FeatureSetEvent;
import linqs.gaia.graph.event.GraphEvent;
import linqs.gaia.graph.event.GraphEventListener;
import linqs.gaia.graph.event.NodeAddedEvent;
import linqs.gaia.graph.event.NodeRemovedEvent;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SimplePair;

/**
 * An in memory implementation of Graph interface.  An object
 * is created for all nodes and edges, DGNode, DGDirected, and DGUndirected,
 * and are stored in memory.  The schema, features, and feature values
 * are also stored in memory.  In this implementation, a wrapper is built
 * around all explicit features such that each of the stored features,
 * DGFeature, store the feature values using the Decorable object
 * as key.
 * <p>
 * Note:
 * <UL>
 * <LI> This Graph implementation supports closed values for saving space.  Use
 * them as often as possible when dealing with medium to large datasets.
 * </UL>
 * 
 * 
 * @author namatag
 *
 */
public class DataGraph extends DGSchemaManager implements Graph {
	private static final long serialVersionUID = 1L;
	private GraphID id;
	private Integer internalid;
	private int internalidcounter = 0;
	
	/**
	 * Get an internal id.  This is unique to everything in this
	 * graph.
	 * 
	 * @return Internal id
	 */
	protected int getInternalID() {
		return internalidcounter++;
	}
	
	/**
	 * Store all the graph events listeners for the graph
	 */
	private Collection <GraphEventListener> eventlisteners;
	
	/**
	 * Map of all nodes keyed first by schema ID, then by object ID
	 */
	private Map<String,Map<String, Node>> id2node;
	
	/**
	 * Map of all edges keyed first by schema ID, then by object ID
	 */
	private Map<String,Map<String, Edge>> id2edge;
	
	/**
	 * Object which implements the system data manager
	 */
	DGSystemDataManager sdmanager = new DGSystemDataManager();
	
	/**
	 * Constructor
	 * <p>
	 * Note: A graph schema is automatically added upon construction.
	 * 
	 * @param id Graph ID
	 */
	public DataGraph(GraphID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph ID cannot be null");
		}
		
		// Automatically add a graph schema for the given schema id
		this.addSchema(id.getSchemaID(), new Schema(SchemaType.GRAPH));
		
		this.id = id;
		this.internalid = this.getInternalID();
		
		eventlisteners = new ArrayList<GraphEventListener>();
		id2node = new ConcurrentHashMap<String,Map<String, Node>> ();
		id2edge = new ConcurrentHashMap<String,Map<String, Edge>> ();
	}
	
	public Node addNode(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding a node with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		if(this.hasNode(id)){
			throw new InvalidOperationException("Node with the given id already exists: id="+id);
		}
		
		if(!this.hasSchema(id.getSchemaID())) {
			throw new InvalidOperationException("Adding a node with an undefined schema: "
					+id+" with schema id "+id.getSchemaID());
		}
		
		SchemaType schematype = this.getSchema(id.getSchemaID()).getType();
		if(!schematype.equals(SchemaType.NODE)) {
			throw new InvalidOperationException("Specified schema id is not a node schema: "
					+id+" with schema type "+schematype.name());
		}
		
		Node n = new DGNode(this, id);
		if(!this.id2node.containsKey(n.getSchemaID())){
			throw new InvalidStateException("id2node should have been initialized already");
		}
		
		this.id2node.get(id.getSchemaID()).put(id.getObjID(), n);
		
		this.processListeners(new NodeAddedEvent(n));
		
		return n;
	}
	
	/**
	 * Add edge to DGGraph
	 * 
	 * @param e Edge
	 */
	private void addEdge(Edge e){
		if(!this.id2edge.containsKey(e.getSchemaID())){
			throw new InvalidStateException("id2edge should have been initialized already");
		}
		
		this.id2edge.get(e.getSchemaID()).put(e.getID().getObjID(), e);
		
		this.processListeners(new EdgeAddedEvent(e));
	}
	
	public DirectedEdge addDirectedEdge(GraphItemID id, Iterator<Node> sources, Iterator<Node> targets) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding an edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		if(this.hasEdge(id)){
			throw new InvalidOperationException("Edge with the given id already exists: id="+id);
		}
		
		if(!this.hasSchema(id.getSchemaID())) {
			throw new InvalidOperationException("Adding an edge with an undefined schema: "+id);
		}
		
		SchemaType idschematype = this.getSchema(id.getSchemaID()).getType();
		if(!idschematype.equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Specified schema id is not a directed edge schema: "
					+id+" with schema type "+idschematype.name());
		}
		
		DGDirected edge = new DGDirected(this, id);
		while(sources.hasNext()){
			// Add node to edge and notify node of the new edge
			edge.addSourceNode(sources.next());
		}
		
		while(targets.hasNext()){
			// Add node to edge and notify node of the new edge
			edge.addTargetNode(targets.next());
		}
		
		edge.checkValidity("Attempting to add invalid edge: "+edge);
		
		this.addEdge(edge);
		
		return edge;
	}
	
	public DirectedEdge addDirectedEdge(GraphItemID id, Iterable<Node> sources, Iterable<Node> targets) {
		return addDirectedEdge(id, sources.iterator(), targets.iterator());
	}
	
	public DirectedEdge addDirectedEdge(GraphItemID id, Node source, Node target) {
		if(source == null) {
			throw new InvalidOperationException("Source node is null for edge "+id);
		}
		
		if(target == null) {
			throw new InvalidOperationException("Target node is null for edge "+id);
		}
		
		return addDirectedEdge(id,
				Arrays.asList(new Node[]{source}).iterator(),
				Arrays.asList(new Node[]{target}).iterator());
	}

	public UndirectedEdge addUndirectedEdge(GraphItemID id, Iterator<Node> nodes) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!id.getGraphID().equals(this.getID())) {
			throw new InvalidOperationException("Adding an edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		if(this.hasEdge(id)){
			throw new InvalidOperationException("Edge with the given id already exists: id="+id);
		}
		
		if(!this.hasSchema(id.getSchemaID())) {
			throw new InvalidOperationException("Adding an edge with an undefined schema: "+id);
		}
		
		SchemaType idschematype = this.getSchema(id.getSchemaID()).getType();
		if(!idschematype.equals(SchemaType.UNDIRECTED)) {
			throw new InvalidOperationException("Specified schema id is not an udirected edge schema: "
					+id+" with schema type "+idschematype.name());
		}
		
		DGUndirected edge = new DGUndirected(this, id);
		while(nodes.hasNext()){
			// Add node to edge and notify node of the new edge
			edge.addNode(nodes.next());
		}
		
		edge.checkValidity("Attempting to add invalid edge: "+edge);
		
		this.addEdge(edge);
		
		return edge;
	}
	
	public UndirectedEdge addUndirectedEdge(GraphItemID id, Iterable<Node> nodes) {
		return addUndirectedEdge(id, nodes.iterator());
	}
	
	public UndirectedEdge addUndirectedEdge(GraphItemID id, Node node1, Node node2) {
		if(node1 == null) {
			throw new InvalidOperationException("First node is null for edge "+id);
		}
		
		if(node2 == null) {
			throw new InvalidOperationException("Second node is null for edge "+id);
		}
		
		return addUndirectedEdge(id, Arrays.asList(new Node[]{node1,node2}));
	}
	
	public UndirectedEdge addUndirectedEdge(GraphItemID id, Node node) {
		if(node == null) {
			throw new InvalidOperationException("First node is null for edge "+id);
		}
		
		return addUndirectedEdge(id, Arrays.asList(new Node[]{node}));
	}

	public Edge getEdge(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		String objid = id.getObjID();
		GraphID gid = id.getGraphID();
		
		if((gid!=null && !gid.equals(this.getID())) || !this.id2edge.containsKey(schemaid)) {
			return null;
		}
		
		return id2edge.get(schemaid).get(objid);
	}

	private Collection<Edge> getEdgeCollections() {
		Set<Edge> all = new HashSet<Edge>();
		Set<String> schemas = this.id2edge.keySet();
		for(String sid:schemas){
			Collection<Edge> edges = new LinkedList<Edge>(this.id2edge.get(sid).values());
			all.addAll(edges);
		}
		
		return all;
	}
	
	public Iterator<Edge> getEdges() {
		return this.getEdgeCollections().iterator();
	}
	
	public Iterator<Edge> getEdges(String schemaID) {
		if(!this.hasSchema(schemaID) ||
			(this.getSchema(schemaID).getType() != SchemaType.DIRECTED && 
			this.getSchema(schemaID).getType() != SchemaType.UNDIRECTED )) {
			throw new InvalidOperationException("No edges defined with schema ID="+schemaID);
		}
		
		return this.id2edge.get(schemaID).values().iterator();
	}
	
	public Iterable<Edge> getIterableEdges() {
		return new EdgeIterable(this);
	}
	
	public Iterable<Edge> getIterableEdges(String schemaID) {
		return new EdgeIterable(this, schemaID);
	}
	
	/**
	 * Helper class for use with getIterableEdges
	 * 
	 * @author namatag
	 */
	private static class EdgeIterable implements Iterable<Edge> {
		private Graph g = null;
		private String schemaID = null;
		
		public EdgeIterable(Graph g) {
			this.g = g;
		}
		
		public EdgeIterable(Graph g, String schemaID) {
			this.g = g;
			this.schemaID = schemaID;
		}

		public Iterator<Edge> iterator() {
			if(schemaID != null) {
				return g.getEdges(schemaID);
			} else {
				return g.getEdges();
			}
		}
	}

	public Iterator<GraphItem> getGraphItems(String schemaID) {
		List<GraphItem> gitems = new ArrayList<GraphItem>();
		if(this.id2node.containsKey(schemaID)){
			gitems.addAll(this.id2node.get(schemaID).values());
		} else if(this.id2edge.containsKey(schemaID)){
			gitems.addAll(this.id2edge.get(schemaID).values());
		} else {
			throw new InvalidOperationException("No graph items defined with schema ID="+schemaID);
		}
		
		return gitems.iterator();
	}

	public Iterator<GraphItem> getGraphItems() {
		List<GraphItem> allgis = new ArrayList<GraphItem>(this.numEdges()+this.numNodes());
		Iterator<Node> nodes = this.getNodes();
		while(nodes.hasNext()){
			allgis.add(nodes.next());
		}
		
		Iterator<Edge> edges = this.getEdges();
		while(edges.hasNext()){
			allgis.add(edges.next());
		}
		
		return allgis.iterator();
	}
	
	public Iterable<GraphItem> getIterableGraphItems(String schemaID) {
		return new GraphItemIterable(this, schemaID);
	}
	
	public Iterable<GraphItem> getIterableGraphItems() {
		return new GraphItemIterable(this);
	}
	
	/**
	 * Helper class for use with getIterableGraphItems
	 * 
	 * @author namatag
	 *
	 */
	private static class GraphItemIterable implements Iterable<GraphItem> {
		private Graph g = null;
		private String schemaID = null;
		
		public GraphItemIterable(Graph g) {
			this.g = g;
		}
		
		public GraphItemIterable(Graph g, String schemaID) {
			this.g = g;
			this.schemaID = schemaID;
		}

		public Iterator<GraphItem> iterator() {
			if(schemaID != null) {
				return g.getGraphItems(schemaID);
			} else {
				return g.getGraphItems();
			}
		}
	}
	
	public int numGraphItems(String schemaID) {
		if(this.id2node.containsKey(schemaID)){
			return this.id2node.get(schemaID).values().size();
		} else if(this.id2edge.containsKey(schemaID)){
			return this.id2edge.get(schemaID).values().size();
		} else {
			if(!this.hasSchema(schemaID)) {
				throw new InvalidOperationException("No graph items defined with schema ID="+schemaID);
			}
		}
		
		return -1;
	}

	public Node getNode(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		String objid = id.getObjID();
		GraphID gid = id.getGraphID();
		
		if((gid!=null && !gid.equals(this.getID())) || !this.id2node.containsKey(schemaid)) {
			return null;
		}
		
		return id2node.get(schemaid).get(objid);
	}

	private Collection<Node> getNodesCollection(){
		List<Node> all = new ArrayList<Node>();
		Set<String> schemas = this.id2node.keySet();
		for(String sid:schemas){
			Collection<Node> nodes = new LinkedList<Node>(this.id2node.get(sid).values());
			all.addAll(nodes);
		}
		
		return all;
	}
	
	public Iterator<Node> getNodes() {
		return this.getNodesCollection().iterator();
	}
	
	public Iterator<Node> getNodes(String schemaID) {
		if(!this.hasSchema(schemaID) || this.getSchema(schemaID).getType() != SchemaType.NODE) {
			throw new InvalidOperationException("No nodes defined with schema ID="+schemaID);
		}
		
		return this.id2node.get(schemaID).values().iterator();
	}
	
	public Iterable<Node> getIterableNodes() {
		return new NodeIterable(this);
	}
	
	public Iterable<Node> getIterableNodes(String schemaID) {
		return new NodeIterable(this, schemaID);
	}
	
	/**
	 * Helper class for use with getIterableNodes
	 * 
	 * @author namatag
	 */
	private static class NodeIterable implements Iterable<Node> {
		private Graph g = null;
		private String schemaID = null;
		
		public NodeIterable(Graph g) {
			this.g = g;
		}
		
		public NodeIterable(Graph g, String schemaID) {
			this.g = g;
			this.schemaID = schemaID;
		}

		public Iterator<Node> iterator() {
			if(schemaID != null) {
				return g.getNodes(schemaID);
			} else {
				return g.getNodes();
			}
		}
	}

	public boolean hasEdge(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		String objid = id.getObjID();
		
		GraphID gid = id.getGraphID();
		if(gid!=null && !gid.equals(this.getID())) {
			return false;
		}
		
		return this.id2edge.containsKey(schemaid)
			&& this.id2edge.get(schemaid).containsKey(objid);
	}

	public boolean hasNode(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		String objid = id.getObjID();
		
		GraphID gid = id.getGraphID();
		if(gid!=null && !gid.equals(this.getID())) {
			return false;
		}
		
		return this.id2node.containsKey(schemaid)
			&& this.id2node.get(schemaid).containsKey(objid);
	}

	public int numEdges() {
		return this.getEdgeCollections().size();
	}

	public int numNodes() {
		return this.getNodesCollection().size();
	}

	public void removeAllEdges() {
		List<Edge> edges = new LinkedList<Edge>(this.getEdgeCollections());
		
		for(Edge e:edges){
			this.removeEdge(e);
		}
	}

	public void removeAllNodes() {
		List<Node> nodes = new LinkedList<Node>(this.getNodesCollection());
		
		for(Node n:nodes){
			this.removeNode(n);
		}
	}
	
	public void removeNodesWithEdges(String schemaID) {
		List<Node> nodes = IteratorUtils.iterator2nodelist(this.getNodes(schemaID));
		
		for(Node n:nodes){
			this.removeNodeWithEdges(n);
		}
	}
	
	public void removeAllGraphItems(String schemaID) {
		if(this.id2node.containsKey(schemaID)){
			Collection<Node> nodes = this.id2node.get(schemaID).values();
			for(Node n:nodes) {
				this.removeNode(n);
			}
		} else if(this.id2edge.containsKey(schemaID)){
			Collection<Edge> edges = this.id2edge.get(schemaID).values();
			for(Edge e:edges) {
				this.removeEdge(e);
			}
		} else {
			if(!this.hasSchema(schemaID)) {
				throw new InvalidOperationException("No graph items defined with schema ID="+schemaID);
			}
		}
	}

	public void removeEdge(Edge e) {
		this.removeEdge((GraphItemID) e.getID());
	}

	public void removeEdge(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.getID())) {
			throw new InvalidOperationException("Removing an edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		Edge e = this.getEdge(id);
		
		if(e == null) {
			throw new InvalidOperationException("No edge defined with ID="+id);
		}
		
		// Remove feature values for item before removing it
		this.removeFeatureValues(e);
		
		// Notify affected nodes
		Iterator<Node> nodes = e.getAllNodes();
		while(nodes.hasNext()) {
			DGNode n = (DGNode) nodes.next();
			n.edgeRemovedNotification(e);
		}
		
		// Remove edge from graph
		this.id2edge.get(id.getSchemaID()).remove(id.getObjID());
		
		// Create edge removed event
		this.processListeners(new EdgeRemovedEvent(e));
	}
	
	public void removeNode(Node n) {
		this.removeNode((GraphItemID) n.getID());
	}

	public void removeNode(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphID gid = id.getGraphID();
		if(gid==null) {
			id = new GraphItemID(this.getID(), id.getSchemaID(), id.getObjID());
		} else if(!gid.equals(this.getID())) {
			throw new InvalidOperationException("Removing an edge with wrong Graph ID: "
					+id.getGraphID()
					+" not "
					+this.getID());
		}
		
		Node n = this.getNode(id);
		if(n == null) {
			throw new InvalidOperationException("No node defined with ID="+id);
		}
		
		// Remove feature for the given node
		this.removeFeatureValues(n);
		
		// Notify affected edges
		Iterator<Edge> edges = n.getAllEdges();
		while(edges.hasNext()) {
			// Remove edges made invalid by the removal of this node
			DGEdge e = (DGEdge) edges.next();
			e.nodeRemovedNotification(n);
			
			if(!e.isValid()) {
				Log.WARN("Automatically Removing Edge "+e+" with the removal of "+n);
				this.removeEdge(e);
			}
		}
		
		// Remove node from graph 
		this.id2node.get(id.getSchemaID()).remove(id.getObjID());
		
		// Apply graph event
		this.processListeners(new NodeRemovedEvent(n));
	}
	
	public void removeNodeWithEdges(Node n) {
		if(n==null) {
			throw new InvalidOperationException("Node cannot be null");
		}
		
		n.removeIncidentEdges();
		this.removeNode(n);
	}

	public void removeNodeWithEdges(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		Node n = this.getNode(id);
		this.removeNodeWithEdges(n);
	}
	
	public void removeGraphItem(GraphItem gi) {
		if(gi instanceof Node) {
			this.removeNode((Node) gi);
		} else if(gi instanceof Edge) {
			this.removeEdge((Edge) gi);
		} else {
			throw new UnsupportedTypeException("Unsupported Graph Item Type: "
					+gi.getClass().getCanonicalName());
		}
	}

	public void removeGraphItem(GraphItemID id) {
		GraphItem gi = this.getGraphItem(id);
		this.removeGraphItem(gi);
	}
	
	private void removeFeatureValues(Decorable d) {
		Schema schema = d.getSchema();
		Iterator<SimplePair<String,Feature>> fitr = schema.getAllFeatures();
		while(fitr.hasNext()) {
			SimplePair<String,Feature> pair = fitr.next();
			String fid = pair.getFirst();
			Feature f = pair.getSecond();
			
			if(f instanceof ExplicitFeature) {
				// Remove all explicitly defined values
				d.setFeatureValue(fid, FeatureValue.UNKNOWN_VALUE);
			} else if(f instanceof DerivedFeature) {
				// If caching, remove the cached value from
				// the cache of the derived feature
				if(((DerivedFeature) f).isCaching()) {
					((DerivedFeature) f).resetCache(d);
				}
			}
		}
	}
	
	public FeatureValue getFeatureValue(String featureid) {		
		Feature f = this.getInternalSchema(this.getSchemaID()).getFeature(featureid);
		FeatureValue value = null;
		
		if(f instanceof ExplicitFeature) {
			DGExplicitFeature dgf = (DGExplicitFeature) f;
			value = dgf.getFeatureValue(this.internalid);
		} else if(f instanceof DerivedFeature) {
			value = ((DerivedFeature) f).getFeatureValue(this);
		}
		
		return value;
	}
	
	public List<FeatureValue> getFeatureValues(List<String> featureids) {
		if(featureids==null) {
			throw new InvalidStateException("List of feature ids cannot be null");
		}
		
		List<FeatureValue> values = new ArrayList<FeatureValue>();
		
		for(String fid:featureids) {
			values.add(this.getFeatureValue(fid));
		}
		
		return values;
	}
	
	public boolean hasFeatureValue(String featureid) {		
		return !this.getFeatureValue(featureid).equals(FeatureValue.UNKNOWN_VALUE);
	}

	public Schema getSchema() {
		return this.getSchema(this.id.getSchemaID());
	}

	public String getSchemaID() {
		return this.id.getSchemaID();
	}

	public void setFeatureValue(String featureid, FeatureValue value) {
		Feature f = this.getInternalSchema(this.getSchemaID()).getFeature(featureid);
		
		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "+f);
		}
		
		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, value);
		
		this.processListeners(new FeatureSetEvent(this, featureid, previous, value));
	}
	
	public void setFeatureValue(String featureid, String value) {
		Schema schema = this.getInternalSchema(id.getSchemaID());

		if(!schema.hasFeature(featureid)) {
			throw new InvalidOperationException("Feature "+featureid+" not defined for "+this.getSchemaID());
		}

		Feature f = schema.getFeature(featureid);

		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		FeatureValue newvalue = null;
		if(f instanceof StringFeature) {
			newvalue = new StringValue(value);
		} else if(f instanceof NumFeature) {
			newvalue = new NumValue(Double.parseDouble(value));
		} else if(f instanceof CategFeature) {
			newvalue = new CategValue(value);
		} else if(f instanceof MultiCategFeature) {
			Set<String> set = new HashSet<String>(Arrays.asList(value.split(",")));
			newvalue = new MultiCategValue(set);
		} else if(f instanceof MultiIDFeature) {
			String[] ids = value.split(",");
			Set<ID> idset = new HashSet<ID>();
			for(String id:ids) {
				idset.add(ID.parseID(id));
			}
			newvalue = new MultiIDValue(idset);
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, newvalue);

		this.processListeners(new FeatureSetEvent(this, featureid, previous, newvalue));
	}

	public void setFeatureValue(String featureid, double value) {
		Schema schema = this.getInternalSchema(id.getSchemaID());

		if(!schema.hasFeature(featureid)) {
			throw new InvalidOperationException("Feature "+featureid+" not defined for "+this.getSchemaID());
		}

		Feature f = schema.getFeature(featureid);

		if(!(f instanceof ExplicitFeature)) {
			throw new InvalidOperationException("Feature value cannot be set for: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		FeatureValue newvalue = null;
		if(f instanceof NumFeature) {
			newvalue = new NumValue(value);
		} else if(f instanceof StringFeature) {
			newvalue = new StringValue(""+value);
		} else if(f instanceof CategFeature) {
			newvalue = new CategValue(((CategFeature) f).getAllCategories().get((int) value));
		} else {
			throw new UnsupportedTypeException("Unsupported feature type: "
					+featureid+" of type "+f.getClass().getCanonicalName());
		}
		
		FeatureValue previous = this.getFeatureValue(featureid);
		((DGExplicitFeature) f).setFeatureValue(this.internalid, newvalue);

		this.processListeners(new FeatureSetEvent(this, featureid, previous, newvalue));
	}
	
	public void setFeatureValues(List<String> featureids, List<FeatureValue> values) {
		if(featureids==null || values==null) {
			throw new InvalidStateException("Feature ids or values is null:"
					+" featureids="+featureids
					+" values="+values);
		}
		
		if(featureids.size()!=values.size()) {
			throw new InvalidStateException("The number of feature ids and values must match:"
					+" #ids="+featureids.size()
					+" #ofvalues="+values.size());
		}
		
		int size = featureids.size();
		for(int i=0; i<size; i++) {
			this.setFeatureValue(featureids.get(i), values.get(i));
		}
	}
	
	public void removeFeatureValue(String featureid) {
		if(this.getFeatureValue(featureid).equals(FeatureValue.UNKNOWN_VALUE)) {
			throw new InvalidOperationException("Value to remove is already unknown: "+featureid);
		}
		
		this.setFeatureValue(featureid, FeatureValue.UNKNOWN_VALUE);
	}
	
	public GraphID getID() {
		return this.id;
	}
	
	// Implemented here to make sure that the id2node and id2edge features are set.
	public void addSchema(String schemaID, Schema schema) {
		if(this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was previously defined: "+schemaID);
		}
		
		if(!schemaidpattern.matcher(schemaID).matches()) {
			throw new InvalidOperationException("Invalid feature id: "+schemaID);
		}
		
		// Initialize the id2node and id2edge entries for this schema
		SchemaType stype = schema.getType();
		if(stype.equals(SchemaType.NODE)) {
			this.id2node.put(schemaID,
					new ConcurrentHashMap<String, Node>());
		} else if(stype.equals(SchemaType.DIRECTED)
				|| stype.equals(SchemaType.UNDIRECTED)) {
			this.id2edge.put(schemaID,
					new ConcurrentHashMap<String, Edge>());
		} else if(stype.equals(SchemaType.GRAPH)) {
			// No additional processing needed
		} else {
			throw new UnsupportedTypeException("Unsupported schema type: "+schema.getType());
		}
		
		this.setSchema(schemaID, schema, null);
	}
	
	// Implement here to make sure that the id2node and id2edge features are set.
	public void removeSchema(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		// When removing schema, remove all instances with that schema
		SchemaType stype = this.getSchema(schemaID).getType();
		if(stype.equals(SchemaType.GRAPH)) {
			if(this.getSchemaID().equals(schemaID)) {
				throw new InvalidOperationException("Cannot remove graph schema: "+schemaID);
			}
			
			// Note: Remove extra graph schema from graph below
		} else if(stype.equals(SchemaType.NODE)) {
			this.removeAllGraphItems(schemaID);
			this.id2node.remove(schemaID);
		} else if(stype.equals(SchemaType.DIRECTED)
				|| stype.equals(SchemaType.UNDIRECTED)) {
			this.removeAllGraphItems(schemaID);
			this.id2edge.remove(schemaID);
		} else {
			throw new UnsupportedTypeException("Unsupported schema type: "+stype);
		}
		
		this.id2internalschema.remove(schemaID.intern());
		this.id2schema.remove(schemaID.intern());
	}

	public void addListener(GraphEventListener gel) {
		this.eventlisteners.add(gel);
	}

	public void processListeners(GraphEvent event) {
		for(GraphEventListener gel:this.eventlisteners){
			gel.execute(event);
		}
	}

	public void removeAllListeners() {
		this.eventlisteners.clear();
	}

	public void removeListener(GraphEventListener gel) {
		this.eventlisteners.remove(gel);
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
		if(!(obj instanceof Graph)){
			return false;
		}
		
		return this.id.equals(((Graph) obj).getID());
	}
	
	public int hashCode() {
	    return this.id.hashCode();
	}

	public void destroy() {
		// Just clear the maps instead
		// of calling remove on each edge and node
		this.id2edge.clear();
		this.id2node.clear();
		this.removeAllSchemas();
		this.removeAllSystemData();
		
		GraphRegistry.removeGraph(this);
	}
	
	public String toString(){
		return GraphItemUtils.getGraphIDString(this);
	}
	
	public Graph copy(String objid) {
		GraphID copyid = new GraphID(this.getSchemaID(), objid);
		Graph copyg = new DataGraph(copyid);
		GraphUtils.copy(this, copyg);
		
		return copyg;
	}
	
	public void copy(Graph g) {
		GraphUtils.copy(this, g);
	}
	
	public Graph copySchema(String objid) {
		GraphID copyid = new GraphID(this.getSchemaID(), objid);
		Graph copyg = new DataGraph(copyid);
		GraphUtils.copySchema(this, copyg);
		
		return copyg;
	}

	public GraphItem getGraphItem(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		SchemaType type = this.getSchemaType(schemaid);
		
		if(type.equals(SchemaType.NODE)) {
			return this.getNode(id);
		} else if(type.equals(SchemaType.UNDIRECTED)
				|| type.equals(SchemaType.DIRECTED)) {
			return this.getEdge(id);
		} else {
			throw new UnsupportedTypeException("Unsupported Schema Type: "+type);
		}
	}
	
	public GraphItem getEquivalentGraphItem(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		GraphItemID equivid = new GraphItemID(id.getSchemaID(), id.getObjID());
		
		return this.getGraphItem(equivid);
	}
	
	public GraphItem getEquivalentGraphItem(GraphItem gi) {
		if(gi==null) {
			throw new InvalidOperationException("Graph Item cannot be null");
		}
		
		GraphItemID id = gi.getID();
		GraphItemID equivid = new GraphItemID(id.getSchemaID(), id.getObjID());
		
		return this.getGraphItem(equivid);
	}

	public boolean hasGraphItem(GraphItemID id) {
		if(id==null) {
			throw new InvalidOperationException("Graph Item ID cannot be null");
		}
		
		String schemaid = id.getSchemaID();
		SchemaType type = this.getSchemaType(schemaid);
		
		if(type.equals(SchemaType.NODE)) {
			return this.hasNode(id);
		} else if(type.equals(SchemaType.DIRECTED) 
				|| type.equals(SchemaType.UNDIRECTED)) {
			return this.hasEdge(id);
		} else {
			throw new UnsupportedTypeException("Unsupported Schema Type: "
					+schemaid+" of type "+type);
		}
	}
	
	public boolean hasEquivalentGraphItem(GraphItemID id) {
		id = new GraphItemID(id.getSchemaID(), id.getObjID());
		
		String schemaid = id.getSchemaID();
		SchemaType type = this.getSchemaType(schemaid);
		
		if(type.equals(SchemaType.NODE)) {
			return this.hasNode(id);
		} else if(type.equals(SchemaType.DIRECTED) 
				|| type.equals(SchemaType.UNDIRECTED)) {
			return this.hasEdge(id);
		} else {
			throw new UnsupportedTypeException("Unsupported Schema Type: "
					+schemaid+" of type "
					+type);
		}
	}
	
	public boolean hasEquivalentGraphItem(GraphItem gi) {
		return this.hasEquivalentGraphItem(gi.getID());
	}

	public void removeAllSystemData() {
		this.sdmanager.removeAllSystemData();
	}

	public void removeSystemData(ID id) {
		this.sdmanager.removeSystemData(id);
	}

	public String getSystemData(String key) {
		return this.sdmanager.getSystemData(key);
	}

	public String getSystemData(ID id, String key) {
		return this.sdmanager.getSystemData(id, key);
	}

	public void setSystemData(String key, String value) {
		this.sdmanager.setSystemData(key, value);
	}

	public void setSystemData(ID id, String key, String value) {
		this.sdmanager.setSystemData(id, key, value);
	}

	public void removeSystemData(String key) {
		this.sdmanager.removeSystemData(key);
	}

	public void removeSystemData(ID id, String key) {
		this.sdmanager.removeSystemData(id, key);
	}

	@Override
	protected String getGraphSchemaID() {
		return this.getSchemaID();
	}
}
