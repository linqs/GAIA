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
package linqs.gaia.graph.noise.attribute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Randomly changes the value of a graph items to another.
 * This noise generator has been implemented for both explicit
 * categorical features, as well as numeric features which are binary (i.e., 0.0 or 1.0 values).
 * This implementation is different from {@link linqs.gaia.graph.noise.attribute.RandomFlipAttributes}
 * in that it allows switching, with the given property, the same number
 * features without the sparse value as those flipped with the sparse value.
 * (e.g., we can specify that for every word which we flip from observed to not-observed
 * in the document, we can flip a previously unobserved word as observed.)
 * 
 * Required Parameters:
 * <UL>
 * <LI>schemaid-Schema id of schema and graph items whose categorical values will be flipped.
 * <LI>sparsevalue-If specified, it means that the attribute value is sparse
 * and that a majority of the attributes belong to some value (i.e., 0 meaning a word is not in a document).
 * We may not want to flip those since it will alter this skew.
 * Instead, we may only want to flip when
 * we encounter one of the sparse values (i.e., 1 meaning a word is in a document).
 * This parameter defines the string representation of the sparse value for
 * which we want to do this (i.e., 1).
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex pattern
 * for feature IDs in the form REGEX:&lt;pattern&gt;
 * (e.g., color,size,REGEX:\\d,length).
 * <LI>excludefeatures-Same format as include features
 * but any matching feature id and/or regex pattern
 * is removed.
 * <LI>changevalue-If yes, always set the attribute to a new value
 * (i.e., do not allow it to remain the same value).
 * <LI>probflip-Probability of flipping an attribute.  Default is .25.
 * <LI>probflipcommon-Probability of flipping an attribute with a non sparse value
 * for each sparse value flipped.  Default is 0.
 * <LI>seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomFlipSparseAttributes extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid;
	private String sparsevalue;
	private double probflip;
	private double probflipcommon;
	private boolean changevalue;
	private Random rand;
	
	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		Schema schema = g.getSchema(schemaid);
		List<String> fids = FeatureUtils.parseFeatureList(this,
				schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		
		// Iterate over graph all graph items with the given schema
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			this.addNoise(gi, fids);
		}
	}
	
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		Schema schema = d.getSchema();
		List<String> fids = FeatureUtils.parseFeatureList(this,
					schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		
		this.addNoise(d, fids);
	}
	
	private void addNoise(Decorable d, List<String> fids) {
		Schema schema = d.getSchema();
		
		// Store the feature ids of the non-sparse valued feature
		// since we may want to flip a number of them corresponding
		// to the number of sparse values flipped
		List<String> nonsparse = new ArrayList<String>();
		int numflipped = 0;
		
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			if(f instanceof ExplicitCateg) {
				// Handle explicit categorical features
				UnmodifiableList<String> cats = ((ExplicitCateg) f).getAllCategories();
				int catsize = cats.size();
				
				String oldvalue = d.getFeatureValue(fid).getStringValue();
				if(!oldvalue.equals(sparsevalue)) {
					// Only flip the non-sparse values
					// Done so for sparse sets, the common value doesn't become
					// too common.
					nonsparse.add(fid);
					continue;
				}
				
				// Only flip given some probability
				if(rand.nextDouble() > probflip) {
					continue;
				}
				
				// Randomly select a category
				int index = rand.nextInt(catsize);
				if(changevalue && catsize > 1) {
					while(oldvalue.equals(cats.get(index))) {
						index = rand.nextInt(catsize);
					}
				}
				
				d.setFeatureValue(fid, new CategValue(cats.get(index)));
				numflipped++;
			} else if(f instanceof ExplicitNum) {
				double oldvalue = ((NumValue) d.getFeatureValue(fid)).getNumber();
				if(oldvalue != 0.0 && oldvalue != 1.0) {
					throw new UnsupportedTypeException("Numeric values must be either 0 or 1: "
							+d+"."+fid+"="+oldvalue);
				}
				
				double sparsevaluedouble = Double.parseDouble(sparsevalue);
				if(oldvalue!=sparsevaluedouble) {
					// Only flip the non-sparse values
					// Done so for sparse sets, the common value doesn't become
					// too common.
					nonsparse.add(fid);
					continue;
				}
				
				// Only flip given some probability
				if(rand.nextDouble() > probflip) {
					continue;
				}
				
				double value = 0;
				if(changevalue) {
					value = (oldvalue + 1) % 2;
				} else {
					// Randomly select a category
					value = rand.nextInt(2);
				}
				
				d.setFeatureValue(fid, new NumValue(value));
				numflipped++;
			} else {
				throw new UnsupportedTypeException("Unsupported Type: "+f.getClass().getCanonicalName());
			}
		}
		
		// If nonsparse is not empty, that means
		// sparse values were encountered and we may
		// want to flip an equal number of non-sparse values.
		if(!nonsparse.isEmpty() && probflipcommon > 0.0) {
			for(int i=0; i<numflipped; i++) {
				// With the given probability, flip
				// a common value for each of the sparse value flipped.
				if(rand.nextDouble() < probflipcommon) {
					String fid = nonsparse.get(rand.nextInt(nonsparse.size()));
					Feature f = schema.getFeature(fid);
					
					if(f instanceof ExplicitCateg) {
						// Handle explicit categorical features
						UnmodifiableList<String> cats = ((ExplicitCateg) f).getAllCategories();
						int catsize = cats.size();
						
						String oldvalue = d.getFeatureValue(fid).getStringValue();
						
						// Randomly select a category
						int index = rand.nextInt(catsize);
						if(changevalue && catsize > 1) {
							while(oldvalue.equals(cats.get(index))) {
								index = rand.nextInt(catsize);
							}
						}
						
						d.setFeatureValue(fid, new CategValue(cats.get(index)));
					} else if(f instanceof ExplicitNum) {
						double oldvalue = ((NumValue) d.getFeatureValue(fid)).getNumber();
						if(oldvalue != 0.0 && oldvalue != 1.0) {
							throw new UnsupportedTypeException("Numeric values must be either 0 or 1: "
									+d+"."+fid+"="+oldvalue);
						}
						
						double value = 0;
						if(changevalue) {
							value = (oldvalue + 1) % 2;
						} else {
							// Randomly select a category
							value = rand.nextInt(2);
						}
						
						d.setFeatureValue(fid, new NumValue(value));
					} else {
						throw new UnsupportedTypeException("Unsupported Type: "+f.getClass().getCanonicalName());
					}
					
					// Remove feature id from nonsparse
					// so we don't keep flipping the same attribute
					nonsparse.remove(fid);
					if(nonsparse.isEmpty()) {
						break;
					}
				}
			}
		}
	}
	
	private void initialize() {
		initialize = false;
		
		// Get parameters
		schemaid = this.getStringParameter("schemaid");
		sparsevalue = this.getStringParameter("sparsevalue");
		
		probflip = .25;
		if(this.hasParameter("probflip")) {
			probflip = this.getDoubleParameter("probflip");
		}
		
		probflipcommon = 0;
		if(this.hasParameter("probflipcommon")) {
			probflipcommon = this.getDoubleParameter("probflipcommon");
		}
		
		changevalue = false;
		if(this.hasParameter("changevalue")) {
			changevalue = this.hasYesNoParameter("changevalue", "yes");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		
		rand = new Random(seed);
	}
}
