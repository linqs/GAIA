package linqs.gaia.graph.event;

import linqs.gaia.graph.Edge;

/**
 * Graph Event that called when an edge is added.
 * The event has a pointer to the added edge.
 * 
 * @author namatag
 *
 */
public class EdgeAddedEvent implements GraphEvent {
	private Edge e;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param e Edge that was added
	 */
	public EdgeAddedEvent(Edge e) {
		this.e = e;
	}
	
	/**
	 * Get edge that was added
	 * 
	 * @return Edge that was added
	 */
	public Edge getAddedEdge() {
		return e;
	}
}
