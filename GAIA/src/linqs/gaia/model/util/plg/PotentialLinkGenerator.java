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

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;

/**
 * Interface for all link generators.
 * Link generators enumerate links that may exist
 * but currently do not.  It is for use in
 * defining potential links in link prediction.
 * 
 * For a given graph, the link generator will return
 * an iterator over a set of potential edges.
 * This set of edges is used in some link prediction
 * models and are the only ones considered for those models.
 * 
 * @see linqs.gaia.model.lp.LinkPredictor
 * 
 * @author namatag
 */
public interface PotentialLinkGenerator extends Configurable {
	
	/**
	 * Return the set of edges as an iterator.
	 * The returned edge is initialized only prior to
	 * adding to the graphs.  If there are a lot of
	 * potential edges, you can look at them one by one
	 * in this manner and keep only those that you predict
	 * exists (i.e., you can delete the newly created edge).
	 * 
	 * @param g Graph to predicted edges over
	 * @param edgeschemaid Schema ID of edge we're generating
	 * @return Iterator over potential edges
	 */
	Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid);
	
	/**
	 * Add all the potential links all at once.
	 */
	void addAllLinks(Graph g, String edgeschemaid);
	
	/**
	 * Add all the predicted links all at once.
	 * To identify which links were predicted and
	 * which are known to exist, an indicator feature
	 * must be defined.
	 * 
	 * @param g Graph to predicted edges over
	 * @param edgeschemaid Schema ID of edge we're generating
	 * @param existfeature Exist feature to set to whether the
	 * edge exists, doesn't exist, or whose existence is unknown.
	 * @param setasnotexist If true, set the created features values as not
	 * existing.  Otherwise, the feature value is set as unknown.
	 */
	void addAllLinks(Graph g, String edgeschemaid, String existfeature, boolean setasnotexist);
}
