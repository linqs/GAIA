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
package linqs.gaia.model.oc.active;

import java.util.List;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.oc.active.query.Query;

/**
 * Interface for multi-task active learning algorithms
 * 
 * @author namatag
 *
 */
public interface MultiTaskActiveLearning extends Configurable {
	/**
	 * Initialize the active learning algorithm to create
	 * queries for items of the specified schemas and features.
	 * There must be a one-to-one correspondence between each schema ID and feature ID.
	 * 
	 * @param targetschemaids List of schema IDs
	 * @param targetfeatureids List of feature IDs
	 */
	void initialize(List<String> targetschemaids, List<String> targetfeatureids);
	
	/**
	 * Return an ordered list over the queries of the graph.
	 * The queries are ordered by the informativeness (i.e., score)
	 * of the query.
	 * 
	 * @param g Graph
	 * @param numqueries Number of queries to return
	 * @return Ordered list of items and features
	 */
	List<Query> getQueries(Graph g, int numqueries);
	
	/**
	 * Return an ordered list over the queries of the specified items.
	 * The queries are ordered by the informativeness (i.e., score)
	 * of the query.
	 * 
	 * @param testitems Items to apply active learning over
	 * @param numqueries Number of queries to return
	 * @return Ordered list of items and features
	 */
	List<Query> getQueries(List<Iterable<? extends Decorable>> testitems, int numqueries);
}
