package linqs.gaia.graph.transformer;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Base class for classes designed to transform parts of the graph
 * 
 * @author namatag
 *
 */
public abstract class Transformer extends BaseConfigurable implements Configurable {
	/**
	 * Perform the transformation
	 */
	public abstract void transform(Graph graph);
}
