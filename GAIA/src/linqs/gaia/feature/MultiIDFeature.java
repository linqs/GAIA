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
package linqs.gaia.feature;

/**
 * Multiple ID feature interface for features which return
 * a list of ID objects.  The ID objects can be used
 * to refer to another identifiable object (i.e., graphs, nodes, edges,
 * graph items in the same or across graphs).
 * For example, a node created from "merging" nodes
 * in entity resolution can have a feature which lists
 * all the IDs of the nodes it was merged from.
 * <p>
 * MultiIDFeature features all return MultiIDValue objects.
 * 
 * @see linqs.gaia.feature.values.MultiIDValue
 * 
 * @author namatag
 *
 */
public interface MultiIDFeature extends Feature {
	
}
