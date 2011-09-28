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
package linqs.gaia.graph.datagraph;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;

/**
 * Edge implementation of DataGraph
 * 
 * @see DataGraph
 * 
 * @author namatag
 *
 */
public abstract class DGEdge extends DGGraphItem implements Edge {
	private static final long serialVersionUID = 1L;
	
	public DGEdge(DataGraph graph, GraphItemID schemaID) {
		super(graph, schemaID);
	}
	
	public String toString(){
		return GraphItemUtils.getEdgeIDString(this);
	}
	
	/**
	 * Internal method to handle maintenance of edge when nodes are removed.
	 * 
	 * @param n Node
	 */
	protected abstract void nodeRemovedNotification(Node n);
	
	/**
	 * Check if the edge is valid.  If not, throw an exception.
	 * 
	 * @param message String message to include in exception if invalid.
	 */
	protected abstract void checkValidity(String message);
	
	/**
	 * Check to see if edge is valid.
	 * 
	 * @return True if valid.  False otherwise.
	 */
	protected abstract boolean isValid();
}
