package linqs.gaia.graph.filter;

import java.util.Iterator;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;

/**
 * Remove all edges which have less than or greater than two incident nodes
 * 
 * @author namatag
 *
 */
public class RemoveHyperedges extends Filter {
	@Override
	public void filter(Graph graph) {
		Iterator<Edge> eitr = graph.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(e.numNodes()!=2) {
				graph.removeEdge(e);
			}
		}
	}
}
