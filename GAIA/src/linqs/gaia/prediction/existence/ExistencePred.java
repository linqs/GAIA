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
package linqs.gaia.prediction.existence;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.prediction.Probability;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.prediction.Weighted;
import linqs.gaia.util.ArrayUtils;

/**
 * Created to handle existence (e.g., link existence) predictions.
 * 
 * @see ExistencePredGroup
 * 
 * @author namatag
 *
 */
public class ExistencePred implements SingleValue, Probability, Weighted {
	private String truevalue;
	private String predvalue;
	private double[] probs;
	private double weight = 1;
	private String id;
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * Prediction, by default, is set as having the weight of 1.
	 * 
	 * @param truevalue True value of this existence prediction
	 */
	public ExistencePred(String truevalue) {
		this(null, truevalue, null, 1);
	}
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * Prediction, by default, is set as having the weight of 1.
	 * 
	 * @param truevalue True value for this existence prediction
	 * @param probs Probability distribution
	 */
	public ExistencePred(String truevalue, double[] probs) {
		this(null, truevalue, probs, 1);
	}
	
	/**
	 * Existence prediction constructor.
	 * Prediction is set as having the positive value with probability 1.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * 
	 * @param truevalue True value for this positive prediction
	 * @param weight Weight
	 */
	public ExistencePred(String truevalue, double weight) {
		this(null, truevalue, null, weight);
	}
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * 
	 * @param truevalue True value for this existence prediction
	 * @param probs Probability distribution
	 */
	public ExistencePred(String truevalue, double[] probs, double weight) {
		this(null, truevalue, probs, weight);
	}
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * Prediction, by default, is set as having the weight of 1.
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value of this existence prediction
	 */
	public ExistencePred(String id, String truevalue) {
		this(id, truevalue, null, 1);
	}
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * Prediction, by default, is set as having the weight of 1.
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value for this existence prediction
	 * @param probs Probability distribution
	 */
	public ExistencePred(String id, String truevalue, double[] probs) {
		this(id, truevalue, probs, 1);
	}
	
	/**
	 * Existence prediction constructor.
	 * Prediction is set as having the positive value with probability 1.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value for this positive prediction
	 * @param weight Weight
	 */
	public ExistencePred(String id, String truevalue, double weight) {
		this(id, truevalue, null, weight);
	}
	
	/**
	 * Existence prediction constructor.
	 * If the probability is not specified or null,
	 * the prediction is set to positive or negative value with probability 1.0
	 * (depending on whether or not it is added to {@link ExistencePredGroup}
	 * using {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)}).
	 * 
	 * @param id ID of item predicted
	 * @param truevalue True value for this existence prediction
	 * @param probs Probability distribution
	 */
	public ExistencePred(String id, String truevalue, double[] probs, double weight) {
		if(!truevalue.equals(ExistencePredGroup.EXIST)
				&& !truevalue.equals(ExistencePredGroup.NOTEXIST)) {
			throw new InvalidStateException("Invalid true value: "+truevalue);
		}
		
		this.id = id;
		this.truevalue = truevalue.intern();
		this.probs = probs;
		this.weight = weight;
	}
	
	/**
	 * Internal method to set the predicted value based on
	 * whether {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)})
	 * is used.
	 * 
	 * @param predvalue
	 */
	protected void setPredValue(String predvalue) {
		if(predvalue!=null && !predvalue.equals(ExistencePredGroup.EXIST)
				&& !predvalue.equals(ExistencePredGroup.NOTEXIST)) {
			throw new InvalidStateException("Invalid predicted value: "+predvalue);
		}
		
		this.predvalue = predvalue;
	}
	
	/**
	 * Internal method to set the probability based on
	 * whether {@link ExistencePredGroup#addPrediction(ExistencePred)} or
	 * {@link ExistencePredGroup#addNegativePrediction(ExistencePred)})
	 * is used.
	 * 
	 * @param predvalue
	 */
	protected void setProbs(double[] probs) {
		this.probs = probs;
	}
	
	public String getPredValue() {
		return this.predvalue;
	}

	public String getTrueValue() {
		return truevalue;
	}

	public double[] getProbs() {
		if(probs==null && this.getPredValue()!=null) {
			// Return default values
			if(this.getPredValue().equals(ExistencePredGroup.EXIST)) {
				probs = ExistencePredGroup.EXISTPROB;
			} else {
				probs = ExistencePredGroup.NOTEXISTPROB;
			}
		}
		
		return probs;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public String getID() {
		return this.id;
	}
	
	/**
	 * Return the string representation of this {@link ExistencePred} object
	 * in the format:<br>
	 * &lt;id&gt;\t&lt;truevalue&gt;\t&lt;predvalue&gt;\t&ltprobability&gt;\t&lt;weight&gt;<br>
	 * where
	 * <UL>
	 * <LI> The values are tab delimited.
	 * <LI> &lt;id&gt; is the identifier of the object show attribute was predicted
	 * (e.g., SocialNetwork.Facebook.Person.BobSmith) or NULL if not specified.
	 * <LI> &lt;truevalue&gt; is the true category value (e.g., EXIST)
	 * <LI> &lt;predvalue&gt; is the predicted category value (e.g., NOTEXIST) or NULL if not specified.
	 * <LI> &lt;probability&gt; is an comma delimited values representing the probability distribution
	 * (e.g., .9,.1 following the order of categories in the corresponding {@link ExistencePredGroup})
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
	 * Parse the {@link ExistencePred} object from the input string assuming
	 * that the format of the string uses that defined in {@link #toString()}.
	 * 
	 * @param s String representation
	 * @return {@link ExistencePred} object corresponding to string
	 */
	public static ExistencePred parseString(String s) {
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
		
		ExistencePred ep = new ExistencePred(id, truevalue, probsarray, weightdouble);
		ep.setPredValue(predvalue);
		
		return ep;
	}
}
