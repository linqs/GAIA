package linqs.gaia.graph.statistic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.MapUtils;

/**
 * Statistic which computes the percentage of each node schema is incident
 * an edge schema type.  The following keys are used:
 * <UL>
 * <LI>&lt;edgeschemaid&gt;-PercentSourceNodesWithSid-&lt;nodeschemaid&gt; (for directed edges)
 * <LI>&lt;edgeschemaid&gt;-PercentTargetNodesWithSid-&lt;nodeschemaid&gt; (for directed edges)
 * <LI>&lt;edgeschemaid&gt;-PercentNodesWithSid-&lt;nodeschemaid&gt; (for undirected edges)
 * </UL>
 * where &lt;edgeschemaid&gt; is the specified edge schema ID and &lt;nodeschemaid&gt;
 * is all defined node schema ids in the graph.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> edgeschemaid-Schema ID of the edge to get the percent of incident nodes for.
 * If not defined, the statistic is computed for all edge schema IDs in the graph.
 * </UL>
 * 
 * @author namatag
 *
 */
public class PercentIncidentBySchemaID  extends BaseConfigurable implements GraphStatistic {
	private boolean initialize = true;
	private String edgeschemaid = null;
	
	private void initialize() {
		initialize = false;
		
		edgeschemaid = null;
		if(this.hasParameter("edgeschemaid")) {
			edgeschemaid = this.getStringParameter("edgeschemaid");
		}
	}
	
	public Map<String, Double> getStatisticDoubles(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		if(edgeschemaid == null) {
			Map<String,Double> stats = new HashMap<String,Double>();
			// Go over all edge schema ids
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
			while(sitr.hasNext()) {
				String sid = sitr.next();
				stats.putAll(this.getStatisticDoubles(g, sid));
			}
			
			sitr = g.getAllSchemaIDs(SchemaType.DIRECTED);
			while(sitr.hasNext()) {
				String sid = sitr.next();
				stats.putAll(this.getStatisticDoubles(g, sid));
			}
			
			return stats;
		} else {
			return this.getStatisticDoubles(g, edgeschemaid);
		}
	}
	
	private Map<String, Double> getStatisticDoubles(Graph g, String edgeschemaid) {
		Map<String,Double> stats = new HashMap<String,Double>();
		SchemaType edgetype = g.getSchemaType(edgeschemaid);
		
		KeyedCount<String> nodesidcount = new KeyedCount<String>();
		KeyedCount<String> sourcesidcount = new KeyedCount<String>();
		KeyedCount<String> targetsidcount = new KeyedCount<String>();
		Iterator<Edge> eitr = g.getEdges(edgeschemaid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			if(edgetype.equals(SchemaType.DIRECTED)) {
				DirectedEdge de = (DirectedEdge) e;
				Iterator<Node> nitr = de.getSourceNodes();
				while(nitr.hasNext()) {
					sourcesidcount.increment(nitr.next().getSchemaID());
				}
				
				nitr = de.getTargetNodes();
				while(nitr.hasNext()) {
					targetsidcount.increment(nitr.next().getSchemaID());
				}
			} else if(edgetype.equals(SchemaType.UNDIRECTED)) {
				Iterator<Node> nitr = e.getAllNodes();
				while(nitr.hasNext()) {
					nodesidcount.increment(nitr.next().getSchemaID());
				}
			} else {
				throw new UnsupportedTypeException("Invalid edge type: "+edgetype);
			}
		}
		
		// Get statistics for edge schema id
		Iterator<String> nodesids = g.getAllSchemaIDs(SchemaType.NODE);
		while(nodesids.hasNext()) {
			String nodesid = nodesids.next();
			
			if(edgetype.equals(SchemaType.DIRECTED)) {
				stats.put(edgeschemaid+"-PercentSourceNodesWithSid-"+nodesid, sourcesidcount.getPercent(nodesid));
				stats.put(edgeschemaid+"-PercentTargetNodesWithSid-"+nodesid, targetsidcount.getPercent(nodesid));
			} else if(edgetype.equals(SchemaType.UNDIRECTED)) {
				stats.put(edgeschemaid+"-PercentNodesWithSid-"+nodesid, sourcesidcount.getPercent(nodesid));
			} else {
				throw new UnsupportedTypeException("Invalid edge type: "+edgetype);
			}
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
