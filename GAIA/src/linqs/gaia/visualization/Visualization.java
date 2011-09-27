package linqs.gaia.visualization;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Base interface for all implementations of graph visualizers
 * in GAIA.
 * 
 * @author namatag
 *
 */
public interface Visualization extends Configurable {
	/**
	 * Visualize graph
	 * 
	 * @param g Graph
	 */
	void visualize(Graph g);
}
