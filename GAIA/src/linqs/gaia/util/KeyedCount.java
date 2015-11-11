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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
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
 * Data structure for use with counting the number of times a certain String value appears. Useful
 * for getting class distribution..
 * 
 * @author namatag
 * 
 */
public class KeyedCount<K> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<K, Integer> counts;
    private Set<K> keys;
    private int totalCount;
    private KeyedSample<K, String> samples;

    /**
     * Constructor
     */
    public KeyedCount() {
        counts = Collections.synchronizedMap(new LinkedHashMap<K, Integer>());
        keys = Collections.synchronizedSet(new LinkedHashSet<K>());
        totalCount = 0;
    }

    public KeyedCount(int numSamples) {
        this();
        samples = new KeyedSample<K, String>(numSamples);
    }

    /**
     * Increment the count for the given key. Note: Counts always start at 0.
     * 
     * @param key Key
     */
    public void increment(K key) {
        this.increment(key, 1);
    }

    public void increment(K key, String sample) {
        this.increment(key, 1);

        if (this.samples != null) {
            this.samples.addSample(key, sample);
        }
    }

    /**
     * Increment the count for the given key by the specified number of times. This is equivalent to
     * calling {@link #increment} the same number of times as numincrement. Note: Counts always
     * start at 0.
     * 
     * @param key Key
     * @param numincrement Amount to increment count
     */
    public void increment(K key, int numincrement) {
        synchronized (this) {
            int newcount = 0;
            if (counts.containsKey(key)) {
                newcount = counts.get(key);
            }

            newcount += numincrement;
            counts.put(key, newcount);
            keys.add(key);
            totalCount += numincrement;
        }
    }

    /**
     * Decrement the count for the given key. Note: Counts always start at 0.
     * 
     * @param key Key
     */
    public void decrement(K key) {
        synchronized (this) {
            int newcount = 0;
            if (counts.containsKey(key)) {
                newcount = counts.get(key);
            }

            newcount--;
            counts.put(key, newcount);
            keys.add(key);
            totalCount--;
        }
    }

    /**
     * Decrement the count for the given key. Note: Counts always start at 0.
     * 
     * @param key Key
     * @param numdecrement Amount to increment count
     */
    public void decrement(K key, int numdecrement) {
        synchronized (this) {
            int newcount = 0;
            if (counts.containsKey(key)) {
                newcount = counts.get(key);
            }

            newcount -= numdecrement;
            counts.put(key, newcount);
            keys.add(key);
            totalCount -= numdecrement;
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
     * Get the set of keys counted
     * 
     * @return Set of String valued keys.
     */
    public Set<K> getKeys() {
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Get the numeric count of the given key. This will return 0 for values not seen, as well as
     * values seen but with a count 0 (i.e., incremented and decremented to 0).
     * 
     * @param key String valued key
     * @return Count of given key
     */
    public int getCount(K key) {
        if (counts.containsKey(key)) {
            return counts.get(key);
        }

        return 0;
    }

    public List<String> getSamples(K key) {
        if (samples == null) {
            return null;
        }

        return samples.getSamples(key);
    }

    /**
     * Get all counts of this KeyedCount
     * 
     * @return Map where the key is each item and the value is the count of that item
     */
    public Map<K, Integer> getAllCounts() {
        return new LinkedHashMap<K, Integer>(this.counts);
    }

    /**
     * Get the percent count of the given key
     * 
     * @param key String valued key
     * @return Percent of given key
     */
    public double getPercent(K key) {
        if (totalCount == 0) {
            return 0;
        }

        return (double) this.getCount(key) / (double) totalCount;
    }

    /**
     * Return the number of counted values.
     * 
     * @return Total number of the counts
     */
    public int totalCounted() {
        return this.totalCount;
    }

    /**
     * Return key with highest count. If there are multiple with the same count, return a randomly
     * chosen one from among them. A null is returned if no counts are given.
     * 
     * @return Key with highest count
     */
    public K highestCountKey() {
        List<K> hck = this.highestCountKeys();

        if (hck == null || (hck != null && hck.isEmpty())) {
            return null;
        }

        return this.highestCountKeys().get(0);
    }

    /**
     * Return keys with highest count. A null is returned if no counts are given.
     * 
     * @return Keys with highest count
     */
    public List<K> highestCountKeys() {
        List<K> hckeys = null;

        Set<Entry<K, Integer>> entries = counts.entrySet();
        double maxcount = -1;

        for (Entry<K, Integer> e : entries) {
            double val = e.getValue();
            K key = e.getKey();
            if (val > maxcount) {
                maxcount = val;
                hckeys = new LinkedList<K>();
                hckeys.add(key);
            } else if (val == maxcount) {
                hckeys.add(key);
            }
        }

        return hckeys;
    }

    public List<K> topKKeys(int k) {
        TopK<K> topk = new TopK<K>(k);
        Set<Map.Entry<K, Integer>> entries = counts.entrySet();
        for (Map.Entry<K, Integer> e : entries) {
            topk.add(0.0 + e.getValue(), e.getKey());
        }

        return topk.getTopKSorted();
    }

    /**
     * Return key with lowest count If there are multiple with the same value, return a randomly
     * chosen one.
     * 
     * @return Key with lowest count
     */
    public K lowestCountKey() {
        List<K> lck = this.lowestCountKeys();

        if (lck.isEmpty()) {
            return null;
        }

        return this.lowestCountKeys().get(0);
    }

    /**
     * Return keys with lowest count
     * 
     * @return Keys with lowest count
     */
    public List<K> lowestCountKeys() {
        List<K> lckeys = null;

        Set<Entry<K, Integer>> entries = counts.entrySet();
        double mincount = Integer.MAX_VALUE;

        for (Entry<K, Integer> e : entries) {
            double val = e.getValue();
            K key = e.getKey();
            if (val < mincount) {
                mincount = val;
                lckeys = new LinkedList<K>();
                lckeys.add(key);
            } else if (val == mincount) {
                lckeys.add(key);
            }
        }

        return lckeys;
    }

    /**
     * Reset counts
     */
    public void clearCounts() {
        this.counts.clear();
        this.keys.clear();
        this.totalCount = 0;
    }

    /**
     * Remove the key such that if the value was previously defined, it will not be returned in the
     * set of keys
     * 
     * @param key Key to remove
     */
    public void removeKey(K key) {
        synchronized (this) {
            if (this.counts.containsKey(key)) {
                this.totalCount = this.totalCount - this.counts.get(key);
                this.counts.remove(key);
                this.keys.remove(key);
            }
        }
    }

    /**
     * Remove keys whose count is strictly below the specified threshold
     * 
     * @param threshold Count threshold
     */
    public void removeKeysBelowThreshold(int threshold) {
        Set<K> keys = this.getKeys();
        List<K> toremove = new LinkedList<K>();
        for (K k : keys) {
            if (this.getCount(k) < threshold) {
                toremove.add(k);
            }
        }

        for (K k : toremove) {
            this.removeKey(k);
        }
    }

    /**
     * Remove keys whose count is strictly above the specified threshold
     * 
     * @param threshold Count threshold
     */
    public void removeKeysAboveThreshold(int threshold) {
        Set<K> keys = this.getKeys();
        List<K> toremove = new LinkedList<K>();
        for (K k : keys) {
            if (this.getCount(k) > threshold) {
                toremove.add(k);
            }
        }

        for (K k : toremove) {
            this.removeKey(k);
        }
    }

    /**
     * Return number of keys counted
     * 
     * @return Number of keys
     */
    public int numKeys() {
        return this.keys.size();
    }

    /**
     * Return the keys by inreasing order of its value.
     * 
     * @return Ordered list of keys
     */
    public List<K> getOrderedKeys() {
        List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
        entries.addAll(this.counts.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        List<K> keys = new ArrayList<K>();
        for (Entry<K, Integer> e : entries) {
            keys.add(e.getKey());
        }

        return keys;
    }

    /**
     * Print keyed count
     */
    public String toString() {
        if (counts.size() == 0) {
            return "Keyed Count Empty";
        }

        StringBuffer buf = new StringBuffer();
        int highest = this.counts.get(this.highestCountKey());
        int lowest = this.counts.get(this.lowestCountKey());
        List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
        entries.addAll(this.counts.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        for (Entry<K, Integer> e : entries) {
            double percent = (int) (this.getPercent(e.getKey()) * 100000);
            percent = (double) percent / 1000;

            buf.append(e.getKey() + "\t=\t" + e.getValue() + "\t(\t" + percent + "\t%\t)");
            if (highest == e.getValue()) {
                buf.append("\tMAX");
            } else if (lowest == e.getValue()) {
                buf.append("\tMIN");
            }

            if (samples != null) {
                buf.append("\t" + samples.getSamples(e.getKey()));
            }

            buf.append("\n");
        }

        return buf.toString();
    }

    /**
     * Return string representation of count
     * 
     * @param keyvalseparator Separator of key-value pair (i.e., "=" means "key=value")
     * @param countseparator Separator of count pairs (i.e., "," means "key1=value1,key2=value2")
     * @return String representation
     */
    public String toString(String keyvalseparator, String countseparator) {
        return toString(keyvalseparator, countseparator, false);
    }

    /**
     * Return string representation of count
     * 
     * @param keyvalseparator Separator of key-value pair (i.e., "=" means "key=value")
     * @param countseparator Separator of count pairs (i.e., "," means "key1=value1,key2=value2")
     * @param reverseOrder If true, print larger counts before smaller counts
     * @return String representation
     */
    public String toString(String keyvalseparator, String countseparator, boolean reverseOrder) {
        String countsstring = null;
        List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
        entries.addAll(this.counts.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        if (reverseOrder) {
            java.util.Collections.reverse(entries);
        }

        for (Entry<K, Integer> e : entries) {
            if (countsstring == null) {
                countsstring = "";
            } else {
                countsstring += countseparator;
            }

            countsstring += e.getKey() + keyvalseparator + e.getValue();
        }

        return countsstring;
    }

    /**
     * Return string representation of count in the form:
     * [KEY][FIELDSEPARATOR][COUNT][FIELDSEPARATOR][[SAMPLE][[SAMPLESEPARATOR][SAMPLE]]]
     * 
     * @param fieldSeparator Separator of key-value pair (i.e., "=" means "key=value")
     * @param countSeparator Separator of count pairs (i.e., "," means "key1=value1,key2=value2")
     * @param sampleSeparator Separator for samples
     * @return String representation
     */
    public String toString(String fieldSeparator, String countSeparator, String sampleSeparator) {
        StringBuffer countsstring = null;
        List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
        entries.addAll(this.counts.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        for (Entry<K, Integer> e : entries) {
            if (countsstring == null) {
                countsstring = new StringBuffer();
            } else {
                countsstring.append(countSeparator);
            }

            countsstring.append(e.getKey()
                    + fieldSeparator + e.getValue()
                    + fieldSeparator + (samples != null ? ListUtils.list2string(this.samples.getSamples(e.getKey()), sampleSeparator) : "NoSamples"));
        }

        return countsstring == null ? null : countsstring.toString();
    }

    /**
     * Returns a string representation of the count where each sample is placed in a separate line.
     * 
     * @param fieldSeparator Delimiter between fields
     * @param sampleSeparator Delimiter between samples
     * @return String representation
     */
    public String toSampleString(String fieldSeparator, String sampleSeparator) {
        StringBuffer buffer = new StringBuffer();
        List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
        entries.addAll(this.counts.entrySet());

        // Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });

        for (Entry<K, Integer> e : entries) {
            if (samples == null) {
                if (buffer.length() > 0) {
                    buffer.append(sampleSeparator);
                }

                buffer.append(e.getKey()
                        + fieldSeparator + e.getValue()
                        + fieldSeparator + "NoSamples");
            } else {
                List<String> allSamples = this.samples.getSamples(e.getKey());
                for (String s : allSamples) {
                    if (buffer.length() > 0) {
                        buffer.append(sampleSeparator);
                    }

                    buffer.append(e.getKey()
                            + fieldSeparator + e.getValue()
                            + fieldSeparator + s);
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Override count for a specific key
     * 
     * @param key Key for count
     * @param count Count value
     */
    public void setCount(K key, int count) {
        synchronized (this) {
            int oldcount = this.getCount(key);
            int diff = oldcount - count;
            this.totalCount = this.totalCount - diff;

            this.counts.put(key, count);
            this.keys.add(key);
        }
    }

    /**
     * Update the total count counter. For use when you want to update the way percentage is
     * computed.
     * 
     * @param totalcount Total count value to use
     */
    public void setTotalCount(int totalcount) {
        this.totalCount = totalcount;
    }

    /**
     * Return a KeyedCount where the number of a particular count in this KeyedCount is now being
     * used as the key. i.e. Count the number of values which have a given count
     * 
     * @return Inverted KeyedCount
     */
    public KeyedCount<Integer> invert() {
        KeyedCount<Integer> inverted = new KeyedCount<Integer>();
        Set<Entry<K, Integer>> entries = this.counts.entrySet();

        for (Entry<K, Integer> e : entries) {
            inverted.increment(e.getValue());
        }

        return inverted;
    }

    /**
     * Save counts to a file in the form of: [KEY][FIELDSEPARATOR][COUNT][COUNTSEPARATOR]
     * 
     * @param file File to save to
     * @param fieldSeparator Field Separator (e.g., \t)
     * @param countseparator Count Separator (e.g., \n)
     * @param append If we should append or overwrite the target file
     */
    public void saveToFile(String file, String fieldSeparator, String countseparator, boolean append) {
        try {
            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);

            boolean isFirst = true;
            Set<Entry<K, Integer>> entries = this.counts.entrySet();
            for (Entry<K, Integer> e : entries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.write(countseparator);
                }

                out.write(e.getKey() + fieldSeparator + e.getValue());
            }

            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Exception saving counts", e);
        }
    }

    /**
     * Get the minimum percentage count value
     * 
     * @return Minimum percentage
     */
    public double getMinPct() {
        double min = 2;
        Set<K> keys = this.getKeys();
        for (K k : keys) {
            if (min > this.getPercent(k)) {
                min = this.getPercent(k);
            }
        }

        return min;
    }

    /**
     * Save the file with samples
     * 
     * @param file File to save to
     * @param fieldSeparator Field Separator (e.g., \t)
     * @param countSeparator Count Separator (e.g., \n)
     * @param append If we should append or overwrite the target file
     * @param samples Keyed Samples file containing samples to include per key
     */
    public void saveToUTF8File(String file, String fieldSeparator,
            String countSeparator, String sampleSeparator, boolean append, KeyedSample<K, String> samples) {
        saveToUTF8File(file, fieldSeparator, countSeparator, sampleSeparator, append, samples, false);
    }

    public void saveToUTF8File(String file, String keyvalseparator,
            String countseparator, String sampleSeparator,
            boolean append, KeyedSample<K, String> samples, boolean reverseOrder) {
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8"));

            boolean isFirst = true;
            Set<Entry<K, Integer>> entrySet = this.counts.entrySet();

            List<Entry<K, Integer>> entries = new ArrayList<Entry<K, Integer>>();
            entries.addAll(entrySet);

            // Sort the list using an anonymous inner class implementing Comparator for the compare method
            java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>() {
                @SuppressWarnings({
                        "rawtypes", "unchecked"
                })
                public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
                {
                    // Return 0 for a match, -1 for less than and +1 for greater than
                    int match = (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
                    if (match == 0 && entry.getKey() instanceof Comparable && entry1.getKey() instanceof Comparable) {
                        Comparable e = (Comparable) entry.getKey();
                        Comparable e1 = (Comparable) entry1.getKey();
                        match = e.compareTo(e1);
                    }

                    return match;
                }
            });

            if (reverseOrder) {
                java.util.Collections.reverse(entries);
            }

            for (Entry<K, Integer> e : entries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.write(countseparator);
                }

                out.write(e.getKey() + keyvalseparator + e.getValue());

                if (samples != null) {
                    out.write(keyvalseparator + ListUtils.list2string(samples.getSamples(e.getKey()), sampleSeparator));
                }
            }

            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Exception saving counts", e);
        }
    }

    public void saveToUTF8File(String file, String keyvalseparator, String countseparator,
            String sampleseparator, boolean append) {
        saveToUTF8File(file, keyvalseparator, countseparator, sampleseparator, append, this.samples);
    }

    public void saveToUTF8File(String file, String keyvalseparator, String countseparator,
            String sampleseparator, boolean append, boolean reverseOrder) {
        saveToUTF8File(file, keyvalseparator, countseparator, sampleseparator, append, this.samples, reverseOrder);
    }
}
