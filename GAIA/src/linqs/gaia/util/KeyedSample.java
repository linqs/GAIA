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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Collect samples, one set of samples for each specified key.
 * 
 * @see StreamRandomSample
 * 
 * @author namatag
 * 
 * @param <K> Type of keys to use
 * @param <V> Type of sampled objects
 */
public class KeyedSample<K, V> {
    private Random rand;
    private int maxnumsamples;
    private Map<K, StreamRandomSample<V>> key2samples = new HashMap<K, StreamRandomSample<V>>();

    /**
     * Constructor
     * 
     * @param maxnumsamples Number of samples per key to collect
     * @param r Random number generator to use
     */
    public KeyedSample(int maxnumsamples, Random r) {
        this.rand = r;
        this.maxnumsamples = maxnumsamples;
    }

    /**
     * Constructor
     * 
     * @param maxnumsamples Number of samples per key to collect
     */
    public KeyedSample(int maxnumsamples) {
        this.rand = new Random();
        this.maxnumsamples = maxnumsamples;
    }

    /**
     * Key of item to potentially add to sample.
     * 
     * @param key Key for sample
     * @param value Item to potentially add to keyed sample sample
     * @return List of items removed from sample if this sample was added
     */
    public List<V> addSample(K key, V value) {
        StreamRandomSample<V> srs = null;
        if (key2samples.containsKey(key)) {
            srs = key2samples.get(key);
        } else {
            srs = new StreamRandomSample<V>(maxnumsamples, rand);
            key2samples.put(key, srs);
        }

        return srs.add(value);
    }

    public boolean containsKey(K key) {
        return this.key2samples.containsKey(key);
    }

    /**
     * Get a list containing the samples for the specified key
     * 
     * @param key Key
     * @return List of values for key
     */
    public List<V> getSamples(K key) {
        if (this.key2samples.containsKey(key)) {
            return this.key2samples.get(key).getSamples();
        } else {
            return new ArrayList<V>(0);
        }
    }

    /**
     * Get a set containing all the keys
     * 
     * @return Set of keys
     */
    public Set<K> getKeys() {
        return this.key2samples.keySet();
    }

    /**
     * Remove all samples for specified key
     * 
     * @param key Key
     */
    public void removeKey(K key) {
        this.key2samples.remove(key);
    }

    /**
     * Remove keys whose number of samples is strictly below the specified threshold
     * 
     * @param threshold Count threshold
     */
    public void removeKeysBelowThreshold(int threshold) {
        Set<K> keys = this.getKeys();
        List<K> toremove = new LinkedList<K>();
        for (K k : keys) {
            if (this.getSamples(k).size() < threshold) {
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
            if (this.getSamples(k).size() > threshold) {
                toremove.add(k);
            }
        }

        for (K k : toremove) {
            this.removeKey(k);
        }
    }

    /**
     * Remove all samples
     */
    public void clear() {
        this.key2samples.clear();
    }

    /**
     * Print keyed sample with specified delimiters, e.g.:
     * mammal=dog,cat(newline)insect=ladybug(newline)
     * 
     * @return String representation of keyed sample
     */
    public String toString() {
        return toString("=", "\n", ",", "");
    }

    /**
     * Print keyed sample with specified delimiters
     * 
     * @param keyvalseparator Separator between key and the set of items (i.e., if "=",
     *            mammal=dog,cat)
     * @param keyseparator Separator between multiple keys and the set of items (i.e., if ";",
     *            mammal=dog,cat;insect=ladybug)
     * @param valseparator Separator between items in sampled set of items (i.e., if ",",
     *            mammal=dog,cat)
     * 
     * @return String representation of keyed sample
     */
    public String toString(String keyvalseparator, String keyseparator, String valseparator) {
        return this.toString(keyvalseparator, keyseparator, valseparator, "");
    }

    /**
     * Print keyed sample with specified delimiters
     * 
     * @param keyvalseparator Separator between key and the set of items (i.e., if "=",
     *            mammal=dog,cat)
     * @param keyseparator Separator between multiple keys and the set of items (i.e., if ";",
     *            mammal=dog,cat;insect:ladybug)
     * @param valseparator Separator between items in sampled set of items (i.e., if ",",
     *            mammal=dog,cat)
     * @param keyprefix Prefix to add to key values (i.e., if "type-",
     *            type-mammal=dog,cat;type-insect:ladybug)
     * @return String representation of keyed sample
     */
    public String toString(String keyvalseparator, String keyseparator, String valseparator, String keyprefix) {
        StringBuffer samplestring = null;
        Set<Entry<K, StreamRandomSample<V>>> entries = this.key2samples.entrySet();
        for (Entry<K, StreamRandomSample<V>> e : entries) {
            if (samplestring == null) {
                samplestring = new StringBuffer();
            } else {
                samplestring.append(keyseparator);
            }

            samplestring.append(keyprefix + e.getKey() + keyvalseparator + e.getValue().toString(valseparator));
        }

        return samplestring == null ? null : samplestring.toString();
    }

    /**
     * Print keyed sample with specified delimiters
     * 
     * @param file Output file
     * @param keyvalseparator Separator between key and the set of items (i.e., if "=",
     *            mammal=dog,cat)
     * @param keyseparator Separator between multiple keys and the set of items (i.e., if ";",
     *            mammal=dog,cat;insect:ladybug)
     * @param valseparator Separator between items in sampled set of items (i.e., if ",",
     *            mammal=dog,cat)
     * @param keyprefix Prefix to add to key values (i.e., if "type-",
     *            type-mammal=dog,cat;type-insect:ladybug)
     * @param append If true, append to existing file. If false, overwrrite.
     */
    public void saveToFile(String file, String keyvalseparator, String keyseparator, String valseparator, String keyprefix, boolean append) {
        try {
            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);

            boolean isFirst = true;
            Set<Entry<K, StreamRandomSample<V>>> entries = this.key2samples.entrySet();
            for (Entry<K, StreamRandomSample<V>> e : entries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.write(keyseparator);
                }

                out.write(keyprefix + e.getKey() + keyvalseparator + e.getValue().toString(valseparator));
            }

            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Exception saving counts", e);
        }
    }
}
