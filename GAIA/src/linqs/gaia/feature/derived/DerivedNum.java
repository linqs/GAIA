package linqs.gaia.feature.derived;

import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Abstract class for Numeric Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedNum extends BaseDerived implements NumFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
}
