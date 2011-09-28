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
package linqs.gaia.model.er.bootstrap;

import java.io.Serializable;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.GraphItem;

/**
 * Bootstrap interface for use with bootstrapping
 * entity resolution algorithms
 * 
 * @author namatag
 *
 */
public interface ERBootstrap extends Configurable, Serializable {
	/**
	 * Return true if the two graph items are the same
	 * entity and false otherwise.
	 * 
	 * @param gi1 First graph item
	 * @param gi2 Second graph item
	 * @return True if the graph items refer to the same entity, false otherwise
	 */
	boolean isSameEntity(GraphItem gi1, GraphItem gi2);
}
