package linqs.gaia.model.er.merger.feature;

import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;

/**
 * This feature merger get the mean feature value, for all explicit numeric features,
 * among all the given items.  This value is set as the value of the merged item.
 * Items which have the value unknown are ignored.  Non numeric features
 * are set to the majority value of the merged items.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex "patterns"
 * used to identify the set of features to use in the model.
 * All feature ids, from the specified featureschemaid, which match
 * at least one of the patterns is included.  Default is to use
 * all the features defined for the specified schema id.
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
public class MeanFeatureMerger extends FeatureMerger {
	private static final long serialVersionUID = 1L;
	
	private List<String> fids = null;
	
	private void initialize(Decorable mergeditem) {
		if(fids != null) {
			return;
		}
		
		fids = FeatureUtils.parseFeatureList(this, mergeditem.getSchema(),
				IteratorUtils.iterator2stringlist(mergeditem.getSchema().getFeatureIDs()));
	}

	public void merge(Iterable<Decorable> items, Decorable mergeditem) {
		String schemaid = mergeditem.getSchemaID();
		
		initialize(mergeditem);
		
		// All items, including the merged item, must have the same schema id
		Iterator<Decorable> ditr = items.iterator();
		while(ditr.hasNext()) {
			Decorable d = ditr.next();
			if(!d.getSchemaID().equals(schemaid)) {
				throw new InvalidStateException("Decorable item does not have the same" +
						"schema as the item to merge to: "+d+" to "+mergeditem);
			}
		}
		
		// Iterate over all features
		Schema schema = mergeditem.getSchema();
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			// Only merge explicit feature
			if(!(f instanceof ExplicitFeature)) {
				continue;
			}
			
			if(f instanceof NumFeature) {
				// Get all the feature values
				double sum = 0;
				int counter = 0;
				for(Decorable d:items) {
					FeatureValue value = d.getFeatureValue(fid);
					if(!value.equals(FeatureValue.UNKNOWN_VALUE)) {
						NumValue numvalue = (NumValue) value;
						sum += numvalue.getNumber();
						counter++;
					}
				}
				
				if(counter == 0) {
					throw new InvalidStateException("Counter is set to 0 for "+fid);
				}
				
				// Get mean value
				mergeditem.setFeatureValue(fid, new NumValue(sum/(double) counter));
			} else {
				// Get all the feature values
				KeyedCount<FeatureValue> valcount = new KeyedCount<FeatureValue>();
				for(Decorable d:items) {
					FeatureValue value = d.getFeatureValue(fid);
					if(!value.equals(FeatureValue.UNKNOWN_VALUE)) {
						valcount.increment(value);
					}
				}
				
				// Get majority
				FeatureValue majval = valcount.highestCountKey();
				if(majval!=null) {
					mergeditem.setFeatureValue(fid, majval);
				}
			}
		}
	}

}
