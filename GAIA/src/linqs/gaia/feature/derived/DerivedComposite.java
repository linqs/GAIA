package linqs.gaia.feature.derived;

import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * Abstract class for Categorical Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedComposite extends BaseDerived implements CompositeFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
	
	/**
	 * Abstract class to implement to return the list features defined in this composite feature
	 */
	abstract public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures();
	
	/**
	 * Return the number of features defined within this composite feature
	 */
	public int numFeatures() {
		return this.getFeatures().size();
	}
}
