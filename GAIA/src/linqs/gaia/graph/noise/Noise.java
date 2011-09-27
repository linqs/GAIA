package linqs.gaia.graph.noise;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.Graph;

/**
 * Interface of noise to add to Graph.
 * 
 * @author namatag
 *
 */
public abstract class Noise extends BaseConfigurable {
	/**
	 * Add noise to the graph
	 * 
	 * @param g Graph
	 */
	abstract public void addNoise(Graph g);
}
