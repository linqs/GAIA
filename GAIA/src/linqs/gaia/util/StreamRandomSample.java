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
import java.util.List;
import java.util.Random;

/**
 * This utility class can be used to random sample from a stream of data (e.g., data is not known in
 * advance or too large to load in memory to randomly select from directly). This is accomplished by
 * randomly assigning each added item a score. To get K random items, we just select the K items
 * with the highest score.
 * 
 * @author namatag
 * 
 * @param <O> Class of objects to return
 */
public class StreamRandomSample<O> {
    private Random rand = null;
    TopK<O> topksamples = null;

    /**
     * Constructor.
     * 
     * @param maxsamples Number of items to have in final sample
     * @param r Random number generator
     */
    public StreamRandomSample(int maxsamples, Random r) {
        this.rand = r;
        topksamples = new TopK<O>(maxsamples);
    }

    /**
     * Constructor.
     * 
     * @param maxsamples Number of items to have in final sample
     */
    public StreamRandomSample(int maxsamples) {
        this.rand = new Random();
        topksamples = new TopK<O>(maxsamples);
    }

    /**
     * Potentially add object to sampling
     * 
     * @param obj Object to potentially add to sampling
     * @return List of items removed from sample if this sample was added
     */
    public List<O> add(O obj) {
        double rscore = rand.nextDouble();
        return topksamples.add(rscore, obj);
    }

    /**
     * Get sampled information
     * 
     * @return List of sampled objects
     */
    public List<O> getSamples() {
        return new ArrayList<O>(topksamples.getTopK());
    }

    /**
     * Return string representation of samples in the form [sample1,sample2,...samplek]
     */
    public String toString() {
        return "[" + ListUtils.list2string(getSamples(), ",") + "]";
    }

    /**
     * Return string representation of samples delimited by specified delimiter
     * 
     * @param delimiter Delimiter
     * @return Delimited string representation
     */
    public String toString(String delimiter) {
        return ListUtils.list2string(getSamples(), delimiter);
    }

    /**
     * Remove all previous samples
     */
    public void removeAll() {
        topksamples.clear();
    }
}
