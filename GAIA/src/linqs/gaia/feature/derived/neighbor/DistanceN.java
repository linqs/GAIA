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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;

/**
 * Return all unique graph items a distance n away from a specified item.
 * For nodes, this is the set of nodes you can reach within and/or with
 * n edges. For edges, this is the set of edges you can reach with and/or with
 * n nodes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> depth-Depth to go to (i.e., the N value).  Default is infinity
 * (i.e., all transitive neighbors).
 * <LI>distinct-Set to "yes" if you want only the graph items at a given level
 * (i.e., only return those first seen 2 away)
 * and "no" if you want the aggregate up to that level
 * (i.e., only return those 1 and/or 2 away). Default is "no".
 * <LI>schemaid-Set to the schema id of the items you want return
 * where only neighbors depth n away, with the given schema, is returned.
 * <LI>adjclass-Specify the {@link Adjacent} neighbor,
 * instantiated using in {@link Dynamic#forConfigurableName},
 * which defines which items are a distance 1 away.
 * Default is to use {@link Adjacent} with default parameter values.
 * <LI> includeself-If yes, include self as one of the neighbors.
 * Default is no.
 * </UL>
 * 
 * @author namatag
 *
 */
public class DistanceN extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	private Adjacent adj = null;
	private boolean distinct = false;
	private String schemaid = null;
	private int depth = Integer.MAX_VALUE;
	private boolean initialize = true;
	private boolean includeself = false;
	
	public DistanceN(){
		this(new Adjacent());
	}
	
	/**
	 * Allow a previous {@link Adjacent} to be passed to allow for sharing
	 * in cached information
	 * 
	 * @param adj {@link Adjacent} neighbor to use
	 */
	public DistanceN(Adjacent adj){
		this.adj = adj;
	}
	
	private void initialize() {
		initialize = false;
		
		this.distinct = this.hasYesNoParameter("distinct","yes");
		
		if(this.hasParameter("schemaid")) {
			this.schemaid = this.getStringParameter("schemaid");
		}
		
		if(this.hasParameter("depth")) {
			this.depth = (int) this.getDoubleParameter("depth");
		}
		
		if(this.depth < 1) {
			throw new ConfigurationException("Invalid value for depth: "+this.depth);
		}
		
		if(this.hasParameter("adjclass")) {
			String d1class = this.getStringParameter("adjclass");
			adj = (Adjacent) Dynamic.forConfigurableName(Adjacent.class, d1class, this);
		}
		
		includeself = this.hasYesNoParameter("includeself", "yes");
	}
	
	@Override
	public Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		if(initialize) {
			this.initialize();
		}
		
		Set<GraphItem> deps = new HashSet<GraphItem>();
		List<Set<GraphItem>> depths = new ArrayList<Set<GraphItem>>();
		Set<GraphItem> added = new HashSet<GraphItem>();
		
		Set<GraphItem> unexplored = new HashSet<GraphItem>();
		unexplored.add(gi);
		int depth = this.depth;
		
		for(int i=0; i<depth; i++){
			Set<GraphItem> newunexplored = new HashSet<GraphItem>();
			for(GraphItem ugi:unexplored){
				Iterable<GraphItem> d1gitems = adj.getNeighbors(ugi);
				for(GraphItem d1gi:d1gitems) {
					newunexplored.add(d1gi);
				}
			}
			
			newunexplored.removeAll(added);
			depths.add(newunexplored);
			unexplored = newunexplored;
			
			added.addAll(newunexplored);
			
			if(unexplored.isEmpty()) {
				break;
			}
		}
		
		if(this.distinct){
			// Handle case where no neighbors
			// of a given value is in a given depth
			if(depths.size()==depth) {
				deps.addAll(depths.get(depth-1));
			}
		} else {
			deps.addAll(added);
		}
		
		// Do not include the data item itself
		deps.remove(gi);
		
		// Only return those with the matching schema
		if(this.schemaid != null) {
			Set<GraphItem> currdeps = new HashSet<GraphItem>();
			
			for(GraphItem dgi:deps) {
				if(dgi.getSchemaID().equals(this.schemaid)) {
					currdeps.add(dgi);
				}
			}
			
			deps = currdeps;
		}
		
		// Forcively include self
		if(includeself) {
			deps.add(gi);
		}
		
		return deps;
	}
	
	public void setCache(boolean shouldcache) {
		super.setCache(shouldcache);
		adj.setCache(shouldcache);
	}
	
	public void reset(GraphItem gi) {
		super.reset(gi);
		adj.reset(gi);
	}
	
	public void resetAll() {
		super.resetAll();
		adj.resetAll();
	}
}
