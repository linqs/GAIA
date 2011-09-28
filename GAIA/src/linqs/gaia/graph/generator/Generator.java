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
package linqs.gaia.graph.generator;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Base interface for graph generation models i.e., Preferential Attachment, Forest Fire
 * 
 * @author namatag
 *
 */
public interface Generator extends Configurable {
	/**
	 * Generate a graph using this model
	 * 
	 * @return Graph
	 */
	Graph generateGraph();
	
	/**
	 * Generate a graph using this model
	 * 
	 * @param objid Object ID to use for graph
	 * @return Graph
	 */
	Graph generateGraph(String objid);
}
