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
package linqs.gaia.graph.noise.graphitem;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.noise.Noise;

/**
 * Removes all graph items with the given schema.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> schemaid-Schema ID of graph items to remove
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> removeschema-If yes, remove the schema for the schemaid.  Default is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveGraphItems extends Noise {

	@Override
	public void addNoise(Graph g) {
		String schemaid = this.getStringParameter("schemaid");
		g.removeAllGraphItems(schemaid);
		
		if(this.hasYesNoParameter("removeschema", "yes")) {
			g.removeSchema(schemaid);
		}
	}
}
