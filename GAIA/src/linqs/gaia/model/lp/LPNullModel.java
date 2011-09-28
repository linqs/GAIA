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
package linqs.gaia.model.lp;

import java.io.File;
import java.util.Iterator;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;

/**
 * This is a null LP model.  When run, it makes no predictions or predicts
 * all proposed edges as existing.
 * For use mainly for testing, as well as to establish baselines.
 * 
 * Optional Parameters:
 * <UL>
 * <LI> predictall-If yes, predict all edges proposed by potential link
 * generator as existing.
 * </UL>
 * 
 * @author namatag
 *
 */
public class LPNullModel extends BaseConfigurable implements LinkPredictor {
	private static final long serialVersionUID = 1L;
	private boolean predictall = false;
	private boolean initialize = true;
	private String edgeschemaid;
	
	private void initialize(String edgeschemaid) {
		this.predictall = this.hasYesNoParameter("predictall", "yes");
		this.initialize = false;
		this.edgeschemaid = edgeschemaid;
	}
	
	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String existfeature) {
		if(initialize) {
			this.initialize(edgeschemaid);
		}
		
		// Do nothing
	}

	public void learn(Graph graph, PotentialLinkGenerator generator,
			String edgeschemaid) {
		if(initialize) {
			this.initialize(edgeschemaid);
		}
		
		// Do nothing
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges) {
		// Do nothing
	}

	public void predict(Graph graph, PotentialLinkGenerator generator) {
		if(predictall) {
			generator.addAllLinks(graph, edgeschemaid);
		} else {
			// Do nothing
		}
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		// Add exist feature, if not already defined
		Schema schema = graph.getSchema(edgeschemaid);
		if(existfeature!=null && !schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(edgeschemaid, schema);
		}
		
		if(predictall) {
			Iterator<Edge> eitr = unknownedges.iterator();
			while(eitr.hasNext()) {
				eitr.next().setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
			}
		} else {
			// Do nothing
		}
	}

	public void predict(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		// Add exist feature, if not already defined
		Schema schema = graph.getSchema(edgeschemaid);
		if(existfeature!=null && !schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(edgeschemaid, schema);
		}
		
		if(predictall) {
			generator.addAllLinks(graph, edgeschemaid, existfeature, false);
		} else {
			// Do nothing
		}
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		this.initialize(edgeschemaid);
	}

	public void saveModel(String directory) {
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
