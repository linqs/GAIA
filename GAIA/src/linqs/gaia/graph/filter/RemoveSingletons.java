package linqs.gaia.graph.filter;

import java.util.Iterator;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;

/**
 * Remove all nodes which have no edges.
 * 
 * @author namatag
 *
 */
public class RemoveSingletons extends Filter {
	@Override
	public void filter(Graph graph) {
		Iterator<Node> nitr = graph.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(n.numEdges()==0) {
				graph.removeNode(n);
			}
		}
	}
}
