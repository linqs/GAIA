package linqs.gaia.feature.explicit;

import java.util.List;

import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.util.UnmodifiableList;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Explicit implementation of a multi value categorical feature
 * <p>
 * Note: This feature assumes that the categories can be stored in memory.
 * 
 * @author namatag
 *
 */
public class ExplicitMultiCateg extends BaseExplicit implements ExplicitFeature, MultiCategFeature {
	private UnmodifiableList<String> categories;
	
	public ExplicitMultiCateg(UnmodifiableList<String> categories, FeatureValue closeddefault){
		if(categories.isEmpty()){
			throw new InvalidAssignmentException("No categories defined");
		}
		
		this.categories = categories;
		this.initialize(closeddefault);
	}
	
	public ExplicitMultiCateg(UnmodifiableList<String> categories){
		this(categories, null);
	}
	
	public ExplicitMultiCateg(List<String> categories){
		this(new UnmodifiableList<String>(categories), null);
	}
	
	public ExplicitMultiCateg(List<String> categories, FeatureValue closeddefault){
		this(new UnmodifiableList<String>(categories), closeddefault);
	}
	
	public ExplicitMultiCateg(String[] categories){
		this(new UnmodifiableList<String>(categories), null);
	}
	
	public ExplicitMultiCateg(String[] categories, FeatureValue closeddefault){
		this(new UnmodifiableList<String>(categories), closeddefault);
	}
	
	public boolean isValidValue(FeatureValue value) {
		// If the base says its valid, return true.
		// Note: Check base first since we need to support UnknownValue
		if(super.isValidValue(value)) {
			return true;
		}
		
		// If value is of the wrong type, return false
		if(value==null || !(value instanceof MultiCategValue)) {
			return false;
		}
		
		// Return true if its one of the valid categories.  False otherwise.
		MultiCategValue mcvalue = (MultiCategValue) value;
		UnmodifiableSet<String> cats = mcvalue.getCategories();
		for(String cat:cats) {
			if(!this.categories.contains(cat)) {
				return false;
			}
		}
		
		double[] probs = mcvalue.getProbs();
		if(probs!=null && probs.length!=categories.size()) {
			return false;
		}
		
		return true;
	}

	public UnmodifiableList<String> getAllCategories() {
		return this.categories;
	}

	public Feature copy() {
		return new ExplicitMultiCateg(this.getAllCategories(), this.getClosedDefaultValue());
	}
	
	public int numCategories() {
		return this.categories.size();
	}
}
