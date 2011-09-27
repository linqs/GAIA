package linqs.gaia.graph.datagraph.feature.explicit;

import java.util.HashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.UnmodifiableList;

public class DGExplicitCateg extends ExplicitCateg implements CategFeature, DGExplicitFeature {
	private ExplicitCateg ec = null;
	private HashMap<Integer, CategValue> id2value;
	
	private HashMap<CategValue, CategValue> catvalue2catvalue;
	
	public DGExplicitCateg(ExplicitCateg ec) {
		super(ec.getAllCategories());
		this.id2value = new HashMap<Integer,CategValue>();
		this.ec = ec;
		
		// Handle categorical values where the prob is set to null
		if(this.ec.isClosed()) {
			CategValue cvalue = (CategValue) this.ec.getClosedDefaultValue();
			if(cvalue.getProbs()==null) {
				UnmodifiableList<String> cats = this.getAllCategories();
				double[] probs = new double[cats.size()];
				probs[cats.indexOf(cvalue.getCategory())]=1;
				
				cvalue = new CategValue(cvalue.getCategory().intern(), probs);
				this.ec = new ExplicitCateg(this.ec.getAllCategories(), cvalue);
			}
		}
		
		this.catvalue2catvalue = new HashMap<CategValue, CategValue>();
		
		// Store a categorical value for categorical values
		// of this feature where the probability is 1 for the
		// category and 0 otherwise.
		// Note: This is to save memory.
		UnmodifiableList<String> cats = this.getAllCategories();
		int i = 0;
		for(String cat:cats) {
			double[] probs = new double[cats.size()];
			probs[i] = 1;
			CategValue cv = new CategValue(cat, probs);
			this.catvalue2catvalue.put(cv, cv);
			i++;
		}
	}
	
	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			return id2value.get(di);
		}
		
		// Handle unknown value
		if(ec.isClosed()){
			return ec.getClosedDefaultValue();
		} else {
			return FeatureValue.UNKNOWN_VALUE;
		}
	}

	public void setFeatureValue(Integer di, FeatureValue value) {
		// Verify that the value is valid
		if(!this.isValidValue(value)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+value);
		}
		
		// If the value is set as unknown value, don't store it in map.
		if(value.equals(FeatureValue.UNKNOWN_VALUE)){
			if(id2value.containsKey(di)) {
				this.id2value.remove(di);
			}
		} else if(this.isClosed() && value.equals(ec.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			CategValue cvalue = (CategValue) value;
			double[]  probs = cvalue.getProbs();
			
			// Handle categorical values where the prob is set to null
			if(probs==null) {
				UnmodifiableList<String> cats = this.getAllCategories();
				probs = new double[cats.size()];
				probs[cats.indexOf(cvalue.getCategory())]=1;
				
				cvalue = new CategValue(cvalue.getCategory().intern(), probs);
			}
			
			// Do not create a new categorical value object
			// for values where the probability is 1 for
			// the category and 0 otherwise.
			// Note: Optimization similar to String.intern().
			if(this.catvalue2catvalue.containsKey(cvalue)) {
				cvalue = this.catvalue2catvalue.get(cvalue);
			}
			
			this.id2value.put(di, cvalue);
		}
	}

	public FeatureValue getClosedDefaultValue() {
		return ec.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return ec.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return ec.isValidValue(value);
	}

	public UnmodifiableList<String> getAllCategories() {
		return ec.getAllCategories();
	}

	public Feature copy() {
		return this.ec.copy();
	}

	public ExplicitFeature getOrigFeature() {
		return this.ec;
	}
}
