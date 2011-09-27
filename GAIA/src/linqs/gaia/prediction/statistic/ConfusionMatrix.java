package linqs.gaia.prediction.statistic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.log.Log;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.PositiveOnlyGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.util.KeyedCount;

/**
 * Returns the confusion matrix of some predictions.
 * <p>
 * Note: The prediction group must be a {@link CategoricalValuedGroup}
 * with predictions which are {@link SingleValue}.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> supportnullpreds-If set to yes, the confusion matrix will support
 * no prediction being made for a given value.  In this case, it will
 * create a new column in the confusion matrix for unpredicted values.
 * If not specified, unpredicted values will be completely ignored.
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConfusionMatrix extends BaseConfigurable implements Statistic {
	private List<String> categories;
	private static final String HEADER = "ConfusionMatrix";
	private static final String NULL_PRED = "NULL_PRED";
	
	public Map<String, Double> calcStatisticDoubles(PredictionGroup predictions) {
		boolean supportnullpreds = this.hasParameter("supportnullpreds", "yes");
		
		LinkedHashMap<String, Double> stats = null;
		if(predictions instanceof PositiveOnlyGroup) {
			PositiveOnlyGroup pog = (PositiveOnlyGroup) predictions;
			Object pos = pog.getPositiveValue();
			Object neg = pog.getNegativeValue();
			
			double cm[] = StatisticUtils.getBinaryConfusionMatrix(predictions, pos);
			double tp = cm[0];
			double fp = cm[1];
			double tn = cm[2];
			double fn = cm[3];
			
			String posstring = pos.toString();
			String negstring = neg.toString();
			categories = Arrays.asList(new String[]{neg.toString(), pos.toString()});
			stats = new LinkedHashMap<String,Double>(categories.size()*categories.size());
			
			stats.put(posstring+":"+posstring, tp);
			stats.put(posstring+":"+negstring, fn);
			stats.put(negstring+":"+posstring, fp);
			stats.put(negstring+":"+negstring, tn);
		} else if(predictions instanceof CategoricalValuedGroup){
			CategoricalValuedGroup cvpreds = (CategoricalValuedGroup) predictions;
			
			categories = cvpreds.getCategories().copyAsList();
			if(supportnullpreds) {
				categories.add(ConfusionMatrix.NULL_PRED);
			}
			
			// Initialize HashMap to maximum size
			// Note: We're allowing for no predictions
			stats = new LinkedHashMap<String,Double>(categories.size()*categories.size());
			KeyedCount<String> kc = new KeyedCount<String>();
			
			Iterator<? extends Prediction> pitr = predictions.getAllPredictions();
			while(pitr.hasNext()) {
				Prediction p = pitr.next();
				
				SingleValue svp = (SingleValue) p;
				
				if(!categories.contains(svp.getTrueValue()) && !categories.contains(svp.getPredValue())) {
					throw new InvalidStateException("Confusion matrix requires that the true"
							+" and predicted values must match the category:"
							+" True: "+svp.getTrueValue()
							+" Predicted="+svp.getPredValue());
				}
				
				Object predvalue = svp.getPredValue();
				String pvstring = ConfusionMatrix.NULL_PRED;
				if(predvalue != null) {
					pvstring = svp.getPredValue().toString();
				} else {
					if(!supportnullpreds) {
						Log.WARN("A predicted value was not given for the predicted object." +
								"  Enable support for these unknown values.");
					}
				}
				
				String key = svp.getTrueValue()+":"+pvstring;
				kc.increment(key);
			}
			
			for(int t=0;t<categories.size();t++){
				for(int p=0;p<categories.size();p++){
					String key = categories.get(t)+":"+categories.get(p);
					int count = kc.getCount(key);
					stats.put(key, 0.0+count);
				}
			}
		} else {
			throw new UnsupportedTypeException("Invalid prediction type: "+predictions.getClass().getCanonicalName());
		}
		
		return stats;
	}

	public String getStatisticString(PredictionGroup predictions) {
		Map<String, Double> doublestats = this.calcStatisticDoubles(predictions);
		StringBuffer buf = new StringBuffer();
		buf.append("Confusion Matrix (Row True, Column Predicted):\n");
		for(int t=0;t<categories.size();t++){
			buf.append("\t"+categories.get(t));
		}
		
		buf.append("\n");
		for(int t=0;t<categories.size();t++){
			if(categories.get(t).equals(ConfusionMatrix.NULL_PRED)) {
				continue;
			}
			
			buf.append(categories.get(t));
			for(int p=0;p<categories.size();p++){
				
				String key = categories.get(t)+":"+categories.get(p);
				int count = doublestats.get(key).intValue();
				buf.append("\t"+count);
			}
			
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	/**
	 * A string with semicolon delimited key value pair
	 * (i.e., key1=val1;key2=val2) is returned.
	 * Key is a colon delimited pair of the form
	 * truevalue:predvalue and value is the corresponding
	 * number in the confusion matrix.
	 */
	public Map<String, String> getStatisticStrings(PredictionGroup predictions) {
		Map<String, String> stringstats = new HashMap<String,String>(1);
		
		String value = null;
		Map<String, Double> doublestats = this.calcStatisticDoubles(predictions);
		Set<Entry<String,Double>> entries = doublestats.entrySet();
		for(Entry<String,Double> e:entries){
			if(value==null) {
				value = "";
			} else {
				value += ";";
			}
			
			value = e.getKey()+"="+e.getValue();
		}
		
		stringstats.put(HEADER, value);
		
		return stringstats;
	}

	/**
	 * This function is undefined for a confusion matrix.  A -1 will
	 * be returned instead.
	 */
	public Map<String, Double> getStatisticDoubles(PredictionGroup predictions) {
		Map<String, Double> result = new HashMap<String, Double>(1);
		result.put(HEADER, -1.0);
		
		return result;
	}
	
	public List<String> getHeader() {
		return Arrays.asList(HEADER);
	}
}
