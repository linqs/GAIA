/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
