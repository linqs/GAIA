package linqs.gaia.graph;


/**
 * Base interface for all parts of the code which require
 * the graph in order to do its function.
 * 
 * @author namatag
 *
 */
public interface GraphDependent {
	/**
	 * Set the graph for the object
	 * 
	 * @param g Graph
	 */
	void setGraph(Graph g);
}
