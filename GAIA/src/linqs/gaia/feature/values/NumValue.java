package linqs.gaia.feature.values;

import java.io.Serializable;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.NumFeature;

/**
 * Numeric value returned by all {@link NumFeature} features.
 * 
 * @author namatag
 *
 */
public class NumValue implements FeatureValue, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Double value = null;
	
	/**
	 * Constructor
	 * 
	 * @param value Numeric value
	 */
	public NumValue(Double value) {
		if(value == null || value.equals(Double.NaN)){
			throw new InvalidAssignmentException("Cannot set null or NaN number.  " +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.  Value="+value);
		}
		
		this.value = value;
	}
	
	/**
	 * Constructor
	 * 
	 * @param value Numeric value
	 */
	public NumValue(Integer value) {
		if(value == null){
			throw new InvalidAssignmentException("Cannot set null or NaN number.  " +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.  Value="+value);
		}
		
		this.value = 0.0+value;
	}
	
	/**
	 * Return numeric value
	 * 
	 * @return Numeric value
	 */
	public Double getNumber() {
		return this.value;
	}

	public Object getRawValue() {
		return this.value;
	}

	public String getStringValue() {
		return this.value.toString();
	}

	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY]
	 */
	public String toString() {
		return this.getClass().getCanonicalName()+"="+this.getNumber();
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (!(obj instanceof NumValue)) {
	      return false;
	    }
	    
	    NumValue value = (NumValue) obj;
	    
	    return this.value.equals(value.value);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.value.hashCode();
	    
	    return hash;
	}
}
