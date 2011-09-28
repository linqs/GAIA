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
package linqs.gaia.feature.derived.aggregate;

import java.util.LinkedList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedComposite;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.derived.composite.CVNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * This is a multi-valued feature which returns
 * the absolute difference in values between
 * the numeric valued features of a node.
 * 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureschemaid-Feature schema ID of the connected graph items to consider.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Class of {@link Neighbor} implementation,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use when calculating
 * neighborhood.  Default is {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
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
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class NeighborValueDifference extends DerivedComposite implements 
	GraphDependent {
	private UnmodifiableList<SimplePair<String, CVFeature>> features = null;
	private List<String> featureids;
	private String featureschemaid;
	private Graph g;
	
	private String neighborclass = Adjacent.class.getCanonicalName();
	private Neighbor neighbor = null;
	
	private static NumValue numvalue0 = new NumValue(0.0);
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		if(features==null) {
			this.initialize();
		}
		
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		
		// Get difference of numeric features
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(SimplePair<String, CVFeature> pair:features) {
			double diff = Double.POSITIVE_INFINITY;
			String fid = pair.getFirst();
			
			// Iterate all items connected to this item
			Iterable<GraphItem> currneighbors = this.neighbor.getNeighbors(gi);
			for(GraphItem conn : currneighbors) {
				if(!conn.getSchemaID().equals(featureschemaid)) {
					throw new InvalidStateException("Encountered unexpected schema: "
							+" Encountered "+conn.getSchemaID()
							+" but expected "+featureschemaid);
				}
				
				if(diff == Double.POSITIVE_INFINITY) {
					// Get the first value
					if(conn.hasFeatureValue(pair.getFirst())) {
						diff = ((NumValue) conn.getFeatureValue(fid)).getNumber();
					}
				} else {
					// Subtract next value
					if(conn.hasFeatureValue(pair.getFirst())) {
						diff -= ((NumValue) conn.getFeatureValue(fid)).getNumber();
					}
				}
			}
			
			// Return appropriate features
			diff = Math.abs(diff);
			
			if(diff==0) {
				fvalues.add(numvalue0);
			} else {
				fvalues.add(new NumValue(diff));
			}
		}
		
		return (FeatureValue) new CompositeValue(fvalues);
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		if(features==null) {
			this.initialize();
		}
		
		this.neighbor = neighbor;
	}
	
	/**
	 * Initialize information required by the feature
	 */
	protected void initialize() {
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}
		
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
		featureschemaid = this.getStringParameter("featureschemaid");
		// Use specified features or, if not specified, use all features
		// for the schema of the specified feature schema id.
		Schema schema = g.getSchema(featureschemaid);
		this.featureids = FeatureUtils.parseFeatureList(this, schema,
				FeatureUtils.getFeatureIDs(schema, 2, NumFeature.class));
		
		// Verify all features are numeric
		for(String fid:featureids) {
			if(!(schema.getFeature(fid) instanceof NumFeature)) {
				throw new ConfigurationException("Only numeric features supported: "+
						fid+" of type "+ schema.getFeature(fid).getClass().getCanonicalName());
			}
		}
		
		// Return numerically valued items
		List<SimplePair<String, CVFeature>> fpairs = new LinkedList<SimplePair<String, CVFeature>>();
		for(String fid:featureids) {
			fpairs.add(new SimplePair<String,CVFeature>(fid, new CVNum()));
		}
		
		this.features = new UnmodifiableList<SimplePair<String, CVFeature>>(fpairs);
	}
	
	public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures() {
		if(features==null) {
			this.initialize();
		}
		
		return features;
	}

	public void setGraph(Graph g) {
		this.g = g;
	}
	
	public int numFeatures() {
		return this.getFeatures().size();
	}
}
