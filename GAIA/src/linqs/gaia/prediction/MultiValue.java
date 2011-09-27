package linqs.gaia.prediction;

import linqs.gaia.util.UnmodifiableList;


/**
 * Predictions where a multiple classes are predicted.
 * 
 * @author namatag
 *
 */
public interface MultiValue extends Prediction {	
	/**
	 * Get true class values
	 * 
	 * @return True value
	 */
	UnmodifiableList<Object> getTrueValues();
	
	/**
	 * Get predicted class value
	 * If no value was predicted, return an empty list.
	 * 
	 * @return Predicted value
	 */
	UnmodifiableList<Object> getPredValues();
}
