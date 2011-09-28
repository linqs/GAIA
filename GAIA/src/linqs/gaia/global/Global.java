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
package linqs.gaia.global;

/**
 * Class containing all references to static variables used in the code.
 * 
 * @author namatag
 *
 */
public class Global {
	/** Stem name for creation of anonymous identifiers */
	private static final String anonymous_stem = "gaia_anon:";

	/** Count of number of anonymous items requested */
	private static int anonymous_count = 0;
	
	/**
	 * Request a new feature name for an input feature that is anonymous.
	 *
	 * @return A name to use for the anonymous feature.
	 */
	public static String requestAnonymousID() {
		return anonymous_stem+(anonymous_count++);
	}
	
	/**
	 * Request the next number in the local counter
	 *
	 * @return A name to use for the anonymous feature.
	 */
	public static int requestGlobalCounterValue() {
		return anonymous_count++;
	}
	
	/**
	 * Resets all global variables
	 */
	public static void reset(){
		anonymous_count = 0;
	}
}
