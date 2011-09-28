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
package linqs.gaia.graph.filter;

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.filter.Filter;

/**
 * Filter to remove the specified feature from the given schema.
 * <p>
 * Required:
 * <UL>
 * <LI> schemaID-Feature schema id of schema to change
 * <LI> featureID-Comma delimited list of features to remove
 * </UL>
 * 
 * @author namatag
 *
 */
public class RemoveFeature extends Filter {
	@Override
	public void filter(Graph graph) {
		String schemaID = this.getStringParameter("schemaID");
		String fids[] = this.getStringParameter("featureID").split(",");
		
		Schema schema = graph.getSchema(schemaID);
		for(String fid:fids){
			schema.removeFeature(fid);
		}
		
		graph.updateSchema(schemaID, schema);
	}
}
