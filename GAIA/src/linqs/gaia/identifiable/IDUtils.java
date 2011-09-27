package linqs.gaia.identifiable;

import linqs.gaia.global.Global;
import linqs.gaia.graph.Graph;

/**
 * General utilities for working with IDs
 * 
 * @author namatag
 *
 */
public class IDUtils {
	
	/**
	 * Generate a graph item id given the graph and schema id.
	 * The object id for the graph item is acquired from 
	 * Global.requestAnonymousID and is chosen such that
	 * it is guaranteed no other graph item already has the id.
	 * 
	 * @param g Graph to add the graph item to
	 * @param schemaid Schema ID
	 * @return Graph Item ID
	 * @deprecated Replaced with {@link GraphItemID#generateGraphItemID}
	 */
	public static GraphItemID createAnonymousGIID(Graph g, String schemaid) {
		return createAnonymousGIID(g, schemaid, "");
	}
			
	/**
	 * Generate a graph item id given the graph and schema id.
	 * The object id for the graph item is acquired from 
	 * Global.requestAnonymousID and is chosen such that
	 * it is guaranteed no other graph item already has the id.
	 * 
	 * @param g Graph to add the graph item to
	 * @param schemaid Schema ID
	 * @param objidprefix Prefix to use before the object id
	 * @return Graph Item ID
	 * @deprecated Replaced with {@link GraphItemID#generateGraphItemID}
	 */
	public static GraphItemID createAnonymousGIID(Graph g, String schemaid, String objidprefix) { 
		GraphItemID newid = null;
		do {
			newid = new GraphItemID(g.getID(), schemaid, objidprefix+Global.requestGlobalCounterValue()+"");
		} while(g.hasGraphItem(newid));
		
		return newid;
	}
	
	/**
	 * Return an ID with the Graph ID declaration not included.
	 * 
	 * @param id GraphItemID to remove the declaration from
	 * @return GraphItemID with null for the GraphID
	 * @deprecated Replaced with {@link GraphItemID#copyWithoutGraphID()}
	 */
	public static GraphItemID removeGraphID(GraphItemID id) {
		return new GraphItemID(id.getSchemaID(), id.getObjID());
	}
}
