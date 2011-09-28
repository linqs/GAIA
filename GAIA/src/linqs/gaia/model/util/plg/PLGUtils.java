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
package linqs.gaia.model.util.plg;

import java.util.Iterator;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.ListUtils;

/**
 * Utilities for potential link generators
 * 
 * @author namatag
 *
 */
public class PLGUtils {
	/**
	 * Add all the links specified by the iterator returned by
	 * a potential link generator.
	 * 
	 * @param g Graph
	 * @param edgeschemaid Schema ID of edges being added
	 * @param eitr Iterator over potential links as returned by getLinksIteratively.
	 */
	protected static void addAllLinks(Graph g, String edgeschemaid, Iterator<Edge> eitr) {
		while(eitr.hasNext()) {
			eitr.next();
		}
	}
	
	/**
	 * Add all the links specified by the iterator returned by
	 * a potential link generator.
	 *  
	 * @param g Graph
	 * @param edgeschemaid Schema ID of edges being added
	 * @param existfeature Feature ID of existence feature.  If not defined,
	 * the existence feature is automatically added.
	 * @param eitr Iterator over potential links as returned by getLinksIteratively.
	 * @param setnotexist If yes, set the generated links to non-existing/negative links.
	 * They are set to existing/positive links by default.
	 */
	protected static void addAllLinks(Graph g, String edgeschemaid, String existfeature,
			Iterator<Edge> eitr, boolean setnotexist) {
		Schema schema = g.getSchema(edgeschemaid);
		
		// Check to see if exist feature is already defined
		if(schema.hasFeature(existfeature)) {
			Feature f = schema.getFeature(existfeature);
			if(!(f instanceof CategFeature)) {
				throw new ConfigurationException("Invalid existence feature defined: "
						+edgeschemaid+" "+f.getClass().getCanonicalName());
			}
			
			// If it is, verify the right categories are defined.
			CategFeature cf = (CategFeature) f;
			if(!cf.getAllCategories().copyAsList().equals(LinkPredictor.EXISTENCE)) {
				throw new ConfigurationException("Invalid existence feature defined: "
						+edgeschemaid+" "+ListUtils.list2string(cf.getAllCategories().copyAsList(),","));
			}
		} else {
			// If it isn't, add the feature
			Feature f = new ExplicitCateg(LinkPredictor.EXISTENCE);
			schema.addFeature(existfeature, f);
			g.updateSchema(edgeschemaid, schema);
			
			// Assign all current edges with the exist feature with
			// a value with a probability of 1 of existing
			Iterator<GraphItem> itr = g.getGraphItems(edgeschemaid);
			while(itr.hasNext()) {
				GraphItem gi = itr.next();
				gi.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
			}
		}
		
		// Add edges
		while(eitr.hasNext()) {
			// By definition, edges are added as you iterate
			Edge e = eitr.next();
			
			// By default, the edges will be added with the existence feature
			// set to unknown.  If requested, they can also all be set to NOTEXIST.
			if(setnotexist) {
				e.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
			}
		}
	}
}
