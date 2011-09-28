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
package linqs.gaia.graph.event;

import linqs.gaia.model.Model;

/**
 * Event called whenever current modification of the data is complete by a given model.
 * This event, for example, is called when a model completes a given set of
 * prediction changes to a graph.
 * 
 * @author namatag
 *
 */
public class ModelCompletedEvent implements GraphEvent {
	private Model m;
	
	/**
	 * Constructor
	 * 
	 * @param m Model that completed predictions
	 */
	public ModelCompletedEvent(Model m) {
		this.m = m;
	}
	
	/**
	 * Return the completed model
	 * 
	 * @return Completed model
	 */
	public Model getCompletedModel() {
		return m;
	}
}
