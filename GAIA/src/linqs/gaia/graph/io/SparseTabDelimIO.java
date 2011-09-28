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
package linqs.gaia.graph.io;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.FileFormatException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.UnknownValue;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * A tab delimited format designed for dealing with sparse
 * data.  The format is the same as that of TabDelimIO
 * with the exception of how and what feature values are saved.
 * The feature value includes the name of the features and is of the form:
 * <p>
 * &lt;featurevalue&gt; := &lt;featurename&gt;=&lt;value&gt;[:P=<probability>]
 * <p>
 * Also, unlike TabDelimIO, SparseTabDelimIO does not save
 * the values of features for nodes, edges, or graphs
 * whose value is not known and for closed features whose
 * value is the same as the default value.
 * As a result, this format can save a significant amount of disk
 * space to store the graph, as well as speed up
 * loading and saving for graphs with sparse features.
 * 
 * Required Parameters:
 * <UL>
 * <LI> Same as {@link TabDelimIO}.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> Same as {@link TabDelimIO}.
 * </UL>
 * 
 * @see linqs.gaia.graph.io.TabDelimIO
 * 
 * @author namatag
 *
 */
public class SparseTabDelimIO extends TabDelimIO implements IO {
	public final static String FEATURENAME_DELIMITER = "=";
	
	/**
	 * Process the feature values for the given decorable item
	 */
	protected void addFeatureValues(Decorable di, String[] values, Set<String> loadfids){
		if(values==null) {
			return;
		}
		
		Schema schema = di.getSchema();
		
		List<String> addfids = new ArrayList<String>();
		List<FeatureValue> addvalues = new ArrayList<FeatureValue>();
		
		for(String rawvalue:values) {
			String fid = rawvalue.split(FEATURENAME_DELIMITER)[0];
			
			// Don't load feature value if not in list
			if(loadfids!=null && !loadfids.contains(fid)) {
				continue;
			}
			
			String value = rawvalue.substring(fid.length()+1);
			Feature f = schema.getFeature(fid);
			FeatureValue fvalue = parseFeatureValue(f, value);
			
			// Don't bother adding a value which matches the closed default
			if(((ExplicitFeature) f).isClosed()
					&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fvalue)) {
				continue;
			}
			
			// Do not add a value for something that's specified as unknown
			if(fvalue != null){
				// A closed feature cannot be defined as unknown
				if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)
						&& ((ExplicitFeature) f).isClosed()) {
					throw new FileFormatException(
							"Closed features cannot be set to unknown: "
							+di+"."+fid);
				}
				
				addfids.add(fid);
				addvalues.add(fvalue);
			}
		}
		
		// Insert all feature values all at once
		di.setFeatureValues(addfids, addvalues);
	}
	
	protected void writeValues(BufferedWriter out, List<String> fids,
			Decorable di, boolean savederived) throws Exception {
		Schema schema = di.getSchema();
		for(String fid:fids) {
			Feature f = schema.getFeature(fid);
			
			if(f instanceof CompositeFeature) {
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> cfpairs = cf.getFeatures();
				FeatureValue cffv = di.getFeatureValue(fid);
				
				// Get composite values, if known
				List<FeatureValue> cfvalues = null;
				if(cffv instanceof CompositeValue) {
					CompositeValue cvfv = (CompositeValue) cffv;
					cfvalues = cvfv.getFeatureValues().copyAsList();
					
					if(cfvalues.size() != cfpairs.size()) {
						throw new InvalidStateException("Number of composite features does not match "+
								"number of composite values returned: " +
								"#features="+cfvalues.size()+" #values="+cfvalues.size());
					}
				}
				
				// Write out the values of the individual features
				for(int i=0; i<cfpairs.size(); i++) {
					String cffid = cfpairs.get(i).getFirst();
					FeatureValue fv = null;
					if(cfvalues==null) {
						// Encountered unknown value for the composite feature
						fv = FeatureValue.UNKNOWN_VALUE;
					} else {
						// Get corresponding composite value
						fv = cfvalues.get(i);
					}
					
					if(fv instanceof UnknownValue
							|| (!savederived && f instanceof DerivedFeature)) {
						// Don't write unknown values, default values, or derived features
					} else if(f instanceof ExplicitFeature
							&& ((ExplicitFeature) f).isClosed()
							&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fv)) {
						// Don't write values to features match the closed default
					} else {
						out.write(DELIMITER);
						
						// Write the name prior to each value
						out.write(this.getNewCFFID(fid, cffid)+FEATURENAME_DELIMITER);
						// Save the value, making sure to replace
						// any tabs with a space.
						out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
						
						// Print probability, if applicable
						if(fv instanceof CategValue){
							out.write(PROB_DELIMITER+ArrayUtils.array2String(((CategValue) fv).getProbs(),","));
						} else if(fv instanceof MultiCategValue){
							out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
						}
					}
				}
			} else {
				FeatureValue fv = di.getFeatureValue(fid);
				if(fv instanceof UnknownValue
						|| (!savederived && f instanceof DerivedFeature)) {
					// Don't wring unknown values, default values, or derived features
				} else if(f instanceof ExplicitFeature
						&& ((ExplicitFeature) f).isClosed()
						&& ((ExplicitFeature) f).getClosedDefaultValue().equals(fv)) {
					// Don't write values to features match the closed default
				} else {
					out.write(DELIMITER);
					
					// Write the name prior to each value
					out.write(fid+FEATURENAME_DELIMITER);
					// Save the value, making sure to replace
					// any tabs with a space.
					out.write(fv.getStringValue().replaceAll("[\\t\\n\\r]+", " "));
					
					// Print probability, if applicable
					if(fv instanceof CategValue && ((CategValue) fv).getProbs()!=null){
						// Do not print probability if the probability is 1
						double[] probs = ((CategValue) fv).getProbs();
						boolean catprob1 = false;
						double sum = 0;
						for(double prob:probs) {
							if(prob==1) {
								catprob1 = true;
							}
							
							sum+=prob;
						}
						
						// We are currently not enforcing that the probability
						// has to add up to 1.  This ensures that if the probability
						// is being used in a different way, the probabilities are saved.
						if(!catprob1 || sum>1) {
							out.write(PROB_DELIMITER+ArrayUtils.array2String(probs,","));
						}
					} else if(fv instanceof MultiCategValue && ((MultiCategValue) fv).getProbs()!=null){
						out.write(PROB_DELIMITER+ArrayUtils.array2String(((MultiCategValue) fv).getProbs(),","));
					}
				}
			}
		}
	}
}
