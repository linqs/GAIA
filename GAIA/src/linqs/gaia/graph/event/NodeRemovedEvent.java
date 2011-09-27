package linqs.gaia.graph.event;

import linqs.gaia.graph.Node;

/**
 * Graph Event that called when an node is removed.
 * The event has a pointer to the removed node.
 * 
 * @author namatag
 *
 */
public class NodeRemovedEvent implements GraphEvent {
	private Node n;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param n Node that was removed
	 */
	public NodeRemovedEvent(Node n) {
		this.n = n;
	}
	
	/**
	 * Get node that was removed
	 * 
	 * @return Removed node
	 */
	public Node getRemovedNode() {
		return n;
	}
}
