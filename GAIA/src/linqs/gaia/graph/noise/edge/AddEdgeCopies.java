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
package linqs.gaia.graph.noise.edge;

import java.util.Iterator;
import java.util.Random;

import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.identifiable.GraphItemID;

/**
 * Create copies of the current set of edges
 * 
 * Required Parameters:
 * <UL>
 * <LI>edgeschemaid-Schema ID of edges to copy
 * </UL>
 * Optional Parameters:
 * <UL>
 * <LI>copyedgeschemaid-Schema ID of copied edges.  Default is to use the specified edgeschemaid.
 * <LI>numcopies-Number of copied edges to add, per edge.  Default is 1.
 * <LI>probaddcopy-Probability of copying an edge.
 * <LI>copyattributes-If "yes", the copy the attributes of the original to each copy.  Default is "no".
 * <LI>seed-Random number generator seed to use.
 * </UL>
 * 
 * @author namatag
 *
 */
public class AddEdgeCopies extends Noise {

	@Override
	public void addNoise(Graph g) {
		String edgesid = this.getStringParameter("edgeschemaid");
		String copyedgesid = edgesid;
		if(this.hasParameter("copyedgeschemaid")) {
			copyedgesid = this.getStringParameter("copyedgeschemaid");
			
			if(!edgesid.equals(copyedgesid) && !g.hasSchema(copyedgesid)) {
				g.addSchema(copyedgesid, g.getSchema(edgesid));
			}
		}
		
		boolean isdirected = g.getSchemaType(edgesid).equals(SchemaType.DIRECTED);
		boolean copyattributes = this.hasYesNoParameter("copyattributes", "yes");
		
		int numcopies = 1;
		if(this.hasParameter("numcopies")) {
			numcopies = this.getIntegerParameter("numcopies");
		}
		
		double probaddcopy = 1;
		if(this.hasParameter("probaddcopy")) {
			probaddcopy = this.getDoubleParameter("probaddcopy");
		}
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = this.getIntegerParameter("seed");
		}
		Random rand = new Random(seed);
		
		// Go over each edge
		Iterator<Edge> eitr = g.getEdges(edgesid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			// With some probability, make a copy or continue
			double chance = rand.nextDouble();
			if(chance > probaddcopy) {
				continue;
			}
			
			Edge copye = null;
			if(isdirected) {
				DirectedEdge de = (DirectedEdge) e;
				Iterator<Node> sources = de.getSourceNodes();
				Iterator<Node> targets = de.getTargetNodes();
				
				for(int i=0; i<numcopies; i++) {
					copye = g.addDirectedEdge(GraphItemID.generateGraphItemID(g, copyedgesid, ""),
						sources, targets);
				}
			} else {
				for(int i=0; i<numcopies; i++) {
					copye = g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, copyedgesid, ""),
						e.getAllNodes());
				}
			}
			
			// Copy attributes, if needed
			if(copyattributes) {
				FeatureUtils.copyFeatureValues(e, copye);
			}
		}
	}

}
