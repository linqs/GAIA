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
package linqs.gaia.configurable.test;

import linqs.gaia.configurable.BaseConfigurable;

/**
 * Simple configurable object for use with testing
 * 
 * @author namatag
 *
 */
public class TestConfigurableObject extends BaseConfigurable {
	// Do nothing
	public TestConfigurableObject() {
		
	}
	
	// Create configurable object with configuration id
	public TestConfigurableObject(String cid) {
		this();
		this.setCID(cid);
	}
}
