package linqs.gaia.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.util.IteratorUtils;

public class GraphItemUtils {

	/**
	 * Brief representation of {@link Graph}.
	 * For use in identifying object in logs.
	 * 
	 * @param g Graph
	 * @return String
	 */
	public static String getGraphIDString(Graph g){
		return "GRAPH."+g.getID();
	}

	/**
	 * Brief representation of {@link Node}.
	 * For use in identifying object in logs.
	 * 
	 * @param n Node
	 * @return String
	 */
	public static String getNodeIDString(Node n){
		return "NODE."+n.getID();
	}

	/**
	 * Brief representation of {@link Edge}.
	 * For use in identifying object in logs.
	 * 
	 * @param e Edge
	 * @return String
	 */
	public static String getEdgeIDString(Edge e){
		String type = null;
		
		if(e instanceof DirectedEdge){
			type = "DIRECTED";
		} else if(e instanceof UndirectedEdge){
			type = "UNDIRECTED";
		} else {
			throw new UnsupportedTypeException("Invalid edge object: "+e.getClass().getCanonicalName());
		}
		
		return type+"."+e.getID();
	}
	
	/**
	 * Return a string which lists the nodes of an edge.
	 * For undirected edges, it is of the form [node1,node2].
	 * For directed edges, it is of the form [n1,n2][n3,n4] where
	 * n1 and n2 are source nodes and n3 and n4 are target nodes.
	 * 
	 * @param e Edge to print
	 * @return String
	 */
	public static String getEdgeNodesString(Edge e) {
		String nodestring = "";
		
		if(e instanceof DirectedEdge){
			DirectedEdge de = (DirectedEdge) e;
			
			// Print source nodes
			Iterator<Node> nitr = de.getSourceNodes();
			nodestring += "["+IteratorUtils.iterator2string(nitr, ",")+"]";
			
			// Print target nodes
			nitr = de.getTargetNodes();
			nodestring += "["+IteratorUtils.iterator2string(nitr, ",")+"]";
		} else if(e instanceof UndirectedEdge){
			Iterator<Node> nitr = e.getAllNodes();
			nodestring += "["+IteratorUtils.iterator2string(nitr, ",")+"]";
		} else {
			throw new UnsupportedTypeException("Invalid edge object: "+e.getClass().getCanonicalName());
		}
		
		return nodestring;
	}

	/**
	 * Get all items connected to the specified
	 * item by one graph item (i.e., all nodes with
	 * an edge for a given node, all edges with a shared
	 * node for a given edge)
	 * 
	 * @param gi GraphItem
	 * @param connschemaid Schema ID of connecting item
	 * @param itemschemaid Schema ID of connected items to return
	 * @return Set of neighbors
	 * @deprecated Use Adjacent neighbors instead
	 */
	public static Set<GraphItem> getOneAway(GraphItem gi, String connschemaid, String itemschemaid) {
		Iterator<GraphItem> connitems = null;
		if(connschemaid != null) {
			connitems = gi.getIncidentGraphItems(connschemaid);
		} else {
			connitems = gi.getIncidentGraphItems();
		}
		
		Set<GraphItem> neighbors = new HashSet<GraphItem>();
		
		while(connitems.hasNext()) {
			GraphItem n = connitems.next();
			Iterator<GraphItem> nitr = null;
			if(itemschemaid!=null) {
				nitr = n.getIncidentGraphItems(itemschemaid);
			} else {
				nitr = n.getIncidentGraphItems();
			}
			
			while(nitr.hasNext()) {
				neighbors.add(nitr.next());
			}
		}
		
		// Item is not one of its neighbors
		neighbors.remove(gi);
		
		return neighbors;
	}
	
	/**
	 * Get all items connected to the specified
	 * item by one graph item (i.e., all nodes with
	 * an edge shared to a given node, all edges with a shared
	 * node for a given edge)
	 * 
	 * @param gi GraphItem
	 * @return Set of neighbors
	 * @deprecated Use getAdjacentGraphItems instead
	 */
	public static Set<GraphItem> getOneAway(GraphItem gi) {
		return getOneAway(gi, null, null);
	}
	
	/**
	 * Remove all the graph items incident to this node.
	 * (i.e., Remove all edges incident to a node)
	 * 
	 * @param gi Graph Item whose incident graph items to remove
	 */
	public static void removeAllIncident(GraphItem gi) {
		Iterator<GraphItem> gitr = gi.getIncidentGraphItems();
		Graph g = gi.getGraph();
		while(gitr.hasNext()) {
			GraphItem currgi = gitr.next();
			g.removeGraphItem(currgi);
		}
	}
	
	/**
	 * Get a randomly selected graph item from the graph
	 * 
	 * @param g Graph
	 * @param schemaID Schema ID of item to select
	 * @param rand Random number generator
	 * @return Randomly selected item
	 */
	public static GraphItem getRandomGraphItem(Graph g, String schemaID, Random rand) {
		Iterable<GraphItem> gitrbl = g.getIterableGraphItems(schemaID);
		// Optimization which maybe valid, depending on implementation
		if(gitrbl instanceof List) {
			List<?> list = (List<?>) gitrbl;
			return (GraphItem) list.get(rand.nextInt(list.size()));
		}
		
		Iterator<GraphItem> gitr = g.getGraphItems(schemaID);
		int random = rand.nextInt(g.numGraphItems(schemaID));
		int counter = 0;
		GraphItem randomitem = null;
		while(gitr.hasNext()) {
			if(random==counter) {
				randomitem = gitr.next();
				break;
			}
			
			gitr.next();
			counter++;
		}
		
		return randomitem;
	}
	
	/**
	 * Get a randomly selected node from the graph
	 * 
	 * @param g Graph
	 * @param schemaID Schema ID of node to select.  If null, randomly
	 * choose from among all nodes.
	 * @param rand Random number generator
	 * @return Randomly selected node
	 */
	public static Node getRandomNode(Graph g, String schemaID, Random rand) {
		Iterable<Node> nitrbl = g.getIterableNodes();
		// Optimization which maybe valid, depending on implementation
		if(nitrbl instanceof List) {
			List<?> list = (List<?>) nitrbl;
			return (Node) list.get(rand.nextInt(list.size()));
		}
		
		Iterator<Node> gitr = nitrbl.iterator();
		int numnodes = 0;
		if(schemaID==null) {
			gitr = g.getNodes();
			numnodes = g.numNodes();
		} else {
			gitr = g.getNodes(schemaID);
			numnodes = g.numGraphItems(schemaID);
		}
		
		int random = rand.nextInt(numnodes);
		int counter = 0;
		Node randomitem = null;
		while(gitr.hasNext()) {
			if(random==counter) {
				randomitem = gitr.next();
				break;
			}
			
			gitr.next();
			counter++;
		}
		
		return randomitem;
	}
}
