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

import linqs.gaia.graph.Node;

/**
 * Graph Event that called when an node is removed.
 * The event has a pointer to the removed node.
 * 
 * @author namatag
 *
 */
public class NodeRemovedEvent implements GraphEvent {
	private Node n;
	
	/**
	 * Create instance of Graph Event
	 * 
	 * @param n Node that was removed
	 */
	public NodeRemovedEvent(Node n) {
		this.n = n;
	}
	
	/**
	 * Get node that was removed
	 * 
	 * @return Removed node
	 */
	public Node getRemovedNode() {
		return n;
	}
}
