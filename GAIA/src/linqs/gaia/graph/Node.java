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
 * A Node interface.  Nodes are normally data objects such as a person or event.
 * 
 * @see Edge
 * @see Graph
 * 
 * @author namatag
 *
 */
public interface Node extends GraphItem {
	/**
	 * Return all incident Edges
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<Edge> getAllEdges();

	/**
	 * Return all incident Edges with the given schemaID
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param schemaID Node Schema ID
	 * @return Iterator over edges
	 */
	Iterator<Edge> getAllEdges(String schemaID);

	/**
	 * Return incident Edges where this node is a source
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<DirectedEdge> getEdgesWhereSource();
	
	/**
	 * Return incident Edges where this node is a source
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param schemaID Schema ID of the edges
	 * 
	 * @return Iterator over edges
	 */
	Iterator<DirectedEdge> getEdgesWhereSource(String schemaID);
	
	/**
	 * Return the number of incident Edges where this node is a source
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @return Number of edges
	 */
	int numEdgesWhereSource();
	
	/**
	 * Return the number of incident Edges where this node is a source
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @param schemaID Schema ID of the edges
	 * 
	 * @return Number of edges
	 */
	int numEdgesWhereSource(String schemaID);

	/**
	 * Return adjacent Edges where this node is a target
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<DirectedEdge> getEdgesWhereTarget();
	
	/**
	 * Return adjacent Edges where this node is a target
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param schemaID Schema ID of the edges
	 * 
	 * @return Iterator over edges
	 */
	Iterator<DirectedEdge> getEdgesWhereTarget(String schemaID);
	
	/**
	 * Return the number of adjacent Edges where this node is a target
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @return Number of edges
	 */
	int numEdgesWhereTarget();
	
	/**
	 * Return the number of adjacent Edges where this node is a target
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @param schemaID Schema ID of the edges
	 * 
	 * @return Number of edges
	 */
	int numEdgesWhereTarget(String schemaID);

	/**
	 * Return adjacent undirected Edges
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<UndirectedEdge> getUndirEdges();
	
	/**
	 * Return adjacent directed Edges
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over edges
	 */
	Iterator<DirectedEdge> getDirEdges();
	
	/**
	 * Return the number of adjacent undirected Edges
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @return Number of edges
	 */
	int numUndirEdges();
	
	/**
	 * Return the number of adjacent directed Edges
	 * <p>
	 * Note: The counted items are unique.
	 * i.e., an item connected to by more than one way is only counted once
	 * 
	 * @return Number of edges
	 */
	int numDirEdges();

	/**
	 * Get the number of unique Edges incident to node
	 * 
	 * @return Number of Edges
	 */
	int numEdges();

	/**
	 * Return if the specified Edge is incident to the Node
	 * 
	 * @param e Edge to check
	 * @return True if the Edge is adjacent, False otherwise
	 */
	boolean isIncident(Edge e);
	
	/**
	 * Return true if this Node has an Directed Edge with a Node n
	 * where this Node is a source and n is a target
	 * 
	 * @param n Node to check a Edge with
	 * @return True if its the Node has a Edge with a Node n
	 */
	boolean isAdjacentTarget(Node n);
	
	/**
	 * Return true if this Node has a Directed Edge of the specified schema
	 * with a Node n where this Node is a source and n is a target
	 * 
	 * @param n Node to check a Edge with
	 * @param edgeschemaID schema ID of the edge
	 * @return True if its the Node has a Edge with a Node n
	 */
	boolean isAdjacentTarget(Node n, String edgeschemaID);
	
	/**
	 * Return true if this Node has a Directed Edge with a Node n
	 * where this Node is a target and n is a source
	 * 
	 * @param n Node to check a Edge with
	 * @return True if its the Node has a Edge with a Node n
	 */
	boolean isAdjacentSource(Node n);
	
	/**
	 * Return true if this Node has a Directed Edge of the specified schema
	 * with a Node n where this Node is a target and n is a source
	 * 
	 * @param n Node to check a Edge with
	 * @param edgeschemaID schema ID of the edge
	 * @return True if its the Node has a Edge with a Node n
	 */
	boolean isAdjacentSource(Node n, String edgeschemaID);
	
	/**
	 * Return an iterator of the unique set nodes which are a target of a directed
	 * edge where this node is a source of that edge.
	 * 
	 * @return Iterator over target nodes
	 */
	Iterator<Node> getAdjacentTargets();
	
	/**
	 * Return an iterator of the unique set nodes which are a target of a directed
	 * edge of the specified schema where this node is a source of that edge.
	 * 
	 * @param edgeschemaID schema ID of the node
	 * @return Iterator over target nodes
	 */
	Iterator<Node> getAdjacentTargets(String edgeschemaID);
	
	/**
	 * Return the size of the unique set nodes which are a target of a directed
	 * edge where this node is a source of that edge.
	 * 
	 * @return Size of target set
	 */
	int numAdjacentTargets();
	
	/**
	 * Return the size of the unique set nodes which are a target of a directed
	 * edge of the specified schema where this node is a source of that edge.
	 * 
	 * @param edgeschemaID schema ID of the edge
	 * @return Size of target set
	 */
	int numAdjacentTargets(String edgeschemaID);
	
	/**
	 * Return an iterator of the unique set nodes which are a source of a directed
	 * edge where this node is a target of that edge.
	 * 
	 * @return Iterator over source nodes
	 */
	Iterator<Node> getAdjacentSources();
	
	/**
	 * Return an iterator of the unique set nodes which are a source of a directed
	 * edge of the specified schema where this node is a target of that edge.
	 * 
	 * @param edgeschemaID schema ID of the edge
	 * @return Iterator over source nodes
	 */
	Iterator<Node> getAdjacentSources(String edgeschemaID);
	
	/**
	 * Return the size of the unique set nodes which are a source of a directed
	 * edge with this node where this node is a target of that edge.
	 * 
	 * @return Size of source set
	 */
	int numAdjacentSources();
	
	/**
	 * Return the size of the unique set nodes which are a source of a directed
	 * edge of the specified schema where this node is a target of that edge.
	 * 
	 * @param schemaID schema ID of the edge
	 * @return Size of source set
	 */
	int numAdjacentSources(String edgeschemaID);
	
	/**
	 * Return an iterator of the unique set nodes which are adjacent to this node.
	 * 
	 * @return Iterator over adjacent nodes
	 */
	Iterator<Node> getAdjacentNodes();
	
	/**
	 * Return an iterator of the unique set of nodes which are
	 * adjacent to this node given an edge of the specified schema.
	 * 
	 * @param edgeschemaID Schema ID of the edge
	 * @return Iterator over adjacent nodes
	 */
	Iterator<Node> getAdjacentNodes(String edgeschemaID);
	
	/**
	 * Return the size of the unique set of nodes which are adjacent
	 * 
	 * @return Size of source set
	 */
	int numAdjacentNodes();
	
	/**
	 * Return the size of the unique set of nodes which are adjacent
	 * given an edge with the specified schema ID
	 * 
	 * @param edgeschemaID Schema ID of the edge
	 * @return Size of source set
	 */
	int numAdjacentNodes(String edgeschemaID);
	
	/**
	 * Remove all edges incident to this node
	 */
	void removeIncidentEdges();
	
	/**
	 * Remove all edges incident to this node with the given schema id
	 */
	void removeIncidentEdges(String edgeschemaid);
	
	/**
	 * String representation of Node.  The format output should match
	 * that defined in GraphItemUtils.getNodeIDString
	 * 
	 * @return String representation
	 */
	String toString();
}
