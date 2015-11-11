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
import java.util.Map;
import java.util.Set;

/**
 * Utility for automatically breaking a set of values into evenly sized bins.
 * 
 * @author namatag
 *
 */
public class BinnedCount {
    private KeyedCount<Double> kc = new KeyedCount<Double>();
    private int binsize = 0;
    private int maxsize = Integer.MAX_VALUE;

    public BinnedCount(int binsize) {
        this.binsize = binsize;
    }

    public BinnedCount(int binsize, int maxsize) {
        this.binsize = binsize;
        this.maxsize = maxsize;
    }

    public Double compBinValue(double value) {
        if (value > maxsize) {
            value = maxsize;
        }

        if (value == 0) {
            return -1.0;
        }

        Double multiplier = value / this.binsize;

        return (double) multiplier.intValue() * binsize;
    }

    public void addValue(double value) {
        kc.increment(compBinValue(value));
    }

    public Set<Double> getBins() {
        return kc.getKeys();
    }

    public int getBinSize(double value) {
        return kc.getCount(compBinValue(value));
    }

    public String toString() {
        return kc.toString();
    }

    public String toString(String keyvalseparator, String countseparator) {
        return kc.toString(keyvalseparator, countseparator);
    }

    public String toOrderedString(String keyvalseparator, String countseparator) {
        List<Double> keys = new ArrayList<Double>(kc.getKeys());
        Collections.sort(keys);

        String countsstring = null;
        for (Double k : keys) {
            if (countsstring == null) {
                countsstring = "";
            } else {
                countsstring += countseparator;
            }

            countsstring += k + keyvalseparator + kc.getCount(k);
        }

        return countsstring;
    }

    public Map<Double, Integer> getAllBinSizes() {
        return kc.getAllCounts();
    }

    public Double getPercentInBin(Double key) {
        return kc.getPercent(compBinValue(key));
    }

    public int totalCounted() {
        return kc.totalCounted();
    }

    public Double highestCountBin() {
        return kc.highestCountKey();
    }

    public List<Double> highestCountBins() {
        return kc.highestCountKeys();
    }

    public Double lowestCountBin() {
        return kc.lowestCountKey();
    }

    public List<Double> lowestCountBins() {
        return kc.lowestCountKeys();
    }

    public void clearCounts() {
        kc.clearCounts();
    }

    public void removeBin(Double key) {
        kc.removeKey(compBinValue(key));
    }

    public int numBins() {
        return kc.numKeys();
    }
}
