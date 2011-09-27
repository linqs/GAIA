package linqs.gaia.prediction;

import linqs.gaia.util.UnmodifiableList;


/**
 * Predictions where the values for the prediction belong to a
 * categorical set.
 * 
 * @author namatag
 *
 */
public interface CategoricalValuedGroup extends PredictionGroup {
	/**
	 * Return list of the strings that the prediction value may hold
	 * 
	 * @return List of Strings
	 */
	UnmodifiableList<String> getCategories();
}
