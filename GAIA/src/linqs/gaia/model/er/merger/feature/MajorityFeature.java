package linqs.gaia.model.er.merger.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;

/**
 * This feature merger get the majority feature value, for all explicit features,
 * among all the given items.  This value is set as the value of the merged item.
 * Items which have the value unknown are ignored.  If more than one value have
 * the same number of items which have it, one is randomly chosen.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> numvaluetype-If specified, instead of returning the most common
 * numeric value for numeric feature, return an aggregate on the specified type. Option are:
 *   <UL>
 *   <LI> min-Mininum value of numeric feature
 *   <LI> max-Maximum value of numeric feature
 *   <LI> mean-Mean value of numeric feature
 *   <LI> stdev-Standard deviation value of numeric feature
 *   </UL>
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
public class MajorityFeature extends FeatureMerger {
	private static final long serialVersionUID = 1L;
	
	private List<String> fids = null;
	private String numvaluetype = null;
	
	private static FeatureValue NUMVALUE0 = new NumValue(0);
	private static FeatureValue NUMVALUE1 = new NumValue(1);
	
	private void initialize(Decorable mergeditem) {
		fids = FeatureUtils.parseFeatureList(this, mergeditem.getSchema(),
				IteratorUtils.iterator2stringlist(mergeditem.getSchema().getFeatureIDs()));
		
		if(this.hasParameter("numvaluetype")) {
			numvaluetype = this.getCaseParameter("numvaluetype", new String[]{"min","max","mean","stddev"});
		}
	}

	public void merge(Iterable<Decorable> items, Decorable mergeditem) {
		String schemaid = mergeditem.getSchemaID();
		
		// Initialize
		if(fids == null) {
			initialize(mergeditem);
		}
		
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
			KeyedCount<FeatureValue> valcount = new KeyedCount<FeatureValue>();
			List<Double> numvalues = new ArrayList<Double>();
			for(Decorable d:items) {
				FeatureValue value = d.getFeatureValue(fid);
				if(!value.equals(FeatureValue.UNKNOWN_VALUE)) {
					// Get all numeric features
					if(numvaluetype!=null && value instanceof NumValue) {
						numvalues.add(((NumValue) value).getNumber());
					} else {
						valcount.increment(value);
					}
				}
			}
			
			// Get majority
			FeatureValue modefv = null;
			if(numvaluetype==null || numvalues.isEmpty()) {
				// Get the most common value
				modefv = valcount.highestCountKey();
				if(modefv==null) {
					// If none were encountered (i.e., no neighbors), return as unknown
					modefv = FeatureValue.UNKNOWN_VALUE;
				}
			} else {
				double[] array = new double[numvalues.size()];
				for(int i=0; i<numvalues.size(); i++) {
					array[i]=numvalues.get(i);
				}
				
				// Aggregate numbers are appropriate
				double currnumval = 0;
				if(numvaluetype.equals("mean")) {
					currnumval = ArrayUtils.average(array);
				} else if(numvaluetype.equals("min")) {
					currnumval = ArrayUtils.minValue(array);
				} else if(numvaluetype.equals("max")) {
					currnumval = ArrayUtils.maxValue(array);
				} else if(numvaluetype.equals("stddev")) {
					currnumval = ArrayUtils.stddev(array);
				} else {
					throw new ConfigurationException("Unsupported numvaluetype: "+numvaluetype);
				}
				
				// To save memory, specially with caching, minimize having to instantiate new objects
				if(currnumval==0.0) {
					modefv = NUMVALUE0;
				} else if(currnumval==1.0) {
					modefv = NUMVALUE1;
				} else {
					modefv = new NumValue(currnumval);
				}
			}
			
			if(modefv!=null) {
				mergeditem.setFeatureValue(fid, modefv);
			}
		}
	}

}
