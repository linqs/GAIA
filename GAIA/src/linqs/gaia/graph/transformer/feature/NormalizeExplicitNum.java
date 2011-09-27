package linqs.gaia.graph.transformer.feature;

import java.util.Iterator;
import java.util.List;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.MinMax;

/**
 * <p>Normalize features as follows:<br>
 * normalized value = (old value - min value)/(max value - min value). </p>
 * 
 * Required Parameters:
 * <UL>
 * <LI>schemaid-Schema id of features to normalize.
 * By default all numeric explicit features are normalized.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex "patterns"
 * used to identify the set of features to use in the model.
 * All feature ids, from the specified featureschemaid, which match
 * at least one of the patterns is included.  Default is to use
 * all the {@link NumFeature} features defined for the specified schema id.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI>excludefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex "patterns"
 * used to identify the set of features to use in the model.
 * Given the set of feature ids which match at least
 * one pattern of includefeatures (or the default set of features when
 * includefeatures is not specified), remove all feature ids
 * which match at least one of these patterns.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NormalizeExplicitNum extends Transformer {
	@Override
	public void transform(Graph graph) {
		String schemaid = this.getStringParameter("schemaid");
		Schema schema = graph.getSchema(schemaid);
		
		List<String> fids = FeatureUtils.parseFeatureList(this,
			schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			if(f instanceof ExplicitNum) {
				this.normalizeFeature(graph, schemaid, fid);
			}
		}
	}
	
	private void normalizeFeature(Graph graph, String schemaid, String fid) {
		MinMax minmax = new MinMax();
		
		// Get the minimum and maximum value for a numeric feature
		// Note:  Skip those whose values are unknown.
		Iterator<GraphItem> gitr = graph.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			
			if(gi.hasFeatureValue(fid)) {
				minmax.addValue(((NumValue) gi.getFeatureValue(fid)).getNumber());
			}
		}
		
		// Normalize feature
		// Note:  Skip those whose values are unknown.
		gitr = graph.getGraphItems(schemaid);
		double min = minmax.getMin();
		double maxmindiff = minmax.getMax() - min;
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			
			if(!gi.hasFeatureValue(fid)) {
				continue;
			}
			
			double orig = ((NumValue) gi.getFeatureValue(fid)).getNumber();
			double normalized = 1;
			if(maxmindiff!=0) {
				normalized = (orig - min) / (maxmindiff);
			}
			
			gi.setFeatureValue(fid, new NumValue(normalized));
		}
	}
}
