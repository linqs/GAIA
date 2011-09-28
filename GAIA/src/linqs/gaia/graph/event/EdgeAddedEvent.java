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

import linqs.gaia.graph.Edge;

/**
 * Graph Event that called when an edge is added.
 * The event has a pointer to the added edge.
 * 
 * @author namatag
 *
 */
public class EdgeAddedEvent implements GraphEvent {
	private Edge e;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param e Edge that was added
	 */
	public EdgeAddedEvent(Edge e) {
		this.e = e;
	}
	
	/**
	 * Get edge that was added
	 * 
	 * @return Edge that was added
	 */
	public Edge getAddedEdge() {
		return e;
	}
}
