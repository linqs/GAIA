package linqs.gaia.feature.derived;

import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Abstract class for String Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedString extends BaseDerived implements StringFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
}
