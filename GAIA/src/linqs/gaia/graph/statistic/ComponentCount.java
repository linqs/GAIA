package linqs.gaia.graph.statistic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.MapUtils;

/**
 * Returns statistics about the number of components
 * in the graph, as well as the number of nodes
 * and edges in each component.  The values defined
 * are keyed as follows:
 * <UL>
 * <LI>NumberOfComponents
 * <LI>NumberInComponent-&lt;id&gt;
 * <LI>NumberNodesInComponent-&lt;id&gt;
 * <LI>NumberEdgesInComponent-&lt;id&gt;
 * </UL>
 * where values with "-&lt;id&gt;" are defined for each component.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>componentfeatureid-Feature ID of numeric feature to store component ID.
 * Set if you wish to keep the component ID feature values.
 * </UL>
 * 
 * @author namatag
 *
 */
public class ComponentCount extends BaseConfigurable implements GraphStatistic {
	public static final String DEFAULT_COMPONENT_FEATUREID = "GAIA-ComponentID";
	
	public Map<String, Double> getStatisticDoubles(Graph g) {
		String fid = ComponentCount.DEFAULT_COMPONENT_FEATUREID;
		if(this.hasParameter("componentfeatureid")) {
			fid = this.getStringParameter("componentfeatureid");
		}
		
		// Store which component a graph item belongs to
		// in a feature
		Iterator<String> sids = g.getAllSchemaIDs();
		while(sids.hasNext()) {
			String sid = sids.next();
			Schema schema = g.getSchema(sid);
			if(schema.getType().equals(SchemaType.NODE)
					|| schema.getType().equals(SchemaType.DIRECTED)
					|| schema.getType().equals(SchemaType.UNDIRECTED)) {
				
				schema.addFeature(fid, new ExplicitNum());
			} else if(schema.getType().equals(SchemaType.GRAPH)){
				continue;
			} else {
				throw new UnsupportedTypeException("Unsupported Schema Type: "+schema.getType());
			}
			
			g.updateSchema(sid, schema);
		}
		
		Iterator<Node> nodes = g.getNodes();
		double componentcount = 0;
		while(nodes.hasNext()) {
			Node n = nodes.next();
			
			if(this.hasFeature(n, fid)) {
				continue;
			}
			
			// Increment counter
			componentcount++;
			
			// Set this component ID to node and all connected to this node
			this.recursivelySetcomponentID(n, fid, componentcount);
		}
		
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
		KeyedCount<String> nodeccount = new KeyedCount<String>();
		KeyedCount<String> edgeccount = new KeyedCount<String>();
		
		// Count nodes in a given component
		nodes = g.getNodes();
		while(nodes.hasNext()) {
			Node n = nodes.next();
			int id = ((NumValue) n.getFeatureValue(fid)).getNumber().intValue();
			nodeccount.increment(id+"");
		}
		
		// Count edges in a given component
		Iterator<Edge> edges = g.getEdges();
		while(edges.hasNext()) {
			Edge e = edges.next();
			int id = ((NumValue) e.getFeatureValue(fid)).getNumber().intValue();
			edgeccount.increment(id+"");
		}
		
		// Fill statistics
		stats.put("NumberOfComponents", componentcount);
		for(int i=1; i<=componentcount; i++) { 
			stats.put("NumberInComponent-"+i, 0.0+nodeccount.getCount(""+i)+edgeccount.getCount(""+i));
			stats.put("NumberNodesInComponent-"+i, 0.0+nodeccount.getCount(""+i));
			stats.put("NumberEdgesInComponent-"+i, 0.0+edgeccount.getCount(""+i));
		}
		
		// Remove feature if not requested
		if(!this.hasParameter("componentfeatureid")) {
			sids = g.getAllSchemaIDs();
			while(sids.hasNext()) {
				String sid = sids.next();
				Schema schema = g.getSchema(sid);
				if(schema.getType().equals(SchemaType.NODE)
						|| schema.getType().equals(SchemaType.DIRECTED)
						|| schema.getType().equals(SchemaType.UNDIRECTED)) {
					
					schema.removeFeature(fid);
				} else if(schema.getType().equals(SchemaType.GRAPH)){
					continue;
				} else {
					throw new UnsupportedTypeException("Unsupported Schema Type: "+schema.getType());
				}
				
				g.updateSchema(sid, schema);
			}
		}
		
		return stats;
	}
	
	/**
	 * Set the graph item with the component ID and
	 * then call this same function over all items connected
	 * to the graph item.
	 * 
	 * @param gi Graph Item
	 * @param fid Component feature id
	 * @param componentid Current component id
	 */
	private void recursivelySetcomponentID(GraphItem gi, String fid, double componentid) {
		if(this.hasFeature(gi, fid)) {
			return;
		}
		
		gi.setFeatureValue(fid, new NumValue(componentid));
		Iterator<GraphItem> itr = gi.getIncidentGraphItems();
		while(itr.hasNext()) {
			GraphItem currgi = itr.next();
			this.recursivelySetcomponentID(currgi, fid, componentid);
		}
	}
	
	/**
	 * Return true if the component id is set, and false otherwise.
	 * 
	 * @param gi Graph Item
	 * @param fid Component feature id
	 * @return True if feature set, false otherwise
	 */
	private boolean hasFeature(GraphItem gi, String fid) {
		return !gi.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE);
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(this.getStatisticDoubles(g),
				new LinkedHashMap<String,String>());
	}
}
