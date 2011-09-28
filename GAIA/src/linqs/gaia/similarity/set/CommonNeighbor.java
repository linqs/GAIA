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
package linqs.gaia.similarity.set;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.similarity.NormalizedSetSimilarity;

/**
 * Calculate common neighbor similarity between two sets of objects.
 * <p>
 * Reference:
 * "Collective Entity Resolution in Relational Data", Indrajit Bhattacharya and Lise Getoor,
 * ACM Transactions on Knowledge Discovery from Data (ACM-TKDD), 2007.
 * <p>
 * CommonNbrScore(ci,cj)=(1/K)x|(Nbr(ci) intersect Nbr(cj))|
 * <p>
 * Note: The unnormalized similarity is not divided by K and is just
 * the size of the intersection of the two sets.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>K-Constant used to make sure measure is less than 1.  K=100 by default.
 * </UL>
 * 
 * @author namatag
 */
public class CommonNeighbor extends BaseConfigurable implements NormalizedSetSimilarity {
	private static final long serialVersionUID = 1L;
	
	public double getSimilarity(Set<? extends Object> item1, Set<? extends Object> item2) {
		Set<Object> isect = new HashSet<Object>(item1);
		isect.retainAll(item2);
		
		return ((double) isect.size());
	}

	public double getNormalizedSimilarity(Set<? extends Object> item1,
			Set<? extends Object> item2) {
		double sim = this.getSimilarity(item1, item2);
		double K = 100;
		if(this.hasParameter("K")){
			K = this.getDoubleParameter("K");
			
			if(K<=0 || K < sim){
				throw new ConfigurationException("Invalid K value: "+K);
			}
		}
		
		return sim/K;
	}
}
