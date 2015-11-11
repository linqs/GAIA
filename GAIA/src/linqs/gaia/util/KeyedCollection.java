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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.exception.InvalidOperationException;

/**
 * Abstract class for storing object in a keyed collection
 * 
 * @see KeyedList
 * @see KeyedSet
 * 
 * @author namatag
 * 
 */
public abstract class KeyedCollection<C extends Collection<V>, K, V> {
    protected Map<K, C> keyedcollections;

    protected abstract C createCollection();

    public KeyedCollection() {
        keyedcollections = new ConcurrentHashMap<K, C>(1);
    }

    /**
     * Add item to a collection keyed by the given value.
     * 
     * @param key
     *            Key of the collection the object should be added to
     * @param value
     *            Value you want added to the specified collection
     */
    public synchronized void addItem(K key, V value) {
        C currcollection = null;

        if (keyedcollections.containsKey(key)) {
            currcollection = keyedcollections.get(key);
        } else {
            currcollection = this.createCollection();
        }

        currcollection.add(value);
        keyedcollections.put(key, currcollection);
    }

    /**
     * Get collection for given key.
     * 
     * @param key
     *            Key
     * @return Collection of given key
     */
    public C getCollection(K key) {
        return keyedcollections.get(key);
    }

    /**
     * Remove item to a collection keyed by the given value.<br>
     * Note: If a value is removed from a key not previously specified, an
     * exception is thrown.
     * 
     * @param key
     *            Key of the collection the object should be removed from
     * @param value
     *            Value you want removed from the specified collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     */
    public synchronized boolean removeItem(K key, V value) {
        C currcollection = null;

        if (keyedcollections.containsKey(key)) {
            currcollection = keyedcollections.get(key);
            return currcollection.remove(value);
        } else {
            throw new InvalidOperationException("Key not previously specified: " + key
                    + " in key=value pair: " + key + "=" + value);
        }
    }

    /**
     * Get the list of keys.
     * 
     * @return Set of keys.
     */
    public Set<K> getKeys() {
        return this.keyedcollections.keySet();
    }

    /**
     * Return true if the key is defined
     * 
     * @param key
     *            Key to check
     * @return True if defined, false otherwise.
     */
    public boolean hasKey(K key) {
        return this.keyedcollections.containsKey(key);
    }

    /**
     * Total number of items in all the keyed collections
     * 
     * @return Number of items in all collections
     */
    public int totalNumItems() {
        int total = 0;
        Set<Entry<K, C>> entries = this.keyedcollections.entrySet();
        for (Entry<K, C> entry : entries) {
            total += entry.getValue().size();
        }

        return total;
    }

    /**
     * Return the number of defined keys
     * 
     * @return Number of keys
     */
    public int numKeys() {
        return this.keyedcollections.keySet().size();
    }

    /**
     * Return all items in all the keyed collections
     * 
     * @return Items in all collections
     */
    public Set<V> getAllItems() {
        HashSet<V> allitems = new HashSet<V>();
        Set<Entry<K, C>> entries = this.keyedcollections.entrySet();
        for (Entry<K, C> entry : entries) {
            allitems.addAll(entry.getValue());
        }

        return allitems;
    }

    /**
     * Get list for given object.
     * 
     * @return All defined lists
     */
    public List<C> getAllCollections() {
        List<C> lists = new ArrayList<C>();
        Collection<C> cvals = this.keyedcollections.values();
        lists.addAll(cvals);

        return lists;
    }

    /**
     * Remove collection specified by key.
     * 
     * @param key
     *            Key
     * @return previous value associated with specified key, or <tt>null</tt> if
     *         there was no mapping for key.
     */
    public synchronized C remove(K key) {
        return this.keyedcollections.remove(key);
    }

    /**
     * Remove all collections
     */
    public synchronized void removeAll() {
        this.keyedcollections.clear();
    }

    /**
     * Return String representation of keyed collection.
     */
    public String toString() {
        Map<K, C> map = this.keyedcollections;

        StringBuffer buf = new StringBuffer();
        buf.append("# of Keys defined: " + map.keySet().size() + "\n");
        Set<Entry<K, C>> entries = map.entrySet();
        for (Entry<K, C> entry : entries) {
            LinkedList<V> list = new LinkedList<V>(entry.getValue());
            buf.append(entry.getKey() + "[size=" + list.size() + "]: " + ListUtils.list2string(list, ",") + "\n");
        }

        return buf.toString();
    }
}
