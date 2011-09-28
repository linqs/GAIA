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
package linqs.gaia.model.gd;

import linqs.gaia.graph.Graph;
import linqs.gaia.model.Model;

/**
 * Interface for all implementation of group detection models.
 * 
 * @author namatag
 *
 */
public interface GroupDetection extends Model {
	/**
	 * Learn the group detection model in a graph where we have
	 * the nodes to cluster, group nodes which represent the groups
	 * they belong to, and an edge from a node to the groups it belongs to.
	 * 
	 * @param graph Graph to apply group detection over
	 * @param groupschemaid Schema ID of group nodes
	 * @param nodeschemaid Schema ID of node we are clustering
	 * @param memberofschemaid Schema ID of directed "member-of" edges from
	 * node node to the group it belongs to
	 */
	void learn(Graph graph, String nodeschemaid, String groupschemaid, String memberofschemaid);
	
	/**
	 * Learn the group detection model in a graph where we have
	 * the nodes to cluster and hyperedges which represent groups.
	 * Nodes incident to the same hyperedge belong to that group.
	 * 
	 * @param graph Graph to apply group detection over
	 * @param groupschemaid Schema ID of group hyperedges
	 * @param nodeschemaid Schema ID of node we are clustering
	 * node node to the group it belongs to
	 */
	void learn(Graph graph, String nodeschemaid, String groupschemaid);
	
	/**
	 * Predict the groups in a graph where we have
	 * the nodes to cluster, group nodes which represent the groups
	 * they belong to, and an edge from a node to the groups it belongs to.
	 * 
	 * @param graph Graph to apply group detection over
	 * @param groupschemaid Schema ID of group nodes
	 * @param memberofschemaid Schema ID of directed "member-of" edges from
	 * node node to the group it belongs to
	 */
	void predictAsNode(Graph graph, String groupschemaid, String memberofschemaid);
	
	/**
	 * Predict the groups in a graph where we have
	 * the nodes to cluster and hyperedges which represent groups.
	 * Nodes incident to the same hyperedge belong to that group.
	 * 
	 * @param graph Graph to apply group detection over
	 * @param groupschemaid Schema ID of group nodes
	 * node node to the group it belongs to
	 */
	void predictAsEdge(Graph graph, String groupschemaid);
}
