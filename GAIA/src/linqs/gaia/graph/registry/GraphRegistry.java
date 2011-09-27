package linqs.gaia.graph.registry;

import java.util.HashMap;
import java.util.Map;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Graph registry is a static, global object
 * where graphs (and graph items therein) can be registered and accessed
 * from anywhere in the GAIA code.
 * 
 * @author namatag
 *
 */
public class GraphRegistry {
	/**
	 * Internal map storing the registered graph
	 */
	private static Map<GraphID,Graph> id2graph = new HashMap<GraphID,Graph>();
	
	/**
	 * Register the graph.
	 * 
	 * @param g Graph to register
	 */
	public static void registerGraph(Graph g) {
		if(id2graph.containsKey(g.getID())) {
			throw new InvalidStateException("Graph with the given id is already registered."
					+" Either load or generate the graph with a different ID," 
					+" destroy the previous graph with this id Graph.destroy(),"
					+" or remove the graph with this id from the GraphRegistry: "
					+g.getID());
		}
		
		id2graph.put(g.getID(), g);
	}
	
	/**
	 * Register the graph.
	 * 
	 * @param g Graph to register
	 */
	public static void updateGraph(Graph g) {
		if(!id2graph.containsKey(g.getID())) {
			Log.WARN("Unable to update. " +
					"Graph with the given id is not already registered: "+g.getID());
		}
		
		id2graph.put(g.getID(), g);
	}
	
	/**
	 * Get the graph with the given id.
	 * An exception is throw if a graph with the given ID is not defined.
	 * 
	 * @param id ID of graph
	 * @return Graph
	 */
	public static Graph getGraph(GraphID id) {
		if(id==null || !GraphRegistry.isRegistered(id)) {
			String regids = null;
			UnmodifiableSet<GraphID> uset = GraphRegistry.getRegisteredGraphIDs();
			for(GraphID gid:uset) {
				if(regids==null) {
					regids = "";
				} else {
					regids += ",";
				}
				
				regids += gid.toString();
			}
			
			throw new ConfigurationException("Specified output graph not found: "
					+id+" Registered graphs are: "+regids);
		}
		
		return id2graph.get(id);
	}
	
	/**
	 * Get the graph item with the given id.
	 * A null is returned if there is no graph item
	 * in any of the registered graphs.
	 * 
	 * @param id ID of Graph Item
	 * @return Graph Item
	 */
	public static GraphItem getGraphItem(GraphItemID id) {
		Graph g = id2graph.get(id.getGraphID());
		
		if(g==null) {
			return null;
		}
		
		return g.getGraphItem(id);
	}
	
	/**
	 * Check to see if the graph or graph item
	 * is registered (i.e., accessible through the registry).
	 * 
	 * @param id ID to check in registry
	 * @return True if the graph or graph item is accessible
	 * through the registry, and False otherwise.
	 */
	public static boolean isRegistered(ID id) {
		if(id instanceof GraphID) {
			return id2graph.containsKey(id);
		} else if(id instanceof GraphItemID) {
			GraphItemID giid = (GraphItemID) id;
			Graph g = id2graph.get(giid.getGraphID());
			
			if(g != null) {
				return g.hasGraphItem(giid);
			} else {
				return false;
			}
		} else {
			throw new UnsupportedTypeException("ID type not supported: "
					+id.getClass().getCanonicalName());
		}
	}
	
	/**
	 * Get the ids of all registered graphs
	 * 
	 * @return Set of graph ids
	 */
	public static UnmodifiableSet<GraphID> getRegisteredGraphIDs() {
		return new UnmodifiableSet<GraphID>(id2graph.keySet());
	}
	
	/**
	 * Get the all registered graphs
	 * 
	 * @return Set of Graphs
	 */
	public static UnmodifiableSet<Graph> getRegisteredGraphs() {
		return new UnmodifiableSet<Graph>(id2graph.values());
	}
	
	/**
	 * Remove the specified Graph from the registry
	 * 
	 * @param g Graph to remove
	 */
	public static void removeGraph(Graph g) {
		id2graph.remove(g.getID());
	}
	
	/**
	 * Clear all graphs from the registry
	 */
	public static void clearRegistry() {
		id2graph.clear();
	}
}
