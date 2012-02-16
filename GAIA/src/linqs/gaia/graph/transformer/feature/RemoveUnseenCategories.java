package linqs.gaia.graph.transformer.feature;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.UnmodifiableList;

/**
 * For features of type CategFeature,
 * modify the feature so as not to include categories
 * which do not appear in the specified graph.
 * <p>
 * Note: This does not retain the probability distributions
 * associated with the categories as it is no longer valid.
 * It instead sets the probability of the specified category as 1.0
 * and all else to 0.0.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of items to remove
 * <LI> featureid-Feature ID of feature whose value we are considering
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveUnseenCategories extends Transformer{

	@Override
	public void transform(Graph graph) {
		String schemaid = this.getStringParameter("schemaid");
		String featureid = this.getStringParameter("featureid");
		
		Schema schema = graph.getSchema(schemaid);
		Feature f = schema.getFeature(featureid);
		if(!(f instanceof CategFeature)) {
			throw new UnsupportedTypeException("Only categorically valued feature supported: "
					+featureid
					+" is of type "+f.getClass().getCanonicalName());
		}
		
		// Create temporary feature
		String tmpfeatureid = schema.generateRandomFeatureID();
		schema.addFeature(tmpfeatureid, f);
		graph.updateSchema(schemaid, schema);
		
		// Count items
		KeyedCount<String> labelcount = new KeyedCount<String>();
		Iterator<GraphItem> gitr = graph.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(featureid);
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			}
			
			labelcount.increment(((CategValue) fv).getCategory());
			gi.setFeatureValue(tmpfeatureid, fv);
		}
		
		// Identify represented categories
		UnmodifiableList<String> categories = ((CategFeature) f).getAllCategories();
		List<String> tokeep = new LinkedList<String>();
		for(String c:categories) {
			if(labelcount.getCount(c)>0) {
				tokeep.add(c);
			}
		}
		
		// Remove old feature
		schema = graph.getSchema(schemaid);
		schema.removeFeature(featureid);
		graph.updateSchema(schemaid, schema);
		
		// Add new feature
		schema = graph.getSchema(schemaid);
		schema.addFeature(featureid, new ExplicitCateg(tokeep));
		graph.updateSchema(schemaid, schema);
		
		// Restore value
		gitr = graph.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(tmpfeatureid);
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			}
			
			CategValue cv = (CategValue) fv;
			gi.setFeatureValue(featureid, cv.getCategory());
		}
		
		// Remove temporary featureid
		schema = graph.getSchema(schemaid);
		schema.removeFeature(tmpfeatureid);
		graph.updateSchema(schemaid, schema);
	}
}
