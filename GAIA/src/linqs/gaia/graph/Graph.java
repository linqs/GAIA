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

import linqs.gaia.configurable.Configurable;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.SchemaManager;
import linqs.gaia.graph.event.GraphEvent;
import linqs.gaia.graph.event.GraphEventListener;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.Identifiable;

/**
 * Interface class for the underlying data representation.
 * There are two main items: Node and Edge.
 * A Node is a data object (i.e. person, event) and a Edge
 * is a connection between Node objects (i.e. friend, participatedIn).
 * <p>
 * There is also support for GraphEventListeners to execute some code
 * when some action is performed.
 * <p>
 * Notes:
 * <UL>
 * <LI> The graph constructor must be of the form Graph(GraphID id)
 * (i.e., Take as argument a GraphID representing the id of the graph).
 * <LI> The graph constructor must automatically create a graph schema
 * for the graph given the schema ID.
 * </UL>
 * 
 * @see GraphItem
 * @see Node
 * @see Edge
 * @see GraphEventListener
 * 
 * @author namatag
 *
 */
public interface Graph extends Decorable, Identifiable<GraphID>,
	SchemaManager, Configurable, SystemDataManager {
	
	/**
	 * Adds a Node to this data graph with this ID.
	 * <p>
	 * An NodeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID of node to add
	 * @return Added node
	 */
	Node addNode(GraphItemID id);

	/**
	 * Returns <code>true</code> if this graph has a Node with the
	 * specified ID.
	 *
	 * @param id The ID for the Node to check.
	 * @return <code>true</code> if some Node in the graph has the
	 * specified ID, and <code>false</code> otherwise.
	 */
	boolean hasNode(GraphItemID id);

	/**
	 * Returns the Node in the graph with the specified ID, or
	 * null if no such Node exists.
	 *
	 * @param id The ID for the Node to return.
	 * @return The Node with the matching {@link GraphItemID}, or
	 * <code>null</code> if no such Node exists.
	 */
	Node getNode(GraphItemID id);

	/**
	 * Returns an iterator over all nodes contained
	 * in the graph
	 * 
	 * @return An unmodifiable collected of Nodes
	 */
	Iterator<Node> getNodes();
	
	/**
	 * Returns an iterator over all nodes with the given
	 * schema id contained in the graph
	 * 
	 * @param schemaID ID of interest
	 * @return An unmodifiable collected of Nodes
	 */
	Iterator<Node> getNodes(String schemaID);
	
	/**
	 * Returns an iterable object over all nodes contained
	 * in the graph
	 * 
	 * @return An unmodifiable collected of Nodes
	 */
	Iterable<Node> getIterableNodes();
	
	/**
	 * Returns an iterable object over all nodes with the given
	 * schema id contained in the graph
	 * 
	 * @param schemaID ID of interest
	 * @return An unmodifiable collected of Nodes
	 */
	Iterable<Node> getIterableNodes(String schemaID);
	
	/**
	 * Removes a Node and all features for that node from the graph.
	 * <p>
	 * Note: If the removal of the node makes an edge invalid,
	 * the affected edge is removed automatically.
	 * <p>
	 * An NodeRemovedEvent is called at the end of this function.
	 * 
	 * @param n The Node to remove.
	 */
	void removeNode(Node n);

	/**
	 * Removes a Node and all features for that node from the graph.
	 * <p>
	 * Note: If the removal of the node makes an edge invalid,
	 * (i.e., Directed Edges must have one source and one target and
	 * Undirected Edges must have at least one node)
	 * the affected edge is removed automatically.
	 * <p>
	 * An NodeRemovedEvent is called at the end of this function
	 * 
	 * @param id ID of node to remove
	 */
	void removeNode(GraphItemID id);
	
	/**
	 * Removes a Node and all features for that node from the graph,
	 * as well as all edges the node is incident to
	 * <p>
	 * An NodeRemovedEvent is called at the end of this function
	 * and an EdgeRemovedEvent is called for all incident edges
	 * 
	 * @param n The Node to remove.
	 */
	void removeNodeWithEdges(Node n);

	/**
	 * Removes a Node and all features for that node from the graph,
	 * as well as all edges the node is incident to.
	 * <p>
	 * An NodeRemovedEvent is called at the end of this function
	 * and an EdgeRemovedEvent is called for all incident edges
	 * 
	 * @param id ID of node to remove
	 */
	void removeNodeWithEdges(GraphItemID id);

	/**
	 * Removes all Nodes from the graph.
	 * <p>
	 * An NodeRemovedEvent is called when each node is removed.
	 * 
	 */
	void removeAllNodes();
	
	/**
	 * Removes all Nodes, with the given schema, from the graph.
	 * All edges incident one of these nodes are also removed.
	 * Note:  This is equivalent to calling {@link #removeNodeWithEdges(GraphItemID)}
	 * on all edges with the specified schemaID.
	 * <p>
	 * An NodeRemovedEvent is called when each node is removed.
	 * 
	 */
	void removeNodesWithEdges(String schemaID);

	/**
	 * Returns the number of Nodes in the graph.
	 *
	 * @return The number of Nodes contained in the graph.
	 */
	int numNodes();

	/**
	 * Adds a Directed Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param sources Nodes to include as sources
	 * @param targets Nodes to include as targets
	 * @return DirectedEdge
	 */
	DirectedEdge addDirectedEdge(GraphItemID id, Iterator<Node> sources, Iterator<Node> targets);
	
	/**
	 * Adds a Directed Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param sources Node to include as sources
	 * @param targets Node to include as targets
	 * @return DirectedEdge
	 */
	DirectedEdge addDirectedEdge(GraphItemID id, Iterable<Node> sources, Iterable<Node> targets);
	
	/**
	 * Adds a Directed Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param source Node to include as source
	 * @param target Node to include as target
	 * @return DirectedEdge
	 */
	DirectedEdge addDirectedEdge(GraphItemID id, Node source, Node target);

	/**
	 * Adds an Undirected Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param nodes Nodes the edge is incident to
	 * @return UndirectedEdge
	 */
	UndirectedEdge addUndirectedEdge(GraphItemID id, Iterator<Node> nodes);
	
	/**
	 * Adds an Undirected Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param nodes Nodes the edge is incident to
	 * @return UndirectedEdge
	 */
	UndirectedEdge addUndirectedEdge(GraphItemID id, Iterable<Node> nodes);
	
	/**
	 * Adds an Undirected Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param node1 First node in undirected edge
	 * @param node2 First second node in undirected edge
	 * @return UndirectedEdge
	 */
	UndirectedEdge addUndirectedEdge(GraphItemID id, Node node1, Node node2);
	
	/**
	 * Adds an Undirected Edge to the graph.
	 * <p>
	 * An EdgeAddedEvent is called at the end of this function.
	 * 
	 * @param id ID for Edge to be added
	 * @param node First node in undirected edge
	 * @return UndirectedEdge
	 */
	UndirectedEdge addUndirectedEdge(GraphItemID id, Node node);

	/**
	 * Returns <code>true</code> if this graph has an Edge with the
	 * specified ID.
	 *
	 * @param id ID for the Edge to check.
	 * @return <code>true</code> if some Edge in the graph has the
	 * specified ID, and <code>false</code> otherwise.
	 */
	boolean hasEdge(GraphItemID id);

	/**
	 * Returns the Edge in the data model with the specified ID, or
	 * null if no such Edge exists.
	 *
	 * @param id The ID for the Edge to return.
	 * @return The Edge with the matching {@link GraphItemID}, or
	 * <code>null</code> if no such Edge exists.
	 */
	Edge getEdge(GraphItemID id);

	/**
	 * Returns an iterator over all Edges
	 * contained in the graph.
	 *
	 * @return Iterator over Nodes
	 */
	Iterator<Edge> getEdges();
	
	/**
	 * Returns an iterator over all Edges with the given schema
	 * contained in the graph.
	 * 
	 * @param schemaID ID of interest
	 * @return Iterator over Nodes
	 */
	Iterator<Edge> getEdges(String schemaID);
	
	/**
	 * Returns an iterable object over all Edges
	 * contained in the graph.
	 *
	 * @return Iterator over Nodes
	 */
	Iterable<Edge> getIterableEdges();
	
	/**
	 * Returns an iterable object over all Edges with the given schema
	 * contained in the graph.
	 * 
	 * @param schemaID ID of interest
	 * @return Iterator over Nodes
	 */
	Iterable<Edge> getIterableEdges(String schemaID);

	/**
	 * Removes a Edge and all features for that Edge from the graph
	 * <p>
	 * An EdgeRemovedEvent is called at the end of this function.
	 * 
	 * @param e The Edge to be removed.
	 */
	void removeEdge(Edge e);

	/**
	 * Removes a Edge and all features for that Edge from the graph
	 * <p>
	 * An EdgeRemovedEvent is called at the end of this function.
	 * 
	 * @param id ID of edge to remove
	 */
	void removeEdge(GraphItemID id);

	/**
	 * Removes all Edges from the graph
	 * <p>
	 * An EdgeRemovedEvent is called for each removed edge.
	 */
	void removeAllEdges();

	/**
	 * Returns the number of Edges in the graph
	 *
	 * @return The number of Edges contained in the graph
	 */
	int numEdges();
	
	/**
	 * Returns <code>true</code> if this graph has a GraphItem with the
	 * specified ID.
	 *
	 * @param id ID for the GraphItem to check.
	 * @return <code>true</code> if some GraphItem in the graph has the
	 * specified ID, and <code>false</code> otherwise.
	 */
	boolean hasGraphItem(GraphItemID id);
	
	/**
	 * Returns <code>true</code> if this graph has a GraphItem with the
	 * same schema and object id as that in the specifid id.
	 *
	 * @param id ID for the GraphItem to check.
	 * @return <code>true</code> if some GraphItem in the graph has the
	 * specified ID, and <code>false</code> otherwise.
	 */
	boolean hasEquivalentGraphItem(GraphItemID id);
	
	/**
	 * Returns <code>true</code> if this graph has a GraphItem with the
	 * with the same schema and object id.
	 *
	 * @param gi Graph Item to check the existence of an equivalent graph item for
	 * @return <code>true</code> if some GraphItem in the graph has the
	 * specified ID, and <code>false</code> otherwise.
	 */
	boolean hasEquivalentGraphItem(GraphItem id);

	/**
	 * Returns the GraphItem in the data model with the specified ID, or
	 * null if no such GraphItem exists.
	 *
	 * @param id The ID for the GraphItem to return.
	 * @return The GraphItem with the matching {@link GraphItemID}, or
	 * <code>null</code> if no such graph item exists.
	 */
	GraphItem getGraphItem(GraphItemID id);
	
	/**
	 * Returns the GraphItem in the graph with the specified ID, or
	 * null if no such GraphItem exists.  Unlike getGraphItem,
	 * if a direct match cannot be found, a partial match maybe
	 * returned where the id of the graph item
	 * is the same except for the graph id (i.e., the schema id and
	 * object id match).
	 *
	 * @param id The ID for the GraphItem to return.
	 * @return The GraphItem with the matching {@link GraphItemID}, or
	 * <code>null</code> if no such graph item exists.
	 */
	GraphItem getEquivalentGraphItem(GraphItemID id);
	
	/**
	 * Returns the GraphItem in the graph which has the same ID, or
	 * null if no such GraphItem exists.  Unlike getGraphItem,
	 * if a direct match cannot be found, a partial match maybe
	 * returned where the id of the graph item
	 * is the same except for the graph id (i.e., the schema id and
	 * object id match).
	 *
	 * @param  gi Graph Item to get an equivalent graph item for
	 * @return The GraphItem with the matching {@link GraphItemID}, or
	 * <code>null</code> if no such graph item exists.
	 */
	GraphItem getEquivalentGraphItem(GraphItem gi);

	/**
	 * Get all GraphItems of the specified schema ID.
	 * 
	 * @param schemaID ID of interest
	 * @return List of Graph Items
	 */
	Iterator<GraphItem> getGraphItems(String schemaID);

	/**
	 * Get all GraphItems
	 * 
	 * @return List of Graph Items
	 */
	Iterator<GraphItem> getGraphItems();
	
	/**
	 * Get an iterable object over all GraphItems of the specified schema ID.
	 * 
	 * @param schemaID ID of interest
	 * @return List of Graph Items
	 */
	Iterable<GraphItem> getIterableGraphItems(String schemaID);

	/**
	 * Get an iterable object over all GraphItems
	 * 
	 * @return List of Graph Items
	 */
	Iterable<GraphItem> getIterableGraphItems();
	
	/**
	 * Remove the specified Graph Item from the graph
	 * 
	 * @param gi Graph Item to remove
	 */
	void removeGraphItem(GraphItem gi);
	
	/**
	 * Remove the specified Graph Item from the graph
	 * 
	 * @param id ID of GraphItem to remove
	 */
	void removeGraphItem(GraphItemID id);
	
	/**
	 * Removes all Graph Items of the given schema id from the graph.
	 * <p>
	 * An NodeRemovedEvent or EdgeRemovedEvent is called
	 * when each node or edge is removed.
	 * 
	 * @param schemaID ID of interest
	 * 
	 */
	void removeAllGraphItems(String schemaID);
	
	/**
	 * Return the number of graph items with the given schema ID.
	 * 
	 * @param schemaID ID of interest
	 * @return Number of graph items with schema
	 */
	int numGraphItems(String schemaID);

	/**
	 * Add listener
	 * 
	 * @param gel Graph event listener
	 */
	void addListener(GraphEventListener gel);

	/**
	 * Remove listener
	 * 
	 * @param gel Graph event listener
	 */
	void removeListener(GraphEventListener gel);

	/**
	 * Remove all listeners
	 */
	void removeAllListeners();

	/**
	 * Process {@link GraphEventListener}
	 * 
	 * @param event Event
	 */
	void processListeners(GraphEvent event);

	/**
	 * Clear all the items, schemas and features from the graph.
	 * After calling this method, the graph can no longer
	 * be used.
	 */
	void destroy();
	
	/**
	 * Return a copy of this graph
	 * 
	 * @param objid Object ID to use in the graph copy
	 * @return Copy of graph
	 */
	Graph copy(String objid);
	
	/**
	 * Copy the graph into the given graph
	 * 
	 * @param g Graph to copy this graph to
	 */
	void copy(Graph g);
	
	/**
	 * Return an empty graph with the same schema as this graph
	 * 
	 * @param objid Object ID to use in the graph copy
	 * @return Copy of graph
	 */
	Graph copySchema(String objid);

	/**
	 * String representation of Graph.  The format output should match
	 * that defined in GraphItemUtils.getGraphIDString
	 * 
	 * @return String representation
	 */
	String toString();
}
