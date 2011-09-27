package linqs.gaia.graph.generator.decorator;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Interface for all decorators.  Decorators will add features to the specified
 * graph.  When used with a graph generator, it can result in synthetic data
 * with features.
 * 
 * @author namatag
 *
 */
public interface Decorator extends Configurable {
	/**
	 * Decorate the specified graph
	 * 
	 * @param g Graph to decorate
	 */
	void decorate(Graph g);
}
