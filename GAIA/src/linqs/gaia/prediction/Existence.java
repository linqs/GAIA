package linqs.gaia.prediction;


/**
 * For predictions related to whether or not something exists
 * 
 * @author namatag
 *
 */
public interface Existence extends Prediction {
	/**
	 * Predicted value of is this a prediction that something exists or that
	 * something doesn't exist.
	 * 
	 * @return Existence status
	 */
	boolean getPredExist();
	
	/**
	 * True value of is this a prediction that something exists or that
	 * something doesn't exist.
	 * 
	 * @return Existence status
	 */
	boolean getTrueExist();
	
	/**
	 * Get object whose existence status was predicted
	 * 
	 * @return Object predicted
	 */
	Object getPredObject();
}
