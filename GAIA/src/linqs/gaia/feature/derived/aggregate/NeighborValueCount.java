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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedComposite;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.derived.composite.CVNum;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.KeyedSum;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * This is a multi-valued derived feature which, for every
 * possible category of a given categorical feature,
 * return the number of neighbors for that value.
 * This feature only applies to graph items.
 * Neighborhood is defined by the specified Neighbor class.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureid-ID of feature to aggregate over
 * <LI> featureschemaid-Schema ID of schema where the feature to aggregate over is
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-Class of {@link Neighbor} implementation,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use when calculating
 * neighborhood.  Default is {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI> weightbyprob-If "yes", instead of returning the number of neighbors
 * with a given label, use the sum of the probability each neighbor has that label.
 * Default is "no".
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class NeighborValueCount extends DerivedComposite implements GraphDependent {
	
	protected UnmodifiableList<SimplePair<String, CVFeature>> features = null;
	protected String featureid;
	protected String featureschemaid;
	protected Graph g;
	private String neighborclass = Adjacent.class.getCanonicalName();
	private Neighbor neighbor = null;
	protected boolean weightbyprob = false;
	
	private static NumValue numvalue0 = new NumValue(0.0);
	
	public FeatureValue calcFeatureValue(Decorable di) {	
		// Get counts of features from neighbors
		KeyedSum<String> count = this.getCount(di, this.weightbyprob);
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(SimplePair<String, CVFeature> pair:features) {
			double value = count.getSum(pair.getFirst());
			if(value==0) {
				fvalues.add(numvalue0);
			} else {
				fvalues.add(new NumValue(value));
			}
		}
		
		return (FeatureValue) new CompositeValue(fvalues);
	}
	
	protected KeyedSum<String> getCount(Decorable di, boolean weight) {
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		KeyedSum<String> count = new KeyedSum<String>();
		Iterable<GraphItem> neighbors = this.neighbor.getNeighbors(gi);
		for(GraphItem ngi : neighbors) {
			
			// Skip neighbors not of the specified feature schema id
			if(!ngi.getSchemaID().equals(this.featureschemaid)) {
				continue;
			}
			
			FeatureValue fvalue = ngi.getFeatureValue(featureid);
			
			if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
				// Don't count neighbors with missing values
				continue;
			} else {
				CategValue cvalue = (CategValue) fvalue;
				if(weightbyprob) {
					int i = 0;
					double[] probs = cvalue.getProbs();
					for(SimplePair<String, CVFeature> pair:features) {
						count.add(pair.getFirst(), probs[i]);
						i++;
					}
				} else if(this.hasYesNoParameter("weightbyincident", "yes")) { 
					String weightincidentsid = this.getStringParameter("weightincidentsid");
					String weightincidentfid = this.getStringParameter("weightincidentfid");
					
					Iterator<GraphItem> gitr = gi.getIncidentGraphItems(weightincidentsid, ngi);
					while(gitr.hasNext()) {
						GraphItem currgi = gitr.next();
						double currweight = ((NumValue) currgi.getFeatureValue(weightincidentfid)).getNumber();
						count.add(cvalue.getCategory(), currweight);
					}
				} else {
					count.add(cvalue.getCategory(), 1.0);
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Set neighbor class to use
	 * 
	 * @param neighbor Neighbor class to use
	 */
	public void setNeighbor(Neighbor neighbor) {
		this.initializeFeature();
		
		this.neighbor = neighbor;
	}
	
	public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures() {
		this.initializeFeature();
		
		return features;
	}
	
	protected void initialize() {
		
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}

		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
		this.weightbyprob = this.hasYesNoParameter("weightbyprob", "yes");
		
		featureid = this.getStringParameter("featureid");
		featureschemaid = this.getStringParameter("featureschemaid");
		
		if(this.g == null) {
			throw new InvalidStateException("Graph item not set");
		}
		
		Schema schema = g.getSchema(featureschemaid);
		Feature f = schema.getFeature(featureid);
		
		if(!(f instanceof CategFeature)) {
			throw new ConfigurationException("Feature expected to be categorical: "
					+f.getClass().getCanonicalName());
		}
		
		UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
		List<SimplePair<String, CVFeature>> catpairs = new LinkedList<SimplePair<String, CVFeature>>();
		for(String cat:cats) {
			catpairs.add(new SimplePair<String,CVFeature>(cat, new CVNum()));
		}
		
		this.features = new UnmodifiableList<SimplePair<String, CVFeature>>(catpairs);
	}

	public void setGraph(Graph g) {
		this.g = g;
	}

	public int numFeatures() {
		return this.getFeatures().size();
	}
	
	public void cacheNeighbor(boolean iscaching) {
		this.initializeFeature();
		
		this.neighbor.setCache(iscaching);
	}
	
	public void clearNeighborCache() {
		this.neighbor.resetAll();
	}
}
