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
package linqs.gaia.similarity.list;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.similarity.ListDistance;
import linqs.gaia.similarity.NormalizedListSimilarity;

/**
 * Calculate the match between two feature lists.
 * <p>
 * This is related to the Hamming distance where 
 * of the two object lists, we add 1 to the distance if they entries are not equal
 * and add 0 if they are.  For similarity, we do the opposite and add
 * a 1 to the similarity count if they are equal and add 0 if they are not.
 * For the normalized similarity, instead of returning
 * a distance, we return (unnormalized similarity)/(list size).
 * <p>
 * <UL>
 * <LI> sparsevalue-String representation of the sparse object used.
 * If specified, a match is only counted if the string representation
 * of both objects are the same as the sparse value.  If the object being
 * compared is a {@link FeatureValue}, the value returned by {@link FeatureValue#getStringValue()}
 * instead of the {@link FeatureValue#toString()} value.
 * </UL>
 * @author namatag
 *
 */
public class FeatureMatch extends BaseConfigurable implements NormalizedListSimilarity,
	ListDistance {
	
	private static final long serialVersionUID = 1L;
	private String sparsevalue = null;
	private boolean initialize = true;
	
	private void initialize() {
		initialize = false;
		sparsevalue = this.getStringParameter("sparsevalue", null);
	}

	public double getSimilarity(List<? extends Object> item1, List<? extends Object> item2) {
		double distance = this.getDistance(item1, item2);
		double size = item1.size();
		
		return (size - distance);
	}

	public double getNormalizedSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		double size = item1.size();
		if(size == 0) {
			return 0;
		}
		
		return this.getSimilarity(item1, item2)/size;
	}

	public double getDistance(List<? extends Object> item1,
			List<? extends Object> item2) {
		if(initialize) {
			this.initialize();
		}
		
		double distance = 0;
		if(item1.size()!=item2.size()){
			throw new InvalidOperationException("Incomparable feature lists of varying sizes: item1="
					+item1.size()+ " item2="+item2.size());
		}
		
		double size = item1.size();
		for(int i=0; i<size; i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			if((i1val==null && i2val!=null)
				|| (i2val==null && i1val!=null)
				|| (i1val!=null && i2val!=null && !i1val.equals(i2val))){
				
				distance+=1;
			} else {
				// Even if they match, if sparse value is specified,
				// only count a match if both are equal to the sparse value.
				if(sparsevalue != null && i1val!=null && i2val!=null) {
					String i1valstring = i1val instanceof FeatureValue
						? ((FeatureValue) i1val).getStringValue(): i1val.toString();
					
					// Even if they match, count as not matching
					// if the value they match for is not the sparse value
					// Note:  Since by this point the values must be the same,
					// we only need to check sparse value match for the first one.
					if(!i1valstring.equals(sparsevalue)) {
						distance+=1;
					}
				}
			}
		}
		
		return distance;
	}
}
