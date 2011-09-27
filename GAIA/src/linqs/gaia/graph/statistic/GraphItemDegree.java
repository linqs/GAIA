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
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.util.MapUtils;

/**
 * Returns the min, max, average, standard deviation,
 * and number with degree 0 of the degrees for the nodes in the specified graph
 * where degree is defined as the number of unique edges the node
 * belongs to, or for edges, the number of nodes in an edge.
 * The values are presented for all the nodes and edges as a whole
 * and by schema ID.  The values returned are keyed as follows:
 * 
 * <UL>
 * <LI> MinDegreeOfNodes
 * <LI> MaxDegreeOfNodes
 * <LI> AvgDegreeOfNodes
 * <LI> StdDevDegreeOfNodes
 * <LI> MinUndirectedDegreeOfNodes
 * <LI> MaxUndirectedDegreeOfNodes
 * <LI> AvgUndirectedDegreeOfNodes
 * <LI> StdDevUndirectedDegreeOfNodes
 * <LI> MinDirectedDegreeOfNodes
 * <LI> MaxDirectedDegreeOfNodes
 * <LI> AvgDirectedDegreeOfNodes
 * <LI> StdDevDirectedDegreeOfNodes
 * <LI> MinInDegreeOfNodes
 * <LI> MaxInDegreeOfNodes
 * <LI> AvgInDegreeOfNodes
 * <LI> StdDevInDegreeOfNodes
 * <LI> MinOutDegreeOfNodes
 * <LI> MaxOutDegreeOfNodes
 * <LI> AvgOutDegreeOfNodes
 * <LI> StdDevOutDegreeOfNodes
 * <LI> MinDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MaxDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> AvgDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MinUndirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MaxUndirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> AvgUndirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevUndirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MinDirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MaxDirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> AvgDirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevDirectedDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MinInDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MaxInDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> AvgInDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevInDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MinOutDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MaxOutDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> AvgOutDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> StdDevOutDegreeOfNodesSchema:&lt;schemaid&gt;
 * <LI> MinDegreeOfEdges
 * <LI> MaxDegreeOfEdges
 * <LI> AvgDegreeOfEdges
 * <LI> StdDevDegreeOfEdges
 * <LI> MinDegreeOfEdgesSchema:&lt;schemaid&gt;
 * <LI> MaxDegreeOfEdgesSchema:&lt;schemaid&gt;
 * <LI> AvgDegreeOfEdgesSchema:&lt;schemaid&gt;
 * <LI> StdDevDegreeOfEdgesSchema:&lt;schemaid&gt;
 * </UL>
 * 
 * where values with "&lt;schemaid&gt;" are returned for the appropriate
 * schema ids.
 * <p>
 * @author namatag
 *
 */
public class GraphItemDegree extends BaseConfigurable implements GraphStatistic {
	public Map<String, Double> getStatisticDoubles(Graph g) {
		
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
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
		
		double[] dstats = GraphStatisticUtils.degreeStats(g.getIterableNodes());
		stats.put("MinDegreeOfNodes", dstats[0]);
		stats.put("MaxDegreeOfNodes", dstats[1]);
		stats.put("AvgDegreeOfNodes", dstats[2]);
		stats.put("StdDevDegreeOfNodes", dstats[3]);
		stats.put("NumberWith0DegreeOfNodes", dstats[4]);
		
		dstats = GraphStatisticUtils.degreeStats(g.getIterableNodes(), null, "undirected");
		stats.put("MinUndirectedDegreeOfNodes", dstats[0]);
		stats.put("MaxUndirectedDegreeOfNodes", dstats[1]);
		stats.put("AvgUndirectedDegreeOfNodes", dstats[2]);
		stats.put("StdDevUndirectedDegreeOfNodes", dstats[3]);
		stats.put("NumberWith0UndirectedDegreeOfNodes", dstats[4]);
		
		dstats = GraphStatisticUtils.degreeStats(g.getIterableNodes(), null, "inandout");
		stats.put("MinDirectedDegreeOfNodes", dstats[0]);
		stats.put("MaxDirectedDegreeOfNodes", dstats[1]);
		stats.put("AvgDirectedDegreeOfNodes", dstats[2]);
		stats.put("StdDevDirectedDegreeOfNodes", dstats[3]);
		stats.put("NumberWith0DirectedDegreeOfNodes", dstats[4]);
		
		dstats = GraphStatisticUtils.degreeStats(g.getIterableNodes(), null, "in");
		stats.put("MinInDegreeOfNodes", dstats[0]);
		stats.put("MaxInDegreeOfNodes", dstats[1]);
		stats.put("AvgInDegreeOfNodes", dstats[2]);
		stats.put("StdDevInDegreeOfNodes", dstats[3]);
		stats.put("NumberWith0InDegreeOfNodes", dstats[4]);
		
		dstats = GraphStatisticUtils.degreeStats(g.getIterableNodes(), null, "out");
		stats.put("MinOutDegreeOfNodes", dstats[0]);
		stats.put("MaxOutDegreeOfNodes", dstats[1]);
		stats.put("AvgOutDegreeOfNodes", dstats[2]);
		stats.put("StdDevOutDegreeOfNodes", dstats[3]);
		stats.put("NumberWith0OutDegreeOfNodes", dstats[4]);
		
		for(String sid:nodesids) {
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid));
			stats.put("MinDegreeOfNodesSchema:"+sid, dstats[0]);
			stats.put("MaxDegreeOfNodesSchema:"+sid, dstats[1]);
			stats.put("AvgDegreeOfNodesSchema:"+sid, dstats[2]);
			stats.put("StdDevDegreeOfNodesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0DegreeOfNodesSchema:"+sid, dstats[4]);
			
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid), null, "undirected");
			stats.put("MinUndirectedDegreeOfNodesSchema:"+sid, dstats[0]);
			stats.put("MaxUndirectedDegreeOfNodesSchema:"+sid, dstats[1]);
			stats.put("AvgUndirectedDegreeOfNodesSchema:"+sid, dstats[2]);
			stats.put("StdDevUndirectedDegreeOfNodesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0UndirectedDegreeOfNodesSchema:"+sid, dstats[4]);
			
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid), null, "inandout");
			stats.put("MinDirectedDegreeOfNodesSchema:"+sid, dstats[0]);
			stats.put("MaxDirectedDegreeOfNodesSchema:"+sid, dstats[1]);
			stats.put("AvgDirectedDegreeOfNodesSchema:"+sid, dstats[2]);
			stats.put("StdDevDirectedDegreeOfNodesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0DirectedDegreeOfNodesSchema:"+sid, dstats[4]);
			
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid), null, "in");
			stats.put("MinInDegreeOfNodesSchema:"+sid, dstats[0]);
			stats.put("MaxInDegreeOfNodesSchema:"+sid, dstats[1]);
			stats.put("AvgInDegreeOfNodesSchema:"+sid, dstats[2]);
			stats.put("StdDevInDegreeOfNodesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0InDegreeOfNodesSchema:"+sid, dstats[4]);
			
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid), null, "out");
			stats.put("MinOutDegreeOfNodesSchema:"+sid, dstats[0]);
			stats.put("MaxOutDegreeOfNodesSchema:"+sid, dstats[1]);
			stats.put("AvgOutDegreeOfNodesSchema:"+sid, dstats[2]);
			stats.put("StdDevOutDegreeOfNodesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0OutDegreeOfNodesSchema:"+sid, dstats[4]);
		}
		
		dstats = GraphStatisticUtils.degreeStats(g.getIterableEdges());
		stats.put("MinDegreeOfEdges", dstats[0]);
		stats.put("MaxDegreeOfEdges", dstats[1]);
		stats.put("AvgDegreeOfEdges", dstats[2]);
		stats.put("StdDevDegreeOfEdges", dstats[3]);
		stats.put("NumberWith0DegreeOfEdges", dstats[4]);
		
		for(String sid:edgesids) {
			dstats = GraphStatisticUtils.degreeStats(OCUtils.getIterableItems(g, sid));
			stats.put("MinDegreeOfEdgesSchema:"+sid, dstats[0]);
			stats.put("MaxDegreeOfEdgesSchema:"+sid, dstats[1]);
			stats.put("AvgDegreeOfEdgesSchema:"+sid, dstats[2]);
			stats.put("StdDevDegreeOfEdgesSchema:"+sid, dstats[3]);
			stats.put("NumberWith0DegreeOfEdgeSchema:"+sid, dstats[4]);
		}
		
		return stats;
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(
				this.getStatisticDoubles(g),
				new LinkedHashMap<String, String>());
	}
}
