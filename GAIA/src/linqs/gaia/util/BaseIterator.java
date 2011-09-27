package linqs.gaia.util;

import java.util.Iterator;

/**
 * Abstract class to simplify creating iterators over objects
 * 
 * @author namatag
 *
 * @param <E> Type of object the iterator is over
 */
public abstract class BaseIterator<E> implements Iterator<E> {
	protected E next = null;
	private boolean shouldInitialize = true;
	
	/**
	 * Returns the next item of the iterator or
	 * null if there are no other items in the iterator.
	 * 
	 * @return Next item
	 */
	public abstract E getNext();
	
	public boolean hasNext() {
		// Initialize, if needed
		if(shouldInitialize) {
			initialize();
		}
		
		// Check to see if there is a next one stored
		if(next == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public E next() {
		// Initialize, if needed
		if(shouldInitialize) {
			initialize();
		}
		
		E prevnext = next;
		next = this.getNext();
		
		return prevnext; 
	}
	
	/**
	 * Initialize the iterator by setting
	 * the first value for the iterator
	 */
	private void initialize() {
		shouldInitialize = false;
		next = this.getNext();
	}
	
	/**
	 * The default iterator does not perform
	 * anything on remove.  Overwrite this method
	 * depending on your requirements.
	 */
	public void remove() {
		// Do nothing
	}
}
