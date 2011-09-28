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
 * Directed hyperedge interface.  Directed edges have to sets of nodes:
 * the source nodes and target nodes.
 * <p>
 * A valid directed edge must have at least one source node and one target node.
 * An exception is thrown when an edge is modified or added which does not match
 * this criterion.
 * 
 * @see Edge
 * @see UndirectedEdge
 * 
 * @author namatag
 *
 */
public interface DirectedEdge extends Edge {
	/**
	 * Add Node to source. An exception is thrown if a
	 * node is already a source in this edge.
	 * 
	 * @param n Node to add
	 */
	void addSourceNode(Node n);
	
	/**
	 * Add Node to target. An exception is thrown if a
	 * node is already a target in this edge.
	 * 
	 * @param n Node to add
	 */
	void addTargetNode(Node n);
	
	/**
	 * Return source Nodes for a directed Edge
	 * 
	 * @return Iterator over all source nodes
	 */
	Iterator<Node> getSourceNodes();
	
	/**
	 * Return target Nodes for a directed Edge
	 * 
	 * @return Iterator over all target nodes
	 */
	Iterator<Node> getTargetNodes();
	
	/**
	 * Return number of source nodes
	 * 
	 * @return Number of source nodes
	 */
	int numSourceNodes();
	
	/**
	 * Return number of target nodes
	 * 
	 * @return Number of target nodes
	 */
	int numTargetNodes();
	
	/**
	 * Remove Node from the sources of this Edge
	 * 
	 * @param n Node to remove
	 */
	void removeSourceNode(Node n);
	
	/**
	 * Remove Node from the targets of this Edge
	 * 
	 * @param n Node to remove
	 */
	void removeTargetNode(Node n);
	
	/**
	 * Return true if this Node is a source participant in this Edge
	 * 
	 * @param n Node to check
	 * @return True if its the Node is a source participant in this Edge
	 */
	boolean isSource(Node n);
	
	/**
	 * Return true if this Node is a target participant in this Edge
	 * 
	 * @param n Node to check
	 * @return True if its the Node is a target participant in this Edge
	 */
	boolean isTarget(Node n);
	
	/**
	 * Return true if the specified edge has the same source nodes
	 * 
	 * @param e Edge to compare against
	 * @return True if it has the same nodes.  False otherwise.
	 */
	boolean hasSameSources(DirectedEdge e);
	
	/**
	 * Return true if the specified edge has the same target nodes
	 * 
	 * @param e Edge to compare against
	 * @return True if it has the same nodes.  False otherwise.
	 */
	boolean hasSameTargets(DirectedEdge e);
}
