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
