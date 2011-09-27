package linqs.gaia.prediction.statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.Probability;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.ProbDist;

/**
 * Compute the average KL-Divergence to the true value
 * where the true value has a probability 1.0 for the real value
 * and 0 otherwise.
 * <p>
 * Note: The prediction group must be a {@link CategoricalValuedGroup}
 * with predictions which are {@link SingleValue} and has {@link Probability}.
 * Also, to deal with cases where this is not defined, we replace
 * replace 0.0 elements in the probability distribution with some epsilon
 * using {@link ProbDist#makeElementsNonZero(double[])};
 * 
 * @author namatag
 *
 */
public class AverageKLDivergence extends BaseConfigurable implements Statistic {
	private static String kldivergence = "kldivergence";

	public List<String> getHeader() {
		List<String> header = new ArrayList<String>();
		header.add(kldivergence);
		
		return header;
	}

	public Map<String, Double> getStatisticDoubles(PredictionGroup predictions) {
		Map<String,Integer> cat2index = new HashMap<String,Integer>(); 
		List<String> categories = null;
		if(predictions instanceof CategoricalValuedGroup) { 
			categories = ((CategoricalValuedGroup) predictions).getCategories().copyAsList();
		} else {
			throw new UnsupportedTypeException("Unsupported prediction group type: "
					+predictions.getClass().getCanonicalName());
		}
		
		// Map labels to indices
		for(int i=0; i<categories.size(); i++) {
			cat2index.put(categories.get(i), i);
		}
		
		Map<String, Double> stats = new HashMap<String,Double>(1);
		Iterator<? extends Prediction> itr = predictions.getAllPredictions();
		
		MinMax mm = new MinMax();
		while(itr.hasNext()) {
			Prediction p = itr.next();
			
			if(!(p instanceof SingleValue)){
				throw new UnsupportedTypeException("Unsupported prediction type: "
						+p.getClass().getCanonicalName());
			}
			
			SingleValue svp = (SingleValue) p;
			if(svp.getTrueValue()==null){
				throw new InvalidStateException("True value not provided for: "+svp);
			}
			
			if(!(p instanceof Probability)){
				throw new UnsupportedTypeException("Unsupported prediction type: "
						+p.getClass().getCanonicalName());
			}
			
			double[] predprobs = ((Probability) p).getProbs();
			double[] trueprobs = new double[predprobs.length];
			trueprobs[cat2index.get(svp.getTrueValue())]=1;
			
			mm.addValue(computeKLDivergence(predprobs, trueprobs));
		}
		
		stats.put(AverageKLDivergence.kldivergence, mm.getMean());
		
		return stats;
	}
	
	/**
	 * Compute the KL divergence between the discrete probability
	 * distribution represented by the array of doubles.
	 * 
	 * Note: In order to handle the special where an element in the array q is zero,
	 * those values are replaced with an epsilon. See {@link ProbDist#makeElementsNonZero}
	 * for details.
	 * 
	 * @param p Array of doubles representing the first probility distribution
	 * @param q Array of doubles representing the second probility distribution
	 * @return KL-Divergence of p from q
	 */
	public static double computeKLDivergence(double[] p, double[] q) {
		double kldiv = 0;
		
		// Ensure KL-Divergence remains defined by replacing all 0 with epsilon
		q = ProbDist.makeElementsNonZero(q);
		
		int size = p.length;
		for(int i=0; i<size; i++) {
			if(p[i]==0) {
				// Add 0, do nothing
			} else {
				kldiv += p[i] * (Math.log(p[i]/q[i]) / Math.log(2));
			}
		}
		
		return kldiv;
	}
	
	/**
	 * Compute the symmetric KL divergence between the discrete probability
	 * distribution represented by the array of doubles.
	 * 
	 * Note: In order to handle the special where an element in the array q is zero,
	 * those values are replaced with an epsilon. See {@link ProbDist#makeElementsNonZero}
	 * for details.
	 * 
	 * @param p Array of doubles representing the first probility distribution
	 * @param q Array of doubles representing the second probility distribution
	 * @return KL-Divergence of p from q
	 */
	public static double computeSymmetricKLDivergence(double[] p, double[] q) {
		return computeKLDivergence(p,q) + computeKLDivergence(q,p);
	}
	
	public String getStatisticString(PredictionGroup predictions) {
		return AverageKLDivergence.kldivergence+"="+this.getStatisticDoubles(predictions).get(AverageKLDivergence.kldivergence);
	}

	public Map<String, String> getStatisticStrings(PredictionGroup predictions) {
		Map<String, String> stringstats = new HashMap<String,String>(1);
		stringstats.put(AverageKLDivergence.kldivergence, this.getStatisticString(predictions));
		
		return stringstats;
	}

}
