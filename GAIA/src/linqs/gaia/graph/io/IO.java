package linqs.gaia.graph.io;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Interface for loading and saving {@link Graph} objects from the provided
 * file format.
 * 
 * @author namatag
 *
 */
public interface IO extends Configurable {
	/**
	 * Loads the nodes, edges, and attributes specified in the
	 * input to the returned graph.
	 * 
	 * @return Graph to load the nodes
	 */
	public Graph loadGraph();
	
	/**
	 * Loads the nodes, edges, and attributes specified in the
	 * input to the returned graph using the provided the parameter
	 * as the object id of the graph (ignoring any other previous saved object ID for the graph).
	 * 
	 * @param objid Object ID to load graph with
	 * @return Graph to load the nodes
	 */
	public Graph loadGraph(String objid);

	/**
	 * Saves the nodes, edges, and attributes specified in the
	 * input to the specified format.
	 * 
	 * @param g Graph to load the nodes
	 */
	public void saveGraph(Graph g);
}