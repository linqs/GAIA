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
