package linqs.gaia.graph.transformer.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.util.ListUtils;

/**
 * Summarize the sparse valued feature as a string valued
 * feature which lists the feature ids of all the sparse
 * features whose value is not the sparse/default value given.
 * For example, if the default value is "0.0" and a node has
 * features w1, w2, and w3 with values 0.0, 1.0, 2.0 respectively,
 * then the summary string feature for that node will be
 * "w2,w3".
 * Required Parameters:
 * <UL>
 * <LI> summarysid-Schema ID of items whose sparse features we want to summarize
 * <LI> summaryfid-Feature ID of the string valued, comma delimited, summary feature
 * <LI> defaultvalue-String representation of default/sparse value
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> removesummarized-If "yes", remove all the features whose values were summarized.
 * Default is "no".
 * <LI> removeallsparse-If "yes", remove all items which have the default/sparse values
 * for all the features to summarize.  Default is "no".
 * <LI> summaryinclude-Features whose values we want to summarize.
 * If not defined, all features of type {@link ExplicitFeature} are used.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI> summaryexclude-Features whose values we do not want to summarize.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SummarizeSparseFeatures extends Transformer {
	@Override
	public void transform(Graph g) {
		String summarysid = this.getStringParameter("summarysid");
		String summaryfid = this.getStringParameter("summaryfid");
		String defaultvalue = this.getStringParameter("defaultvalue");
		boolean removesummarized = this.getYesNoParameter("removesummarized","no");
		boolean removeallsparse = this.getYesNoParameter("removeallsparse","no");
		
		// Add string feature, if not defined
		Schema schema = g.getSchema(summarysid);
		if(!schema.hasFeature(summaryfid)) {
			schema.addFeature(summaryfid, new ExplicitString());
			g.updateSchema(summarysid, schema);
		}
		
		// Get feature values to compute over
		List<String> fids = FeatureUtils.parseFeatureList(this,
				schema,
				FeatureUtils.getFeatureIDs(schema, 0),
				"summaryinclude", "summaryexclude");
		
		// Iterate over nodes
		List<GraphItem> allsparse = new LinkedList<GraphItem>();
		Iterator<GraphItem> nitr = g.getGraphItems(summarysid);
		while(nitr.hasNext()) {
			GraphItem n = nitr.next();
			List<String> currfids = new ArrayList<String>();
			for(String fid:fids) {
				// Find features which this node has a non-default value for
				String value = n.getFeatureValue(fid).getStringValue();
				if(!value.equals(defaultvalue)) {
					currfids.add(fid);
				}
			}
			
			if(removeallsparse && currfids.isEmpty()) {
				allsparse.add(n);
			}
			
			// Set summary feature with a comma delimited list of the featureids
			n.setFeatureValue(summaryfid, new StringValue(ListUtils.list2string(currfids, ",")));
		}
		
		// Remove nodes with all sparse values
		if(removeallsparse) {
			for(GraphItem gi:allsparse) {
				g.removeGraphItem(gi);
			}
		}
		
		// Remove summarized feature, if requested
		if(removesummarized) {
			schema = g.getSchema(summarysid);
			for(String fid:fids) {
				schema.removeFeature(fid);
			}
			
			g.updateSchema(summarysid, schema);
		}
	}
}
