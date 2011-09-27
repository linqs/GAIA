package linqs.gaia.util;

/**
 * Utility for computing the enthropy of some probability distribution
 * 
 * @author namatag
 *
 */
public class Entropy {
	private static double logbase2 = Math.log(2);
	
	/**
	 * Computes the enthropy given the probability distribution
	 * 
	 * @param prob Array of double
	 * @return Enthropy
	 */
	public static double computeEntropy(double[] prob) {
		double e = 0;
		for(double p:prob) {
			// Handle special case of p=0
			// Note: Compute for log base 2
			e += p==0 ? 0 : p*(Math.log(p)/logbase2);
		}
		
		return e==0 ? e : -1.0 * e;
	}
}
