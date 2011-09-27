package linqs.gaia.graph.statistic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.MapUtils;

/**
 * Graph statistic which shows the number of graph items
 * have an unknown value for all possible features
 * over all possible schemas.
 * The values will be returned keyed by:
 * <br>
 * &lt;sid&gt;.&lt;fid&gt;=&lt;numberofmissingvalues&gt;
 * <br>
 * where &lt;sid&gt; is the schema id and &lt;fid&gt; is the
 * feature id of the schema.
 * 
 * @author namatag
 *
 */
public class MissingFeatureValueCount extends BaseConfigurable implements GraphStatistic {

	public Map<String, Double> getStatisticDoubles(Graph g) {
		Map<String, Double> statistics = new LinkedHashMap<String, Double>();
		
		// Get the list of schema ids
		Iterator<String> sitr = g.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			Schema schema = g.getSchema(sid);
			
			// Get the list of features ids
			Iterator<String> fitr = schema.getFeatureIDs();
			while(fitr.hasNext()) {
				String fid = fitr.next();
				int numunknown = 0;
				int numknown = 0;
				int total = 0;
				SchemaType type = schema.getType();
				
				Feature f = schema.getFeature(fid);
				if(f instanceof ExplicitFeature && ((ExplicitFeature) f).isClosed()) {
					// Closed features cannot have any unknown values
				} else {
					if(type.equals(SchemaType.NODE)
							|| type.equals(SchemaType.DIRECTED)
							|| type.equals(SchemaType.UNDIRECTED)) {
						// Go over all graph items
						Iterator<GraphItem> gitr = g.getGraphItems(sid);
						while(gitr.hasNext()) {
							GraphItem gi = gitr.next();
							if(gi.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)) {
								numunknown++;
							} else {
								numknown++;
							}
							
							total++;
						}
					} else if(type.equals(SchemaType.GRAPH)) {
						if(g.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)) {
							numunknown++;
						} else {
							numknown++;
						}
						
						total++;
					} else {
						throw new InvalidStateException("");
					}
				}
				
				if(numunknown!=0) {
					statistics.put(sid+"."+fid, 0.0+numunknown);
				}
			}
		}
		
		return statistics;
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(this.getStatisticDoubles(g),
				new LinkedHashMap<String,String>());
	}

}
