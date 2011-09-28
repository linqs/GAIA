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
package linqs.gaia.graph;


/**
 * Base interface for all parts of the code which require
 * the graph in order to do its function.
 * 
 * @author namatag
 *
 */
public interface GraphDependent {
	/**
	 * Set the graph for the object
	 * 
	 * @param g Graph
	 */
	void setGraph(Graph g);
}
