package linqs.gaia.feature.explicit;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;

/**
 * Explicit implementation of a multi id feature
 * 
 * @author namatag
 *
 */
public class ExplicitMultiID extends BaseExplicit implements ExplicitFeature, MultiIDFeature {
	public ExplicitMultiID() {
		super();
	}
	
	public ExplicitMultiID(FeatureValue closeddefault) {
		super(closeddefault);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		// Note: Check base first since we need to support UnknownValue
		return super.isValidValue(value) || value instanceof MultiIDValue;
	}

	public Feature copy() {
		return new ExplicitMultiID(this.getClosedDefaultValue());
	}
}
