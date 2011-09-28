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
import linqs.gaia.similarity.ListDistance;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.util.Numeric;

/**
 * Returns the sum of the absolute value of differences of numeric interpretations
 * of the values given.  (e.g., [1,2,3] [0,1,2] is (1-0)+(2-1)+(3-2)=1) where
 * double values of objects are parsed by Numeric.ParseDouble.
 * <p>
 * The normalized similarity is the average difference of the values.
 * (i.e., the difference/number of items in a list).  Normalized
 * similarity assumes that all values are between [0,1].
 * An exception is thrown when a value is encountered not between
 * 0 and 1.
 * For similarity, we return the normalized similarity value.
 * 
 * Optional parameters:
 * <UL>
 * <LI> normfactor-Normalization factor for items where the normalized
 * value is (original distance)/normfactor.  If the returned value is not
 * normalized, an exception is thrown unless capnormfactor is set to yes.
 * <LI> capnormfactor-If yes, if the distance is creater than the norm
 * factor, the value is set to 1 (the maximum value).
 * </UL>
 * 
 * @see linqs.gaia.util.Numeric
 * 
 * @author namatag
 *
 */
public class NumericDifference extends BaseConfigurable implements NormalizedListSimilarity,
	ListDistance {

	private static final long serialVersionUID = 1L;

	public double getSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		return this.getNormalizedSimilarity(item1, item2);
	}

	public double getNormalizedSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		double distance = 0;
		double size = item1.size();
		if(item1.size()!=item2.size()){
			throw new InvalidOperationException("Incomparable feature lists of varying sizes: item1="
					+item1.size()+ " item2="+item2.size());
		}
		
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			if(i1val == null || i2val == null) {
				throw new InvalidOperationException("Values cannot be null: "
						+"item1="+i1val+ " item2="+i2val);
			}
			
			// Object must be some sort of numeric feature
			double dval1 = Numeric.parseDouble(i1val);
			double dval2 = Numeric.parseDouble(i2val);
			
			if(this.hasParameter("normfactor")) {
				double normfactor = this.getDoubleParameter("normfactor");
				distance += Math.abs((dval1-dval2));
				
				if(this.hasYesNoParameter("capnormfactor","yes") && distance > normfactor) {
					distance = normfactor;
				}
				
				if(distance > normfactor) {
					throw new InvalidOperationException("Distance is larger than the normalization factor: "
							+" distance="+distance+" normfactor="+normfactor);
					
				}
				
				distance = distance / normfactor;
			} else {
				if(dval1 < 0 || dval1 > 1 || dval2 < 0 || dval2 > 1) {
					throw new InvalidOperationException("Values not normalized: "
							+"item1="+dval1+ " item2="+dval2);
				}
				
				distance += Math.abs((dval1-dval2));
			}
		}
		
		return 1 - (distance/size);
	}

	public double getDistance(List<? extends Object> item1,
			List<? extends Object> item2) {
		double distance = 0;
		if(item1.size()!=item2.size()){
			throw new InvalidOperationException("Incomparable feature lists of varying sizes: item1="
					+item1.size()+ " item2="+item2.size());
		}
		
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			if(i1val == null || i2val == null) {
				throw new InvalidOperationException("Values cannot be null: "
						+"item1="+i1val+ " item2="+i2val);
			}
			
			// Object must be some sort of numeric feature
			double dval1 = Numeric.parseDouble(i1val);
			double dval2 = Numeric.parseDouble(i2val);
			
			distance += Math.abs((dval1-dval2));
			
			// Normalize distance, if requested
			if(this.hasParameter("normfactor")) {
				double normfactor = this.getDoubleParameter("normfactor");
				if(distance > normfactor) {
					throw new InvalidOperationException("Distance is larger than the normalization factor: "
							+" distance="+distance+" normfactor="+normfactor);
					
				}
				
				distance = distance / normfactor;
			}
		}
		
		return distance;
	}
}
