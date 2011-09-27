package linqs.gaia.graph.noise;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Interface of attribute noise to add to Graph.
 * 
 * @author namatag
 *
 */
public abstract class AttributeNoise extends Noise {
	/**
	 * Add noise to the decorable item
	 * 
	 * @param d Decorable Item
	 */
	abstract public void addNoise(Decorable d);
}
