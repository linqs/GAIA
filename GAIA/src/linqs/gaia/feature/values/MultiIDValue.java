package linqs.gaia.feature.values;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.identifiable.ID;
import linqs.gaia.identifiable.Identifiable;
import linqs.gaia.util.UnmodifiableSet;

/**
 * Feature value objects which contains a set of ID objects.
 * 
 * @see linqs.gaia.feature.MultiIDFeature
 * 
 * @author namatag
 *
 */
public class MultiIDValue implements FeatureValue {
	private UnmodifiableSet<ID> ids = null;
	
	/**
	 * Create a MultiID value the set of IDs
	 * 
	 * @param ids Set of IDs
	 */
	public MultiIDValue(Set<ID> ids) {
		this(new UnmodifiableSet<ID>(ids));
	}
	
	/**
	 * Create a MultiID value the set of IDs
	 * 
	 * @param ids Set of IDs
	 */
	public MultiIDValue(UnmodifiableSet<ID> ids) {
		this.ids = ids;
	}
	
	/**
	 * Create a MultiID value with one ID.
	 * 
	 * @param id ID to store
	 */
	public MultiIDValue(ID id) {
		Set<ID> s = new HashSet<ID>();
		s.add(id);
		
		this.ids = new UnmodifiableSet<ID>(s);
	}
	
	/**
	 * Check to see if the id is in the feature.
	 * 
	 * @param id ID to check
	 * @return True if the ID is in the feature, False otherwise.
	 */
	public boolean hasID(ID id) {
		return this.ids.contains(id);
	}
	
	/**
	 * Check to see if the id if the identifiable object is in the feature.
	 * 
	 * @param id Identifiable object whose ID to check
	 * @return True if the Identifiable object ID is in the feature, False otherwise.
	 */
	public boolean hasID(Identifiable<?> id) {
		return this.ids.contains(id.getID());
	}
	
	/**
	 * Return the set of IDs of this feature value
	 * 
	 * @return Set of ids
	 */
	public UnmodifiableSet<ID> getIDs() {
		return this.ids;
	}
	
	/**
	 * Return the ID of this feature value.
	 * An exception is thrown if the feature value has
	 * more than one ID.
	 * 
	 * @return Set of ids
	 */
	public ID getID() {
		if(this.ids.size() != 1) {
			throw new InvalidStateException("There is more than 1 ID in this feature value: "+this.ids);
		}
		
		return this.ids.iterator().next();
	}
	
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if(this == obj) {
			return true;
		}

		if (!(obj instanceof MultiIDValue)) {
			return false;
		}

		MultiIDValue value = (MultiIDValue) obj;

		return this.ids.equals(value.ids);
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + this.ids.hashCode();

		return hash;
	}
	
	public Object getRawValue() {
		return this.ids;
	}
	
	/**
	 * Return a comma delimited list of ID toString representations.
	 */
	public String getStringValue() {
		String output = null;
		
		for(ID id:this.ids) {
			if(output!=null) {
				output += ",";
			} else {
				output = "";
			}
			
			output += id.toString();
		}
		
		return output;
	}
	
	/**
	 * String representation of feature of the form:<br>
	 * [FEATURE_CLASS]=[CATEGORY]
	 */
	public String toString() {
		return this.getClass().getCanonicalName()+"="+this.getStringValue();
	}
}
