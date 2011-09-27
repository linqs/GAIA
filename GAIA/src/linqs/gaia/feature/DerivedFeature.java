package linqs.gaia.feature;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Interface {@link Feature} for all features where the value
 * is computed from some source or set of current values rather
 * than being explicitly specified by the user e.g. aggregate features.
 *  
 * @author namatag
 *
 */
public interface DerivedFeature extends Feature, Configurable {
	/**
	 * Specify if the feature is caching
	 * 
	 * @return True if caching.  False otherwise.
	 */
	boolean isCaching();
	
	/**
	 * Set whether or not feature should cache
	 * 
	 * @param shouldCache True if the feature should cache, false otherwise.
	 */
	void setCache(boolean shouldCache);
	
	/**
	 * Remove all cached values
	 */
	void resetCache();
	
	/**
	 * Clear cached value for the given item
	 * 
	 * @param d Decorable item
	 */
	void resetCache(Decorable d);
	
	/**
	 * Return the feature value for the Decorable item 
	 * 
	 * @param di Decorable Item
	 * @return Feature value for item
	 */
	FeatureValue getFeatureValue(Decorable di);
}
