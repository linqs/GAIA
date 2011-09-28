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
package linqs.gaia.feature.derived.neighbor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.IteratorUtils;

/**
 * Return all unique adjacent items to a {@link GraphItem}.
 * For {@link Node}, these are the set of {@link Edge} items including that{@link Node}.
 * For {@link Edge}, these are the set of {@link Node} items participating in that {@link Edge}.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>unique-If "yes", the returned collection is a unique set of neighbors (no duplicates).
 * If "no", the collection may contain multiple entries of the same neighbor.  Default is yes.
 * <LI>includeself-If "yes", if a node has a self-link, include the node as its own neighbor.
 * If "no", remove the node from the returned collection.  Default is no.
 * <LI>incidentsid-Schema ID of incident edges.  If not specified, all incident edges are returned.
 * <LI>dirtype-If the connecting sid is a node, this specifies the
 * types of directed edge to include.  Options include:
 * <UL>
 * <LI>sourceonly-Return only edges which this node is incident as a source
 * <LI>targetonly-Return only edges which this node is incident as a target
 * <LI>all-Return all incident edges (Default)
 * </UL>
 * </UL>
 * 
 * @author namatag
 *
 */
public class Incident extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	private boolean unique = false;
	private boolean includeself = false;
	private String incidentsid = null;
	private int dirtype = 3;
	
	private void initialize() {
		initialize = false;
		unique = this.hasYesNoParameter("unique","yes");
		includeself = this.hasYesNoParameter("includeself","yes");
		if(this.hasParameter("incidentsid")) {
			incidentsid = this.getStringParameter("incidentsid");
		}
		
		if(this.hasParameter("dirtype")) {
			String dirtype = this.getCaseParameter("dirtype", new String[]{"sourceonly","targetonly","all"});
			
			if(dirtype.equals("sourceonly")) {
				this.dirtype = 1;
			} else if(dirtype.equals("targetonly")) {
				this.dirtype = 2;
			} else {
				this.dirtype = 3;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		if(initialize) {
			this.initialize();
		}
		
		Collection<GraphItem> deps = null;
		
		if(unique) {
			deps = new HashSet<GraphItem>();
		} else {
			deps = new ArrayList<GraphItem>();
		}
		
		List<GraphItem> iterator2list = null;
		if(incidentsid==null) {
			if(dirtype==1) {
				iterator2list = IteratorUtils.iterator2list(((Node) gi).getEdgesWhereSource());
			} else if(dirtype==2) {
				iterator2list = IteratorUtils.iterator2list(((Node) gi).getEdgesWhereTarget());
			} else {
				iterator2list = IteratorUtils.iterator2list(gi.getIncidentGraphItems());
			}
		} else {
			if(dirtype==1) {
				iterator2list = IteratorUtils.iterator2list(((Node) gi).getEdgesWhereSource(incidentsid));
			} else if(dirtype==2) {
				iterator2list = IteratorUtils.iterator2list(((Node) gi).getEdgesWhereTarget(incidentsid));
			} else {
				iterator2list = IteratorUtils.iterator2list(gi.getIncidentGraphItems(incidentsid));
			}
		}
		deps.addAll(iterator2list);
		
		// Do not include the data item itself
		if(!includeself) {
			deps.removeAll(Arrays.asList(new GraphItem[]{gi}));
		}
		
		return deps;
	}
}
