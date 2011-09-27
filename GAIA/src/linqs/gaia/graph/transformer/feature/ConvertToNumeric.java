package linqs.gaia.graph.transformer.feature;

import java.util.Iterator;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;

/**
 * Create a numeric feature for the specified features
 * using the string representation of the feature values
 * as the string representations of the numeric values.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> features-Comma delimited list of features in the form SCHEMAID.FEATUREID
 * (i.e., person.age, person.weight).
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConvertToNumeric extends Transformer {

	@Override
	public void transform(Graph graph) {
		String[] pairs = this.getStringParameter("features").split(",");
		
		for(String p:pairs) {
			String[] sfpair = p.split("\\.");
			
			String sid = sfpair[0];
			String fid = sfpair[1];
			
			convertToNum(graph, sid, fid);
		}
	}
	
	/**
	 * Convert the specified feature for the specified schema id and graph
	 * to an explicit valued numeric feature.
	 * 
	 * @param graph Graph
	 * @param sid Schema ID
	 * @param fid Feature ID
	 */
	public static void convertToNum(Graph graph, String sid, String fid) {
		String tmpfid = "gaiatemp-"+fid;
		Schema schema = graph.getSchema(sid);
		
		// Add temporary feature
		Feature tempf = new ExplicitString();
		schema.addFeature(tmpfid, tempf);
		graph.updateSchema(sid, schema);
		
		// Copy values to temporary feature
		Iterator<GraphItem> gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(fid);
			
			if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				// Copy feature value to temporary feature
				gi.setFeatureValue(tmpfid, new StringValue(fv.getStringValue()));
			}
		}
		
		// Remove old feature
		schema.removeFeature(fid);
		graph.updateSchema(sid, schema);
		
		// Create numeric feature
		Feature categf = new ExplicitNum();
		schema.addFeature(fid, categf);
		graph.updateSchema(sid, schema);
		
		// Set the value for the categorical feature
		gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(tmpfid);
			
			// Skip unknown values
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			}
			
			// Copy feature value to temporary feature
			gi.setFeatureValue(fid, new NumValue(Double.parseDouble(fv.getStringValue())));
		}
		
		// Remove temporary feature
		schema.removeFeature(tmpfid);
		graph.updateSchema(sid, schema);
	}

}
