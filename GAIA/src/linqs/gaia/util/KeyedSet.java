package linqs.gaia.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Store object in an Set keyed by a given key.
 * Can be useful for hashing or just keeping objects in
 * separate sets by key.
 * 
 * @author namatag
 *
 */
public class KeyedSet<K,V> extends KeyedCollection<Set<V>,K,V> {
	private Integer defaultsize = null;
	private boolean returnempty = false;
	
	public KeyedSet(){
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param defaultsize Default size of newly created sets
	 * @param returnempty If true, return an empty set when getting
	 * a collection which has no items.  Otherwise, return a null.
	 */
	public KeyedSet(int defaultsize, boolean returnempty){
		super();
		this.defaultsize = defaultsize;
		this.returnempty = returnempty;
	}
	
	/**
	 * Get set for given object.
	 * 
	 * @param key Key for set
	 * @return A Set corresponding to the specified key.
	 */
	public Set<V> getSet(K key){
		Set<V> set = this.getCollection(key);
		
		if(returnempty) {
			return set==null ? new HashSet<V>(this.createCollection())
					: new HashSet<V>(set);
		} else {
			return new HashSet<V>(set);
		}
	}

	@Override
	protected Set<V> createCollection() {
		if(defaultsize==null) {
			return new HashSet<V>();
		} else {
			return new HashSet<V>(this.defaultsize);
		}
	}
}
