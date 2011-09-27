package linqs.gaia.graph;

/**
 * Undirected hyperedge interface.  Directed edges have to sets of nodes:
 * the source nodes and target nodes.
 * <p>
 * A valid undirected edge must have at least one node.  An exception
 * is thrown when an edge is modified or added which does not match
 * this criterion.
 * 
 * @see Edge
 * @see DirectedEdge
 * 
 * @author namatag
 *
 */
public interface UndirectedEdge extends Edge {
	/**
	 * Add Node to Edge. An exception is thrown if a
	 * node is already in this edge.
	 * 
	 * @param n Node to add
	 */
	void addNode(Node n);
}
