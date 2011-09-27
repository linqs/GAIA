package linqs.gaia.model.util.plg;

import java.util.Iterator;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;

public interface NodeCentricPLG extends PotentialLinkGenerator {
	/**
	 * Return the set of edges adjacent to given node as an iterator.
	 * The returned edge is initialized only prior to
	 * adding to the graphs.  If there are a lot of
	 * potential edges, you can look at them one by one
	 * in this manner and keep only those that you predict
	 * exists (i.e., you can delete the newly created edge).
	 * 
	 * @param g Graph to predicted edges over
	 * @param n Node adjacent to all edges returned
	 * @param edgeschemaid Schema ID of edge we're generating
	 * @return Iterator over potential edges
	 */
	public Iterator<Edge> getLinksIteratively(Graph g, Node n, String edgeschemaid);
}
