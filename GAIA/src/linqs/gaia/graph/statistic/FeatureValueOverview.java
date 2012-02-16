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
package linqs.gaia.graph.statistic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.MapUtils;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Graph statistic which shows an overview of all defined features
 * over all defined schemas.
 * The values will be returned keyed by:
 * <br>
 * &lt;sid&gt;.&lt;fid&gt;=&lt;overview&gt;
 * <br>
 * where &lt;sid&gt; is the schema id and &lt;fid&gt; is the
 * feature id of the schema.
 * <p>
 * For composite features,
 * the value will be keyed by:
 * <br>
 * &lt;sid&gt;.&lt;fid&gt;.&lt;cid&gt;=&lt;overview&gt;
 * <br>
 * where .&lt;cfid&gt; is the name of that value in the composite feature.
 * <p>
 * The value of overview varies on the type of attribute.
 * For categories, we return the counts for all values.
 * For numbers, we return the min, max, and mean values for that number,
 * as well as the number of 0 values and number of unique values.
 * For all other types, we return the number of unique values
 * {@link FeatureValue#getStringValue()} value.
 * <p>
 * We also have a special value keyed by:
 * <br>
 * &lt;sid&gt;.NumberOfFeatures=&lt;numberoffeatures&gt;
 * <br>
 * which specifies, for each schema, how many features are defined in that schema.
 * <p>
 * Note: The output is a string representation and is undefined
 * for {@link #getStatisticDoubles(Graph)} where all values are set to -1.
 * 
 * @author namatag
 *
 */
public class FeatureValueOverview extends BaseConfigurable implements GraphStatistic {

	public Map<String, Double> getStatisticDoubles(Graph g) {
		Map<String, Double> statistics = new LinkedHashMap<String, Double>();
		Map<String, String> summary = getStatisticStrings(g);
		Set<String> keys = summary.keySet();
		for(String key:keys) {
			statistics.put(key, -1.0);
		}
		
		return statistics;
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticStrings(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		Map<String, String> statistics = new LinkedHashMap<String, String>();
		
		// Get the list of schema ids
		Iterator<String> sitr = g.getAllSchemaIDs();
		while(sitr.hasNext()) {
			String sid = sitr.next();
			Schema schema = g.getSchema(sid);
			
			// Exclude statistics on the graph itself
			if(g.getSchemaType(sid).equals(SchemaType.GRAPH)) {
				continue;
			}
			
			// Get the list of features ids
			statistics.put(sid+".NumberOfFeatures", ""+schema.numFeatures());
			
			Iterator<String> fitr = schema.getFeatureIDs();
			while(fitr.hasNext()) {
				String fid = fitr.next();
				
				String value = getFeatureValueOverview(g, sid, fid);
				statistics.put(sid+"."+fid, value);
			}
		}
		
		return statistics;
	}
	
	/**
	 * Get an overview of the feature values of the feature
	 * defined in the given schema.
	 * 
	 * @param g Graph to print overview for
	 * @param schemaid Schema ID of schema whose values we want an overview for
	 * @param fid Feature ID of the feature whose value we want an overview for
	 * @return String overview of the feature
	 */
	public static String getFeatureValueOverview(Graph g, String schemaid, String fid) {
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(fid);
		StringBuffer buf = new StringBuffer();
		
		if(f instanceof CompositeFeature) {
			// Handle composite features
			CompositeFeature cf = (CompositeFeature) f;
			UnmodifiableList<SimplePair<String,CVFeature>> cffeatures = cf.getFeatures();
			
			boolean cache = false;
			if(!cf.isCaching()) {
				cf.setCache(true);
				cache = true;
			}
			
			for(int i=0; i<cffeatures.size(); i++) {
				SimplePair<String,CVFeature> pair = cffeatures.get(i);
				String cfid = pair.getFirst();
				CVFeature cvf = pair.getSecond();
				
				Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
				double numunknown = 0;
				double numknown = 0;
				MinMax mm = new MinMax();
				int numzero = 0;
				KeyedCount<String> catvalues = new KeyedCount<String>();
				Set<String> values = new HashSet<String>();
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					FeatureValue fv = gi.getFeatureValue(fid);
					if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						fv = ((CompositeValue) fv).getFeatureValues().get(i);
					}
					
					// Count number known or unknown value
					if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						numunknown++;
						continue;
					} else {
						numknown++;
					}
					
					// Process based on type
					if(cvf instanceof NumFeature) {
						double value = ((NumValue) fv).getNumber();
						if(value==0) {
							numzero++;
						}
						
						mm.addValue(value);
						values.add(fv.getStringValue());
					} else if(cvf instanceof CategFeature) {
						catvalues.increment(((CategValue) fv).getCategory());
					} else if(cvf instanceof StringFeature) {
						values.add(fv.getStringValue());
					} else {
						values.add(fv.getStringValue());
					}
				}
				
				buf.append(schemaid+"."+fid+"."+cfid+": #unknown="+numunknown+" #known="+numknown);
				if(cvf instanceof NumFeature) {
					buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
					+" #zero="+numzero+" #uniquevalues="+values.size());
				} else if(cvf instanceof CategFeature) {
					List<String> keys = new ArrayList<String>(catvalues.getKeys());
					Collections.sort(keys);
					for(String key:keys) {
						buf.append(" #"+key+"="+catvalues.getCount(key));
					}
				} else if(cvf instanceof StringFeature) {
					buf.append(" #uniquevalues="+values.size());
				} else {
					buf.append(" #uniquevalues="+values.size());
				}
			}
			
			if(cache) {
				((DerivedFeature) f).resetCache();
				((DerivedFeature) f).setCache(false);
			}
		} else {
			// Handle non-composite features
			Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
			double numunknown = 0;
			double numknown = 0;
			MinMax mm = new MinMax();
			int numzero = 0;
			KeyedCount<String> catvalues = new KeyedCount<String>();
			Set<String> values = new HashSet<String>();
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();
				FeatureValue fv = gi.getFeatureValue(fid);
				
				// Count number known or unknown value
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					numunknown++;
					continue;
				} else {
					numknown++;
				}
				
				// Process based on type
				if(f instanceof NumFeature) {
					double value = ((NumValue) fv).getNumber();
					if(value==0) {
						numzero++;
					}
					
					mm.addValue(value);
					values.add(fv.getStringValue());
				} else if(f instanceof CategFeature) {
					catvalues.increment(((CategValue) fv).getCategory());
				} else if(f instanceof StringFeature) {
					values.add(fv.getStringValue());
				} else {
					values.add(fv.getStringValue());
				}
			}
			
			buf.append(schemaid+"."+fid+": #unknown="+numunknown+" #known="+numknown);
			if(f instanceof NumFeature) {
				buf.append(" min="+mm.getMin()+" max="+mm.getMax()+" mean="+mm.getMean()
							+" #zero="+numzero+" #uniquevalues="+values.size());
			} else if(f instanceof CategFeature) {
				List<String> keys = new ArrayList<String>(catvalues.getKeys());
				Collections.sort(keys);
				for(String key:keys) {
					buf.append(" #"+key+"="+catvalues.getCount(key));
				}
			} else if(f instanceof StringFeature) {
				buf.append(" #uniquevalues="+values.size());
			} else {
				buf.append(" #uniquevalues="+values.size());
			}
		}
		
		return buf.toString();
	}
}
