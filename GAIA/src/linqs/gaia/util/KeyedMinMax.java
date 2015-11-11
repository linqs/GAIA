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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for computing Min and Max for certain keys
 * 
 * @author namatag
 *
 * @param <K1> Primary Key
 */
public class KeyedMinMax<K1> {
    Map<K1, MinMax> minmaxs = Collections.synchronizedMap(new LinkedHashMap<K1, MinMax>());

    public void add(K1 key, double value) {
        if (!minmaxs.containsKey(key)) {
            minmaxs.put(key, new MinMax());
        }

        minmaxs.get(key).addValue(value);
    }

    public MinMax getMinMax(K1 key) {
        return minmaxs.get(key);
    }
}
