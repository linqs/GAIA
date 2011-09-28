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
package linqs.gaia.similarity.list;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.similarity.ListDistance;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.similarity.NormalizedStringSimilarity;
import linqs.gaia.similarity.string.CharacterMatch;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.MinMax;

/**
 * Compute the string similarity of the string of objects.
 * The string representation is used for each object, except
 * in the case of StringValue objects where the actual string
 * in the feature value is used.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> stringsimclass-{@link NormalizedStringSimilarity} class to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.similarity.string.CharacterMatch}.
 * <LI> delimiter-If set, assume the values of each object represent
 * a delimited list of string values.  In that case, compute
 * all pairwise values in the first object to those in the second object.
 * The values are then aggregated using the different types below.
 * <LI> aggtype-Parameter can be set to min, max or mean when a delimiter is specified.
 * When a delimiter is defined, either the min, max, or mean
 * values for all pairwise comparisons between the the delimited
 * list of string will be used.  Default is max.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class StringListSimilarity extends BaseConfigurable
	implements NormalizedListSimilarity, ListDistance {
	
	private static final long serialVersionUID = 1L;
	
	private NormalizedStringSimilarity ssim = null;
	private String delimiter = null;
	private String aggtype = "max";
	
	private void initialize() {
		String ssimclass = CharacterMatch.class.getCanonicalName();
		if(this.hasParameter("stringsimclass")) {
			ssimclass = this.getStringParameter("stringsimclass");
		}
		
		if(this.hasParameter("delimiter")) {
			this.delimiter = this.getStringParameter("delimiter");
		}
		
		if(this.hasParameter("aggtype")) {
			this.aggtype = this.getCaseParameter("aggtype", new String[]{"min","max","mean"});
		}
		
		ssim = (NormalizedStringSimilarity) Dynamic.forConfigurableName(NormalizedStringSimilarity.class,
				ssimclass, this);
	}
	
	public double getSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		if(ssim==null) {
			this.initialize();
		}
		
		return this.getNormalizedSimilarity(item1, item2);
	}

	public double getNormalizedSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		if(ssim==null) {
			this.initialize();
		}
		
		double similarity = 0;
		double size = item1.size();
		if(item1.size()!=item2.size()){
			throw new InvalidOperationException("Incomparable feature lists of varying sizes: item1="
					+item1.size()+ " item2="+item2.size());
		}
		
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			if(i1val == null || i2val == null) {
				throw new InvalidOperationException("Values cannot be null: "
						+"item1="+i1val+ " item2="+i2val);
			}
			
			String sval1 = null;
			String sval2 = null;
			
			// Object must be some sort of string feature
			if(i1val instanceof StringValue) {
				sval1 = ((StringValue) i1val).getStringValue();
			} else if(i1val.equals(FeatureValue.UNKNOWN_VALUE)) {
				sval1 = "";
			} else {
				sval1 = i1val.toString();
			}
			
			// Object must be some sort of string feature
			if(i2val instanceof StringValue) {
				sval2 = ((StringValue) i2val).getStringValue();
			} else if(i2val.equals(FeatureValue.UNKNOWN_VALUE)) {
				sval2 = "";
			} else {
				sval2 = i2val.toString();
			}
			
			Double currsim = null;
			if(delimiter==null) {
				// Get the similarity of the objects
				currsim = ssim.getNormalizedSimilarity(sval1, sval2);
			} else {
				// Get the similarity of the delimited
				// strings of the object
				MinMax mm = new MinMax();
				String[] svparts1 = sval1.split(delimiter);
				String[] svparts2 = sval2.split(delimiter);
				
				// Make sure there's at least one entry in each
				if(svparts1 == null || svparts1.length == 0) {
					svparts1 = new String[]{""};
				}
				
				if(svparts2 == null || svparts2.length == 0) {
					svparts2 = new String[]{""};
				}
				
				for(String svp1:svparts1) {
					for(String svp2:svparts2) {
						mm.addValue(ssim.getNormalizedSimilarity(svp1, svp2));
					}
				}
				
				// Aggregate the similarities somehow
				if(this.aggtype.equals("max")) {
					currsim = mm.getMax();
				} else if(this.aggtype.equals("min")) {
					currsim = mm.getMin();
				} else if(this.aggtype.equals("mean")) {
					currsim = mm.getMean();
				} else {
					throw new ConfigurationException("Invalid aggtype: "+this.aggtype);
				}
			}
			
			if(currsim<0 || currsim>1) {
				throw new InvalidStateException("Normalized value expected (between 0 and 1, inclusive): "
						+currsim);
			}
			
			similarity +=  currsim;
		}
		
		if(size==0) {
			return 0;
		} else {
			return similarity/size;
		}
	}

	public double getDistance(List<? extends Object> item1,
			List<? extends Object> item2) {
		return 1.0-this.getNormalizedSimilarity(item1, item2);
	}
}
