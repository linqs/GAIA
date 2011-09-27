package linqs.gaia.feature;

import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * A Composite feature is a feature which returns multiple
 * feature values rather than one.  The reason this was
 * added was to allow features which should be calculated
 * together.  For example, calculating the percent of neighbors
 * with a given label over all sets of possible labels
 * would be very inefficient to do one at a time.
 * Calculating all these percents simultaneously, and returning
 * them all at once, is far more efficient.
 * <p>
 * The feature values are of given types and match a given label
 * which indicates a sub type of the feature.  For example,
 * for the percent feature given above, the label might be
 * the corresponding label you're counting.
 * <p>
 * Composite features must return a Composite Value.
 * 
 * @see linqs.gaia.feature.values.CompositeValue
 * 
 * @author namatag
 *
 */
public interface CompositeFeature extends DerivedFeature {
	/**
	 * Return a list of Label-Feature Pairs for
	 * features defined in this composite feature
	 * 
	 * @return List of Label-Feature Pairs
	 */
	UnmodifiableList<SimplePair<String, CVFeature>> getFeatures();
	
	/**
	 * Return size of list of Label-Feature Pairs for
	 * features defined in this composite feature
	 * 
	 * @return Number of features
	 */
	int numFeatures();
}
