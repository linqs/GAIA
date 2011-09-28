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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.log.Log;
import linqs.gaia.util.ListUtils;

/**
 * Does a random sampling over the set of Decorable items to create equal sized splits.
 * <p>
 * Note:  The implementation currently stores the splits in memory.  The implementation
 * can be changed to store the splits elsewhere, i.e. from a file, by making a custom
 * iterable object.
 * 
 * Required Parameters:
 * <UL>
 * <LI>numsubsets-Number of subsets to split to
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>numsubsets-Number of subsets to split to
 * <LI>seed-Random generator seed for sampling.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomSampler extends DecorableSampler {
	private Random rand = null;
	
	public void generateSampling(Iterator<? extends Decorable> items) {
		this.numsubsets = (int) this.getDoubleParameter("numsubsets");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
		
		this.subsets.clear();
		for(int i=0; i<this.numsubsets; i++){
			this.subsets.add(new LinkedList<Decorable>());
		}
		
		// Iterate over decorable items
		int siindex = 0;
		List<Integer> shuffledindices = null;
		while(items.hasNext()) {
			Decorable di = items.next();
			if(siindex % subsets.size() == 0) {
				// Vary the order the subsets are added into
				shuffledindices = ListUtils.shuffledIndices(subsets, rand);
			}
			
			// Add decorable items to subsets
			subsets.get(shuffledindices.get(siindex)).add(di);
			
			// Store list of all items
			this.allitems.add(di);
		}
		
		// Show statistics per split
		if(Log.SHOWDEBUG) {
			for(int i=0; i<this.numsubsets;i++){
				Log.DEBUG("Split "+i+": "+subsets.size());
			}
		}
	}
}
