package linqs.gaia.model.er.merger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.er.merger.feature.FeatureMerger;
import linqs.gaia.model.er.merger.node.IncidentEdgeMerger;
import linqs.gaia.util.UnmodifiableSet;

public class MergeUtils {
	/**
	 * Merge the nodes in the graph using ER Links.
	 * The ER links and the schema for them will be removed.
	 * 
	 * @param graph Graph to merged nodes in
	 * @param edgeschemaid Schema ID of the ER links
	 * @param reffeatureid Feature which will hold the ids of the merged items
	 * @param fmerger Feature Merger
	 * @param iemerger Edge Merger
	 */
	public static void mergeUsingERLinks(Graph graph, String refschemaid, String edgeschemaid, String reffeatureid,
			FeatureMerger fmerger, IncidentEdgeMerger iemerger) {
		mergeUsingERLinks(graph, edgeschemaid, refschemaid, reffeatureid, fmerger, iemerger, true);
	}
	
	/**
	 * Merge the nodes in the graph using ER Links.
	 * The ER links will be removed.
	 * 
	 * @param graph Graph to merged nodes in
	 * @param refschemaid Schema ID of reference nodes
	 * @param edgeschemaid Schema ID of the ER links
	 * @param reffeatureid Feature which will hold the ids of the merged items
	 * @param fmerger Feature Merger
	 * @param iemerger Edge Merger
	 * @param removeschema If true, remove the schema of the erlinks.
	 * Otherwise, remove the edges but do not remove the schema
	 */
	public static void mergeUsingERLinks(Graph graph, String refschemaid, String edgeschemaid,
			String reffeatureid,
			FeatureMerger fmerger, IncidentEdgeMerger iemerger,
			boolean removeschema) {
		// Assume transitivity and merged appropriate nodes
		// Get sets of entities
		List<Set<Node>> entitysets = ERUtils.getTransitiveEntity(graph, refschemaid, edgeschemaid);
		
		// Remove the ER edges
		graph.removeAllGraphItems(edgeschemaid);
		
		if(removeschema) {
			graph.removeSchema(edgeschemaid);
		}
		
		for(Set<Node> eset:entitysets) {
			// Get the IDs for the references making sure to handle the case
			// where nodes might have been the result of previous of merges
			Set<ID> ids = null;
			if(reffeatureid != null) {
				ids = new HashSet<ID>();
				for(Node n:eset) {
					FeatureValue value = n.getFeatureValue(reffeatureid);
					
					// Handle case where the node to merged was the result
					// of a previous merge
					if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
						ids.add(n.getID());
					} else {
						MultiIDValue refvalue = (MultiIDValue) value;
						ids.addAll(refvalue.getIDs().copyAsSet());
					}
				}
			}
			
			// Merge nodes in the entity set
			Node mergedn = Merger.mergeNodes(graph, eset, fmerger, iemerger);
			
			if(ids != null) {
				// Add feature if not already defined
				Schema schema = mergedn.getSchema();
				if(!schema.hasFeature(reffeatureid)) {
					schema.addFeature(reffeatureid, new ExplicitMultiID());
					graph.updateSchema(mergedn.getSchemaID(), schema);
				}
				
				// Set reference feature value based on saved items
				MultiIDValue value = new MultiIDValue(ids);
				mergedn.setFeatureValue(reffeatureid, value);
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph after merging: "+GraphUtils.getSimpleGraphOverview(graph));
		}
	}
	
	/**
	 * Get the ids of the nodes merged to form this node.
	 * If the node is not the result of a merge,
	 * the id of the node is return.
	 * 
	 * @param mergedn Merged node id
	 * @param reffid Feature id of feature which contains a MultiIDFeature.
	 * @return Set of GraphItemIDs
	 */
	public static Set<GraphItemID> getMergeIDs(Node mergedn, String reffid) {
		Set<GraphItemID> refids = new HashSet<GraphItemID>();
		
		// If the nodes we predicted over are a result of an
		// entity resolution merging, get the merged input graph nodes
		FeatureValue value = mergedn.getFeatureValue(reffid);
		if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
			// Handle case if the nodes predicted over can be the result
			// of merging but, in this case, is not
			refids.add(mergedn.getID());
		} else {
			// Handle case if the nodes predicted over are the result of merging
			UnmodifiableSet<ID> currrefids = ((MultiIDValue) value).getIDs();
			for(ID id:currrefids) {
				refids.add((GraphItemID) id);
			}
		}
		
		return refids;
	}
}
