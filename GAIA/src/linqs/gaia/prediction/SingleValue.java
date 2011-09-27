package linqs.gaia.prediction;


/**
 * Predictions where a single class is predicted.
 * 
 * @author namatag
 *
 */
public interface SingleValue extends Prediction {
	/**
	 * Get true class value
	 * 
	 * @return True value
	 */
	Object getTrueValue();
	
	/**
	 * Get predicted class value.
	 * If no was predicted, return null.
	 * 
	 * @return Predicted value
	 */
	Object getPredValue();
}
