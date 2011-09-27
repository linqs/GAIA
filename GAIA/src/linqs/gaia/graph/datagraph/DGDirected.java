package linqs.gaia.graph.datagraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DGDirected extends DGEdge implements DirectedEdge {
	private static final long serialVersionUID = 1L;
	
	/** Stores the sources for directed edges */
	private Set<Node> sources;
	
	/** Stores the targets for directed edges */
	private Set<Node> targets;
	
	public DGDirected(DataGraph graph, GraphItemID id) {
		super(graph, id);
		this.sources = new HashSet<Node>(1);
		this.targets = new HashSet<Node>(1);
	}

	@Override
	protected void nodeRemovedNotification(Node n) {
		this.removeNode(n);
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets() {
		Set<GraphItem> allitems = new HashSet<GraphItem>(this.sources.size()+this.targets.size());
		allitems.addAll(this.sources);
		allitems.addAll(this.targets);
		
		return allitems;
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		Set<GraphItem> allitems = new HashSet<GraphItem>(this.sources.size()+this.targets.size());
		for(Node n:sources){
			if(n.getSchemaID().equals(schemaID)){
				allitems.add(n);
			}
		}
		
		for(Node n:targets){
			if(n.getSchemaID().equals(schemaID)){
				allitems.add(n);
			}
		}
		
		return allitems;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(GraphItem adjacent) {
		
		Set<GraphItem> allitems = new HashSet<GraphItem>(this.sources.size()+this.targets.size());
		for(Node n:sources){
			if(n.isIncident(adjacent)){
				allitems.add(n);
			}
		}
		
		for(Node n:targets){
			if(n.isIncident(adjacent)){
				allitems.add(n);
			}
		}
		
		return allitems;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID, GraphItem adjacent) {
		
		Set<GraphItem> allitems = new HashSet<GraphItem>(this.sources.size()+this.targets.size());
		for(Node n:sources){
			if(n.getSchemaID().equals(schemaID) && n.isIncident(adjacent)){
				allitems.add(n);
			}
		}
		
		for(Node n:targets){
			if(n.getSchemaID().equals(schemaID) && n.isIncident(adjacent)){
				allitems.add(n);
			}
		}
		
		return allitems;
	}

	public void addSourceNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n
					+" not in "+this.getGraph());
		}
		
		if(this.isSource(n)) {
			throw new InvalidOperationException(
					"Node already in source of directed edge: "+n+" in edge "+this);
		}
		
		this.sources.add(n);
		
		// Notify node that it was added to an edge
		((DGNode) n).edgeAddedNotification(this, true);
	}

	public void addTargetNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n
					+" not in "+this.getGraph());
		}
		
		if(this.isTarget(n)) {
			throw new InvalidOperationException(
					"Node already in target of directed edge: "+n+" in edge "+this);
		}
		
		this.targets.add(n);
		
		// Notify node that it was added to an edge
		((DGNode) n).edgeAddedNotification(this, false);
	}

	public Iterator<Node> getSourceNodes() {
		return this.sources.iterator();
	}

	public Iterator<Node> getTargetNodes() {
		return this.targets.iterator();
	}

	public boolean isSource(Node n) {
		return this.sources.contains(n);
	}

	public boolean isTarget(Node n) {
		return this.targets.contains(n);
	}

	public void removeSourceNode(Node n) {
		((DGNode) n).edgeRemovedNotification(this);
		this.sources.remove(n);
	}

	public void removeTargetNode(Node n) {
		((DGNode) n).edgeRemovedNotification(this);
		this.targets.remove(n);
	}

	public Iterator<Node> getAllNodes() {
		Collection<Node> allnodes = new HashSet<Node>(this.sources.size()+this.targets.size());
		allnodes.addAll(this.sources);
		allnodes.addAll(this.targets);
		
		return allnodes.iterator();
	}

	public Iterator<Node> getAllNodes(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		Collection<Node> allnodes = new HashSet<Node>(this.sources.size()+this.targets.size());
		
		for(Node n: this.sources){
			if(n.getSchemaID().equals(schemaID)){
				allnodes.add(n);
			}
			
		}
		
		for(Node n: this.targets){
			if(n.getSchemaID().equals(schemaID)){
				allnodes.add(n);
			}
			
		}
		
		return allnodes.iterator();
	}

	public boolean isIncident(Node n) {
		return this.sources.contains(n) || this.targets.contains(n);
	}

	public void removeNode(Node n) {
		boolean removed = false;
		if(this.isSource(n)){
			removed = true;
			this.removeSourceNode(n);
		}
		
		if(this.isTarget(n)){
			removed = true;
			this.removeTargetNode(n);
		}
		
		if(!removed){
			throw new InvalidOperationException("Node is not incident on this edge: "+GraphItemUtils.getNodeIDString(n));
		}
	}

	public int numSourceNodes() {
		return this.sources.size();
	}

	public int numTargetNodes() {
		return this.targets.size();
	}

	public int numNodes() {
		Collection<Node> allnodes = new HashSet<Node>(this.sources.size()+this.targets.size());
		allnodes.addAll(this.sources);
		allnodes.addAll(this.targets);
		
		return allnodes.size();
	}

	/**
	 * Check validity of the edge
	 */
	@Override
	protected void checkValidity(String message) {
		if(!isValid()){
			throw new InvalidStateException(message);
		}
	}

	@Override
	protected boolean isValid() {
		if(this.numSourceNodes()<=0 || this.numTargetNodes()<=0) {
			return false;
		}
		
		return true;
	}

	public boolean hasSameSources(DirectedEdge e) {
		Set<Node> eset = new HashSet<Node>(IteratorUtils.iterator2nodelist(e.getSourceNodes()));
		Set<Node> sset = new HashSet<Node>(sources);
		
		return eset.equals(sset);
	}

	public boolean hasSameTargets(DirectedEdge e) {
		Set<Node> eset = new HashSet<Node>(IteratorUtils.iterator2nodelist(e.getTargetNodes()));
		Set<Node> tset = new HashSet<Node>(targets);
		
		return eset.equals(tset);
	}

	public boolean hasSameNodes(Edge e) {
		if(!(e instanceof DGDirected)) {
			throw new InvalidStateException("Edge is not directed: "+e
					+" of type "+e.getClass().getCanonicalName());
		}
		
		DirectedEdge de = (DirectedEdge) e;
		return this.hasSameSources(de) && this.hasSameTargets(de);
	}
}
