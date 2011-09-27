package linqs.gaia.feature;

import linqs.gaia.feature.values.FeatureValue;

/**
 * Interface {@link Feature} for all features where the value
 * is explicitly specified by the user.
 * <p>
 * There are two types of explicit features: Open and Closed.
 * An open explicit feature means that there maybe missing values.
 * Any value not explicitly set is assumed to be missing.
 * A closed explicit feature means that all values are known
 * for this feature.  Any value not explicitly set is assumed
 * to be some predefined default value.  The distinction is often
 * made in order to save memory or disk space.  If there is a huge
 * skew in the values toward one value, not having to explicitly
 * store that value can result in huge savings.
 *  
 * @author namatag
 *
 */
public interface ExplicitFeature extends Feature {
	/**
	 * Check to see if the object is a valid value for this feature
	 * 
	 * @param value Value whose value we're checking
	 * @return True if it is valid, False otherwise
	 */
	public boolean isValidValue(FeatureValue value);
	
	/**
	 * Check to see if the feature is open or close
	 * 
	 * @return True if closed.  False otherwise.
	 */
	public boolean isClosed();
	
	/**
	 * Get the default value to use if the feature
	 * is a closed feature
	 * 
	 * @return Default feature value
	 */
	public FeatureValue getClosedDefaultValue();
}
