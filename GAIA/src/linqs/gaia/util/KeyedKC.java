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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Utility for two level keyed count.
 * 
 * @author namatag
 *
 * @param <K1> Primary Key
 * @param <K2> Secondary Key
 */
public class KeyedKC<K1, K2> {
    private Map<K1, KeyedCount<K2>> key2kc;
    private Set<K1> keys;
    private int totalCount;

    /**
     * Constructor
     */
    public KeyedKC() {
        key2kc = Collections.synchronizedMap(new LinkedHashMap<K1, KeyedCount<K2>>());
        keys = Collections.synchronizedSet(new LinkedHashSet<K1>());
        totalCount = 0;
    }

    public void increment(K1 key1, K2 key2) {
        this.increment(key1, key2, 1);
    }

    public void increment(K1 key1, K2 key2, int numincrement) {
        KeyedCount<K2> kc = null;
        if (key2kc.containsKey(key1)) {
            kc = key2kc.get(key1);
        } else {
            kc = new KeyedCount<K2>();
            key2kc.put(key1, kc);
            keys.add(key1);
        }

        totalCount += numincrement;

        kc.increment(key2, numincrement);
    }

    public void decrement(K1 key1, K2 key2) {
        this.decrement(key1, key2, 1);
    }

    public void decrement(K1 key1, K2 key2, int numdecrement) {
        KeyedCount<K2> kc = null;
        if (key2kc.containsKey(key1)) {
            kc = key2kc.get(key1);
        } else {
            kc = new KeyedCount<K2>();
            key2kc.put(key1, kc);
            keys.add(key1);
        }

        totalCount -= numdecrement;

        kc.decrement(key2, numdecrement);
    }

    public KeyedCount<K2> getKeyedCount(K1 key1) {
        return key2kc.get(key1);
    }

    /**
     * Get the list of keys counted
     * 
     * @return Set of String valued keys.
     */
    public Set<K1> getKeys() {
        return Collections.unmodifiableSet(keys);
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
     * Return a delimited string representation of this object. If key1separator="|",
     * key1valseparator=":", key2valseparator="=", and countseparator=",", then you would get a
     * string like "bin1:blue=1,red=4,green=3|bin2:blue=11,red=4|bin3:green=5" where
     * [bin1,bin2,bin3] are primary keys and [red,blue,green] are secondary keys.
     * 
     * @param key1separator Separator between primary key values
     * @param key1valseparator Separator between primary key and its secondary keys
     * @param key2valseparator Separator between the secondary keys and count
     * @param countseparator Separator between count values provided for each secondary key
     * @return String representation
     */
    public String toString(String key1separator, String key1valseparator, String key2valseparator, String countseparator) {
        StringBuffer countsstring = null;
        Set<Entry<K1, KeyedCount<K2>>> entries = this.key2kc.entrySet();
        for (Entry<K1, KeyedCount<K2>> e : entries) {
            if (countsstring == null) {
                countsstring = new StringBuffer();
            } else {
                countsstring.append(key1separator);
            }

            countsstring.append(e.getKey() + key1valseparator + e.getValue().toString(key2valseparator, countseparator));
        }

        return countsstring.toString();
    }

    public String toString() {
        return toString("|", ":", "=", ",");
    }

    public String toStringColumns() {
        return toString("\n\n", "\n", "\t", "\n");
    }

    public void saveToFile(String file, String key1separator, String key1valseparator, String key2valseparator, String countseparator, boolean append) {
        try {
            FileWriter fstream = new FileWriter(file, append);
            BufferedWriter out = new BufferedWriter(fstream);

            Set<Entry<K1, KeyedCount<K2>>> entries = this.key2kc.entrySet();
            for (Entry<K1, KeyedCount<K2>> e : entries) {
                out.write(e.getKey() + key1valseparator + e.getValue().toString(key2valseparator, countseparator) + "\n");
            }

            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveToUTF8File(String file, String key1separator, String key1valseparator, String key2valseparator, String countseparator, boolean append) {
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("outfilename"), "UTF-8"));

            Set<Entry<K1, KeyedCount<K2>>> entries = this.key2kc.entrySet();
            for (Entry<K1, KeyedCount<K2>> e : entries) {
                out.write(e.getKey() + key1valseparator + e.getValue().toString(key2valseparator, countseparator) + "\n");
            }

            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
