package linqs.gaia.feature.derived.neighbor;

import java.io.Serializable;
import java.util.HashMap;

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
	
	protected boolean isCaching = false;
	protected HashMap<GraphItem,Iterable<GraphItem>> depcache
			= new HashMap<GraphItem,Iterable<GraphItem>>();
	
	/**
	 * For a given GraphItem, return the collection of GraphItems for use
	 * with generating the relational feature.
	 * 
	 * @param gi GraphItem to consider
	 * @return Collection of the neighboring GraphItems of the specified GraphItem
	 */
	public Iterable<GraphItem> getNeighbors(GraphItem gi) {
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
	 * Calculate dependency for the Graph Item.
	 * 
	 * @param gi Graph Item to get dependencies for
	 * @return Set of dependencies
	 */
	protected abstract Iterable<GraphItem> calcNeighbors(GraphItem gi);
}
