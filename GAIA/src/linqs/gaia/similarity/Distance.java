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

/**
 * Base interface of all implementations of unnormalized distance measures.
 * By definition, distance is symmetric i.e., dist(a,b) == dist(b,a)
 * where the HIGHER the returned similarity value, the LESS similar
 * the two items are.
 * 
 * @author namatag
 *
 */
public interface Distance<O> {
	/**
	 * Return distance between two items
	 * 
	 * @param item1 First Item
	 * @param item2 Second Item
	 * 
	 * @return Unnormalized distance
	 */
	double getDistance(O item1, O item2);
}
