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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.IteratorUtils;

/**
 * Class to specify which items to get relational or aggregate
 * features from.  These "neighbors" can be cached for when
 * the dependencies are not changing.
 * 
 * @author namatag
 *
 */
public abstract class Neighbor extends BaseConfigurable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	protected boolean isCaching = false;
	protected Map<GraphItem,Iterable<GraphItem>> depcache
			= new ConcurrentHashMap<GraphItem,Iterable<GraphItem>>();
	
	/**
	 * For a given GraphItem, return the collection of GraphItems for use
	 * with generating the relational feature.
	 * 
	 * @param gi GraphItem to consider
	 * @return Collection of the neighboring GraphItems of the specified GraphItem
	 */
	public Iterable<GraphItem> getNeighbors(GraphItem gi) {
		initializeNeighbor();
		
		if(this.isCaching() && this.depcache.containsKey(gi)){
			return this.depcache.get(gi);
		}
		
		Iterable<GraphItem> deps = calcNeighbors(gi);
		if(this.isCaching()){
			this.depcache.put(gi, deps);
		}
		
		return deps;
	}
	
	/**
	 * Return the number of neighbors for item
	 * 
	 * @param gi Graph item to count neighbors for
	 * @return Number of neighbors
	 */
	public int numNeighbors(GraphItem gi) {
		Iterable<GraphItem> itrbl = getNeighbors(gi);
		return IteratorUtils.numIterable(itrbl);
	}
	
	/**
	 * If the neighbors are cached to reduce run time,
	 * call this function when you want the value to be fully
	 * recalculated.
	 * 
	 * @param gi Graph Item to calculate dependency for
	 */
	public void reset(GraphItem gi) {
		this.depcache.remove(gi);
	}
	
	/**
	 * If the neighbors are cached to reduce run time,
	 * call this function when you want the value to be fully
	 * recalculated.
	 */
	public void resetAll() {
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
		
		this.isCaching = iscaching;
	}
	
	/**
	 * Check to see if the dependency is caching
	 * 
	 * @return True if caching and false otherwise
	 */
	public boolean isCaching() {
		return this.isCaching;
	}
	
	/**
	 * Initialize the Neighbor object using the
	 * {@link #initialize()} method.
	 * If you need to initialize for a specific method in your neighbor object,
	 * use this, instead of {@link #initialize()},
	 * to ensure initialization is thread safe.
	 * 
	 */
	protected void initializeNeighbor() {
		// Return quickly, if initialization definitely done
		if(!initialize) {
			return;
		}
		
		// Ensure that it is only initialized once
		synchronized(this) {
			if(initialize) {
				this.initialize();
				initialize = false;
			}
		}
	}
	
	/**
	 * Calculate dependency for the Graph Item.
	 * 
	 * @param gi Graph Item to get dependencies for
	 * @return Set of dependencies
	 */
	protected abstract Iterable<GraphItem> calcNeighbors(GraphItem gi);
	
	/**
	 * Protected function to initialize any parameters of the neighbor object.
	 * If you need to initialize for a specific method in your neighbor object,
	 * use {@link #initializeNeighbor()} to ensure initialization is thread safe.
	 */
	abstract protected void initialize();
}
