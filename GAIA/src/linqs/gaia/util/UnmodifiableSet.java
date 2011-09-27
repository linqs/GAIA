package linqs.gaia.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to wrap lists to make them unmodifiable.
 * 
 * @author namatag
 *
 * @param <C> Contained item type
 */
public class UnmodifiableSet<C> implements Iterable<C> {
	private Set<C> set;
	
	/**
	 * Constructor
	 * 
	 * @param set Set to use
	 */
	public UnmodifiableSet(Set<C> set) {
		this.set = new HashSet<C>(set);
	}
	
	/**
	 * Constructor
	 * 
	 * @param collection Collection to use
	 */
	public UnmodifiableSet(Collection<C> collection) {
		this.set = new HashSet<C>(collection);
	}
	
	public UnmodifiableSet(C[] array) {
		this.set = new HashSet<C>(Arrays.asList(array));
	}
	
	public int size() {
		return this.set.size();
	}
	
	public boolean contains(Object o) {
		return this.set.contains(o);
	}
	
	public boolean containsAll(Collection<?> c) {
		return this.set.containsAll(c);
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof UnmodifiableSet<?>){
			UnmodifiableSet<?> checkobj = (UnmodifiableSet<?>) obj;
			return checkobj.set.equals(this.set);
		} else if(obj instanceof Set<?>){
			// Support equality to set objects
			return this.set.equals(obj);
		}
		
		return false;
	}
	
	public int hashCode() {
		return this.set.hashCode();
	}
	
	public Iterator<C> iterator() {
		return Collections.unmodifiableSet(this.set).iterator();
	}
	
	public boolean isEmpty() {
		return this.set.isEmpty();
	}
	
	public Set<C> copyAsSet() {
		return new HashSet<C>(this.set);
	}
	
	public Object[] toArray() {
		return this.set.toArray();
	}
	
	public Object[] toArray(Object[] a) {
		return this.set.toArray(a);
	}
}
