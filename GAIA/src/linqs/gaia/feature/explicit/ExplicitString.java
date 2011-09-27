package linqs.gaia.feature.explicit;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;

/**
 * Explicit implementation of a string feature
 * 
 * @author namatag
 *
 */
public class ExplicitString extends BaseExplicit implements ExplicitFeature, StringFeature {
	public ExplicitString() {
		super();
	}
	
	public ExplicitString(FeatureValue closeddefault) {
		super(closeddefault);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		// Note: Check base first since we need to support UnknownValue
		return super.isValidValue(value) || value instanceof StringValue;
	}

	public Feature copy() {
		return new ExplicitString(this.getClosedDefaultValue());
	}
}
