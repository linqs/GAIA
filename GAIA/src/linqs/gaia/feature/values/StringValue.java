package linqs.gaia.feature.values;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.StringFeature;

/**
 * String value returned by all {@link StringFeature} features.
 * 
 * @author namatag
 *
 */
public class StringValue implements FeatureValue {
	private String stringvalue = null;
	
	/**
	 * Constructor
	 * 
	 * @param value String value
	 */
	public StringValue(String value) {
		if(value == null){
			throw new InvalidAssignmentException("Cannot set null string." +
					"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}
		
		this.stringvalue = value;
	}
	
	/**
	 * Return string value
	 * 
	 * @return String value
	 */
	public String getString() {
		return this.stringvalue;
	}
	
	public Object getRawValue() {
		return this.stringvalue;
	}

	public String getStringValue() {
		return this.stringvalue;
	}
	
	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY]
	 */
	public String toString() {
		return this.getClass().getCanonicalName()+"="+this.getStringValue();
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (!(obj instanceof StringValue)) {
	      return false;
	    }
	    
	   StringValue value = (StringValue) obj;
	    
	    return this.stringvalue.equals(value.stringvalue);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.stringvalue.hashCode();
	    
	    return hash;
	}
}
