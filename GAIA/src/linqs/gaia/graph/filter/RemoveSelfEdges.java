package linqs.gaia.graph.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.log.Log;

/**
 * Remove all edges which are self edges.
 * A self edge is defined as an undirected hyperedge edge with only one node
 * or directed edge with the same source and target node.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> edgesid-Schema ID of edges whose value to remove.  If not specified,
 * all edges are considered.
 * </UL>
 * @author namatag
 *
 */
public class RemoveSelfEdges extends Filter {

	@Override
	public void filter(Graph graph) {
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph before removing self edges: "+GraphUtils.getSimpleGraphOverview(graph));
		}
		
		String edgesid = this.getStringParameter("edgesid",null);
		
		// Iterate over edges
		List<Edge> toremove = new LinkedList<Edge>();
		Iterator<Edge> eitr = edgesid==null ? graph.getEdges() : graph.getEdges(edgesid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(e.numNodes()!=2) {
				toremove.add(e);
			}
		}
		
		// Remove self edges
		for(Edge e:toremove) {
			graph.removeEdge(e);
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph after removing self edges: "+GraphUtils.getSimpleGraphOverview(graph));
		}
	}
}
