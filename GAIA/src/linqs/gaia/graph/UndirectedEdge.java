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
 * Undirected hyperedge interface.  Directed edges have to sets of nodes:
 * the source nodes and target nodes.
 * <p>
 * A valid undirected edge must have at least one node.  An exception
 * is thrown when an edge is modified or added which does not match
 * this criterion.
 * 
 * @see Edge
 * @see DirectedEdge
 * 
 * @author namatag
 *
 */
public interface UndirectedEdge extends Edge {
	/**
	 * Add Node to Edge. An exception is thrown if a
	 * node is already in this edge.
	 * 
	 * @param n Node to add
	 */
	void addNode(Node n);
}
