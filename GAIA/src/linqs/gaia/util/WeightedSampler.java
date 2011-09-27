package linqs.gaia.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import linqs.gaia.exception.InvalidStateException;

/**
 * Implementation for performing a weighted sampling given a set
 * of objects and some positive numeric value indicating that objects weight.
 * 
 * @author namatag
 *
 */
public class WeightedSampler {
	/**
	 * Perform a weighted sampling
	 * 
	 * @param objects List of objects
	 * @param weights List of weights (with a one-to-one correspondence, by index, to the list of objects)
	 * @param numsamples Number of samples to return
	 * @param withreplacement If true, do a weighted sampling with replacement
	 * @param rand Random number generator
	 * @return Objects sampled by weight
	 */
	public static List<Object> performWeightedSampling(List<?> objects, List<Double> weights,
			int numsamples, boolean withreplacement, Random rand) {
		if(!withreplacement) {
			// If sampling without replacement, we may need to modify the object list
			objects = new ArrayList<Object>(objects);
			weights = new ArrayList<Double>(weights);
		}
		
		if(objects.size()!=weights.size()) {
			throw new InvalidStateException("Object and weight lists must be of the same length: "
					+"Object List Size="+objects.size()+" Weight List Size="+weights.size());
		}
		
		int numobj = objects.size();
		if(!withreplacement && numobj<numsamples) {
			throw new InvalidStateException("Number of objects cannot be less than the number of samples: "
					+numobj+"<"+numsamples);
		}
		
		Double[] upperbounds = new Double[objects.size()];
		
		double sum = 0;
		int i=0;
		for(Double w:weights) {
			if(w<=0 || Double.isInfinite(w) || Double.isNaN(w)) {
				throw new InvalidStateException("Weight must be positive: "+w);
			}
			
			sum+=w;
		}
		
		// Normalize weights
		double upperbound = 0.0;
		double normalizer = sum;
		for(i=0; i<numobj; i++) {
			upperbound += weights.get(i)/normalizer;
			upperbounds[i] = upperbound;
		}
		
		List<Object> samples = new ArrayList<Object>();
		for(i=0; i<numsamples; i++) {
			int matchindex = -1;
			double rval = rand.nextDouble();
			numobj = upperbounds.length;
			
			// Randomly sample by randomly choosing
			// a number and finding the bin it belongs too
			// assuming the array contains the upperbound of each bin.
			if(rval < upperbounds[0]) {
				// Check first
				matchindex=0;
			} else if(rval > upperbounds[upperbounds.length-2]) {
				// Check last by seeing if its larger than the second to the last upperbound
				// (last upperbound should just be 1).
				matchindex=upperbounds.length-1;
			} else if(numobj<5){
				// If its small enough, just do a search
				for(int j=0; j<numobj; j++) {
					if(rval<upperbounds[j]) {
						matchindex=j;
						break;
					}
				}
			} else {
				// Do binary search
				int lb = 0;
				int ub = upperbounds.length-1;
				while(true) {
					if(lb>ub) {
						throw new InvalidStateException("Value not found: "+rval);
					} else if(ub-lb<5) {
						// If its small enough, just do a search
						for(int j=lb; j<=ub; j++) {
							if(rval<upperbounds[j]) {
								matchindex = j;
								break;
							}
						}
						
						break;
					} else if(lb==ub) {
						matchindex = lb;
						break;
					}
					
					int b = (int) Math.ceil((double) (ub+lb)/2.0);
					if(rval<upperbounds[b] && rval>upperbounds[b-1]) {
						matchindex = b;
						break;
					} else if(rval==upperbounds[b]) {
						matchindex = (b+1)<upperbounds.length ? b+1 : b;
						break;
					} else if(rval<upperbounds[b]) {
						ub = b-1;
					} else if(rval>upperbounds[b]) {
						lb = b+1;
					} else {
						throw new InvalidStateException("Unexpected case encountered during binary search");
					}
				}
			}
			
			samples.add(objects.get(matchindex));
			// If sampling without replacement, we need to update
			// the object list and upperbound array
			if(!withreplacement) {				
				normalizer = normalizer - weights.get(matchindex);
				
				// Remove items from lists
				objects.remove(matchindex);
				weights.remove(matchindex);
				
				// Update upperbounds
				upperbounds = new Double[upperbounds.length-1];
				int ublength = upperbounds.length;
				upperbound=0;
				for(int u=0; u<ublength; u++) {
					upperbound += weights.get(u) / normalizer;
					upperbounds[u] = upperbound;
				}
			}
		}
		
		return samples;
	}
	
	/**
	 * Perform a weighted sampling
	 * 
	 * @param objectweights A map where the key is the object and the value is weight for that object
	 * @param withreplacement If true, do a weighted sampling with replacement
	 * @param rand Random number generator
	 * @return Objects sampled by weight
	 */
	public static List<Object> performWeightedSampling(Map<?,Double> objectweights,
			int numinstances, boolean withreplacement, Random rand) {
		List<Object> objects = new ArrayList<Object>();
		List<Double> weights = new ArrayList<Double>();
		Set<?> keys = objectweights.keySet();
		for(Object k:keys) {
			objects.add(k);
			weights.add(objectweights.get(k));
		}
		
		return WeightedSampler.performWeightedSampling(objects, weights, numinstances, withreplacement, rand);
	}
}
