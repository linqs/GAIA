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
package linqs.gaia.graph.transformer.edge;

import linqs.gaia.graph.EdgeUtils;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.transformer.Transformer;

/**
 * Copy directed edge to undirected edge.
 * The result is that you will have two sets of edges,
 * the original directed edges and the undirected
 * edges which correspond to it.
 * The result is equivalent to calling
 * {@link EdgeUtils#copyDir2Undir(Graph, String, String, boolean)}
 * with the last argument set to true.
 * 
 * Required Parameters:
 * <UL>
 * <LI>dirschemaid-Schema ID of directed edges to convert from
 * <LI>undirschemaid-Schema ID of undirected edges to convert to
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConvertDirToUndir extends Transformer {
	@Override
	public void transform(Graph g) {
		// Get parameters
		String dirschemaid = this.getStringParameter("dirschemaid");
		String undirschemaid = this.getStringParameter("undirschemaid");
		
		EdgeUtils.copyDir2Undir(g, dirschemaid, undirschemaid, true);
	}
}
