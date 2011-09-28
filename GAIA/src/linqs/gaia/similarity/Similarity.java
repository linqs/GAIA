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
package linqs.gaia.similarity;

import java.io.Serializable;

import linqs.gaia.configurable.Configurable;

/**
 * Base interface of all implementations of unnormalized similarity measures.
 * By definition, similarity is symmetric i.e., sim(a,b) == sim(b,a).
 * Also, the HIGHER the returned similarity value, the MORE similar
 * the two items are.
 * 
 * @author namatag
 *
 */
public interface Similarity<O> extends Configurable, Serializable {
	/**
	 * Return similarity between two items
	 * 
	 * @param item1 First Item
	 * @param item2 Second Item
	 * 
	 * @return Unnormalized similarity
	 */
	double getSimilarity(O item1, O item2);
}
