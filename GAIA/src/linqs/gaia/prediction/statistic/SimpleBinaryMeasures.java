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
package linqs.gaia.prediction.statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.SingleValue;

/**
 * <p>Statistics object which handles any simple binary statistics measure
 * involving calculation of True Positive (tp), True Negative (tn),
 * False Positive (fp), and False Negative (fn).</p>
 * 
 * Defined measures are:
 * <UL>
 * <LI>accuracy: Defined as (tp+fp)/(tp+fp+tn+fn)
 * <LI>specificity: Defined as tn/(fp+tn)
 * <LI>precision: Defined as tp/(tp+fp)
 * <LI>recall: Defined as tp/(tp+fn)
 * <LI>fmeasure: Defined as ((1+(Beta*Beta))*precision*recall)/((Beta*Beta*precision)+recall)
 * where Beta, be default, is set to 1 (i.e., F1 measure)
 * <LI>tpr: Defined as tp/(tp + fn) (aka: True Positive Rate)
 * <LI>fpr: Defined as fp/(fp + tn) (aka: False Positive Rate)
 * <LI>numpos: Defined as tp + fn
 * <LI>numneg: Defined as tn + fp
 * <LI>tp: Defined as the number of true positives
 * <LI>fp: Defined as the number of false positives
 * <LI>tn: Defined as the number of true negatives
 * <LI>fn: Defined as the number of false negatives
 * </UL>
 * <p>
 * Note: The prediction group must be a {@link CategoricalValuedGroup}
 * with predictions which are {@link SingleValue}.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>truerootval-Value to consider the true value.  All other values are treated as false.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>sbmstatistics-Comma delimited statistics you want returned i.e.: accuracy,precision
 * <LI>beta-Beta value for fmeasure computation.  This must be a positive real.  Default is 1.
 * </UL>
 * 
 * @author namatag
 *
 */
public class SimpleBinaryMeasures extends BaseConfigurable implements Statistic {
	public Map<String, Double> getStatisticDoubles(PredictionGroup predictions) {
		// Get confusion matrix
		String truerootval = this.getStringParameter("truerootval");
		double[] cmatrix = StatisticUtils.getBinaryConfusionMatrix(predictions, truerootval);
		
		return this.getStatisticDoubles(cmatrix);
	}
	
	protected Map<String, Double> getStatisticDoubles(double[] cmatrix) {
		// Initialize HashMap to maximum size
		LinkedHashMap<String, Double> allstats = new LinkedHashMap<String,Double>();
		
		double tp = cmatrix[0];
		double fp = cmatrix[1];
		double tn = cmatrix[2];
		double fn = cmatrix[3];
		
		// Values from http://en.wikipedia.org/wiki/Receiver_operating_characteristic
		double accuracy = (double) (tp+tn)/(tp+fp+tn+fn);
		double specificity = (double) tn/(fp+tn);
		
		// Handle case when you might have a divide by 0 problem
		double precision = (double) tp/(tp+fp);
		precision = Double.isNaN(precision) ? 0 : precision;
		
		double recall = (double) tp/(tp+fn);
		recall = Double.isNaN(recall) ? 0 : recall;
		
		double beta = this.getDoubleParameter("beta",1.0);
		if(beta<=0 || Double.isInfinite(beta) || Double.isNaN(beta)) {
			throw new ConfigurationException("Beta value for F-Measure must be a positive real number: "+beta);
		}
		double betasqrd = beta*beta;
		double fmeasure = (double) ((1+betasqrd)*precision*recall)/((betasqrd*precision)+recall);
		fmeasure = Double.isNaN(fmeasure) ? 0 : fmeasure;
		
		double tpr = recall;
		double fpr = (double) fp/(fp + tn);
		
		double numpos = tp + fn;
		double numneg = tn + fp;
		
		// If no statistics are defined, return all.
		// Note: Make sure this is consistent with getHeader().
		allstats.put("accuracy", accuracy);
		allstats.put("specificity", specificity);
		allstats.put("fmeasure", fmeasure);
		allstats.put("precision", precision);
		allstats.put("recall", recall);
		allstats.put("tpr", tpr);
		allstats.put("fpr", fpr);
		allstats.put("numpos", 0.0+numpos);
		allstats.put("numneg", 0.0+numneg);
		allstats.put("tp", 0.0+tp);
		allstats.put("fp", 0.0+fp);
		allstats.put("tn", 0.0+tn);
		allstats.put("fn", 0.0+fn);
		
		LinkedHashMap<String, Double> stats = null;
		if(this.hasParameter("sbmstatistics")){
			stats = new LinkedHashMap<String,Double>(4);
			String[] statistics = this.getStringParameter("sbmstatistics").split(",");
			for(String stat : statistics){
				if(!allstats.containsKey(stat)){
					throw new ConfigurationException("Undefined statistic requested: "+stat);
				}
				
				stats.put(stat, allstats.get(stat));
			}
		} else {
			stats = allstats;
		}
		
		return stats;
	}

	public String getStatisticString(PredictionGroup predictions) {
		Map<String, Double> doublestats = this.getStatisticDoubles(predictions);
		String stats = null;
		
		List<String> order = this.getHeader();
		for(String s:order){
			if(stats==null){
				stats = "";
			} else {
				stats += ",";
			}
			
			stats += s+"="+doublestats.get(s);
		}
		
		return stats;
	}

	public Map<String, String> getStatisticStrings(PredictionGroup predictions) {
		Map<String, Double> doublestats = this.getStatisticDoubles(predictions);
		Map<String, String> stringstats = new HashMap<String,String>(doublestats.size());
		
		Set<Entry<String,Double>> entries = doublestats.entrySet();
		for(Entry<String,Double> e:entries){
			stringstats.put(e.getKey(), ""+e.getValue());
		}
		
		return stringstats;
	}

	public List<String> getHeader() {
		// Note: Make sure this is consistent with getDoubles().
		List<String> allheader = new ArrayList<String>();
		allheader.add("accuracy");
		allheader.add("specificity");
		allheader.add("fmeasure");
		allheader.add("precision");
		allheader.add("recall");
		allheader.add("tpr");
		allheader.add("fpr");
		allheader.add("numpos");
		allheader.add("numneg");
		allheader.add("tp");
		allheader.add("fp");
		allheader.add("tn");
		allheader.add("fn");
		
		List<String> header = null;
		if(this.hasParameter("sbmstatistics")){
			String[] statistics = this.getStringParameter("sbmstatistics").split(",");
			header = new ArrayList<String>(statistics.length);
			for(String stat : statistics){
				if(!allheader.contains(stat)) {
					throw new ConfigurationException("Unknown statistic: "+stat);
				}
				
				header.add(stat);
			}
		} else {
			// If no statistics are defined, return all.
			header = allheader;
		}
		
		return header;
	}

}
