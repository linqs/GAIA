package linqs.gaia.feature.derived;

import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.util.UnmodifiableList;

/**
 * Abstract class for Multi-Categorical Valued Derived Features
 * 
 * @author namatag
 *
 */
public abstract class DerivedMultiCateg extends BaseDerived implements MultiCategFeature {
	/**
	 * Protected function to calculate values.
	 * Use with getFeatureValue.
	 * 
	 * @param di Decorable item
	 * @return Computed FeatureValue
	 */
	abstract protected FeatureValue calcFeatureValue(Decorable di);
	
	/**
	 * Abstract class to implement to return the list of categories
	 */
	abstract public UnmodifiableList<String> getAllCategories();
	
	/**
	 * Return the number of features defined within this composite feature
	 */
	public int numCategories() {
		return this.getAllCategories().size();
	}
}
