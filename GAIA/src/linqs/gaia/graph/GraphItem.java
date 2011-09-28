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

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.Identifiable;

/**
 * Interface to handle all functions common to all items stored in {@link Graph}.
 * <p>
 * Note: All graph items are {@link Identifiable}.  All GraphItems
 * are unique given a unique {@link GraphItemID} within a {@link Graph}.
 * 
 * @author namatag
 *
 */
public interface GraphItem extends Decorable, Identifiable<GraphItemID> {
	/**
	 * Return true if this Graph Item is incident to the specified graph item
	 * 
	 * @param gi Graph Item
	 * @return True if this item is incident to the specified item
	 */
	boolean isIncident(GraphItem gi);
	
	/**
	 * Get GraphItems incident to this GraphItem
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getIncidentGraphItems();
	
	/**
	 * Get the number ofGraphItems incident to this GraphItem
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @return Number of graph items
	 */
	int numIncidentGraphItems();
	
	/**
	 * Get GraphItems incident to this GraphItem with the specified schema.
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param sid Schema ID of connected items to get
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getIncidentGraphItems(String sid);
	
	/**
	 * Get number of GraphItems incident to this GraphItem with the specified schema.
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param sid Schema ID of connected items to get
	 * @return Number of graph items
	 */
	int numIncidentGraphItems(String sid);
	
	/**
	 * Get GraphItems incident to this GraphItem where the
	 * GraphItem specified is adjacent given any one of the returned incident GraphItems.
	 * 
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param adjacent Graph Item adjacent to this item
	 * 
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getIncidentGraphItems(GraphItem adjacent);
	
	/**
	 * Get GraphItems, with the specified schema ID, incident to this GraphItem where this
	 * GraphItem specified is adjacent given any one of the returned incident GraphItems.
	 * 
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param sid Schema ID of incident GraphItems
	 * @param adjacent Graph Item adjacent to this item
	 * 
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getIncidentGraphItems(String sid, GraphItem adjacent);
	
	/**
	 * Get number of GraphItems incident to this GraphItem where the
	 * GraphItem specified is adjacent given any one of these incident GraphItems.
	 * 
	 * For {@link Node}, this is the set of edges which contain the node.
	 * For {@link Edge}, this is the set of nodes which the edge contains.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once
	 * 
	 * @param adjacent Adjacent graph item
	 * @return Number of incident graph items
	 */
	int numIncidentGraphItems(GraphItem adjacent);
	
	/**
	 * Return true if this Graph Item is adjacent to the specified graph item
	 * 
	 * @param gi Graph Item
	 * @return True if this item is adjacent to the specified item
	 */
	boolean isAdjacent(GraphItem gi);
	
	/**
	 * Get GraphItems adjacent to this GraphItem.
	 * For {@link Node}, this is the set of nodes which share an edge with the given node.
	 * For {@link Edge}, this is the set of edges which share a node with the given edge.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once.
	 * Also, an item is not adjacent to itself.
	 * 
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getAdjacentGraphItems();
	
	/**
	 * Get number of GraphItems adjacent to this GraphItem.
	 * For {@link Node}, this is the set of nodes which share an edge with the given node.
	 * For {@link Edge}, this is the set of edges which share a node with the given edge.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once.
	 * Also, an item is not adjacent to itself.
	 * 
	 * @return Number of graph items
	 */
	int numAdjacentGraphItems();
	
	/**
	 * Return true if this Graph Item is adjacent to the specified graph item
	 * with a connecting Graph Item of the specified schema id.
	 * 
	 * @param gi Graph Item
	 * @param incidentsid Schema ID of connected items to get
	 * @return True if this item is adjacent to the specified item
	 */
	boolean isAdjacent(GraphItem gi, String incidentsid);
	
	/**
	 * Get GraphItems adjacent to this GraphItem
	 * with a connecting Graph Item of the specified schema id.
	 * For {@link Node}, this is the set of nodes which share an edge
	 * (of the given schema) with the given node.
	 * For {@link Edge}, this is the set of edges which share a node
	 * (of the given schema) with the given edge.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once.
	 * Also, an item is not adjacent to itself.
	 * 
	 * @param incidentsid Schema ID of connecting items.
	 * @return Iterator over graph items
	 */
	Iterator<GraphItem> getAdjacentGraphItems(String incidentsid);
	
	/**
	 * Get number of GraphItems adjacent to this GraphItem
	 * with a connecting Graph Item of the specified schema id..
	 * For {@link Node}, this is the set of nodes which share an edge
	 * (of the given schema) with the given node.
	 * For {@link Edge}, this is the set of edges which share a node
	 * (of the given schema) with the given edge.
	 * <p>
	 * Note: The returned items are unique.
	 * i.e., an item connected to by more than one way is only returned once.
	 * Also, an item is not adjacent to itself.
	 * 
	 * @param incidentsid Schema ID of connecting items
	 * @return Number of graph items
	 */
	int numAdjacentGraphItems(String incidentsid);
	
	/**
	 * Get the Graph this graph item is part of
	 * 
	 * @return Graph this item is a part of
	 */
	Graph getGraph();
	
	/**
	 * Return true if the items are equal.
	 * <p>
	 * Items are equal if the have their {@link GraphItemID} is equal.
	 * 
	 * @param other Object to compare
	 * @return True if the object is equal and false otherwise
	 */
	boolean equals(Object other);
	
	/**
	 * Return a hash code for this by object as used by HashMap, etc.
	 * <p>
	 * Return the Hash code defined by ID.
	 * 
	 * @return Hash code for object
	 */
	public int hashCode();
}
