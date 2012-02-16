package linqs.gaia.graph.statistic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.MapUtils;

/**
 * For all possible edge types, return a statistic which
 * specifies the number of duplicate links of that edge type.
 * (i.e., the same set of nodes are incident to multiple undirected edges,
 * of the same schema id, or the same set of source nodes and the same set
 * of target nodes are incident to multiple directed edges of the same schema id).
 * The statistic is keyed by &lt;edgeschemaid&gt;.duplicatecount
 * where edgeschemaid is the schema ID of each edge type.
 * 
 * @author namatag
 *
 */
public class DuplicateLinkCount extends BaseConfigurable implements GraphStatistic {
	private static String suffix = ".duplicatecount";
	public Map<String, Double> getStatisticDoubles(Graph g) {
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
		
		Iterator<String> esids = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
		while(esids.hasNext()) {
			Set<Edge> processed = new HashSet<Edge>();
			
			String esid = esids.next();
			double count = 0;
			Iterator<Edge> eitr = g.getEdges(esid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				if(processed.contains(e)) {
					continue;
				}
				
				Set<Node> edgenodes = IteratorUtils.iterator2nodeset(e.getAllNodes());
				Node n = (Node) e.getAllNodes().next();
				
				Iterator<Edge> neitr = n.getAllEdges(esid);
				while(neitr.hasNext()) {
					Edge ne = neitr.next();
					if(ne.equals(e)) {
						continue;
					}
					
					Set<Node> nenodes = IteratorUtils.iterator2nodeset(ne.getAllNodes());
					if(edgenodes.equals(nenodes)) {
						count++;
						processed.add(ne);
					}
				}
				
				processed.add(e);
			}
			
			stats.put(esid+suffix, count);
		}
		
		esids = g.getAllSchemaIDs(SchemaType.DIRECTED);
		while(esids.hasNext()) {
			Set<Edge> processed = new HashSet<Edge>();
			
			String esid = esids.next();
			double count = 0;
			Iterator<Edge> eitr = g.getEdges(esid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				if(processed.contains(e)) {
					continue;
				}
				
				Set<Node> sourcenodes = IteratorUtils.iterator2nodeset(((DirectedEdge) e).getSourceNodes());
				Set<Node> targetnodes = IteratorUtils.iterator2nodeset(((DirectedEdge) e).getTargetNodes());
				Node n = sourcenodes.iterator().next();
				
				Iterator<DirectedEdge> neitr = n.getEdgesWhereSource(esid);
				while(neitr.hasNext()) {
					Edge ne = neitr.next();
					if(ne.equals(e)) {
						continue;
					}
					
					Set<Node> nesourcenodes = IteratorUtils.iterator2nodeset(((DirectedEdge) ne).getSourceNodes());
					Set<Node> netargetnodes = IteratorUtils.iterator2nodeset(((DirectedEdge) ne).getTargetNodes());
					if(sourcenodes.equals(nesourcenodes) && targetnodes.equals(netargetnodes)) {
						count++;
						processed.add(ne);
					}
				}
				
				processed.add(e);
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
