package linqs.gaia.graph.datagraph;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;

/**
 * Edge implementation of DataGraph
 * 
 * @see DataGraph
 * 
 * @author namatag
 *
 */
public abstract class DGEdge extends DGGraphItem implements Edge {
	private static final long serialVersionUID = 1L;
	
	public DGEdge(DataGraph graph, GraphItemID schemaID) {
		super(graph, schemaID);
	}
	
	public String toString(){
		return GraphItemUtils.getEdgeIDString(this);
	}
	
	/**
	 * Internal method to handle maintenance of edge when nodes are removed.
	 * 
	 * @param n Node
	 */
	protected abstract void nodeRemovedNotification(Node n);
	
	/**
	 * Check if the edge is valid.  If not, throw an exception.
	 * 
	 * @param message String message to include in exception if invalid.
	 */
	protected abstract void checkValidity(String message);
	
	/**
	 * Check to see if edge is valid.
	 * 
	 * @return True if valid.  False otherwise.
	 */
	protected abstract boolean isValid();
}
