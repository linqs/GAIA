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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.SingleValue;

/**
 * An extension of SimpleBinaryMeasures which takes the average over non binary features.
 * <p>
 * Macro vs. Micro Statistics<br>
 * All of the statistics offered by this module can be calculated for each
 * category and then averaged, or can be calculated over all decisions and then
 * averaged. The former is called macro-averaging (specifically,
 * macro-averaging with respect to category), and the latter is called
 * micro-averaging. The two procedures bias the results differently -
 * micro-averaging tends to over-emphasize the performance on the largest
 * categories, while macro-averaging over-emphasizes the performance on the
 * smallest. It's often best to look at both of them to get a good idea of how
 * your data distributes across categories.
 * For clarity, the name for the statistics returned is prefixed
 * with "micro-" or "macro-" as appropriate (e.g., micro-precision,macro-recall).
 * <p>
 * Note: The prediction group must be a {@link CategoricalValuedGroup}
 * with predictions which are {@link SingleValue}.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> averagetype-Options are micro or macro.
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>sbmstatistics-Comma delimited statistics you want returned.
 * See {@link linqs.gaia.prediction.statistic.SimpleBinaryMeasures SimpleBinaryMeasures}
 * for possible values.
 * </UL>
 * 
 * @author namatag
 *
 */
public class AverageSBM extends SimpleBinaryMeasures implements Statistic {
	private String averagetype = null;
	private boolean initialize = true;
	
	private void initialize() {
		this.initialize = false;
		averagetype = this.getCaseParameter("averagetype", new String[]{"micro","macro"});
	}
	
	public List<String> getHeader() {
		if(initialize) {
			this.initialize();
		}
		
		List<String> header = super.getHeader();
		List<String> newheader = new ArrayList<String>();
		for(String h:header) {
			newheader.add(averagetype+"-"+h);
		}
		
		return newheader;
	}
	
	public Map<String, Double> getStatisticDoubles(PredictionGroup predictions) {
		if(initialize) {
			this.initialize();
		}
		
		if(averagetype.equals("micro")) {
			return this.getMicro(predictions);
		} else {
			return this.getMacro(predictions);
		}
	}
	
	private Map<String, Double> getMacro(PredictionGroup predictions) {
		String truerootvals[] = null;
		if(predictions instanceof CategoricalValuedGroup) {
			truerootvals = (String[])
				((CategoricalValuedGroup) predictions).getCategories().toArray(new String[0]);
		} else {
			throw new ConfigurationException("Unable to establish true values to consider");
		}
		
		if(truerootvals.length<=2) {
			throw new InvalidStateException("Average SBM should never be used for binary classes." +
					" Use "+SimpleBinaryMeasures.class.getCanonicalName()+" instead.");
		}
		
		Map<String, Double> allstats = null;
		for(String trval:truerootvals){
			double[] cmatrix = StatisticUtils.getBinaryConfusionMatrix(predictions, trval);
			Map<String, Double> currstats = super.getStatisticDoubles(cmatrix);
			
			if(allstats==null){
				allstats = new HashMap<String,Double>();
				Set<Entry<String,Double>> entries = currstats.entrySet();
				for(Entry<String,Double> e:entries){
					allstats.put(averagetype+"-"+e.getKey(), e.getValue());
				}
			} else {
				Set<Entry<String,Double>> entries = currstats.entrySet();
				for(Entry<String,Double> e:entries){
					Double newval = e.getValue();
					if(Double.isNaN(newval)) {
						newval = 0.0;
					}
					
					Double oldval = allstats.get(averagetype+"-"+e.getKey());
					if(Double.isNaN(oldval)) {
						oldval = 0.0;
					}
					
					// Add prefix
					allstats.put(averagetype+"-"+e.getKey(), oldval+newval);
				}
			}
		}
		
		double divisor = truerootvals.length;
		Set<String> keys = new HashSet<String>(allstats.keySet());
		for(String key:keys){
			double avgval = allstats.get(key)/divisor;
			allstats.put(key, avgval);
		}
		
		return allstats;
	}
	
	private Map<String, Double> getMicro(PredictionGroup predictions) {
		String truerootvals[] = null;
		if(predictions instanceof CategoricalValuedGroup) {
			truerootvals = (String[])
				((CategoricalValuedGroup) predictions).getCategories().toArray(new String[0]);
		} else {
			throw new ConfigurationException("Unable to establish true values to consider");
		}
		
		if(truerootvals.length<=2) {
			throw new InvalidStateException("Average SBM should never be used for binary classes." +
					" Use "+SimpleBinaryMeasures.class.getCanonicalName()+" instead.");
		}
		
		double[] cmatrix = new double[]{0,0,0,0};
		for(String trval:truerootvals){
			double[] currcmatrix = StatisticUtils.getBinaryConfusionMatrix(predictions, trval);
			cmatrix[0]+=currcmatrix[0];
			cmatrix[1]+=currcmatrix[1];
			cmatrix[2]+=currcmatrix[2];
			cmatrix[3]+=currcmatrix[3];
		}
		
		Map<String,Double> microstats = super.getStatisticDoubles(cmatrix);
		Map<String,Double> allstats = new HashMap<String,Double>();
		Set<Entry<String,Double>> entries = microstats.entrySet();
		for(Entry<String,Double> e:entries){
			// Add prefix
			allstats.put(averagetype+"-"+e.getKey(), e.getValue());
		}
		
		return allstats;
	}
}