package linqs.gaia.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class to wrap lists to make them unmodifiable.
 * 
 * @author namatag
 *
 * @param <C> Contained item type
 */
public class UnmodifiableList<C> implements Iterable<C>, Serializable {
	private static final long serialVersionUID = 1L;
	private List<C> list;
	
	/**
	 * Constructor
	 * 
	 * @param list List to use
	 */
	public UnmodifiableList(List<C> list) {
		this.list = new ArrayList<C>(list);
	}
	
	/**
	 * Constructor
	 * 
	 * @param collection Collection to use
	 */
	public UnmodifiableList(Collection<C> collection) {
		this.list = new ArrayList<C>(collection);
	}
	
	public UnmodifiableList(C[] array) {
		this.list = Arrays.asList(array);
	}
	
	public C get(int index) {
		return this.list.get(index);
	}
	
	public int size() {
		return this.list.size();
	}
	
	public boolean contains(Object o) {
		return this.list.contains(o);
	}
	
	public boolean containsAll(Collection<?> c) {
		return this.list.containsAll(c);
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof UnmodifiableList<?>){
			UnmodifiableList<?> checkobj = (UnmodifiableList<?>) obj;
			return checkobj.list.equals(this.list);
		} else if(obj instanceof List<?>){
			// Support equality to list objects
			return this.list.equals(obj);
		}
		
		return false;
	}
	
	public int hashCode() {
		return this.list.hashCode();
	}
	
	public Iterator<C> iterator() {
		return Collections.unmodifiableList(this.list).iterator();
	}
	
	public boolean isEmpty() {
		return this.list.isEmpty();
	}
	
	public List<C> copyAsList() {
		return new ArrayList<C>(this.list);
	}
	
	public Object[] toArray() {
		return this.list.toArray();
	}
	
	public Object[] toArray(Object[] a) {
		return this.list.toArray(a);
	}
	
	public int indexOf(C o) {
		return this.list.indexOf(o);
	}
	
	public int lastIndexOf(C o) {
		return this.list.lastIndexOf(o);
	}
	
	public List<C> subList(int fromIndex, int toIndex) {
		return Collections.unmodifiableList(this.list).subList(fromIndex, toIndex);
	}
}
