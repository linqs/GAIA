package linqs.gaia.graph.statistic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.util.MapUtils;

/**
 * Returns the counts of the items within the graph.
 * The counts include the number of nodes, the number of edges,
 * and the number of graph items of each schema (with the exception of the
 * schema for the graph itself).  The values returned are keyed
 * as follows:
 * <UL>
 * <LI>NumberOfNodes
 * <LI>NumberOfNodesSchema:&lt;schemaid&gt;
 * <LI>NumberOfEdges
 * <LI>NumberOfEdgesSchema:&lt;schemaid&gt;
 * </UL>
 * where values with "&lt;schemaid&gt;" are returned for the appropriate
 * schema ids.
 * <p>
 * @author namatag
 *
 */
public class GraphItemCount extends BaseConfigurable implements GraphStatistic {

	public Map<String, Double> getStatisticDoubles(Graph g) {
		LinkedHashMap<String, Double> stats = new LinkedHashMap<String, Double>();
		Iterator<String> sids = g.getAllSchemaIDs();
		List<String> nodesids = new LinkedList<String>();
		List<String> edgesids = new LinkedList<String>();
		
		while(sids.hasNext()) {
			String sid = sids.next();
			Schema schema = g.getSchema(sid);
			if(schema.getType().equals(SchemaType.NODE)) {
				nodesids.add(sid);
			} else if(schema.getType().equals(SchemaType.DIRECTED)
					|| schema.getType().equals(SchemaType.UNDIRECTED)) {
				edgesids.add(sid);
			} else if(schema.getType().equals(SchemaType.GRAPH)){
				continue;
			} else {
				throw new UnsupportedTypeException("Unsupported Schema Type: "+schema.getType());
			}
		}
		
		stats.put("NumberOfNodes", g.numNodes()+0.0);
		for(String sid:nodesids) {
			stats.put("NumberOfNodesSchema:"+sid, g.numGraphItems(sid)+0.0);
		}
		
		stats.put("NumberOfEdges", g.numEdges()+0.0);
		for(String sid:edgesids) {
			stats.put("NumberOfEdgesSchema:"+sid, g.numGraphItems(sid)+0.0);
		}
		
		return stats;
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(this.getStatisticDoubles(g),
				new LinkedHashMap<String,String>());
	}
}
