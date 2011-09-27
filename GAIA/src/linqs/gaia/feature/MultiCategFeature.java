package linqs.gaia.feature;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.util.UnmodifiableList;


/**
 * Multi valued categorical feature interface.  This is the same
 * as a {@link CategFeature} but where each {@link Decorable}
 * item may have multiple categories.
 * <p>
 * Multi valued categorical features must return a MultiCategValue object.
 * 
 * @see linqs.gaia.feature.values.MultiCategValue
 * 
 * @author namatag
 *
 */
public interface MultiCategFeature extends Feature {
	/**
	 * Categories valid for feature
	 * 
	 * @return List of categories
	 */
	UnmodifiableList<String> getAllCategories();
	
	/**
	 * Number of categories valid for feature
	 * 
	 * @return Number of categories
	 */
	int numCategories();
}
