package linqs.gaia.feature;

/**
 * Base feature interface which all Features must implement.
 * <p>
 * For all features, return a null value if the value is missing.
 * 
 * @author namatag
 *
 */
public interface Feature {
	/**
	 * Return a copy of the feature
	 * 
	 * @return Copy of feature
	 */
	public Feature copy();
}
