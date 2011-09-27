package linqs.gaia.feature.values;

import java.util.Arrays;
import java.util.Set;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Categorical values returned by all {@link MultiCategFeature} features.
 * 
 * @author namatag
 *
 */
public class MultiCategValue implements FeatureValue {
	private Set<String> values;
	private double[] probs;

	/**
	 * Set the information for the Categorical Value
	 * 
	 * @param values Category values for this item
	 * @param probs Probability for each of the possible categories
	 * (i.e., this has a double for each possible category).
	 */
	public MultiCategValue(Set<String> values, double[] probs) {
		if(values == null){
			throw new InvalidAssignmentException("Cannot set null categories." +
			"Use FeatureValue.UNKNOWN_VALUE for unknown value.");
		}

		this.values = values;
		this.probs = probs;
	}

	/**
	 * Set the information for the Categorical Value
	 * The probability is assumed to be 1 for the categorical values
	 * and 0 for everything else.
	 * 
	 * @param values Category values for this item
	 */
	public MultiCategValue(Set<String> values) {
		this(values, null);
	}

	/**
	 * Iterator over the values of category
	 * 
	 * @return Iterator over category values
	 */
	public UnmodifiableSet<String> getCategories() {
		return new UnmodifiableSet<String>(this.values);
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
		if(cat==null || this.values == null){
			return false;
		} else {
			return this.values.contains(cat);
		}
	}

	public Object getRawValue() {
		return this.getCategories();
	}

	public String getStringValue() {
		return ArrayUtils.array2String(this.values.toArray(), ",");
	}

	/**
	 * String representation of feature of the form:<br>
	 * FEATURE_CLASS=CATEGORY1,...,CATEGORYK [PROBCATEGORY1,...,PROBCATEGORYK]
	 */
	public String toString() {
		String probstring = null;
		double[] probs = this.getProbs();
		if(probs != null) {
			probstring = ArrayUtils.array2String(probs, ",");
		}
		
		return this.getClass().getCanonicalName()+"="+this.getStringValue()+" ["+probstring+"]";
	}

	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof MultiCategValue)) {
			return false;
		}

		MultiCategValue value = (MultiCategValue) obj;
		
		if(this.probs==value.getProbs()) {
			return this.values.equals(value.values);
		} else if(this.probs==null || value.getProbs()==null) {
			// If only one is null, they're not equal
			return false;
		} else {
			return this.values.equals(value.values) &&
				Arrays.equals(this.getProbs(), value.getProbs());
		}
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.getCategories().hashCode();
		if(this.getProbs()!=null) {
			hash = hash * 31 + Arrays.hashCode(this.getProbs());
		}

		return hash;
	}
}
