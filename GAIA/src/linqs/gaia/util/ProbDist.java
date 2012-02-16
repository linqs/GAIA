/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.util;

import java.util.Random;

import linqs.gaia.exception.InvalidStateException;

/**
 * Utility for sampling from and dealing with probability distributions
 * 
 * @author namatag
 *
 */
public class ProbDist {
	/**
	 * Given a discrete probability distribution,
	 * randomly sample from that probability distribution.
	 * 
	 * @param probs Array of doubles representing a probability distribution
	 * where the elements must add up to 1.0.
	 * @param ensurenonzero If true, replace 0.0 elements with some epsilon
	 * using {@link #makeElementsNonZero(double[])}
	 * @param rand Random number generator to use
	 * @return Index of randomly sampled value
	 */
	public static int sampleProbDist(double[] probs, boolean ensurenonzero, Random rand){
		double sum = 0;
		for(double d:probs) {
			// All entries should be positive
			if(d<0) {
				throw new InvalidStateException("All entries must be positive: "
						+ArrayUtils.array2String(probs, ","));
			}
			
			sum+=d;
		}
		
		if(ensurenonzero) {
			makeElementsNonZero(probs);
		}
		
		// Verify entries in array add up to 1 (or close enough given rounding error)
		if(Math.abs(1.0-sum)>.0001) {
			throw new InvalidStateException("Probability distribution should add up to 1: "
					+ArrayUtils.array2String(probs, ",")+" sums up to "+sum);
		}
		
		sum = 0;
		double randval = rand.nextDouble();
		for(int i=0; i<probs.length; i++) {
			sum+=probs[i];
			if(randval<sum) {
				return i;
			}
		}
		
		// Handle case of rounding error
		return probs.length-1;
	}
	
	/**
	 * Replace 0 entries with an epsilon.
	 * The sum of the epsilon is subtracted
	 * evenly from all non-zero values
	 * to ensure the overall sum remains the same.
	 * Note: The default is to assign the epsilon to 1e-10.
	 * If a value in the array is smaller than that,
	 * epsilong is set to the lowest value divided by 10 times
	 * the length of the array.
	 * 
	 * @param p Array of doubles
	 * @return Modified array
	 */
	public static double[] makeElementsNonZero(double[] p) {
		double epsilon = 1e-10;
		double numzero = 0;
		double minvalue = Double.POSITIVE_INFINITY;
		for(double val: p) {
			if(val==0.0) {
				numzero++;
			} else if(val<minvalue){
				minvalue = val;
			}
		}
		
		// Return if there are no zero values
		if(numzero==0) {
			return p;
		}
		
		// If there is at least one zero value and the minvalue is less than the
		// minimum allowed by the current epsilon, update the epsilon
		// to be the lowest value divided by 10 times
		// the length of the array.
		double minnonzero = p.length * epsilon;
		if(minvalue<minnonzero) {
			epsilon = minvalue / (10.0*p.length);
		}
		
		double subval = (numzero * epsilon)/(p.length-numzero);
		for(int i=0; i<p.length; i++) {
			if(p[i]==0.0) {
				p[i] = epsilon;
			} else {
				p[i] = p[i]-subval;
			}
		}
		
		return p;
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
	
	/**
	 * Generete a double array representing a uniform probability
	 * distribution for the given number of possible values.
	 * 
	 * @param numvalues Number of possible values
	 * @return Array representing uniform probability distribution
	 */
	public static double[] genUniformProbDistribution(int numvalues) {
		if(numvalues<1) {
			throw new InvalidStateException("Number of values must be greater than 0: "+numvalues);
		}
		
		double[] probs = new double[numvalues];
		double uniformprob = 1.0 / ((double) numvalues);
		for(int i=0; i<numvalues; i++) {
			probs[i] = uniformprob;
		}
		
		return probs;
	}
}
