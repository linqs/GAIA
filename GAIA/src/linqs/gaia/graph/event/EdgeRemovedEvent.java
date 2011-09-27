package linqs.gaia.graph.event;

import linqs.gaia.graph.Edge;

/**
 * Graph Event that called when an edge is removed.
 * The event has a pointer to the removed edge.
 * 
 * @author namatag
 *
 */
public class EdgeRemovedEvent implements GraphEvent {
	private Edge e;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param e Edge that was removed
	 */
	public EdgeRemovedEvent(Edge e) {
		this.e = e;
	}
	
	/**
	 * Get edge that was removed
	 * 
	 * @return Removed edge
	 */
	public Edge getRemovedEdge() {
		return e;
	}
}
