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
package linqs.gaia.graph.filter;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Base class for all filter implementations.  Filters are classes which
 * remove something from the graph.  The removed item may include nodes,
 * edges, and features.  Examples include: removing features
 * with the given name, removing nodes with degree less than D, etc.
 * 
 * @author namatag
 *
 */
public abstract class Filter extends BaseConfigurable implements Configurable {	
	public abstract void filter(Graph graph);
	
	/**
	 * Static function to call a filters specified in a list in order.
	 * 
	 * @param graph Graph to run the filter over
	 * @param filters List of filters to run in turn
	 */
	public static void filterFeatures(Graph graph, List<Filter> filters){
		for(Filter f: filters){
			f.filter(graph);
		}
	}
}
