package linqs.gaia.model.er.merger.feature;

import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.global.Constants;
import linqs.gaia.util.IteratorUtils;

/**
 * Return if true if either value is true.  A true value is either
 * a numeric value with a value of 1.0 or a categorical value of Constants.TRUE.
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
public class BinaryORFeatureMerger extends FeatureMerger {
	private static final long serialVersionUID = 1L;
	
	private List<String> fids = null;
	private NumValue truenum = new NumValue(1.0);
	private CategValue truecat = new CategValue(Constants.TRUE, new double[]{0,1});
	
	private void initialize(Decorable mergeditem) {
		if(fids != null) {
			return;
		}
		
		fids = FeatureUtils.parseFeatureList(this, mergeditem.getSchema(),
				IteratorUtils.iterator2stringlist(mergeditem.getSchema().getFeatureIDs()));
	}

	public void merge(Iterable<Decorable> items, Decorable mergeditem) {
		String schemaid = mergeditem.getSchemaID();
		
		// Initialize
		initialize(mergeditem);
		
		// All items, including the merged item, must have the same schema id
		boolean isempty = true;
		Iterator<Decorable> ditr = items.iterator();
		while(ditr.hasNext()) {
			Decorable d = ditr.next();
			if(!d.getSchemaID().equals(schemaid)) {
				throw new InvalidStateException("Decorable item does not have the same" +
						"schema as the item to merge to: "+d+" to "+mergeditem);
			}
			
			isempty = false;
		}
		
		if(isempty) {
			throw new InvalidStateException("There are no defined items for: "+mergeditem);
		}
		
		// Iterate over all features
		Schema schema = mergeditem.getSchema();
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			// Only merge explicit feature
			if(!(f instanceof ExplicitFeature)) {
				continue;
			}
			
			// Get all the feature values
			FeatureValue mergeval = FeatureValue.UNKNOWN_VALUE;
			for(Decorable d:items) {
				FeatureValue value = d.getFeatureValue(fid);
				if(!value.equals(FeatureValue.UNKNOWN_VALUE)) {
					mergeval = value;
					
					if(mergeval instanceof NumValue) {
						if(mergeval.equals(truenum)) {
							break;
						}
					} else if(mergeval instanceof CategValue) {
						if(mergeval.equals(truecat)) {
							break;
						}
					} else {
						throw new ConfigurationException("Unsupported feature type: "
								+mergeval.getClass().getCanonicalName());
					}
				}
			}
			
			// Set merged value
			mergeditem.setFeatureValue(fid, mergeval);
		}
	}

}
