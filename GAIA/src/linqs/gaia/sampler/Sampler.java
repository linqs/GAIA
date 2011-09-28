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
package linqs.gaia.sampler;

import java.util.Iterator;

/**
 * Interfaces for dividing datasets into useable splits,
 * for such uses as distinguishing between training and testing data.
 *
 * @param <T> Set T split
 * @author Namata
 */
public interface Sampler<T> {
	/**
	 * Generate a sampling of the provided graph.
	 * 
	 * @param items Set of decorable items to sample
	 */
    public void generateSampling(Iterator<? extends T> items);
    
    /**
     * Return the number of training and test sample pairs generated.
     * 
     * @return Number of training and test sample pairs.
     */
    public int getNumSubsets();
    
    /**
     * Return the decorable items in the specified subset
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
    public Iterable<T> getSubset(int index);
    
    /**
     * Return the decorable items NOT in the specified subset
     * 
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
	public Iterable<T> getNotInSubset(int index);
}
