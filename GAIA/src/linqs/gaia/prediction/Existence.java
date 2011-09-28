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
 * For predictions related to whether or not something exists
 * 
 * @author namatag
 *
 */
public interface Existence extends Prediction {
	/**
	 * Predicted value of is this a prediction that something exists or that
	 * something doesn't exist.
	 * 
	 * @return Existence status
	 */
	boolean getPredExist();
	
	/**
	 * True value of is this a prediction that something exists or that
	 * something doesn't exist.
	 * 
	 * @return Existence status
	 */
	boolean getTrueExist();
	
	/**
	 * Get object whose existence status was predicted
	 * 
	 * @return Object predicted
	 */
	Object getPredObject();
}
