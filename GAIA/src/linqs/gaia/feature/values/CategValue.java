package linqs.gaia.feature.values;

import java.io.Serializable;
import java.util.Arrays;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.util.ArrayUtils;

/**
 * Categorical value returned by all {@link CategFeature} features.
 * 
 * @author namatag
 *
 */
public class CategValue implements FeatureValue, Serializable {
	private static final long serialVersionUID = 1L;
	
	private String value;
	private double[] probs;

	/**
	 * Set the information for the Categorical Value
	 * 
	 * @param value Value of Category for this item
	 * @param probs Probability for each of the possible categories.
	 */
	public CategValue(String value, double[] probs) {
		if(value == null){
			throw new InvalidAssignmentException("Cannot set null category. " +
			"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}

		this.value = value.intern();
		this.probs = probs;
	}

	/**
	 * Set the information for the Categorical Value.
	 * The probability is assumed to be 1 for the categorical value
	 * and 0 for everything else.
	 * 
	 * @param value Value of Category for this item
	 */
	public CategValue(String value) {	
		this(value, null);
	}

	/**
	 * Value of category
	 * 
	 * @return Category value
	 */
	public String getCategory() {
		return this.value;
	}

	/**
	 * Get probabilities over values
	 * 
	 * @return Get probabilities
	 */
	public double[] getProbs() {
		return this.probs;
	}

	/**
	 * Check if the categorical value matches the given category
	 * 
	 * @param cat Category to match
	 * @return True if the category matches the parameter.  False otherwise.
	 */
	public boolean hasCateg(String cat) {
		if(cat==null || this.value == null){
			return false;
		} else {
			return cat.equals(this.value);
		}
	}

	public Object getRawValue() {
		return this.getCategory();
	}

	public String getStringValue() {
		return this.getCategory();
	}

	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY] [PROB]
	 */
	public String toString() {
		String probstring = null;
		double[] probs = this.getProbs();
		if(probs != null) {
			probstring = ArrayUtils.array2String(probs, ",");
		}
		
		return this.getClass().getCanonicalName()+"="+this.getCategory()
			+" ["+probstring+"]";
	}

	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof CategValue)) {
			return false;
		}

		CategValue value = (CategValue) obj;
		
		if(this.probs==value.getProbs()) {
			return this.getCategory().equals(value.getCategory());
		} else if(this.probs==null || value.getProbs()==null) {
			// If only one is null, they're not equal
			return false;
		} else {
			return this.getCategory().equals(value.getCategory()) &&
				Arrays.equals(this.getProbs(), value.getProbs());
		}
	}
	
	public boolean equalsIgnoreProbs(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof CategValue)) {
			return false;
		}

		CategValue value = (CategValue) obj;

		return this.getCategory().equals(value.getCategory());
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.getCategory().hashCode();
		if(this.getProbs()!=null) {
			hash = hash * 31 + Arrays.hashCode(this.getProbs());
		}

		return hash;
	}
}
