package linqs.gaia.model.oc.ncc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.IteratorUtils;

/**
 * This is an unsupervised model for predicting binary valued labels
 * which does a weighted linear combination of the
 * specified normalized numeric valued features or a binary categorical valued feature
 * (wherein the index of the category is used as the value).
 * When the sum of the linear combination
 * is below .5, the first category (specified for the categorical feature)
 * is set as the predicted value and if above .5 (inclusive) the second category
 * is set as the predicted value.
 * 
 * Required Parameters:
 * <UL>
 * <LI> featureweights-Comma delimited feature weight pairs (of the from featureid:weight).
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> normalizeprob-If specified, allow that the sum of the linear combination maybe over 1.
 * In that case, return the probability of the label as one.  However, if this is not
 * specified and the sum of the linear combination is above 1, an exception is thrown.
 * </UL>
 * 
 * @author namatag
 *
 */
public class UnsupervisedLinearWeighting extends BaseVBClassifier implements VBClassifier {
	private static final long serialVersionUID = 1L;
	private List<String> fids = null;
	private List<Double> weights = null;
	private List<String> categories = null;
	private double threshold = 0.5;
	private boolean normalizeprob = false;
	private boolean initialize = true;
	
	private void initialize() {
		initialize = false;
		
		fids = new ArrayList<String>();
		weights = new ArrayList<Double>();
		String featureweights = this.getStringParameter("featureweights").trim();
		String[] parts = featureweights.split(",");
		for(String p:parts) {
			String[] pair = p.split(":");
			if(pair.length!=2) {
				throw new ConfigurationException("Invalid configuration for weights: "+weights);
			}
			
			String fid = pair[0].trim();
			fids.add(fid);
			
			double weight = Double.parseDouble(pair[1].trim());
			weights.add(weight);
		}
		
		this.normalizeprob = this.hasYesNoParameter("normalizeprob", "yes");
	}
	
	@Override
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid,
			List<String> featureids) {
		if(initialize) {
			this.initialize();
		}
		
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		
		Set<Graph> graphs = new HashSet<Graph>();
		for(Decorable d:trainitems) {
			Graph g = ((GraphItem) d).getGraph();
			graphs.add(g);
		}
		
		for(Graph g:graphs) {
			// Verify features are NumFeature
			Schema schema = g.getSchema(targetschemaid);
			for(String fid:fids) {
				Feature f = schema.getFeature(fid);
				if(f instanceof CategFeature) {
					if(((CategFeature) f).numCategories()!=2) {
						throw new ConfigurationException("Categorical Feature not binary: "+fid);
					}
				} else if(f instanceof NumFeature) {
					// Support numeric feature
				} else {
					throw new ConfigurationException("Feature not numeric or binary categorical: "+fid
							+" of type "+f.getClass().getCanonicalName());
				}
			}
			
			// Verify target feature is binary
			if(categories == null) {
				Feature f = schema.getFeature(targetfeatureid);
				if(!(f instanceof CategFeature)) {
					throw new UnsupportedTypeException("Only categorical features supported: "
							+f.getClass().getCanonicalName());
				}
				
				CategFeature cf = (CategFeature) f;
				if(cf.numCategories()!=2) {
					throw new ConfigurationException("Feature not binary categorical: "+targetfeatureid);
				}
				
				categories = IteratorUtils.iterator2stringlist(cf.getAllCategories().iterator());
			}
		}
	}

	@Override
	public FeatureValue predict(Decorable testitem) {
		if(initialize) {
			this.initialize();
		}
		
		// Get categories, if not already defined
		if(categories == null) {
			Feature f = testitem.getSchema().getFeature(targetfeatureid);
			if(!(f instanceof CategFeature)) {
				throw new UnsupportedTypeException("Only categorical features supported: "
						+f.getClass().getCanonicalName());
			}
			
			CategFeature cf = (CategFeature) f;
			if(cf.numCategories()!=2) {
				throw new ConfigurationException("Feature not binary categorical: "+targetfeatureid);
			}
			
			categories = IteratorUtils.iterator2stringlist(cf.getAllCategories().iterator());
		}
		
		double sum = 0;
		int size = fids.size();
		for(int i=0; i<size; i++) {
			double w = weights.get(i);
			double val = 0;
			FeatureValue fv = testitem.getFeatureValue(fids.get(i));
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			} else if(fv instanceof NumValue) {
				NumValue value = (NumValue) testitem.getFeatureValue(fids.get(i));
				val = value.getNumber();
			} else if(fv instanceof CategValue) {
				CategValue cv = (CategValue) fv;
				val = cv.getProbs()[0] < .5 ? 0 : 1;
			} else {
				throw new InvalidStateException("Invalid feature value type: "
						+fv.getClass().getCanonicalName());
			}
			
			if(val<0 || val>1) {
				throw new InvalidStateException("Features must be normalized: "+val);
			}
			
			sum += val * w;
		}
		
		if(normalizeprob && sum>1) {
			sum = 1;
		}
		
		if(sum<0 || sum>1) {
			throw new InvalidStateException("Linear weighted combination out of range: "+sum);
		}
		
		int index = 0;
		if(sum<threshold) {
			index = 0;
		} else {
			index = 1;
		}
		
		String cat = this.categories.get(index);
		double[] probs = new double[2];
		probs[index] = sum;
		probs[(index+1)%2] = 1.0-sum;
		FeatureValue cvalue = new CategValue(cat, probs);
		
		return cvalue;
	}

	@Override
	public void loadVBOC(String directory) {
		// Do nothing
	}

	@Override
	public void saveVBOC(String directory) {
		// Do nothing
	}
}
