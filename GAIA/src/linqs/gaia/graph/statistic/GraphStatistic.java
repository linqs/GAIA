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
package linqs.gaia.graph.statistic;

import java.util.Map;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Interface for statistics computed over the graph.
 * 
 * @author namatag
 *
 */
public interface GraphStatistic extends Configurable {
	/**
	 * Get numeric value of this evaluation metric over the provided data.
	 * 
	 * @param g Graph to calculate statistic over
	 * @return Numeric values of the statistic.  Value is set to -1 if not applicable.
	 */
    Map<String,Double> getStatisticDoubles(Graph g);
    
    /**
     * Get string representation of this evaluation metric over the provided data.
     * 
     * @param g Graph to calculate statistic over
     * @return String values of the evaluation metric.
     */
    Map<String,String> getStatisticStrings(Graph g);
    
    /**
     * Get string representation of this evaluation metric over the provided data.
     * 
     * @param g Graph to calculate statistic over
     * @return String value of the evaluation metric.
     */
    String getStatisticString(Graph g);
}
