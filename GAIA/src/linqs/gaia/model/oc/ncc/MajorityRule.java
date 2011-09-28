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
package linqs.gaia.model.oc.ncc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.UnmodifiableList;

/**
 * This simple classifier predicts a feature based on
 * the labels of neighbors.  It predicts the label
 * that is the majority label among the neighbors.
 * If there are no labeled neighbors, no value is predicted.
 * If there is a tie, the label with the highest overall
 * number in the training set is use.
 * If a tie still exists, one of the labels is randomly chosen.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> incidentsid-Schema ID of incident schemaids.  If not defined, all adjacent edges, through
 * all incident items, are used
 * <LI> usetrainmajority-If set to yes, whenever a majority cannot be taken
 * (i.e., no neighbors are labeled), use the majority label from the training set
 * <LI> skiplabeled-If set to yes, if a node was previously set with a label,
 * do not update it with the majority value.  This allows for iteratively
 * applying majority rule on nodes.
 * </UL>
 * 
 * @author namatag
 *
 */
public class MajorityRule extends BaseVBClassifier implements VBClassifier {
	private static final long serialVersionUID = 1L;
	
	private KeyedCount<String> trainclasscount = new KeyedCount<String>();
	private UnmodifiableList<String> targetcategories;
	private String incidentsid = null;
	
	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid,
			String targetfeatureid, List<String> featureids) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		this.featureids = featureids;
		
		if(this.hasParameter("incidentsid")) {
			this.incidentsid = this.getStringParameter("incidentsid");
		}
		
		for(Decorable d:trainitems) {
			if(targetcategories == null) {
				CategFeature cf = (CategFeature) d.getSchema().getFeature(this.targetfeatureid);
				this.targetcategories = cf.getAllCategories();
			}
			
			FeatureValue fv = d.getFeatureValue(this.targetfeatureid);
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			} else if(!(fv instanceof CategValue)) {
				throw new UnsupportedTypeException("Expected categorical value.  Encountered: "
						+fv.getClass().getCanonicalName());
			} else {
				CategValue cv = (CategValue) fv;
				trainclasscount.increment(cv.getCategory());
			}
		}
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		if(!(testitem instanceof GraphItem)) {
			throw new UnsupportedTypeException("Classifier only defined for Graph Items.  Encountered: "
					+testitem.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) testitem;
		
		if(this.hasYesNoParameter("skiplabeled","yes")) {
			if(gi.hasFeatureValue(this.targetfeatureid)) {
				return gi.getFeatureValue(this.targetfeatureid);
			}
		}
		
		// Get unique set of neighbors of distance 1 away.
		// Namely, for the given graph item, get all graph items connected to the
		// graph items connected to this graph item.
		// This does not include the graph item itself.
		Iterator<GraphItem> connected = null;
		if(this.incidentsid!=null) {
			connected = gi.getIncidentGraphItems(this.incidentsid);
		} else {
			connected = gi.getIncidentGraphItems();
		}
		
		Set<GraphItem> d1neighbors = new HashSet<GraphItem>();
		while(connected.hasNext()) {
			GraphItem cgi = connected.next();
			Iterator<GraphItem> d1itr = cgi.getIncidentGraphItems();
			while(d1itr.hasNext()) {
				d1neighbors.add(d1itr.next());
			}
		}
		
		// Neighbors do not include the item itself
		d1neighbors.remove(gi);
		
		// Count labels of neighbors
		KeyedCount<String> neighborcount = new KeyedCount<String>();
		for(GraphItem d1gi: d1neighbors) {
			FeatureValue fv = d1gi.getFeatureValue(this.targetfeatureid);
			
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			} else {
				CategValue cfv = (CategValue) fv;
				neighborcount.increment(cfv.getCategory());
			}
		}
		
		// Get majority label
		String maxcat = null;
		List<String> maxkeys = neighborcount.highestCountKeys();
		if(maxkeys==null || maxkeys.isEmpty()) {
			if(this.hasParameter("usetrainmajority", "yes")) {
				// Return the majority class in the training set
				maxcat = this.trainclasscount.highestCountKey();
				
				// Return confidence using the training class distribution
				neighborcount = this.trainclasscount;
				
				if(maxcat==null) {
					throw new InvalidStateException("No labeled instances found in training data." +
							"  Unable to compute a most common label.");
				}
			} else {
				// If there is no max defined, return unknown
				return FeatureValue.UNKNOWN_VALUE;
			}
		} else if(maxkeys.size() == 1) {
			// Get majority label
			maxcat = maxkeys.get(0);
		} else {
			// Break tie
			KeyedCount<String> tiecounts = new KeyedCount<String>();
			for(String key:maxkeys) {
				tiecounts.setCount(key, this.trainclasscount.getCount(key));
			}
			
			// If there are still ties at this point, just return one randomly
			List<String> tiemaxkeys = tiecounts.highestCountKeys();
			maxcat = tiemaxkeys.get(0);
		}
		
		// Use the class distributions as the probabilities
		double[] probs = new double[this.targetcategories.size()];
		int i = 0;
		for(String cat:this.targetcategories) {
			probs[i] = neighborcount.getPercent(cat);
			i++;
		}
		
		return new CategValue(maxcat, probs);
	}

	@Override
	public void loadVBOC(String directory) {
		// Handled mainly by BaseVBClassifier
		this.targetcategories =
			new UnmodifiableList<String>(Arrays.asList(this.getStringParameter("saved-targetcategories").split(",")));
	}
	
	@Override
	public void saveVBOC(String directory) {
		// Handled mainly by BaseVBClassifier
		this.setParameter("saved-targetcategories",
				IteratorUtils.iterator2string(this.targetcategories.iterator(), ","));
	}
}
