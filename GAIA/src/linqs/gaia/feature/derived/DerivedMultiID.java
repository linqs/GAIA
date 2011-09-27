package linqs.gaia.feature.derived;

import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Abstract class for Multi ID Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedMultiID extends BaseDerived implements MultiIDFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
}
