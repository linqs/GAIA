package linqs.gaia.graph.datagraph.feature.explicit;

import java.util.HashMap;
import java.util.Set;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

public class DGExplicitMVCateg extends ExplicitMultiCateg implements MultiCategFeature, DGExplicitFeature {
	private ExplicitMultiCateg emvc = null;
	private HashMap<Integer,SimplePair<Set<String>,double[]>> id2value;
	
	public DGExplicitMVCateg(ExplicitMultiCateg emvc) {
		super(emvc.getAllCategories());
		
		this.id2value = new HashMap<Integer,SimplePair<Set<String>,double[]>>();
		this.emvc = emvc;
	}

	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			SimplePair<Set<String>,double[]> pair = id2value.get(di);
			return new MultiCategValue(pair.getFirst(), pair.getSecond());
		}
		
		// Handle unknown value
		if(emvc.isClosed()){
			return emvc.getClosedDefaultValue();
		} else {
			return FeatureValue.UNKNOWN_VALUE;
		}
	}

	public void setFeatureValue(Integer di, FeatureValue value) {
		if(!this.isValidValue(value)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+value);
		}
		
		// If the value is set as unknown value, don't store it in map.
		if(value.equals(FeatureValue.UNKNOWN_VALUE)){
			if(this.id2value.containsKey(di)) {
				this.id2value.remove(di);
			}
		} else if(this.isClosed() && value.equals(emvc.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			MultiCategValue mvc = (MultiCategValue) value;
			Set<String> cats = mvc.getCategories().copyAsSet();
			double[]  probs = mvc.getProbs();
			
			// Handle categorical values where the prob is set to null
			if(probs==null) {
				probs = new double[this.getAllCategories().size()];
				for(int i=0; i<probs.length; i++) {
					probs[i] = 1;
				}
			}
			
			SimplePair<Set<String>,double[]> pair
				= new SimplePair<Set<String>,double[]>(cats, probs);
			id2value.put(di, pair);
		}
	}

	public UnmodifiableList<String> getAllCategories() {
		return emvc.getAllCategories();
	}

	public FeatureValue getClosedDefaultValue() {
		return emvc.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return emvc.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return emvc.isValidValue(value);
	}

	public Feature copy() {
		return this.emvc.copy();
	}
	
	public ExplicitFeature getOrigFeature() {
		return this.emvc;
	}
}
