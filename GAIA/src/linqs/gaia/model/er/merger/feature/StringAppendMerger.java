/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.model.er.merger.feature;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.util.IteratorUtils;

/**
 * Merger to append the value of explicit string features.
 * The appended values are delimited using the specified delimiter.
 * 
 * Optional Parameters:
 * <UL>
 * <LI>delimiter-Delimiter to use to separate the different appended values.
 * Note:  Make sure the delimiter is not found in any of the strings values.
 * Default is a comma.
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
public class StringAppendMerger extends FeatureMerger {
	private static final long serialVersionUID = 1L;
	
	private List<String> fids = null;
	private String delimiter = ",";
	
	private void initialize(Decorable mergeditem) {
		fids = FeatureUtils.parseFeatureList(this, mergeditem.getSchema(),
				IteratorUtils.iterator2stringlist(mergeditem.getSchema().getFeatureIDs()));
		
		if(this.hasParameter("delimiter")) {
			this.delimiter = this.getStringParameter("delimiter");
		}
	}
	
	@Override
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
			
			// Only merge explicit string feature
			if(!(f instanceof ExplicitString)) {
				throw new ConfigurationException("String Append is only valid for String features: "+
						fid+" is "+f.getClass().getCanonicalName());
			}
			
			// Get all the feature values
			Set<String> values = new HashSet<String>();
			for(Decorable d:items) {
				FeatureValue value = d.getFeatureValue(fid);
				if(!value.equals(FeatureValue.UNKNOWN_VALUE)) {
					values.add(((StringValue) value).getStringValue());
				}
			}
			
			// Change String set to delimited list
			String append = null;
			Iterator<String> vitr = values.iterator();
			while(vitr.hasNext()) {
				if(append == null) {
					append = "";
				} else {
					append += delimiter;
				}
				
				append += vitr.next();
			}
			
			// Set the appended value
			mergeditem.setFeatureValue(fid, new StringValue(append));
		}
	}

}
