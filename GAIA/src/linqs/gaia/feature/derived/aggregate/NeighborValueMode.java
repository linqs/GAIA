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

import java.util.List;
import java.util.Random;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.DerivedCateg;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.UnmodifiableList;

/**
 * This is a Categorical Valued derived feature which
 * will return the most common feature value (for the specified feature)
 * among a graph items neighbors.
 * 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureid-ID of feature to aggregate over
 * <LI> featureschemaid-Schema ID of schema where the feature to aggregate over is
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neigborclass-Class of {@link Neighbor} implementation,
 * instantiated using in {@link Dynamic#forConfigurableName}, to use when calculating
 * neighborhood.  Default is {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
 * <LI>defaultmode-If set, whenever a mode cannot be computed (i.e., no neighbors,
 * no majority category among the neighbors), this category is returned.
 * Note: An exception is thrown if this category is not a valid value of the
 * specified feature.  Overrides defaultrandom.
 * <LI>defaultrandom-If "yes", whenever a mode cannot be computed (i.e., no neighbors,
 * no majority category among the neighbors), a random category is chosen
 * from among those observed from the neighbors or, if there are no neighbors or
 * neighbors with known values, from the set of all categories.  Default is "no".
 *      <UL>
 *      <LI>seed-Random generator seed for default random.  Default is 0.
 *      </UL>
 * <LI>usenomodecat-If set, add a special category with the given value.  This
 * category will be returned whenever a mode cannot be calculated
 * (i.e., no neighbors, there is no majority category among the neighbors,
 * or the feature value is unknown for all the neighbors).
 * Overrides defaultmode and defaultrandom.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class NeighborValueMode extends DerivedCateg implements 
	GraphDependent {
	
	protected UnmodifiableList<String> cats = null;
	protected String featureid;
	protected String featureschemaid;
	protected Graph g;
	private String neighborclass = Adjacent.class.getCanonicalName();
	private Neighbor neighbor = null;
	private Random rand = null;
	private String nomodecat = null;

	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		this.initializeFeature();
		
		KeyedCount<String> count = this.getCount(di);
		List<String> highest = count.highestCountKeys();
		
		String value = null;
		if(highest != null && highest.size() == 1) {
			value = highest.get(0);
		} else {
			if(this.nomodecat!=null) {
				value = this.nomodecat;
			} else if(this.hasParameter("defaultmode")) {
				value = this.getStringParameter("defaultmode");
			} else if(this.hasYesNoParameter("defaultrandom", "yes")) {
				if(highest != null) {
					value = highest.get(rand.nextInt(highest.size()));
				} else {
					value = this.cats.get(rand.nextInt(cats.size()));
				}
			}
		}
		
		if(value == null) {
			return FeatureValue.UNKNOWN_VALUE;
		} else {
			return new CategValue(value, FeatureUtils.getCategValueProbs(value, cats));
		}
	}

	public UnmodifiableList<String> getAllCategories() {
		this.initializeFeature();
		
		return cats;
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
	
	/**
	 * Initialize all parameters
	 */
	protected void initialize() {
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
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
		
		List<String> currcats = ((CategFeature) f).getAllCategories().copyAsList();
		if(this.hasParameter("usenomodecat")) {
			this.nomodecat = this.getStringParameter("usenomodecat");
			if(currcats.contains(this.nomodecat)) {
				throw new ConfigurationException(
					"No mode category is already a valid category value: "+this.nomodecat);
			}
			
			currcats.add(this.nomodecat);
		}
		
		this.cats = new UnmodifiableList<String>(currcats);
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		this.rand = new Random(seed);
	}
	
	protected KeyedCount<String> getCount(Decorable di) {
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		KeyedCount<String> count = new KeyedCount<String>();
		Iterable<GraphItem> neighbors = neighbor.getNeighbors(gi);
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
				count.increment(cvalue.getCategory());
			}
		}
		
		return count;
	}
	
	public void setGraph(Graph g) {
		this.g = g;
	}
	
	public int numCategories() {
		return this.getAllCategories().size();
	}
}
