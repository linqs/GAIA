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
package linqs.gaia.prediction;


/**
 * Predictions where predictions include only those things which are predicted positive.
 * 
 * @author namatag
 * 
 */
public interface PositiveOnlyGroup extends PredictionGroup {
	/**
	 * Number of items predicted, including the negative predictions not listed.
	 * 
	 * @return Return the total number
	 */
	long getNumTotal();
	
	/**
	 * Get the number of true positives
	 * 
	 * @return Number of true positives
	 */
	long getNumPositive();
	
	/**
	 * Get the value to use as the positive value
	 * 
	 * @return Object of positive value
	 */
	Object getPositiveValue();
	
	/**
	 * Get the value to use as the negative value
	 * 
	 * @return Object of negative value
	 */
	Object getNegativeValue();
}
