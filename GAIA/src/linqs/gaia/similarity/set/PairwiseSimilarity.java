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
package linqs.gaia.similarity.set;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.util.Dynamic;

/**
 * This set similarity measure is applicable only to sets
 * of graph items.  What it does is that for each item
 * in the two sets, we get the most similar item in the other set.
 * We then take the average of the similarities and return the results.
 * <p>
 * Similarity between the Graph Items are calculated using a normalized
 * list similarity of their feature values.
 * 
 * Required Parameters:
 * <UL>
 * <LI>featuresimclass-{@link NormalizedListSimilarity} measure,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use to calculate feature similarity.
 * </UL>
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
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class PairwiseSimilarity extends BaseConfigurable implements NormalizedSetSimilarity {
	private static final long serialVersionUID = 1L;
	
	private List<String> featureids = null;
	NormalizedListSimilarity featuresim = null;
	
	private void initialize(Set<? extends Object> item1, Set<? extends Object> item2) {
		
		Object o = item1.iterator().next();
		if(!(o instanceof GraphItem)) {
			throw new UnsupportedTypeException("Can only get feature values for Graph Items: "+o);
		}
		
		Schema schema = ((GraphItem) o).getSchema();
		featureids = FeatureUtils.parseFeatureList(this,
							schema, FeatureUtils.getFeatureIDs(schema, 2));
		
		String featuresimclass = this.getStringParameter("featuresimclass");
		featuresim = (NormalizedListSimilarity) 
		Dynamic.forConfigurableName(NormalizedListSimilarity.class, featuresimclass);
		featuresim.copyParameters(this);
	}

	public double getNormalizedSimilarity(Set<? extends Object> item1,
			Set<? extends Object> item2) {
		
		// Return 0 if at least one set is empty
		if(item1.size() == 0 || item2.size() == 0) {
			return 0;
		}
		
		// Initialize
		if(featureids == null) {
			this.initialize(item1, item2);
		}
		
		Map<Object, Double> similarities = new HashMap<Object,Double>();

		// Calculate the similarities of objects in item1 to objects in item2
		for(Object i1:item1) {
			for(Object i2:item2) {
				
				double sim = featuresim.getNormalizedSimilarity(
						this.getFeatureValues(i1), this.getFeatureValues(i2));
				
				// For each item in item1, store the similarity of most similar object
				if(!similarities.containsKey(i1) || (similarities.containsKey(i1) && similarities.get(i1) < sim)) {
					similarities.put(i1, sim);
				}
				
				// For each item in item2, store the similarity of most similar object
				if(!similarities.containsKey(i2) || (similarities.containsKey(i2) && similarities.get(i2) < sim)) {
					similarities.put(i1, sim);
				}
			}
		}
		
		// Sum the similarities for each object
		double sum = 0;
		Set<Entry<Object,Double>> allentries = similarities.entrySet();
		for(Entry<Object,Double> e:allentries) {
			sum += e.getValue();
		}
		
		// Divide sum by number of items in i1 and i2
		return sum / (double) allentries.size();
	}

	public double getSimilarity(Set<? extends Object> item1,
			Set<? extends Object> item2) {
		return this.getNormalizedSimilarity(item1, item2);
	}

	/**
	 * Get a list of the feature values of a node
	 * 
	 * @param n Node to get feature values of
	 * @return List of feature values
	 */
	private List<FeatureValue> getFeatureValues(Object o) {
		if(!(o instanceof GraphItem)) {
			throw new UnsupportedTypeException("Can only get feature values for Graph Items: "+o);
		}
		
		GraphItem gi = (GraphItem) o;
		
		// Get feature values
		List<FeatureValue> fvalues = new ArrayList<FeatureValue>(featureids.size());
		for(String fid:featureids) {
			fvalues.add(gi.getFeatureValue(fid));
		}
		
		return fvalues;
	}
}
