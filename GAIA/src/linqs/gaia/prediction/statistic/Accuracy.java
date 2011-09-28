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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.prediction.PositiveOnlyGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.SingleValue;

/**
 * <p>
 * Accuracy statistics defined as:<br>
 * overallaccuracy = (# of times true value=predicted value)/(# of items)
 * <p>
 * This is different from the accuracy measure defined over binary features
 * (in {@link linqs.gaia.prediction.statistic.SimpleBinaryMeasures SimpleBinaryMeasures})
 * in that the predicted value can be multiclass.
 * <p>
 * Note: The prediction group must have predictions which are {@link SingleValue}.
 * <p>
 * 
 * @author namatag
 *
 */
public class Accuracy extends BaseConfigurable implements Statistic {
	private static String accuracy = "overallaccuracy";

	public List<String> getHeader() {
		List<String> header = new ArrayList<String>();
		header.add(accuracy);
		
		return header;
	}

	public Map<String, Double> getStatisticDoubles(PredictionGroup predictions) {
		Map<String, Double> stats = new HashMap<String,Double>(1);
		Iterator<? extends Prediction> itr = predictions.getAllPredictions();
		
		int nummatch = 0;
		int nummiss = 0;
		while(itr.hasNext()) {
			Prediction p = itr.next();
			
			if(!(p instanceof SingleValue)){
				throw new UnsupportedTypeException("Unsupported prediction type: "
						+p.getClass().getCanonicalName());
			}
			
			SingleValue svp = (SingleValue) p;
			if(svp.getTrueValue()==null){
				throw new InvalidStateException("True value not provided for: "+svp);
			}
			
			if(svp.getPredValue()!=null && svp.getPredValue().equals(svp.getTrueValue())) {
				nummatch++;
			} else {
				nummiss++;
			}
		}
		
		if(predictions instanceof PositiveOnlyGroup) {
			throw new UnsupportedTypeException("This version of accuracy undefined" +
					" for Positive Only Prediction Groups." +
					" Use SimpleBinaryMeasures instead.");
		}
		
		double acc = ((double) nummatch/(double) (nummatch+nummiss));
		stats.put(Accuracy.accuracy, acc);
		
		return stats;
	}

	public String getStatisticString(PredictionGroup predictions) {
		return Accuracy.accuracy+"="+this.getStatisticDoubles(predictions).get(Accuracy.accuracy);
	}

	public Map<String, String> getStatisticStrings(PredictionGroup predictions) {
		Map<String, String> stringstats = new HashMap<String,String>(1);
		stringstats.put(Accuracy.accuracy, this.getStatisticString(predictions));
		
		return stringstats;
	}

}
