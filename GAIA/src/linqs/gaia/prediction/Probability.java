package linqs.gaia.prediction;


public interface Probability extends Prediction {
	/**
	 * Get probability distribution over all possible classes
	 * 
	 * @return Probability Distribution
	 */
	double[] getProbs();
}
