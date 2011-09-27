package linqs.gaia.feature.values;

/**
 * Value to return whenever the value for a given feature is unknown.
 * A new instance of this should never be created.  Use the {@link FeatureValue#UNKNOWN_VALUE} instance.
 * 
 * @author namatag
 *
 */
public class UnknownValue implements FeatureValue {
	/**
	 * String to print for unknown values
	 */
	public static final String UNKNOWN_VALUE="UNKNOWN_VALUE";
	
	/**
	 * Constructor
	 */
	protected UnknownValue() {
		
	}
	
	public Object getRawValue() {
		return UNKNOWN_VALUE;
	}

	public String getStringValue() {
		return UNKNOWN_VALUE;
	}
	
	/**
	 * String representation of feature
	 */
	public String toString() {
		return UNKNOWN_VALUE;
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj || obj instanceof UnknownValue) {
			return true;
		}

		return false;
	}

	public int hashCode() {
		int hash = 1;

		return hash;
	}
}
