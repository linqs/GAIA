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
package linqs.gaia.model.oc;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;

/**
 * Interface for object classification models
 * which can predict target features where each item
 * can have multiple values for that feature.  For example,
 * in protein function prediction, a protein can have
 * multiple different functions.
 * 
 * @author namatag
 *
 */
public interface MultiValueClassifier extends Model {
	/**
	 * Learn a classifier for the given target feature using the following
	 * training items.
	 * 
	 * @param trainitems Decorable items to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of feature to classify
	 */
	void learn(Iterable<? extends Decorable> trainitems, String targetschemaid, String targetfeatureid);
	
	/**
	 * Learn a classifier for the given target feature using the
	 * labeled instances from the given graph.
	 * 
	 * @param traingraph Graph to train over
	 * @param targetschemaid Schema ID of object whose features we're classifying
	 * @param targetfeatureid Feature ID of feature to classify
	 */
	void learn(Graph traingraph, String targetschemaid, String targetfeatureid);
	
	/**
	 * Predict features over the specified test items.
	 * 
	 * @param testitems Decorable items to test over
	 */
	void predict(Iterable<? extends Decorable> testitems);
	
	/**
	 * Predict features over the unlabeled items of the graph
	 * 
	 * @param testgraph Graph to predict feature over
	 */
	void predict(Graph testgraph);
}
