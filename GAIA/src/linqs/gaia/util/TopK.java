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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import linqs.gaia.log.Log;

/**
 * Return the Top K items where the top K is defined as those with the K highest values.
 * <p>
 * Note: This implementation stores a pointer over the nodes of the top K.
 * 
 * @author namatag
 * 
 * @param <O> Comparable object class to order
 */
public class TopK<O> {
    private int k;
    private PriorityQueue<Pair> topK;
    private Double borderval = Double.POSITIVE_INFINITY;
    private boolean savedups = true;
    private boolean reverse = false;
    private int maxDuplicateSizeLimit = -1;

    /**
     * Constructor
     * 
     * @param k Value of K
     * @param savedups If true, you can return more than K objects if there are ties for values. If
     *            false, if there are multiple values with the same score and keeping them all will
     *            result in returning more than K, one of those instances will be arbitrarily
     *            removed.
     */
    public TopK(int k, boolean savedups) {
        this.k = k;
        this.savedups = savedups;
        topK = new PriorityQueue<Pair>(k);
    }

    /**
     * Set maximum number of items allowed in top values including when duplicate valued entries can
     * be found leading to going over K. Once this maximum is reached, all other duplicates. Default
     * is to add all duplicates.
     * 
     * @param maxDuplicateSizeLimit Maximum number of duplicate valued items
     */
    public void setMaxDuplicateSizeLimite(int maxDuplicateSizeLimit) {
        this.maxDuplicateSizeLimit = maxDuplicateSizeLimit;
    }

    /**
     * Constructor
     * 
     * @param k Value of K
     * @param savedups If true, you can return more than K objects if there are ties for values. If
     *            false, if there are multiple values with the same score and keeping them all will
     *            result in returning more than K, one of those instances will be arbitrarily
     *            removed.
     * @param reverse If true, use the natural reverse ordering to get the bottom K instead.
     */
    public TopK(int k, boolean savedups, boolean reverse) {
        this.k = k;
        this.savedups = savedups;
        this.reverse = reverse;
        if (this.reverse) {
            topK = new PriorityQueue<Pair>(k, Collections.reverseOrder());
        } else {
            topK = new PriorityQueue<Pair>(k);
        }
    }

    /**
     * Constructor
     * 
     * @param k Value of K
     */
    public TopK(int k) {
        this(k, false);
    }

    /**
     * Get K value
     * 
     * @return K value
     */
    public int getK() {
        return this.k;
    }

    /**
     * Get top K objects
     * 
     * @return List of top K objects
     */
    @SuppressWarnings("unchecked")
    public Set<O> getTopK() {
        Set<O> vals = new HashSet<O>();
        for (Pair pair : this.topK) {
            vals.add((O) pair.o);
        }

        return vals;
    }

    /**
     * Get top K objects in decreasing order, or, if reverse and you want bottom K, in ascending
     * order
     * 
     * @return List of top K objects
     */
    @SuppressWarnings("unchecked")
    public List<O> getTopKSorted() {
        List<Pair> pairlist = new ArrayList<Pair>(this.topK);
        if (reverse) {
            Collections.sort(pairlist);
        } else {
            Collections.sort(pairlist, Collections.reverseOrder());
        }

        List<O> vals = new ArrayList<O>();
        for (Object pair : pairlist) {
            vals.add((O) ((Pair) pair).o);
        }

        return vals;
    }

    /**
     * Get top K objects with scores in decreasing order, or, if reverse and you want bottom K, in
     * ascending order
     * 
     * @return List of top K objects with scores
     */
    @SuppressWarnings("unchecked")
    public List<SimplePair<O, Double>> getTopKSortedWithScores() {
        List<Pair> pairlist = new ArrayList<Pair>(this.topK);
        if (reverse) {
            Collections.sort(pairlist);
        } else {
            Collections.sort(pairlist, Collections.reverseOrder());
        }

        List<SimplePair<O, Double>> vals = new ArrayList<SimplePair<O, Double>>();
        for (Object pair : pairlist) {
            Pair p = (Pair) pair;
            vals.add(new SimplePair<O, Double>((O) p.o, p.d));
        }

        return vals;
    }

    /**
     * Add item
     * 
     * @param value Value of object
     * @param obj Object
     * @return Items removed as a result of this insertion or the obj itself, if it was not added.
     *         An empty list is returned if there were no items removed.
     */
    @SuppressWarnings("unchecked")
    public List<O> add(Double value, O obj) {
        List<O> removed = new ArrayList<O>();

        if (topK.size() < this.k) {
            // Fill K until full
            this.topK.add(new Pair(value, obj));
            this.borderval = this.topK.peek().d;
        } else if ((reverse && value > this.borderval) || (!reverse && value < this.borderval)) {
            // If adding something whose value is lower
            // than the lowest current value, do not add obj.
            // Instead, return obj as removed.
            removed.add(obj);
        } else if ((reverse && value < this.borderval) || (!reverse && value > this.borderval)) {
            topK.add(new Pair(value, obj));

            // If adding something higher than the lowest value,
            // remove the lowest value and insert the new value.
            List<Pair> dups = new ArrayList<Pair>();
            while (this.topK.peek().d.equals(this.borderval)) {
                dups.add(this.topK.poll());
            }

            // Least common value is a duplicate
            // and the removal results in less than K
            if (this.topK.size() < this.k) {
                if (this.savedups) {
                    // add it all back if saving duplicates
                    this.topK.addAll(dups);
                    dups.clear();
                } else if (!this.savedups) {
                    // add just enough back to get back to K
                    for (Pair p : dups) {
                        if (this.topK.size() < this.k) {
                            this.topK.add(p);
                        } else {
                            removed.add((O) p.o);
                        }
                    }
                }
            } else {
                // All of these were removed
                for (Pair p : dups) {
                    removed.add((O) p.o);
                }
            }

            // Update minval with the current lowest value.
            this.borderval = topK.peek().d;
        } else if (topK.contains(new Pair(value, obj))) {
            // Handle saving duplicates, if requested
            if (savedups || this.topK.size() < this.k) {
                if (maxDuplicateSizeLimit == -1 || topK.size() < maxDuplicateSizeLimit) {
                    topK.add(new Pair(value, obj));
                }
            } else {
                // If not saving duplicates,
                // and this item has a duplicate score,
                // do not add.  Instead, return obj as removed.
                removed.add(obj);
            }

            // Update minval with the current lowest value.
            this.borderval = topK.peek().d;
        } else {
            throw new RuntimeException("Unsupported case");
        }

        return removed;
    }

    public boolean hasPair(Double value, O obj) {
        return topK.contains(new Pair(value, obj));
    }

    public void clear() {
        this.topK.clear();
        this.borderval = Double.POSITIVE_INFINITY;
    }

    public void saveToFile(String file, String keycountseparator, String countseparator) {
        try {

            FileWriter fstream = new FileWriter(file, false);
            BufferedWriter out = new BufferedWriter(fstream);

            boolean isFirst = true;
            Iterator<Pair> entries = this.topK.iterator();
            while (entries.hasNext()) {
                Pair e = entries.next();

                if (isFirst) {
                    isFirst = false;
                } else {
                    out.write(countseparator);
                }

                out.write(e.o + keycountseparator + e.d);
            }

            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Exception saving counts", e);
        }
    }

    /**
     * Pair object to store the object within
     * 
     * @author namatag
     * 
     */
    private class Pair implements Comparable<Pair> {
        public Double d = null;
        public Object o = null;

        public Pair(double d, Object o) {
            this.d = d;
            this.o = o;
        }

        /**
         * Objects are equal if d is equal
         */
        @SuppressWarnings("unchecked")
        public boolean equals(Object obj) {
            // Not strictly necessary, but often a good optimization
            if (this == obj) {
                return true;
            }

            if (obj == null || !(obj instanceof TopK.Pair)) {
                return false;
            }

            Pair p = (Pair) obj;

            return this.d.equals(p.d);
        }

        public int hashCode() {
            return this.d.hashCode();
        }

        /**
         * Comparison is done solely on d
         */
        public int compareTo(Pair o) {
            return this.d.compareTo(o.d);
        }

        public String toString() {
            return this.o + "=" + this.d;
        }
    }

    public static void main(String[] args) {
        Log.showAllLogging();

        TopK<String> topk = new TopK<String>(3, true, false);
        Log.DEBUG("Removed=" + topk.add(10.0, "10a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(3.0, "3") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(4.0, "4") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(5.0, "5") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10d") + " TopK=" + topk.getTopK());
        Log.DEBUG("Sorted: " + topk.getTopKSorted());
        Log.DEBUG("Sorted with scores: " + topk.getTopKSortedWithScores());

        topk = new TopK<String>(3, false, true);
        Log.DEBUG("Removed=" + topk.add(10.0, "10a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(3.0, "3") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(4.0, "4") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(5.0, "5") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10d") + " TopK=" + topk.getTopK());
        Log.DEBUG("Sorted: " + topk.getTopKSorted());
        Log.DEBUG("Sorted with scores: " + topk.getTopKSortedWithScores());

        topk = new TopK<String>(3, false, false);
        Log.DEBUG("Removed=" + topk.add(10.0, "10a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(3.0, "3") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(4.0, "4") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(5.0, "5") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10d") + " TopK=" + topk.getTopK());
        Log.DEBUG("Sorted: " + topk.getTopKSorted());
        Log.DEBUG("Sorted with scores: " + topk.getTopKSortedWithScores());

        topk = new TopK<String>(3, true, true);
        Log.DEBUG("Removed=" + topk.add(10.0, "10a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(1.0, "1b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(2.0, "2c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(3.0, "3") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(4.0, "4") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(5.0, "5") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6a") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(6.0, "6b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10b") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10c") + " TopK=" + topk.getTopK());
        Log.DEBUG("Removed=" + topk.add(10.0, "10d") + " TopK=" + topk.getTopK());
        Log.DEBUG("Sorted: " + topk.getTopKSorted());
        Log.DEBUG("Sorted with scores: " + topk.getTopKSortedWithScores());
    }
}
