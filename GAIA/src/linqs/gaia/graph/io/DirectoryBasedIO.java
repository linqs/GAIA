package linqs.gaia.graph.io;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Interface for loading and saving {@link Graph} objects from an
 * file format that is based on files stored in a directory.
 * 
 * @author namatag
 *
 */
public interface DirectoryBasedIO extends Configurable {
	/**
	 * Loads the nodes, edges, and attributes specified in the
	 * directory to the returned graph.
	 * 
	 * @return Graph to load the nodes
	 */
	public Graph loadGraphFromDir(String directory);
	
	/**
	 * Loads the nodes, edges, and attributes specified in the
	 * directory to the returned graph using the provided the parameter
	 * as the object id of the graph (ignoring any other previous saved object ID for the graph).
	 * 
	 * @param objid Object ID to load graph with
	 * @return Graph to load the nodes
	 */
	public Graph loadGraphFromDir(String directory, String objid);

	/**
	 * Saves the nodes, edges, and attributes specified in the
	 * input to the specified directory.
	 * 
	 * @param g Graph to load the nodes
	 */
	public void saveGraphToDir(String directory, Graph g);
}
