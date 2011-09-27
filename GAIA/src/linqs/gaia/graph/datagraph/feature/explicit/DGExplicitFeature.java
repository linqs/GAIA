package linqs.gaia.graph.datagraph.feature.explicit;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.datagraph.DataGraph;

/**
 * Interface for storing values in the features for use
 * with {@link DataGraph}.
 * <p>
 * Note:
 * <UL>
 * <LI> This is a wrapper for the generic explicit features.
 * <LI> Handling of closed features are not done here.  If a feature is unknown,
 * return a {@link FeatureValue#UNKNOWN_VALUE}.  The appropriate action will be taken
 * by the decorable item itself.
 * </UL>
 * @author namatag
 *
 */
public interface DGExplicitFeature extends ExplicitFeature {
	/**
	 * Get feature value
	 * 
	 * @param internalid Internal ID of item
	 * @return Feature value
	 */
	FeatureValue getFeatureValue(Integer internalid);
	
	/**
	 * Set feature value
	 * 
	 * @param internalid Internal ID of item
	 * @param value Feature value
	 */
	void setFeatureValue(Integer internalid, FeatureValue value);
	
	/**
	 * Return the explicit feature wrapped by the DGExplicitFeature
	 * 
	 * @return Wrapped explicit feature
	 */
	ExplicitFeature getOrigFeature();
}
