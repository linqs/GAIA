package linqs.gaia.graph.event;

import linqs.gaia.graph.Node;

/**
 * Graph Event that called when an node is added.
 * The event has a pointer to the added node.
 * 
 * @author namatag
 *
 */
public class NodeAddedEvent implements GraphEvent {
	private Node n;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param n Node that was added
	 */
	public NodeAddedEvent(Node n) {
		this.n = n;
	}
	
	/**
	 * Get node that was added
	 * 
	 * @return Added node
	 */
	public Node getAddedNode() {
		return n;
	}
}
