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

import java.util.Iterator;

/**
 * A Edge is a connection between Node objects.  This includes
 * notions such as ``friend'' or ``participatedIn.''
 * <p>
 * Note:  If the removal of a node from the graph or from the edge results
 * in an invalid edge, an exception is thrown.
 * 
 * @see Graph
 * @see Node
 * 
 * @author namatag
 *
 */
public interface Edge extends GraphItem {
	/**
	 * Return all adjacent Nodes
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<Node> getAllNodes();

	/**
	 * Return all adjacent Nodes with the given schemaID
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param schemaID Edge Schema ID
	 * @return Iterator over edges
	 */
	Iterator<Node> getAllNodes(String schemaID);

	/**
	 * Get the number of unique nodes incident to edge
	 * 
	 * @return Number of nodes
	 */
	int numNodes();

	/**
	 * Remove the Node from this Edge.
	 * 
	 * @param n Node to remove
	 */
	void removeNode(Node n);

	/**
	 * Return true if this Node is a participant in this Edge
	 * 
	 * @param n Node to check
	 * @return True if its the Node is a participant in this Edge
	 */
	boolean isIncident(Node n);
	
	/**
	 * Return true if the specified edges have the same node set.
	 * For directed edges, it checks to see if the set of sources
	 * and targets are the same.  For undirected edges, it checks
	 * to see if the set of nodes are the same.
	 * 
	 * @param e Edge to compare against
	 * @return True if it has the same nodes.  False otherwise.
	 */
	boolean hasSameNodes(Edge e);
	
	/**
	 * String representation of Edge.  The format output should match
	 * that defined in GraphItemUtils.getEdgeIDString
	 * 
	 * @return String representation
	 */
	String toString();
}
