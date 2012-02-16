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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Store object in an List keyed by a given key.
 * Can be useful for hashing or just keeping objects in
 * separate lists by key.
 * 
 * @author namatag
 *
 */
public class KeyedList<K,V> extends KeyedCollection<List<V>,K,V>{
	private Integer defaultsize = null;
	private boolean returnempty = false;
	
	public KeyedList(){
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param defaultsize Default size of newly created lists
	 * @param returnempty If true, return an empty set when getting
	 * a collection which has no items.  Otherwise, return a null.
	 */
	public KeyedList(int defaultsize, boolean returnempty){
		super();
		this.defaultsize = defaultsize;
		this.returnempty = returnempty;
	}
	
	/**
	 * Get list for given object.
	 * 
	 * @param key Key for list
	 * @return List of given key
	 */
	public List<V> getList(K key){
		List<V> list = this.getCollection(key);
		
		if(returnempty) {
			return list==null ? new ArrayList<V>(this.createCollection()) 
					: new ArrayList<V>(list);
		} else {
			return list==null ? null : new ArrayList<V>(list);
		}
	}

	@Override
	protected List<V> createCollection() {
		if(this.defaultsize==null) {
			return Collections.synchronizedList(new ArrayList<V>());
		} else {
			return Collections.synchronizedList(new ArrayList<V>(this.defaultsize));
		}
	}
}
