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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Data structure for use with summing values for a certain key value. Useful for getting class
 * distribution.
 * 
 * @author namatag
 * 
 */
public class KeyedSum<K> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int numsumover = 0;
    private Map<K, Double> sums;
    private Set<K> keys;
    private double totalSum;

    /**
     * Constructor
     */
    public KeyedSum() {
        sums = Collections.synchronizedMap(new LinkedHashMap<K, Double>());
        keys = Collections.synchronizedSet(new LinkedHashSet<K>());
    }

    /**
     * Add to the sum of the given key. Note: Sum always start at 0.
     * 
     * @param key Key
     * @param value Value to add to sum
     */
    public void add(K key, double value) {
        synchronized (this) {
            double newsum = 0;
            if (sums.containsKey(key)) {
                newsum = sums.get(key);
            }

            newsum += value;
            sums.put(key, newsum);
            keys.add(key);
            totalSum += value;
            numsumover++;
        }
    }

    /**
     * Subtract from the sum of the given key. Note: Sums always start at 0.
     * 
     * @param key Key
     * @param value Value to subtract from sum
     */
    public void subtract(K key, double value) {
        synchronized (this) {
            double newsum = 0;
            if (sums.containsKey(key)) {
                newsum = sums.get(key);
            }

            newsum -= value;
            sums.put(key, newsum);
            keys.add(key);
            totalSum = totalSum - value;
            numsumover--;
        }
    }

    /**
     * Return true value of key was ever used i.e., decremented or incremented
     * 
     * @param key Key
     * @return True if the Key was ever used, False otherwise.
     */
    public boolean hasKey(K key) {
        return keys.contains(key);
    }

    /**
     * Get the list of keys summed
     * 
     * @return Set of String valued keys.
     */
    public Set<K> getKeys() {
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Get the numeric sum of the given key. This will return 0 for values not seen, as well as
     * values seen but with a sum 0 (i.e., incremented and decremented to 0).
     * 
     * @param key String valued key
     * @return sum of given key
     */
    public double getSum(K key) {
        if (sums.containsKey(key)) {
            return sums.get(key);
        }

        return 0;
    }

    /**
     * Get the percent sum of the given key
     * 
     * @param key String valued key
     * @return Percent of given key
     */
    public double getPercent(K key) {
        if (totalSum == 0) {
            return 0;
        }

        return (double) this.getSum(key) / totalSum;
    }

    /**
     * Return the sum of added values.
     * 
     * @return Total number of the sums
     */
    public double totalSum() {
        return this.totalSum;
    }

    /**
     * Return key with highest sum. If there are multiple with the same sum, return a randomly
     * chosen one from among them. A null is returned if no sums are given.
     * 
     * @return Key with highest sum
     */
    public K highestSumKey() {
        List<K> hsk = this.highestSumKeys();

        if (hsk == null || (hsk != null && hsk.isEmpty())) {
            return null;
        }

        return this.highestSumKeys().get(0);
    }

    /**
     * Return keys with highest sum. A null is returned if no sums are given.
     * 
     * @return Keys with highest sum
     */
    public List<K> highestSumKeys() {
        List<K> hskeys = null;

        Set<Entry<K, Double>> entries = sums.entrySet();
        double maxsum = -1;

        for (Entry<K, Double> e : entries) {
            double val = e.getValue();
            K key = e.getKey();
            if (val > maxsum) {
                maxsum = val;
                hskeys = new LinkedList<K>();
                hskeys.add(key);
            } else if (val == maxsum) {
                hskeys.add(key);
            }
        }

        return hskeys;
    }

    /**
     * Return key with lowest sum value. If there are multiple with the same value, return a
     * randomly chosen one.
     * 
     * @return Key with lowest sum
     */
    public K lowestSumKey() {
        List<K> lsk = this.lowestSumKeys();

        if (lsk.isEmpty()) {
            return null;
        }

        return this.lowestSumKeys().get(0);
    }

    /**
     * Return keys with lowest sum
     * 
     * @return Keys with lowest sum
     */
    public List<K> lowestSumKeys() {
        List<K> lskeys = null;

        Set<Entry<K, Double>> entries = sums.entrySet();
        double minsum = Double.POSITIVE_INFINITY;

        for (Entry<K, Double> e : entries) {
            double val = e.getValue();
            K key = e.getKey();
            if (val < minsum) {
                minsum = val;
                lskeys = new LinkedList<K>();
                lskeys.add(key);
            } else if (val == minsum) {
                lskeys.add(key);
            }
        }

        return lskeys;
    }

    /**
     * Reset sums
     */
    public void clearSum() {
        synchronized (this) {
            this.sums.clear();
            this.keys.clear();
            this.totalSum = 0;
            this.numsumover = 0;
        }
    }

    /**
     * Remove the key such that if the value was previously defined, it will not be returned in the
     * set of keys
     * 
     * @param key Key to remove
     */
    public void removeKey(K key) {
        synchronized (this) {
            if (this.sums.containsKey(key)) {
                this.sums.remove(key);
                this.keys.remove(key);
            }
        }
    }

    /**
     * Return number of keys summed
     * 
     * @return Number of keys
     */
    public int numKeys() {
        return this.keys.size();
    }

    /**
     * Return number of summands (i.e., number of times {@link #add} was called minus number of
     * times {@link #subtract} was called).
     * 
     * @return Number of summands
     */
    public int numSummands() {
        return this.numsumover;
    }

    /**
     * Print keyed sum
     */
    public String toString() {
        if (sums.size() == 0) {
            return "Keyed Sum Empty";
        }

        StringBuffer buf = new StringBuffer();
        double highest = this.sums.get(this.highestSumKey());
        double lowest = this.sums.get(this.lowestSumKey());
        List<Entry<K, Double>> entries = new ArrayList<Entry<K, Double>>();
        entries.addAll(this.sums.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Double>>() {
            public int compare(Map.Entry<K, Double> entry, Map.Entry<K, Double> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        for (Entry<K, Double> e : entries) {
            double percent = (int) (this.getPercent(e.getKey()) * 100000);
            percent = (double) percent / 1000;

            buf.append(e.getKey() + "=" + e.getValue() + " (" + percent + "%)");
            if (highest == e.getValue()) {
                buf.append(" MAX");
            } else if (lowest == e.getValue()) {
                buf.append(" MIN");
            }

            buf.append("\n");
        }

        return buf.toString();
    }

    /**
     * Return string representation of sum
     * 
     * @param valseparator Separator of key-value pair (i.e., "=" means "key=value")
     * @param sumseparator Separator of sum pairs (i.e., "," means "key1=value1,key2=value2")
     * @return String representation
     */
    public String toString(String valseparator, String sumseparator) {
        if (sums.size() == 0) {
            return "Keyed Sum Empty";
        }

        StringBuffer buf = null;
        Set<Entry<K, Double>> entries = this.sums.entrySet();
        for (Entry<K, Double> e : entries) {
            if (buf == null) {
                buf = new StringBuffer();
            } else {
                buf.append(sumseparator);
            }

            buf.append(e.getKey() + valseparator + e.getValue());
        }

        return buf.toString();
    }

    public void saveToFile(String file, String keyvalseparator, String countseparator, boolean append) {
        try {

            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);

            boolean isFirst = true;
            Set<Entry<K, Double>> entries = this.sums.entrySet();
            for (Entry<K, Double> e : entries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.write(countseparator);
                }

                out.write(e.getKey() + keyvalseparator + e.getValue());
            }

            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Exception saving counts", e);
        }
    }

    /**
     * Return the keys by inreasing order of its value.
     * 
     * @return Ordered list of keys
     */
    public List<K> getOrderedKeys() {
        List<Entry<K, Double>> entries = new ArrayList<Entry<K, Double>>();
        entries.addAll(this.sums.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Double>>() {
            public int compare(Map.Entry<K, Double> entry, Map.Entry<K, Double> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        List<K> keys = new ArrayList<K>();
        for (Entry<K, Double> e : entries) {
            keys.add(e.getKey());
        }

        return keys;
    }
}
