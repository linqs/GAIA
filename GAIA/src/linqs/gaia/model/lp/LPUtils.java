package linqs.gaia.model.lp;

import java.util.Iterator;

import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;

/**
 * General utilities for use with link prediction tasks
 * 
 * @author namatag
 *
 */
public class LPUtils {
	/**
	 * Return true if n1 and n2 share at least
	 * one edge of the given schema id.
	 * If the schema id is null, return true
	 * if n1 and n2 share at least one edge.
	 * Return false otherwise.
	 * 
	 * @param edgeschemaid Schema ID of edge.  Set to null if you want to consider all edge types.
	 * @param n1 Node
	 * @param n2 Node
	 * @return True if they share an edge.  False otherwise.
	 * @deprecated Use isAdjacent instead
	 */
	public static boolean edgeExists(String edgeschemaid, Node n1, Node n2) {
		Iterator<GraphItem> conn = null;
		if(edgeschemaid!=null) {
			conn = n1.getIncidentGraphItems(edgeschemaid);
		} else {
			conn = n1.getIncidentGraphItems();
		}
		
		while(conn.hasNext()) {
			UndirectedEdge ue = (UndirectedEdge) conn.next();
			if(ue.isIncident(n2)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return true if n1 and n2 share at least
	 * one edge of the given schema id.
	 * Return false otherwise.
	 * 
	 * @param n1 Node
	 * @param n2 Node
	 * @return True if they share an edge.  False otherwise.
	 * @deprecated Use isAdjacent instead
	 */
	public static boolean edgeExists(Node n1, Node n2) {
		return LPUtils.edgeExists(null, n1, n2);
	}
}
