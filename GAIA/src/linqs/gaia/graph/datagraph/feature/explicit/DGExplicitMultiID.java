package linqs.gaia.graph.datagraph.feature.explicit;

import java.util.HashMap;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.identifiable.ID;
import linqs.gaia.util.UnmodifiableSet;

public class DGExplicitMultiID extends ExplicitMultiID implements MultiIDFeature, DGExplicitFeature {
	private ExplicitMultiID emid = null;
	private HashMap<Integer,UnmodifiableSet<ID>> id2value;
	
	public DGExplicitMultiID(ExplicitMultiID es) {
		this.id2value = new HashMap<Integer, UnmodifiableSet<ID>>();
		this.emid = es;
	}
	
	public FeatureValue getFeatureValue(Integer di) {
		if(id2value.containsKey(di)){
			return new MultiIDValue(id2value.get(di));
		}
		
		// Handle unknown value
		if(emid.isClosed()){
			return emid.getClosedDefaultValue();
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
		} else if(this.isClosed() && value.equals(emid.getClosedDefaultValue())) {
			// Default value encountered in a closed feature.
			// Do not add.  The value will be returned by default.
			this.id2value.remove(di);
		} else {
			this.id2value.put(di, ((MultiIDValue) value).getIDs());
		}
	}

	public FeatureValue getClosedDefaultValue() {
		return emid.getClosedDefaultValue();
	}

	public boolean isClosed() {
		return emid.isClosed();
	}

	public boolean isValidValue(FeatureValue value) {
		return emid.isValidValue(value);
	}

	public Feature copy() {
		return this.emid.copy();
	}
	
	public ExplicitFeature getOrigFeature() {
		return this.emid;
	}
}
