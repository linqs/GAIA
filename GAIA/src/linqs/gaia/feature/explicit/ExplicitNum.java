package linqs.gaia.feature.explicit;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;

/**
 * Explicit implementation of a numeric feature
 * 
 * @author namatag
 *
 */
public class ExplicitNum extends BaseExplicit implements ExplicitFeature, NumFeature {
	public ExplicitNum() {
		super();
	}
	
	public ExplicitNum(FeatureValue closeddefault) {
		super(closeddefault);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		// Note: Check base first since we need to support UnknownValue
		return super.isValidValue(value) || value instanceof NumValue;
	}

	public Feature copy() {
		return new ExplicitNum(this.getClosedDefaultValue());
	}
}
