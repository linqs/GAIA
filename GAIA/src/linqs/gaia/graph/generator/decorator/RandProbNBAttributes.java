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
package linqs.gaia.graph.generator.decorator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.UnmodifiableList;

/**
 * Based on {@link NaiveBayesAttributes}, this attribute generation
 * method uses randomly selected word probabilities for each
 * possible label.  This is done in place of probsuccessprimary
 * and probsuccesssecondary used by {@link NaiveBayesAttributes}.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of graph items to set attributes for
 * <LI> targetfeatureid-Feature id of the label feature to add.  Default is "label".
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> maxprob-Maximum probability that a word can exist for any label.
 * This can be used to ensure that no word is too indicative.  Default is 1.
 * <LI> numattributes-Number of numeric attributes to generate per label. Default is 1.
 * <LI> attrprefix-Prefix to use in the feature name.  Default is "w".
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * @author namatag
 *
 */
public class RandProbNBAttributes extends BaseConfigurable implements Decorator {
	protected String schemaid;
	protected String targetfeatureid;
	private String attrprefix = "w";
	protected int numlabels;
	protected int numattributes = 1;
	private int seed = 0;
	
	public void decorate(Graph g) {
		// Set parameters
		this.schemaid = this.getStringParameter("schemaid");
		this.targetfeatureid = this.getStringParameter("targetfeatureid");
		
		if(this.hasParameter("attrprefix")) {
			this.attrprefix = this.getStringParameter("attrprefix");
		}
		
		if(this.hasParameter("numattributes")) {
			this.numattributes = (int) this.getDoubleParameter("numattributes");
		}
		
		double maxprob = this.getDoubleParameter("maxprob",1);
		
		if(this.hasParameter("seed")) {
			this.seed = (int) this.getDoubleParameter("seed");
		}
		Random rand = new Random(this.seed);
		
		// Get the label feature
		Schema schema = g.getSchema(schemaid);
		Feature f = schema.getFeature(targetfeatureid);
		if(!(f instanceof ExplicitCateg)) {
			throw new ConfigurationException("Unsupported feature type: "
					+f.getClass().getCanonicalName());
		}
		UnmodifiableList<String> cats = ((CategFeature) f).getAllCategories();
		numlabels = cats.size();
		
		// Update schema to support new attributes
		int totalwords = (int) (numlabels*numattributes);
		for(int i=0;i<totalwords;i++){
			// Add numeric features for the different words to add
			schema.addFeature(attrprefix+i, new ExplicitNum(new NumValue(0.0)));
		}
		g.updateSchema(schemaid, schema);
		
		// Generate random word probabilities
		List<double[]> probslist = new ArrayList<double[]>(numlabels);
		for(int i=0; i<numlabels; i++) {
			double[] probs = new double[numattributes];
			for(int j=0; j<numattributes; j++) {
				double prob = rand.nextDouble() * maxprob;
				probs[j]=prob;
			}
			
			probslist.add(probs);
		}
		
		// Go over all graph items, with the given schema, and add attributes
		NumValue val0 = new NumValue(0.0);
		NumValue val1 = new NumValue(1.0);
		Iterator<GraphItem> gitr = g.getGraphItems(schemaid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fvalue = gi.getFeatureValue(targetfeatureid);
			if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
				throw new ConfigurationException("All labels must be known: "+
						gi+"."+targetfeatureid+"="+fvalue);
			}
			
			// Select label
			int labelindex = cats.indexOf(((CategValue) fvalue).getCategory());
			double[] probs = probslist.get(labelindex);
			for(int j=0; j<numattributes; j++) {
				double prob = rand.nextDouble();
				if(prob<probs[j]) {
					gi.setFeatureValue(attrprefix+j, val1);
				} else {
					gi.setFeatureValue(attrprefix+j, val0);
				}
			}
		}
	}
}
