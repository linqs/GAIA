package linqs.gaia.graph.statistic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.MapUtils;

/**
 * For all possible edge types, return a statistic which
 * specifies the number of self links of that edge type.
 * (i.e., Undirected edge only has one node and at least one node
 * is in the source and target of the same directed edge).
 * The statistic is keyed by &lt;edgeschemaid&gt;.singletoncount
 * where edgeschemaid is the schema ID of each edge type.
 * 
 * @see GraphUtils#hasSelfLinks(Graph)
 * 
 * @author namatag
 *
 */
public class SelfLinkCount extends BaseConfigurable implements GraphStatistic {
	private static String suffix = ".singletoncount";
	public Map<String, Double> getStatisticDoubles(Graph g) {
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
		
		Iterator<String> esids = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
		while(esids.hasNext()) {
			String esid = esids.next();
			double count = 0;
			Iterator<Edge> eitr = g.getEdges(esid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				// If the undirected edge has only one node, its a self link
				if(e.numNodes()==1) {
					count++;
				}
			}
			
			stats.put(esid+suffix, count);
		}
		
		esids = g.getAllSchemaIDs(SchemaType.DIRECTED);
		while(esids.hasNext()) {
			String esid = esids.next();
			double count = 0;
			Iterator<Edge> eitr = g.getEdges(esid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				DirectedEdge de = (DirectedEdge) e;
				Set<Node> sources = IteratorUtils.iterator2nodeset(de.getSourceNodes());
				Set<Node> targets = IteratorUtils.iterator2nodeset(de.getTargetNodes());
				sources.retainAll(targets);
				
				// If there is at least one node in the intersect of
				// the source and target nodes, its a self link
				if(!sources.isEmpty()) {
					count++;
				}
				stats.put(esid+suffix, count);
			}
			
			stats.put(esid+suffix, count);
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
