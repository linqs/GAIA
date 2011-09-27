package linqs.gaia.feature;

import linqs.gaia.util.UnmodifiableList;


/**
 * Categorical feature interface.  Categorical features are string valued features
 * where the value of the string must be a from a pre-specified set of strings.
 * <p>
 * Categorical features must return a CategValue object.
 * 
 * @see linqs.gaia.feature.values.CategValue
 * 
 * @author namatag
 *
 */
public interface CategFeature extends Feature {
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
