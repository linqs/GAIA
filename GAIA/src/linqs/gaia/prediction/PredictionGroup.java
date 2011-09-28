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
package linqs.gaia.prediction;

import java.util.Iterator;

/**
 * Base interface for the set of generated predictions from a model.
 * 
 * @author namatag
 *
 */
public interface PredictionGroup {
	/**
	 * Get set of predictions
	 * 
	 * @return Set of predictions
	 */
	Iterator<? extends Prediction> getAllPredictions();
	
	/**
	 * Remove the specified predictions from the graph.
	 * i.e., Remove the labels predicted in the graph and set them to unknown
	 */
	void removeAllPredictions();
	
	/**
	 * Return the number of predictions in this group
	 * 
	 * @return Number of predictions
	 */
	int numPredictions();
}
