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
package linqs.gaia.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.UnmodifiableList;

public class FeatureUtils {
	/**
	 * Return the number of items which don't have a value specified
	 * in this feature
	 * 
	 * @param g Graph
	 * @param sid Schema ID of items and the the feature
	 * @param fid Feature to check value in
	 * @return Number of items which don't have that value specified
	 */
	public static int numNotSpecified(Graph g, String sid, String fid) {
		int nummissing = 0;
		Iterator<GraphItem> gitems = g.getGraphItems(sid);
		
		while(gitems.hasNext()){
			if(gitems.next().getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)){
				nummissing++;
			}
		}
		
		return nummissing;
	}
	
	/**
	 * Copy the explicit values from one item to the other.
	 * This method assumes that for each explicit feature for the original
	 * decorable item, there is a feature with the same id and type in the
	 * schema of the copy.
	 * 
	 * @param orig Decorable item to copy values from
	 * @param copy Decorable item to copy values to
	 */
	public static void copyFeatureValues(Decorable orig, Decorable copy) {
		// Copy the explicit feature values from the original
		Schema schema = orig.getSchema();
		Iterator<SimplePair<String, Feature>> fitr = schema.getAllFeatures();
		while(fitr.hasNext()) {
			SimplePair<String, Feature> fpair = fitr.next();
			String fid = fpair.getFirst();
			Feature f = fpair.getSecond();
			
			if(f instanceof ExplicitFeature) {
				copy.setFeatureValue(fid, orig.getFeatureValue(fid));
			}
		}
	}
	
	/**
	 * Copy the values of a given feature to an
	 * equivalent specified feature for the given
	 * set of items.
	 * 
	 * @param items Iterator over decorable items
	 * @param origfeatureid Original feature id
	 * @param copyfeatureid Feature id of copy feature
	 */
	public static void copyFeatureValues(Iterator<? extends Decorable> items,
			String origfeatureid, String copyfeatureid) {
		while(items.hasNext()) {
			Decorable d = items.next();
			
			// Handle case where a feature may not already be defined
			Schema schema = d.getSchema();
			if(!schema.hasFeature(copyfeatureid)) {
				// Create specified feature of the same type
				Feature origf = schema.getFeature(origfeatureid);
				ExplicitFeature copyf = null;
				
				if(origf instanceof NumFeature) {
					copyf = new ExplicitNum();
				} else if(origf instanceof CategFeature) {
					copyf = new ExplicitCateg(((CategFeature) origf).getAllCategories());
				} else if(origf instanceof StringFeature) {
					copyf = new ExplicitString();
				} else if(origf instanceof MultiCategFeature) {
					copyf = new ExplicitMultiCateg(((MultiCategFeature) origf).getAllCategories());
				} else {
					throw new UnsupportedTypeException("Unsupported feature type: "
							+ origf.getClass().getCanonicalName());
				}
				
				// Update schema
				schema.addFeature(copyfeatureid, copyf);
				String schemaid = d.getSchemaID();
				
				// Get the graph which is the SchemaManager
				Graph g = null;
				if(d instanceof Graph) {
					g = (Graph) d;
				} else if(d instanceof GraphItem) {
					g = ((GraphItem) d).getGraph();
				} else {
					throw new UnsupportedTypeException("Unsupported decorable type: "
							+ d.getClass().getCanonicalName());
				}
				
				g.updateSchema(schemaid, schema);
			}
			
			// Copy feature value
			d.setFeatureValue(copyfeatureid, d.getFeatureValue(origfeatureid));
		}
	}
	
	/**
	 * This function will get the feature value for the given feature
	 * over the list of decorable items provided.  This utility is meant
	 * as a way to test how long it takes to calculate feature values.
	 * It can also be used to debug values as it can print the value per item,
	 * as well as how long it took to get that value.
	 * 
	 * @param items Iterator over decorable items
	 * @param featureid ID of feature to get the value of
	 * @param print If true, print the value.  If false, get the value but don't display it.
	 */
	public static void getFeature(Iterator<? extends Decorable> items, String featureid, boolean print) {
		SimpleTimer timer = new SimpleTimer();
		while(items.hasNext()) {
			timer.start();
			Decorable d = items.next();
			FeatureValue value = d.getFeatureValue(featureid);
			
			// If requested, print value
			if(print) {
				Log.INFO(d+"="+value+" "+timer.timeLapse(true));
			}
		}
	}
	
	/**
	 * Get the features by type (derived, underived, or both)
	 * or by class.
	 * 
	 * @param schema Schema to get features from
	 * @param type Set to 0 for only non-derived features,
	 * 1 for only derived features, 2 for all features.
	 * @param fclass Feature class to match to.  Set to null to get all feature value types.
	 * 
	 * @return List of feature ids
	 */
	public static List<String> getFeatureIDs(Schema schema, int type, Class<?> fclass) {
		List<String> features = new LinkedList<String>();
		
		Iterator<SimplePair<String, Feature>> fids = schema.getAllFeatures();
		while(fids.hasNext()) {
			SimplePair<String, Feature> fpair = fids.next();
			Feature f = fpair.getSecond();
			
			// Only Return features of a given feature value type
			if(fclass!= null && !fclass.isInstance(f)) {
				continue;
			}
			
			// Only return features which are non-derived and/or derived
			if(type == 0) {
				if(!(f instanceof DerivedFeature)) {
					features.add(fpair.getFirst());
				}
			} else if(type == 1) {
				if(f instanceof DerivedFeature) {
					features.add(fpair.getFirst());
				}
			} else if(type == 2) {
				features.add(fpair.getFirst());
			} else {
				throw new InvalidStateException(
						"An invalid type was used in private function: "+type);
			}
		}
		
		return features;
	}
	
	/**
	 * Get the features by type (derived, underived, or both).
	 * 
	 * @param schema Schema to get features from
	 * @param type Set to 0 for only non-derived features, 1 for only derived features, 2 for all features.
	 * 
	 * @return List of feature ids
	 */
	public static List<String> getFeatureIDs(Schema schema, int type) {
		return FeatureUtils.getFeatureIDs(schema, type, null);
	}
	
	/**
	 * Process the "includefeatures" and "excludefeatures" parameters in the specified
	 * configurable item, along with the corresponding schema which contains the features
	 * and some default set of feature ids whose values will be used if "includefeatures"
	 * is not defined.  The parameters are used as follows:
	 * <p>
	 * <OL>
	 * <LI> First, this checks to see if "includefeatures"
	 * parameter is defined.  If so, the parameters is treated as a
	 * comma delimited list of feature ids, regex patterns, and/or feature class definitions
	 * which are run over the features of the schema.
	 * 
	 * <UL>
	 * <LI> Regex patterns are of the the form REGEX:&lt;pattern&gt; where
	 * pattern is a Java {@link Pattern}
	 * For example, given a comma delimited list "color,size,REGEX:w\\d,length"
	 * and schema with features "color,size,w1,w2,weight,length",
	 * the method will match "color,size,length,w1,w2".
	 * <LI> Feature class definitions are of the form FEATURECLASS:&lt;class&gt;
	 * where class is the string representation of a Java class.
	 * For example, given a comma delimited list
	 * "color,size,FEATURECLASS:{@link linqs.gaia.feature.NumFeature},length",
	 * schema with features "color,size,w1,w2"
	 * where size is of type {@link NumFeature} and the others are {@link CategFeature},
	 * the method will match "size".
	 * </UL>
	 * 
	 * <LI> Next, check to see if "excludefeatures" parameters are specified.
	 * If so, treat it as the include features was but instead of including
	 * the feature id and/or pattern, remove any matches from the list.
	 * The returned list is the list which matches the include, exclude,
	 * and default feature ids provided.
	 * </OL>
	 * <p>
	 * 
	 * @param conf Configurable object
	 * @param schema Schema of features to consider
	 * @param defaultfids Default feature ids if neither include or exclude are specified
	 * @return Feature ids
	 */
	public static List<String> parseFeatureList(Configurable conf,
			Schema schema, List<String> defaultfids) {
		return FeatureUtils.parseFeatureList(conf, schema, defaultfids,
				"includefeatures", "excludefeatures");
	}
	
	/**
	 * Given a list of possible values and a comma delimited list
	 * of "patterns", return the subset of values which match
	 * at least one of the patterns.  Patterns are either a direct match
	 * (i.e., the case sensitive match) or of the form
	 * REGEX:&lt;pattern&gt; where pattern is a Java {@link Pattern}
	 * For example, given a comma delimited patterns "color,size,REGEX:w\\d,length"
	 * and a set of values, "color,size,w1,w2,weight,length",
	 * the method will match "color,size,length,w1,w2".
	 * 
	 * @param pattern Comma delimited "patterns" to match
	 * @param values Values to consider
	 * @return Subset of values which match at least one of the patterns
	 */
	public static List<String> parseFeatureList(String pattern, List<String> values) {
		String regexprefix = "REGEX:";
		
		List<String> matches = new ArrayList<String>();
		
		// Process include features, if defined
		String[] patterns = pattern.split(",");
		for(String p:patterns) {
			if(p.trim().length()==0) {
				continue;
			}
			
			if(p.startsWith(regexprefix)) {
				// Process regex in the list
				String regex = p.substring(regexprefix.length());
				
				Iterator<String> fitr = values.iterator();
				while(fitr.hasNext()) {
					String currfid = fitr.next();
					if(Pattern.matches(regex, currfid) && !matches.contains(currfid)) {
						matches.add(currfid);
					}
				}
			} else {
				// Do exact match over values
				Iterator<String> fitr = values.iterator();
				while(fitr.hasNext()) {
					String currfid = fitr.next();
					if(currfid.equals(p) && !matches.contains(currfid)) {
						matches.add(currfid);
					}
				}
			}
		}
		
		return matches;
	}
	
	/**
	 * When appropriate, use the other implementation of parseFeatureList
	 * with the default includekey and excludekey values.  Otherwise,
	 * this implements parseFeatureList but allows you to change what the
	 * parameter keys are for the two parameter values.
	 * 
	 * @param conf Configurable object
	 * @param schema Schema of features to consider
	 * @param defaultfids Default feature ids if neither include or exclude are specified
	 * @param includekey Parameter key to get the include key feature ids
	 * @param excludekey Parameter key to get the exclude key feature ids
	 * @return Feature ids
	 */
	public static List<String> parseFeatureList(Configurable conf,
			Schema schema,List<String> defaultfids,
			String includekey, String excludekey) {
		String regexprefix = "REGEX:";
		String featureclassprefix = "FEATURECLASS:";
		
		// Get list to begin with
		List<String> featureids = new ArrayList<String>();
		if(conf.hasParameter(includekey)) {
			// Process include features, if defined
			String[] includefids = conf.getStringParameter(includekey).split(",");
			for(String fid:includefids) {
				if(fid.trim().length()==0) {
					continue;
				}
				
				if(fid.startsWith(regexprefix)) {
					// Process regex in the list
					String regex = fid.substring(regexprefix.length());
					
					Iterator<String> fitr = schema.getFeatureIDs();
					while(fitr.hasNext()) {
						String currfid = fitr.next();
						if(Pattern.matches(regex, currfid) && !featureids.contains(currfid)) {
							featureids.add(currfid);
						}
					}
				} else if(fid.startsWith(featureclassprefix)) {
					// Process regex in the list
					String className = fid.substring(featureclassprefix.length());
					Class<?> currclass = null;
					try {
						currclass = Class.forName(className);
					} catch (ClassNotFoundException e) {
						throw new ConfigurationException("Invalid class: "+className);
					}
					
					Iterator<String> fitr = schema.getFeatureIDs();
					while(fitr.hasNext()) {
						String currfid = fitr.next();
						if(currclass.isInstance(schema.getFeature(currfid))) {
							featureids.add(currfid);
						}
					}
				} else {
					// Treat as feature id.  Just verify its in the right schema.
					if(!schema.hasFeature(fid)) {
						throw new ConfigurationException("Included feature not defined: "+fid);
					} else {
						featureids.add(fid);
					}
				}
			}
		} else if(defaultfids!=null){
			// Use supplied default list
			for(String fid: defaultfids) {
				// Treat as feature id.  Just verify its in the right schema.
				if(!schema.hasFeature(fid)) {
					throw new ConfigurationException("Included feature not defined: "+fid);
				} else {
					featureids.add(fid);
				}
			}
		}
		
		// Exclude the specified features
		if(!featureids.isEmpty() && conf.hasParameter(excludekey)) {
			String[] excludefids = conf.getStringParameter(excludekey).split(",");
			for(String fid:excludefids) {
				if(fid.trim().length()==0) {
					continue;
				}
				
				if(fid.startsWith(regexprefix)) {
					// Process regex in the list
					String regex = fid.substring(regexprefix.length());
					
					// Remove anything in the feature ids list which
					// matches this pattern
					List<String> origlist = new ArrayList<String>(featureids);
					for(String currfid:origlist) {
						if(Pattern.matches(regex, currfid)) {
							featureids.remove(currfid);
						}
					}
				} else if(fid.startsWith(featureclassprefix)) {
					// Process regex in the list
					String className = fid.substring(featureclassprefix.length());
					Class<?> currclass = null;
					try {
						currclass = Class.forName(className);
					} catch (ClassNotFoundException e) {
						throw new ConfigurationException("Invalid class: "+className);
					}
					
					// Remove anything in the feature ids list which
					// matches this pattern
					List<String> origlist = new ArrayList<String>(featureids);
					for(String currfid:origlist) {
						if(currclass.isInstance(schema.getFeature(currfid))) {
							featureids.remove(currfid);
						}
					}
				} else {
					// Remove the feature id from list
					// If the specified feature id is not in the list,
					// log a warning message.
					if(!featureids.contains(fid)) {
						Log.WARN("Feature excluded by default: "+fid);
					}
					
					featureids.remove(fid);
				}
			}
		}
		
		LinkedHashSet<String> uniquefids = new LinkedHashSet<String>(featureids);
		if(uniquefids.size() != featureids.size()) {
			Log.WARN("Duplicate feature ids found in set of features: "+featureids);
			featureids = new ArrayList<String>(uniquefids);
		}
		
		Log.DEBUG("Feature IDs for cid="+conf.getCID()
				+" (using parameters "+includekey+" and "+excludekey+"): "
				+ListUtils.list2string(featureids,","));
		
		return featureids;
	}
	
	/**
	 * Return a default probability for a categorical value where the value specified
	 * has a probability of 1 and all others have a probability of 0.
	 * i.e., value="true" and the categories is ["false","true"], this will return
	 * a double array of [0,1].
	 * 
	 * @param value Category value to set to probability 1
	 * @param categories Possible categories
	 * @return Probability distribution
	 */
	public static double[] getCategValueProbs(String value, UnmodifiableList<String> categories) {
		int index = categories.indexOf(value);
		if(index == -1) {
			throw new InvalidStateException("Value is not a valid category: "
					+value+" instead of "+ListUtils.list2string(categories.copyAsList(), ","));
		}
		
		double[] probs = new double[categories.size()];
		probs[index] = 1;
		
		return probs;
	}
}
