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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.sampler.Sampler;

/**
 * Class for sampling test and training subsets of some set of Decorable items.
 * Once a sampling is generated, the training and test subsets can
 * be accessed by index.
 * 
 * @author namatag
 *
 */
public abstract class DecorableSampler extends BaseConfigurable implements Sampler<Decorable> {
	protected List<List<Decorable>> subsets = new LinkedList<List<Decorable>>();
	protected List<Decorable> allitems = new LinkedList<Decorable>();
	protected int numsubsets = 2;
	
	/**
	 * Generate a sampling of the provided graph.
	 * 
	 * @param items Set of decorable items to sample
	 */
    public abstract void generateSampling(Iterator<? extends Decorable> items);
    
    /**
     * Return the number of training and test sample pairs generated.
     * 
     * @return Number of training and test sample pairs.
     */
    public int getNumSubsets() {
		return this.numsubsets;
	}
    
    /**
     * Return the decorable items in the specified subset
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
    public Iterable<Decorable> getSubset(int index) {
		if(this.subsets.isEmpty()) {
			throw new InvalidStateException("Sample not yet generated");
		}
		
		return this.subsets.get(index);
	}
    
    /**
     * Return the decorable items NOT in the specified subset
     * 
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
	public Iterable<Decorable> getNotInSubset(int index) {
		if(this.subsets.isEmpty()) {
			throw new InvalidStateException("Sample not yet generated");
		}
		
		// Get all instances
		Collection<Decorable> notinsubset = new LinkedList<Decorable>(allitems);
		
		// Remove all instances in the specified subset
		notinsubset.removeAll(subsets.get(index));
		
		return notinsubset;
	}
}
