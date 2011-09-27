package linqs.gaia.graph.datagraph.feature.explicit;

import java.util.HashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;

public class DGExplicitNum extends ExplicitNum implements NumFeature, DGExplicitFeature {
	private ExplicitNum en = null;
	private HashMap<Integer,Double> id2value;
	
	public DGExplicitNum(ExplicitNum en) {
		this.id2value = new HashMap<Integer,Double>();
		this.en = en;
	}
	
	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			return new NumValue(id2value.get(di));
		}
		
		// Handle unknown value
		if(en.isClosed()){
			return en.getClosedDefaultValue();
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
		} else if(this.isClosed() && value.equals(en.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			this.id2value.put(di, ((NumValue) value).getNumber());
		}
	}

	public FeatureValue getClosedDefaultValue() {
		return en.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return en.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return en.isValidValue(value);
	}

	public Feature copy() {
		return this.en.copy();
	}
	
	public ExplicitFeature getOrigFeature() {
		return this.en;
	}
}
