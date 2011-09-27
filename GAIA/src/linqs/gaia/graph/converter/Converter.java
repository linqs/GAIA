package linqs.gaia.graph.converter;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * The covertor interface was created to support exporting a GAIA graph
 * into a graph representation used by another too (e.g., JUNG, Prefuse).
 * This simplifies the ability to use implementations available on these
 * other tools.
 * 
 * @author namatag
 *
 * @param <T> Graph object to convert GAIA graph to
 */
public interface Converter<T> extends Configurable {
	/**
	 * Import the given GAIA graph into a graph from another tool.
	 * 
	 * @param g Graph to import
	 * @return Imported GAIA Graph
	 */
	Graph importGraph(T g);
	
	/**
	 * Export the given GAIA graph into a graph from another tool.
	 * 
	 * @param g GAIA Graph
	 * @return Exported GAIA Graph
	 */
	T exportGraph(Graph g);
}
