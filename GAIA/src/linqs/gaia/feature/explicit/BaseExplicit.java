package linqs.gaia.feature.explicit;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.values.FeatureValue;

/**
 * Base implementation of explicit class to simplify implementations
 * of explicit features.
 * 
 * @author namatag
 *
 */
public abstract class BaseExplicit implements ExplicitFeature {
	private FeatureValue closeddefault;
	
	/**
	 * Default is an open explicit feature
	 */
	protected BaseExplicit(){
		this(null);
	}
	
	protected BaseExplicit(FeatureValue closeddefault){
		initialize(closeddefault);
	}
	
	/**
	 * Initialize base explicit.  Called from base constructor and is done
	 * this way since for it to work for Explicit Categorical values,
	 * the category must be assigned prior to initializing.  Otherwise,
	 * an exception is thrown when we check if the value is valid.
	 * 
	 * @param closeddefault
	 */
	protected void initialize(FeatureValue closeddefault) {
		if(closeddefault != null && !this.isValidValue(closeddefault)){
			throw new InvalidAssignmentException("Invalid Value Specified: "+closeddefault);
		}
		
		this.closeddefault = closeddefault;
	}
	
	/**
	 * Unknown value is a valid value for all categorical features
	 */
	public boolean isValidValue(FeatureValue value) {
		return value == FeatureValue.UNKNOWN_VALUE;
	}
	
	public FeatureValue getClosedDefaultValue() {
		return this.closeddefault;
	}

	public boolean isClosed() {
		return closeddefault != null;
	}
}
