package linqs.gaia.feature.values;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Base interface for the value of a feature corresponding to
 * a specific {@link Decorable} object.
 * <p>
 * Note:
 * <UL>
 * <LI>A {@link FeatureValue} can only be set during construction.
 * <LI>{@link FeatureValue#UNKNOWN_VALUE} should be returned for items where a feature is not defined.
 * </UL>
 * 
 * @author namatag
 *
 */
public interface FeatureValue {
	
	public static FeatureValue UNKNOWN_VALUE = new UnknownValue();
	
	/**
	 * Return the object value of the feature.
	 * For use when you want the value of feature for an object
	 * for things like equality checking or printing
	 * where you don't necessarily need to know the specific
	 * value type.
	 * 
	 * @return Object representation of the value
	 */
	Object getRawValue();
	
	/**
	 * Return a string representation of the value
	 * 
	 * @return String representation of the value
	 */
	String getStringValue();
}
