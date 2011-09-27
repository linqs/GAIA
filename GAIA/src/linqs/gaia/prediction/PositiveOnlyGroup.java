package linqs.gaia.prediction;


/**
 * Predictions where predictions include only those things which are predicted positive.
 * 
 * @author namatag
 * 
 */
public interface PositiveOnlyGroup extends PredictionGroup {
	/**
	 * Number of items predicted, including the negative predictions not listed.
	 * 
	 * @return Return the total number
	 */
	int getNumTotal();
	
	/**
	 * Get the number of true positives
	 * 
	 * @return Number of true positives
	 */
	int getNumPositive();
	
	/**
	 * Get the value to use as the positive value
	 * 
	 * @return Object of positive value
	 */
	Object getPositiveValue();
	
	/**
	 * Get the value to use as the negative value
	 * 
	 * @return Object of negative value
	 */
	Object getNegativeValue();
}
