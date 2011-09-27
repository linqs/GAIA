package linqs.gaia.graph.datagraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DGUndirected extends DGEdge implements UndirectedEdge {
	private static final long serialVersionUID = 1L;
	
	/** Stores the references for this undirected edge  */
	private Set<Node> undirected;
	
	public DGUndirected(DataGraph graph, GraphItemID id) {
		super(graph, id);
		this.undirected = new HashSet<Node>(2);
	}

	@Override
	protected void nodeRemovedNotification(Node n) {
		if(!this.isIncident(n)){
			throw new InvalidOperationException("Node not adjacent to edge");
		}
		
		this.undirected.remove(n);
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets() {
		Set<GraphItem> connected = new HashSet<GraphItem>(this.undirected);
		return connected;
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.getSchemaID().equals(schemaID)){
				connected.add(n);
			}
		}
		
		return connected;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(GraphItem adjacent) {
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.isIncident(adjacent)){
				connected.add(n);
			}
		}
		
		return connected;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID, GraphItem adjacent) {
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.getSchemaID().equals(schemaID) && n.isIncident(adjacent)){
				connected.add(n);
			}
		}
		
		return connected;
	}

	public void addNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n
					+" not in graph "+this.getGraph());
		}
		
		if(this.isIncident(n)) {
			throw new InvalidOperationException("Node already part of the undirected edge: "
						+n+" in edge "+this);
		}
		
		this.undirected.add(n);
		((DGNode) n).edgeAddedNotification(this, null);
	}

	public Iterator<Node> getAllNodes() {
		return this.undirected.iterator();
	}

	public Iterator<Node> getAllNodes(String schemaID) {
		Collection<Node> allrefs = new HashSet<Node>();
		
		for(Node n: this.undirected){
			if(n.getSchemaID().equals(schemaID)){
				allrefs.add(n);
			}
			
		}
		
		return allrefs.iterator();
	}

	public boolean isIncident(Node n) {
		return this.undirected.contains(n);
	}

	public void removeNode(Node n) {
		((DGNode) n).edgeRemovedNotification(this);
		this.undirected.remove(n);
		
		checkValidity("Removing node resulted in invalid edge: Removing "+GraphItemUtils.getNodeIDString(n)+" invalidated "+this);
	}

	public int numNodes() {
		return this.undirected.size();
	}
	
	/**
	 * Check validity of the edge
	 */
	@Override
	protected void checkValidity(String message) {
		if(this.numNodes()==0){
			throw new InvalidStateException(message);
		}
	}

	@Override
	protected boolean isValid() {
		if(this.numNodes()<=0) {
			return false;
		}
		
		return true;
	}

	public boolean hasSameNodes(Edge e) {
		if(!(e instanceof UndirectedEdge)) {
			throw new InvalidStateException("Edge is not undirected: "+e
					+" of type "+e.getClass().getCanonicalName());
		}
		
		Set<Node> nodes = new HashSet<Node>(IteratorUtils.iterator2nodelist(e.getAllNodes()));
		return this.undirected.equals(nodes);
	}
}
