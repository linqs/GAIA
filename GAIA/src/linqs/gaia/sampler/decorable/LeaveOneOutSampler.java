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
import java.util.Iterator;
import java.util.List;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Sampler to perform leave one out cross validation.
 * In this sampler, each subset consists of exactly one
 * of the items.
 * 
 * @author namatag
 *
 */
public class LeaveOneOutSampler extends DecorableSampler {
	@Override
	public void generateSampling(Iterator<? extends Decorable> items) {
		while(items.hasNext()) {
			Decorable d = items.next();
			List<Decorable> split = new ArrayList<Decorable>(1);
			split.add(d);
			this.subsets.add(split);
			
			// Store list of all items
			this.allitems.add(d);
		}
		
		this.numsubsets = this.subsets.size();
	}
}
