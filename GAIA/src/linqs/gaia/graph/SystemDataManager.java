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
package linqs.gaia.graph;

import linqs.gaia.identifiable.ID;

/**
 * A system data manager can store additional information,
 * apart from features, which models or various implementation
 * may need.  For example, experiments involving collective
 * classification can use the system data manager to store
 * the true label.
 * 
 * @author namatag
 *
 */
public interface SystemDataManager {
	/**
	 * Set a system data string with the given key and value
	 * 
	 * @param key Key of system datum
	 * @param value Value of system datum
	 */
	void setSystemData(String key, String value);
	
	/**
	 * Get the system data string with the given key.
	 * A null is returned if a value is not defined.
	 * 
	 * @param key Key of system datum
	 * @return Value of system datum
	 */
	String getSystemData(String key);
	
	/**
	 * Set the system datum but for a specific decorable item.
	 * 
	 * @param id ID of item to store the system datum for
	 * @param key Key of system datum
	 * @param value Value of system datum
	 */
	void setSystemData(ID id, String key, String value);
	
	/**
	 * Set the system datum but for an object with the specific id.
	 * A null is returned if a value is not defined.
	 * 
	 * @param id ID to get the system datum for
	 * @param key Key of system datum
	 * @return Value of system datum
	 */
	String getSystemData(ID id, String key);
	
	/**
	 * Remove the stored system data value with the given key
	 */
	void removeSystemData(String key);
	
	/**
	 * Remove the stored system data value for the given decorable/key pair
	 * 
	 * @param id ID to remove the system datum for
	 * @param key Key of system datum
	 */
	void removeSystemData(ID id, String key);
	
	/**
	 * Clear all the system data for the given id
	 */
	void removeSystemData(ID id);
	
	/**
	 * Remove all the stored system data
	 */
	void removeAllSystemData();
}
