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
package linqs.gaia.prediction.feature;

import linqs.gaia.prediction.Probability;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.prediction.Weighted;
import linqs.gaia.util.ArrayUtils;

/**
 * Category valued feature value prediction.
 * If weight is not defined, the weight is assumed to be 1.
 * 
 * @author namatag
 *
 */
public class CategValuePred implements SingleValue, Probability, Weighted {
	private String predvalue;
	private String truevalue;
	private double[] probs;
	private double weight = 1;
	private String id;
	
	/**
	 * Constructor
	 * 
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 */
	public CategValuePred(String truevalue, String predvalue) {
		this(truevalue, predvalue, null, 1);
	}
	
	/**
	 * Constructor
	 * 
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 * @param probs Probability distribution over possible values.  Set to null if unknown.
	 */
	public CategValuePred(String truevalue, String predvalue, double[] probs) {
		this(truevalue, predvalue, probs, 1);
	}
	
	/**
	 * Constructor
	 * 
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 * @param probs Probability distribution over possible values.  Set to null if unknown.
	 * @param weight Weight of prediction
	 */
	public CategValuePred(String truevalue, String predvalue, double[] probs, double weight) {
		this.predvalue = predvalue.intern();
		this.truevalue = truevalue.intern();
		this.probs = probs;
		this.weight = weight;
	}
	
	/**
	 * Constructor
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 */
	public CategValuePred(String id, String truevalue, String predvalue) {
		this(truevalue, predvalue, null, 1);
	}
	
	/**
	 * Constructor
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 * @param probs Probability distribution over possible values.  Set to null if unknown.
	 */
	public CategValuePred(String id, String truevalue, String predvalue, double[] probs) {
		this(truevalue, predvalue, probs, 1);
	}
	
	/**
	 * Constructor
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value.
	 * @param predvalue Predicted value.  Set to null if unknown.
	 * @param probs Probability distribution over possible values.  Set to null if unknown.
	 * @param weight Weight of prediction
	 */
	public CategValuePred(String id, String truevalue, String predvalue, double[] probs, double weight) {
		this.predvalue = predvalue.intern();
		this.truevalue = truevalue.intern();
		this.probs = probs;
		this.weight = weight;
	}
	
	public double[] getProbs() {
		return probs;
	}
	
	public String getPredValue() {
		return predvalue;
	}

	public String getTrueValue() {
		return truevalue;
	}

	public double getWeight() {
		return this.weight;
	}
	
	public String getID() {
		return this.id;
	}
	
	/**
	 * Return the string representation of this {@link CategValuePred} object
	 * in the format:<br>
	 * &lt;id&gt;\t&lt;truevalue&gt;\t&lt;predvalue&gt;\t&ltprobability&gt;\t&lt;weight&gt;<br>
	 * where
	 * <UL>
	 * <LI> The values are tab delimited.
	 * <LI> &lt;id&gt; is the identifier of the object show attribute was predicted
	 * (e.g., SocialNetwork.Facebook.Person.BobSmith) or NULL if not specified.
	 * <LI> &lt;truevalue&gt; is the true category value (e.g., male)
	 * <LI> &lt;predvalue&gt; is the predicted category value (e.g., female) or NULL if not specified.
	 * <LI> &lt;probability&gt; is an comma delimited values representing the probability distribution
	 * (e.g., .9,.1 following the order of categories in the corresponding {@link CategValuePredGroup})
	 *  or NULL if not specified.
	 * <LI> &lt;weight&gt; is a number representing the weight of the prediction (e.g., 1)
	 * </UL>
	 */
	public String toString() {
		String id = this.id==null ? "NULL" : this.id.toString();
		String probs = this.probs==null ? "NULL" : ArrayUtils.array2String(this.probs,",");
		String predvalue = this.predvalue==null ? "NULL" : this.predvalue;
		
		return id+"\t"+truevalue+"\t"+predvalue+"\t"+probs+"\t"+weight;
	}
	
	/**
	 * Parse the {@link CategValuePred} object from the input string assuming
	 * that the format of the string uses that defined in {@link #toString()}.
	 * 
	 * @param s String representation
	 * @return {@link CategValuePred} object corresponding to string
	 */
	public static CategValuePred parseString(String s) {
		String[] parts = s.split("\t");
		if(parts.length!=5) {
			throw new RuntimeException("Invalid number of items in tab delimited string: "+s);
		}
		
		String id = parts[0].equals("NULL") ? null : parts[0];
		String truevalue = parts[1];
		String predvalue = parts[2].equals("NULL") ? null : parts[2];
		String probs = parts[3].equals("NULL") ? null : parts[3];
		double[] probsarray = ArrayUtils.string2ArrayDouble(probs, ",");
		String weight = parts[4];
		double weightdouble = Double.parseDouble(weight);
		
		return new CategValuePred(id, truevalue, predvalue, probsarray, weightdouble);
	}
}
