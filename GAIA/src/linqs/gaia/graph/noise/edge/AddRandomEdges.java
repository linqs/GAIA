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

import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.log.Log;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.Dynamic;

/**
 * Add spurious edges to the graph.  The edges are added from
 * a potential link generator where all the potential links
 * generated will be added.
 * 
 * Required Parameters:
 * <UL>
 * <LI>edgeschemaid-Schema ID of edges to randomly remove
 * <LI>plgclass-Potential link generator which will create all the
 * we want to randomly add.
 * </UL>
 * Optional Parameters:
 * <UL>
 * <LI>linkexistfid-Categorical feature specifying whether the link is real or randomly added.
 * If the feature doesn't already exist, it is added.  All original
 * edges, as well as those added, with the given edge schema id,
 * all have the value of LinkPredictor.EXIST.
 * <LI>setunknown-If linkexistfid is specified and this is set to yes,
 * we set the existence feature value of the added edges as unknown.
 * Default is to set them as LinkPredictor.EXIST.
 * <LI>setnotexist-If linkexistfid is specified and this is set to yes,
 * we set the existence feature value of the added edges as LinkPredictor.NOTEXIST.
 * Default is to set them as LinkPredictor.EXIST.
 * </UL>
 * 
 * @author namatag
 *
 */
public class AddRandomEdges extends Noise {
	@Override
	public void addNoise(Graph g) {
		String edgeschemaid = this.getStringParameter("edgeschemaid");
		
		String linkexistfid = null;
		if(this.hasParameter("linkexistfid")) {
			linkexistfid = this.getStringParameter("linkexistfid");
			Schema schema = g.getSchema(edgeschemaid);
			if(!schema.hasFeature(linkexistfid)) {
				schema.addFeature(linkexistfid, LinkPredictor.EXISTENCEFEATURE);
				g.updateSchema(edgeschemaid, schema);
				
				// Set current edges as existing
				Iterator<GraphItem> gitr = g.getGraphItems(edgeschemaid);
				while(gitr.hasNext()) {
					GraphItem gi = gitr.next();
					gi.setFeatureValue(linkexistfid, LinkPredictor.EXISTVALUE);
				}
			}
		}
		
		boolean setunknown = this.hasYesNoParameter("setunknown","yes");
		boolean setnotexist = this.hasYesNoParameter("setnotexist","yes");
		
		// Get the potential link generator
		String plgclass = this.getStringParameter("plgclass");
		PotentialLinkGenerator plg = (PotentialLinkGenerator)
			Dynamic.forConfigurableName(PotentialLinkGenerator.class, plgclass);
		plg.copyParameters(this);
		
		// Add all the edges
		int counter = 0;
		Iterator<Edge> eitr = plg.getLinksIteratively(g, edgeschemaid);
		while(eitr.hasNext()) {
			// Add link
			// Note: We do not set to added link as existing or not existing.
			Edge e = eitr.next();
			
			// Set feature as existing, if requested,
			// or leave as unknown if setunknown is yes
			if(linkexistfid!=null && !setunknown) {
				if(setnotexist) {
					e.setFeatureValue(linkexistfid, LinkPredictor.NOTEXISTVALUE);
				} else if(!setunknown) {
					e.setFeatureValue(linkexistfid, LinkPredictor.EXISTVALUE);
				}
			}
			
			counter++;
		}
		
		Log.INFO("Added random edges:"+counter);
	}
}
