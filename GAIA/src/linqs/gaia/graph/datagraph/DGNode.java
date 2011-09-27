package linqs.gaia.graph.datagraph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedSet;

/**
 * Node implementation of DataGraph.
 * 
 * @see DataGraph
 * 
 * @author namatag
 *
 */
public class DGNode extends DGGraphItem implements Node {
	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 1L;
	
	//private Set<UndirectedEdge> undirected;
	//private Set<DirectedEdge> wheresource;
	//private Set<DirectedEdge> wheretarget;
	private KeyedSet<String,UndirectedEdge> sid2undirected;
	private KeyedSet<String,DirectedEdge> sid2wheresource;
	private KeyedSet<String,DirectedEdge> sid2wheretarget;
	
	public DGNode(DataGraph graph, GraphItemID id){
		super(graph, id);
		
		//this.wheresource = new HashSet<DirectedEdge>(2);
		//this.wheretarget = new HashSet<DirectedEdge>(2);
		//this.undirected = new HashSet<UndirectedEdge>(2);
		this.sid2wheresource = new KeyedSet<String,DirectedEdge>(2,true);
		this.sid2wheretarget = new KeyedSet<String,DirectedEdge>(2,true);
		this.sid2undirected = new KeyedSet<String,UndirectedEdge>(2,true);
	}
	
	public String toString(){
		return GraphItemUtils.getNodeIDString(this);
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets() {
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		all.addAll(this.sid2wheresource.getAllItems());
		all.addAll(this.sid2wheretarget.getAllItems());
		all.addAll(this.sid2undirected.getAllItems());

		return all;
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		
		Set<DirectedEdge> wheresource = this.sid2wheresource.getSet(schemaID);
		if(wheresource!=null) {
			for(Edge e:wheresource){
				all.add(e);
			}
		}
		
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getSet(schemaID);
		if(wheretarget!=null) {
			for(Edge e:wheretarget){
				all.add(e);
			}
		}
		
		Set<UndirectedEdge> undirected = this.sid2undirected.getSet(schemaID);
		if(undirected!=null) {
			for(Edge e:undirected){
				all.add(e);
			}
		}

		return all;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(GraphItem adjacent) {
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		
		Set<DirectedEdge> wheresource = this.sid2wheresource.getAllItems();
		for(Edge e:wheresource){
			if(e.isIncident(adjacent)){
				all.add(e);
			}
		}
		
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getAllItems();
		for(Edge e:wheretarget){
			if(e.isIncident(adjacent)){
				all.add(e);
			}
		}
		
		Set<UndirectedEdge> undirected = this.sid2undirected.getAllItems();
		for(Edge e:undirected){
			if(e.isIncident(adjacent)){
				all.add(e);
			}
		}

		return all;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID, GraphItem adjacent) {
		HashSet<GraphItem> all = new HashSet<GraphItem>();
		
		Set<DirectedEdge> wheresource = this.sid2wheresource.getSet(schemaID);
		if(wheresource!=null) {
			for(Edge e:wheresource){
				if(e.isIncident(adjacent)){
					all.add(e);
				}
			}
		}
		
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getSet(schemaID);
		if(wheretarget!=null) {
			for(Edge e:wheretarget){
				if(e.isIncident(adjacent)){
					all.add(e);
				}
			}
		}
		
		Set<UndirectedEdge> undirected = this.sid2undirected.getSet(schemaID);
		if(undirected!=null) {
			for(Edge e:undirected){
				if(e.isIncident(adjacent)){
					all.add(e);
				}
			}
		}

		return all;
	}

	public Iterator<Edge> getAllEdges() {
		HashSet<Edge> all = new HashSet<Edge>();
		
		Set<DirectedEdge> wheresource = this.sid2wheresource.getAllItems();
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getAllItems();
		Set<UndirectedEdge> undirected = this.sid2undirected.getAllItems();
		
		all.addAll(wheresource);
		all.addAll(wheretarget);
		all.addAll(undirected);

		return all.iterator();
	}

	public Iterator<Edge> getAllEdges(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		HashSet<Edge> all = new HashSet<Edge>();
		
		Set<DirectedEdge> wheresource = this.sid2wheresource.getSet(schemaID);
		if(wheresource!=null) {
			for(Edge e:wheresource){
				all.add(e);
			}
		}
		
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getSet(schemaID);
		if(wheretarget!=null) {
			for(Edge e:wheretarget){
				all.add(e);
			}
		}
		
		Set<UndirectedEdge> undirected = this.sid2undirected.getSet(schemaID);
		if(undirected!=null) {
			for(Edge e:undirected){
				all.add(e);
			}
		}

		return all.iterator();
	}

	public Iterator<DirectedEdge> getEdgesWhereSource() {
		return this.sid2wheresource.getAllItems().iterator();
	}
	
	public Iterator<DirectedEdge> getEdgesWhereSource(String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		return this.sid2wheresource.getSet(edgeschemaID).iterator();
	}

	public Iterator<DirectedEdge> getEdgesWhereTarget() {
		return this.sid2wheretarget.getAllItems().iterator();
	}
	
	public Iterator<DirectedEdge> getEdgesWhereTarget(String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		return this.sid2wheretarget.getSet(edgeschemaID).iterator();
	}

	public Iterator<UndirectedEdge> getUndirEdges() {
		return this.sid2undirected.getAllItems().iterator();
	}
	
	public Iterator<DirectedEdge> getDirEdges() {
		Set<DirectedEdge> wheresource = this.sid2wheresource.getAllItems();
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getAllItems();
		
		List<DirectedEdge> dir = new LinkedList<DirectedEdge>();
		dir.addAll(wheresource);
		dir.addAll(wheretarget);
		
		return dir.iterator();
	}

	public boolean isIncident(Edge e) {
		String sid = e.getSchemaID();
		if(e instanceof DirectedEdge) {
			return this.sid2wheresource.getSet(sid).contains(e) ||
				this.sid2wheretarget.getSet(sid).contains(e);
		} else if(e instanceof UndirectedEdge) {
			return this.sid2undirected.getSet(sid).contains(e);
		} else {
			throw new UnsupportedTypeException("Unsupported edge type: "+e.getClass().getCanonicalName());
		}
	}

	public boolean isAdjacentTo(Node n) {
		Iterator<Edge> alledges = this.getAllEdges();
		while(alledges.hasNext()){
			Iterator<Node> allnodes = alledges.next().getAllNodes();
			while(allnodes.hasNext()){
				Node curr_node = allnodes.next();
				if(curr_node.equals(n)){
					return true;
				}
			}
		}

		return false;
	}
	
	public boolean isAdjacentTo(Node n, String edgeschemaID) {
		Iterator<Edge> alledges = this.getAllEdges(edgeschemaID);
		while(alledges.hasNext()){
			Iterator<Node> allnodes = alledges.next().getAllNodes();
			while(allnodes.hasNext()){
				Node curr_node = allnodes.next();
				if(curr_node.equals(n)){
					return true;
				}
			}
		}

		return false;
	}

	public int numEdges() {
		return this.sid2wheresource.totalNumItems()
				+this.sid2wheretarget.totalNumItems()
				+this.sid2undirected.totalNumItems();
	}

	/**
	 * Internal methods to simplify maintaining which edges the node is connected to.
	 * Call whenever a new edge is added which contains this node.
	 * 
	 * @param e Edge
	 */
	protected void edgeAddedNotification(Edge e, Boolean isSource) {
		// Check to see if this is a valid removal
		if(!e.isIncident(this)) {
			throw new InvalidStateException("Malformed Edge "+e+" suppose to contain "+this);
		}
		
		String sid = e.getSchemaID();
		
		if(e instanceof DirectedEdge){
			DirectedEdge de = (DirectedEdge) e;
			if(isSource){
				this.sid2wheresource.addItem(sid, de);
			} else {
				this.sid2wheretarget.addItem(sid, de);
			}
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			this.sid2undirected.addItem(sid, ue);
		} else {
			throw new UnsupportedTypeException("Unknown Edge type: "+e.getClass().getCanonicalName());
		}
	}

	/**
	 * Internal methods to simplify maintaining which edges the node is connected to.
	 * Call whenever an edge is removed which contains this node.
	 * 
	 * @param e Edge
	 */
	protected void edgeRemovedNotification(Edge e) {
		// Check to see if this is a valid removal
		if(!e.isIncident(this)) {
			throw new InvalidStateException("Malformed Edge "+e+" suppose to contain "+this);
		}
		
		String sid = e.getSchemaID();
		if(e instanceof DirectedEdge){
			DirectedEdge de = (DirectedEdge) e;
			if(de.isSource(this)){
				this.sid2wheresource.removeItem(sid, de);
			}
			
			if(de.isTarget(this)){
				this.sid2wheretarget.removeItem(sid, de);
			}
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			this.sid2undirected.removeItem(sid, ue);
		} else {
			throw new UnsupportedTypeException("Unknown Edge type: "+e.getClass().getCanonicalName());
		}
	}

	private Set<Node> getAdjacentTargetSets() {
		Set<Node> nodes = new HashSet<Node>();
		Iterator<DirectedEdge> eitr = this.getEdgesWhereSource();
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			nodes.addAll(IteratorUtils.iterator2nodelist(de.getTargetNodes()));
		}
		
		return nodes;
	}

	private Set<Node> getAdjacentTargetSets(String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		if(!this.getGraph().getSchemaType(edgeschemaID).equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Schema ID is not a Directed Edge: "+edgeschemaID);
		}
		
		Set<Node> nodes = new HashSet<Node>();
		Iterator<DirectedEdge> eitr = this.getEdgesWhereSource(edgeschemaID);
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			nodes.addAll(IteratorUtils.iterator2nodelist(de.getTargetNodes()));
		}
		
		return nodes;
	}

	private Set<Node> getAdjacentSourceSets() {
		Set<Node> nodes = new HashSet<Node>();
		Iterator<DirectedEdge> eitr = this.getEdgesWhereTarget();
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			nodes.addAll(IteratorUtils.iterator2nodelist(de.getSourceNodes()));
		}
		
		return nodes;
	}

	private Set<Node> getAdjacentSourceSets(String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		if(!this.getGraph().getSchemaType(edgeschemaID).equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Schema ID is not a Directed Edge: "+edgeschemaID);
		}
		
		Set<Node> nodes = new HashSet<Node>();
		Iterator<DirectedEdge> eitr = this.getEdgesWhereTarget(edgeschemaID);
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			nodes.addAll(IteratorUtils.iterator2nodelist(de.getSourceNodes()));
		}
		
		return nodes;
	}
	
	private Set<Node> getAdjacentNodeSets() {
		HashSet<Node> all = new HashSet<Node>();
		Iterator<GraphItem> incident = this.getIncidentGraphItems();
		while(incident.hasNext()) {
			GraphItem gi = incident.next();
			Iterator<GraphItem> giitr = gi.getIncidentGraphItems();
			while(giitr.hasNext()) {
				GraphItem currgi = giitr.next();
				if(currgi.equals(this)) {
					continue;
				}

				all.add((Node) currgi);
			}
		}

		return all;
	}

	private Set<Node> getAdjacentNodeSets(String incidentsid) {
		HashSet<Node> all = new HashSet<Node>();
		Iterator<GraphItem> incident = this.getIncidentGraphItems(incidentsid);
		while(incident.hasNext()) {
			GraphItem gi = incident.next();
			Iterator<GraphItem> giitr = gi.getIncidentGraphItems();
			while(giitr.hasNext()) {
				GraphItem currgi = giitr.next();
				if(currgi.equals(this)) {
					continue;
				}

				all.add((Node) currgi);
			}
		}

		return all;
	}
	
	public Iterator<Node> getAdjacentTargets() {
		return this.getAdjacentTargetSets().iterator();
	}

	public Iterator<Node> getAdjacentTargets(String edgeschemaID) {
		return this.getAdjacentTargetSets(edgeschemaID).iterator();
	}

	public Iterator<Node> getAdjacentSources() {
		return this.getAdjacentSourceSets().iterator();
	}

	public Iterator<Node> getAdjacentSources(String edgeschemaID) {
		return getAdjacentSourceSets(edgeschemaID).iterator();
	}
	
	public Iterator<Node> getAdjacentNodes() {
		return this.getAdjacentNodeSets().iterator();
	}

	public Iterator<Node> getAdjacentNodes(String edgeschemaID) {
		return this.getAdjacentNodeSets(edgeschemaID).iterator();
	}

	public boolean isAdjacentTarget(Node n) {
		Iterator<DirectedEdge> eitr = this.getEdgesWhereSource();
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			if(de.isTarget(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentTarget(Node n, String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		if(!this.getGraph().getSchemaType(edgeschemaID).equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Schema ID is not a Directed Edge: "+edgeschemaID);
		}
		
		Iterator<DirectedEdge> eitr = this.getEdgesWhereSource(edgeschemaID);
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			if(de.isTarget(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentSource(Node n) {
		Iterator<DirectedEdge> eitr = this.getEdgesWhereTarget();
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			if(de.isSource(n)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAdjacentSource(Node n, String edgeschemaID) {
		if(!this.getGraph().hasSchema(edgeschemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+edgeschemaID);
		}
		
		if(!this.getGraph().getSchemaType(edgeschemaID).equals(SchemaType.DIRECTED)) {
			throw new InvalidOperationException("Schema ID is not a Directed Edge: "+edgeschemaID);
		}
		
		Iterator<DirectedEdge> eitr = this.getEdgesWhereTarget(edgeschemaID);
		while(eitr.hasNext()) {
			DirectedEdge de = eitr.next();
			if(de.isSource(n)) {
				return true;
			}
		}
		
		return false;
	}

	public int numAdjacentTargets() {
		return this.getAdjacentTargetSets().size();
	}

	public int numAdjacentTargets(String edgeschemaID) {
		return this.getAdjacentTargetSets(edgeschemaID).size();
	}

	public int numAdjacentSources() {
		return this.getAdjacentSourceSets().size();
	}

	public int numAdjacentSources(String edgeschemaID) {
		return this.getAdjacentSourceSets(edgeschemaID).size();
	}
	
	public int numAdjacentNodes() {
		return this.getAdjacentNodeSets().size();
	}

	public int numAdjacentNodes(String edgeschemaID) {
		return this.getAdjacentNodeSets(edgeschemaID).size();
	}

	public void removeIncidentEdges() {
		Iterator<Edge> eitr = this.getAllEdges();
		while(eitr.hasNext()) {
			this.getGraph().removeEdge(eitr.next());
		}
	}

	public void removeIncidentEdges(String edgeschemaid) {
		Iterator<Edge> eitr = this.getAllEdges(edgeschemaid);
		while(eitr.hasNext()) {
			this.getGraph().removeEdge(eitr.next());
		}
	}

	public int numEdgesWhereSource() {
		return this.sid2wheresource.totalNumItems();
	}

	public int numEdgesWhereSource(String schemaID) {
		return this.sid2wheresource.getSet(schemaID).size();
	}

	public int numEdgesWhereTarget() {
		return this.sid2wheretarget.totalNumItems();
	}

	public int numEdgesWhereTarget(String schemaID) {
		return this.sid2wheretarget.getSet(schemaID).size();
	}

	public int numUndirEdges() {
		return this.sid2undirected.totalNumItems();
	}

	public int numDirEdges() {
		Set<DirectedEdge> wheresource = this.sid2wheresource.getAllItems();
		Set<DirectedEdge> wheretarget = this.sid2wheretarget.getAllItems();
		
		List<DirectedEdge> dir = new LinkedList<DirectedEdge>();
		dir.addAll(wheresource);
		dir.addAll(wheretarget);
		
		return dir.size();
	}
}
