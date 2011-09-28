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
package linqs.gaia.sampler.decorable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.SimplePair;

/**
 * Create splits using the ordering specified for the raw value of the feature value.
 * By default, the specified number of splits, K, made over N items, is created where
 * the first split has the first N/K items, the second split has the next N/K items,
 * and so on.  If the feature value is temporal (e.g., the date an edge was added),
 * this will create evenly split the data based on time.
 * If splitbyvalue is yes, the feature value will be used to create the split.
 * For example, if the split value is the year, there will be K split
 * corresponding to the number of unique years in the data such that
 * all items in the same split have the same year and all items with the same value
 * are in the same split.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureid-Feature value whose ordinal value will be used to create an
 * order over the decorable items.  An exception is thrown if this value
 * is unknown for any of the specified decorable items.
 * <LI> numsubsets-This parameter required if splitbyunique is no.
 * Number of subsets of teh data to generate.  If splitbyunique is yes, this parameter
 * will be overriden by the number of unique values in the data.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> splitbyunique-If yes, create splits such as all nodes in the same
 * split have the same value and all nodes with the same value are in the same split.
 * </UL>
 * 
 * @author namatag
 *
 */
public class OrderedFeatureSampler extends DecorableSampler {
	@Override
	public void generateSampling(Iterator<? extends Decorable> items) {
		String featureid = this.getStringParameter("featureid");
		
		boolean splitbyunique = this.hasYesNoParameter("splitbyunique", "yes");
		
		if(!splitbyunique) {
			this.numsubsets = (int) this.getDoubleParameter("numsubsets");
		}
		
		List<SimplePair<Decorable,Object>> ordered =
			new ArrayList<SimplePair<Decorable,Object>>();
		while(items.hasNext()) {
			Decorable d = items.next();
			
			FeatureValue fv = d.getFeatureValue(featureid);
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				throw new InvalidStateException("Feature value unknown: "+d+" for "+featureid);
			}
			
			SimplePair<Decorable,Object> pair = new SimplePair<Decorable,Object>(d,fv.getRawValue());
			ordered.add(pair);
			
			// Store list of all items
			this.allitems.add(d);
		}
		
		Collections.sort(ordered);
		
		if(splitbyunique) {
			// Create splits where all decorable in the same split
			// have the same value
			Object lastfv = null;
			int currsplit = -1;
			for(SimplePair<Decorable,Object> pair:ordered) {
				if(lastfv==null || !lastfv.equals(pair.getSecond())) {
					lastfv = pair.getSecond();
					this.subsets.add(new ArrayList<Decorable>());
					currsplit++;
				}
				
				this.subsets.get(currsplit).add(pair.getFirst());
			}
			
			this.numsubsets = currsplit+1;
		} else {
			// Create roughly equal sized splits
			if(this.numsubsets<0 || this.numsubsets>ordered.size()) {
				throw new ConfigurationException("Invalid number of splits: "+this.numsubsets);
			}
			
			int persplit = ordered.size() / this.numsubsets;
			int numadded = 0;
			int currsplit = -1;
			for(SimplePair<Decorable,Object> pair:ordered) {
				if(numadded % persplit == 0 && this.numsubsets!=this.subsets.size()) {
					this.subsets.add(new ArrayList<Decorable>());
					currsplit++;
				}
				
				this.subsets.get(currsplit).add(pair.getFirst());
				numadded++;
			}
		}
	}
}
