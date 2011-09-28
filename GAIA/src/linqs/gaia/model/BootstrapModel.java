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
package linqs.gaia.model;

/**
 * General interface for all models which has a bootstrapping step.
 * This interface will allow you to specify whether or not you
 * want the model to apply bootstrapping.  Useful when applying
 * predictions in succession.
 * 
 * @author namatag
 *
 */
public interface BootstrapModel {
	/**
	 * Set whether or not to apply bootstrapping in the prediction.
	 * 
	 * @param bootstrap If true, apply bootstrapping.  If false, don't apply.
	 */
	void shouldBootstrap(boolean bootstrap);
}
