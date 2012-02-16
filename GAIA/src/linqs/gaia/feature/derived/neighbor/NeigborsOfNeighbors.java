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
 * Neighbor class defined using other neighbor classes.
 * Given the graph item and a list of neighbor classes,
 * this neighbor class uses the first neighbor class to get
 * the first set of neighbors.  The second neighbor class
 * is then used over all the neighbors in the first set
 * to get a second set of neighbors.  This is repeated
 * until the last neighbor class is applied to the second
 * to the last set of neighbors.
 * The last set of neighbors are then returned.
 * <p>
 * Note: The graph item itself is never returned as its own neighbor.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> neighborclasses-Comma delimited set of neighbor classes where each
 * delimited value uses the format defined in Dynamic.forConfigurable.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NeigborsOfNeighbors extends NeighborWithOmmission {
	private static final long serialVersionUID = 1L;
	
	private List<Neighbor> neighbors = null;
	
	protected void initialize() {
		String[] nclasses = this.getStringParameter("neighborclasses").split(",");
		neighbors = new ArrayList<Neighbor>(nclasses.length);
		for(String n:nclasses) {
			Neighbor currn = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, n, this);
			neighbors.add(currn);
		}
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi, Set<GraphItem> ignoreset) {
		Set<GraphItem> lastset = new HashSet<GraphItem>();
		lastset.add(gi);
		for(Neighbor n:neighbors) {
			Set<GraphItem> currset = new HashSet<GraphItem>();
			for(GraphItem currgi:lastset) {
				Iterable<GraphItem> itrbl = null;
				if(n instanceof NeighborWithOmmission) {
					itrbl = ((NeighborWithOmmission) n).getNeighbors(currgi, ignoreset);
				} else {
					throw new ConfigurationException("Neighbor class is not able to handle the ignore set: "
							+n.getClass().getCanonicalName());
				}
				
				for(GraphItem ngi:itrbl) {
					currset.add(ngi);
				}
			}
			
			lastset=currset;
		}
		
		lastset.remove(gi);
		
		return lastset;
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi, GraphItem ignoregi) {
		Set<GraphItem> lastset = new HashSet<GraphItem>();
		lastset.add(gi);
		for(Neighbor n:neighbors) {
			Set<GraphItem> currset = new HashSet<GraphItem>();
			for(GraphItem currgi:lastset) {
				Iterable<GraphItem> itrbl = null;
				if(n instanceof NeighborWithOmmission) {
					itrbl = ((NeighborWithOmmission) n).getNeighbors(currgi, ignoregi);
				} else {
					throw new ConfigurationException("Neighbor class is not able to handle the ignore set: "
							+n.getClass().getCanonicalName());
				}
				
				for(GraphItem ngi:itrbl) {
					currset.add(ngi);
				}
			}
			
			lastset=currset;
		}
		
		lastset.remove(gi);
		
		return lastset;
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		GraphItem ignoregi = null;
		return calcNeighbors(gi, ignoregi);
	}
	
	/**
	 * If the neighbors are cached to reduce run time,
	 * call this function when you want the value to be fully
	 * recalculated.
	 * 
	 * @param gi Graph Item to calculate dependency for
	 */
	public void reset(GraphItem gi) {
		for(Neighbor n:neighbors) {
			n.reset(gi);
		}
		
		this.depcache.remove(gi);
	}
	
	/**
	 * If the neighbors are cached to reduce run time,
	 * call this function when you want the value to be fully
	 * recalculated.
	 */
	public void resetAll() {
		for(Neighbor n:neighbors) {
			n.resetAll();
		}
		
		this.depcache.clear();
	}
	
	/**
	 * Set whether or not to cache the neighbors
	 * 
	 * @param iscaching True to cache and false otherwise.
	 */
	public void setCache(boolean iscaching) {
		// Clear cache whenever the value is changed
		if(this.isCaching != iscaching){
			this.resetAll();
		}
		
		for(Neighbor n:neighbors) {
			n.setCache(isCaching);
		}
		
		this.isCaching = iscaching;
	}
}
