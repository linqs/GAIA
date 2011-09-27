package linqs.gaia.prediction.statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.log.Log;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.Probability;
import linqs.gaia.prediction.SingleValue;
import linqs.gaia.prediction.existence.ExistencePred;
import linqs.gaia.prediction.existence.ExistencePredGroup;
import linqs.gaia.prediction.feature.CategValuePred;
import linqs.gaia.prediction.feature.CategValuePredGroup;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.SimplePair;

/**
 * Compute the area under the ROC curve.
 * <p>
 * Note: The prediction group must be a {@link CategoricalValuedGroup}
 * with predictions which are {@link SingleValue} and has {@link Probability}.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>truerootval-Value to consider the true/positive label.
 * </UL>
 * 
 * @author srhuang
 * @author namatag
 *
 */
public class AUC extends BaseConfigurable implements Statistic {
	private static String auc = "auc";
	private String truerootval = null;
	private int truerootindex = -1;
	private boolean initialize = true;
	
	private boolean isPO = false;
	private int numpospred, numnegpred, total, totalpos;
	
	private void initialize(PredictionGroup predictions) {
		initialize = false;
		if(!(predictions instanceof CategoricalValuedGroup)) {
			throw new UnsupportedTypeException("Only CategoricalValuedGroup supported: "
					+predictions.getClass().getCanonicalName());
		}
		
		CategoricalValuedGroup cvpg = (CategoricalValuedGroup) predictions;
		truerootval = this.getStringParameter("truerootval");
		truerootindex = cvpg.getCategories().indexOf(truerootval);
	}
	
	public List<String> getHeader() {
		return Arrays.asList(AUC.auc);
	}

	public Map<String,String> getStatisticStrings(PredictionGroup predictions) {
		Map<String, String> stringstats = new HashMap<String,String>(1);
		stringstats.put(AUC.auc, ""+this.getStatisticDoubles(predictions).get(AUC.auc));

		return stringstats;
	}

	public String getStatisticString(PredictionGroup predictions) {
		return AUC.auc+"="+this.getStatisticDoubles(predictions).get(AUC.auc);
	}

	public Map<String,Double> getStatisticDoubles(PredictionGroup predictions) {
		if(initialize) {
			initialize(predictions);
		}
		
		numpospred = 0;
		numnegpred = 0;
		total = 0;
		totalpos = 0;

		Iterator<? extends Prediction> preds = predictions.getAllPredictions();
		List <SingleValue> scpreds = new LinkedList<SingleValue>();
		while(preds.hasNext()) {
			Prediction p = preds.next();
			SingleValue cvp = (SingleValue) p;
			scpreds.add(cvp);
			if(cvp.getTrueValue().equals(truerootval)) {
				numpospred++;
			} else {
				numnegpred++;
			}
		}
		
		// Handle case where we stored some partial set
		// of negative cases in Existence Prediction Group
		if(predictions instanceof ExistencePredGroup) {
			isPO = true;
			ExistencePredGroup epg = (ExistencePredGroup) predictions;
			preds = epg.getPartialNegativePredictions();
			while(preds.hasNext()) {
				Prediction p = preds.next();
				SingleValue cvp = (SingleValue) p;
				scpreds.add(cvp);
				if(cvp.getTrueValue().equals(truerootval)) {
					numpospred++;
				} else {
					numnegpred++;
				}
			}
			
			total = epg.getNumTotal();
			totalpos = epg.getNumPositive();
			
			// Verify that the numbers provided are valid
			if(numpospred > total) {
				throw new InvalidStateException("Total number of provided"
						+" positive predictions more than the possible number of predictions: "
						+numpospred+" positives for "+total+" total");
			}
			
			if(numpospred > totalpos) {
				throw new InvalidStateException("Total number of provided"
						+" positive predictions more than the possible number of positive predictions: "
						+numpospred+" positives for "+totalpos+" total positive");
			}
			
			if((numpospred+numnegpred) > total) {
				throw new InvalidStateException("Total number of provided"
						+" positive and negative predictions more"
						+" than the possible number of positive predictions: "
						+numpospred+" positives with "
						+numnegpred+" negatives for "
						+totalpos+" total");
			}
			
			if(totalpos + numnegpred > total) {
				throw new InvalidStateException("Total number of provided"
						+" total positive and provided negative predictions more"
						+" than the possible number of positive predictions: "
						+numpospred+" positives with "
						+numnegpred+" negatives for "
						+totalpos+" total");
			}
			
			if(totalpos == total) {
				throw new InvalidStateException("AUC implementation not defined for case" +
						" where all predictions are no true negative cases");
			}
		}
		
		// Define comparator for the predicted objects
		Comparator<Prediction> c = new Comparator<Prediction>() {
			public int compare(Prediction p1, Prediction p2) {
				if(Double.isNaN(((Probability) p1).getProbs()[truerootindex])
					|| Double.isNaN(((Probability) p2).getProbs()[truerootindex])) {
					throw new RuntimeException("Probability undefined: "
							+ArrayUtils.array2String(((Probability) p1).getProbs())
							+" and "
							+ArrayUtils.array2String(((Probability) p2).getProbs()));
				}

				// We want the values in decreasing order (not increasing)
				if(((Probability) p1).getProbs()[truerootindex] < ((Probability) p2).getProbs()[truerootindex]) {
					return 1;
				} else if(((Probability) p1).getProbs()[truerootindex] > ((Probability) p2).getProbs()[truerootindex]) {
					return -1;
				} else {
					return 0;
				}
			}
		};

		// Sort the predictions
		Collections.sort(scpreds, c);

		List <SimplePair<Double,String>> examples = new ArrayList<SimplePair<Double,String>>();
		for(Prediction p : scpreds) {
			examples.add(new SimplePair<Double,String>(new Double(((Probability) p).getProbs()[truerootindex]),
					((SingleValue) p).getTrueValue().toString()));
		}
		
		Map <String, Double> stats = new Hashtable <String, Double> ();
		stats.put(AUC.auc, compute_auc(examples));

		return stats;
	}

	private double compute_auc(List <SimplePair <Double, String>> tuples) {
		int P = 0;
		int N = 0;
		int tp_cum = 0;
		int fp_cum = 0;
		double auc = 0;
		double cur_tpr = 0;
		double cur_fpr = 0;
		
		List <SimplePair<Integer, Integer>> roc = compute_roc(tuples);
		for(SimplePair<Integer, Integer> p : roc) {
			P += p.getFirst();
			N += p.getSecond();
		}
		
		for(int q = 0; q < roc.size(); q++) {
			SimplePair<Integer, Integer> p = roc.get(q);

			tp_cum += p.getFirst();
			fp_cum += p.getSecond();

			double tpr = tp_cum / (1.0 * P);
			if(Double.isNaN(tpr)) tpr = 0;
			double fpr = fp_cum / (1.0 * N);
			if(Double.isNaN(fpr)) fpr = 0;

			if(p.getSecond() != 0 || p.getFirst() != 0) {
				// There is some area under the false positives we add to auc
				auc += (cur_tpr * (fpr - cur_fpr)) + ((.5)*(tpr - cur_tpr)*(fpr - cur_fpr));
			}
			
			cur_tpr = tpr;
			cur_fpr = fpr;
		}
		
		if(auc < 0 || auc > 1) {
			throw new InvalidStateException("Invalid AUC computed: "+auc);
		}
		
		return auc;
	}

	private List <SimplePair<Integer, Integer>> compute_roc(List <SimplePair<Double, String>> tuples) {
		List <SimplePair<Integer, Integer>> roc = new ArrayList <SimplePair<Integer, Integer>>();
		int tp = 0;
		int fp = 0;
		double lastThresh = Double.NEGATIVE_INFINITY;
		double curThresh = Double.NEGATIVE_INFINITY;
		for(int q = 0; q < tuples.size(); q++) {
			SimplePair <Double, String> t = tuples.get(q);

			curThresh = t.getFirst();
			if(curThresh != lastThresh) {
				// Print new point
				lastThresh = curThresh;
				if(tp == 0 && fp == 0) {
					roc.add(new SimplePair<Integer, Integer> (0, 0));
				} else {
					roc.add(new SimplePair<Integer, Integer>(tp, fp));
				}
				
				tp = 0;
				fp = 0;
			}
			
			if(t.getSecond().equals(truerootval)) {
				tp++;
			} else {
				fp++;
			}
		}
		
		if(tp == 0 && fp == 0) {
			roc.add(new SimplePair<Integer, Integer> (0, 0));
		} else {
			roc.add(new SimplePair<Integer, Integer>(tp, fp));
		}
		
		// Add point to correspond to predictions not explicitly provided
		if(isPO) {
			// Add last entry in roc to represent all nodes with a probability of 0.0
			int nummissingpos = totalpos-numpospred;
			int nummissingneg = total-totalpos-numnegpred;
			if(nummissingpos==0.0 && nummissingneg==0.0) {
				// All predictions accounted for
			} else {
				if(curThresh==0.0) {
					// Update last entry in roc since some of the explicit
					// predictions also had a probability of 0.0
					roc.remove(roc.size()-1);
				}
				
				roc.add(new SimplePair<Integer, Integer>(totalpos-numpospred, total-totalpos-numnegpred));
			}
		}

		return roc;
	}
	
	public static void main(String[] args) {
		CategValuePredGroup cpgroup = new CategValuePredGroup(Arrays.asList(new String[]{"FALSE","TRUE"}));
		
		CategValuePred cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.9});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.8});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","FALSE", new double[]{0.0,0.8});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.8});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.7});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.6});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.55});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.54});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.53});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.52});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("TRUE","TRUE", new double[]{0.0,0.51});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.505});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.505});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.505});
		cpgroup.addPrediction(cvp);
		cvp = new CategValuePred("FALSE","TRUE", new double[]{0.0,0.505});
		cpgroup.addPrediction(cvp);
		
		AUC auc1 = new AUC();
		auc1.setParameter("truerootval", "TRUE");
		Log.INFO(auc1.getStatisticString(cpgroup));
		
		AUC auc2 = new AUC();
		auc2.setParameter("truerootval", "EXIST");
		ExistencePredGroup epg2 = new ExistencePredGroup();
		epg2.addPrediction(new ExistencePred("EXIST", new double[]{0.5,0.5}));
		epg2.addNegativePrediction(new ExistencePred("NOTEXIST", new double[]{0.4,0.6}));
		epg2.addNegativePrediction(new ExistencePred("NOTEXIST", new double[]{0.5,0.5}));
		epg2.setNumTotal(4);
		epg2.setNumPositive(2);
		Log.INFO(auc2.getStatisticString(epg2));
		
		String tmpfile = FileIO.getTemporaryDirectory()+"/tmp.txt";
		epg2.savePredictions(tmpfile);
		ExistencePredGroup epgcopy = ExistencePredGroup.loadPredictions(tmpfile);
		Log.INFO(auc2.getStatisticString(epgcopy));
		
		AUC auc3 = new AUC();
		auc3.setParameter("truerootval", "EXIST");
		ExistencePredGroup epg3 = new ExistencePredGroup();
		epg3.addPrediction(new ExistencePred("EXIST", new double[]{0.0,1.0}));
		epg3.addPrediction(new ExistencePred("EXIST", new double[]{0.0,1.0}));
		epg3.addNegativePrediction(new ExistencePred("NOTEXIST", new double[]{0.0,1.0}));
		epg3.addNegativePrediction(new ExistencePred("NOTEXIST", new double[]{0.0,1.0}));
		epg3.setNumTotal(4);
		epg3.setNumPositive(2);
		Log.INFO(auc3.getStatisticString(epg3));
		
		epg3.savePredictions(tmpfile);
		epgcopy = ExistencePredGroup.loadPredictions(tmpfile);
		Log.INFO(auc2.getStatisticString(epgcopy));
	}
}
