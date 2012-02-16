package linqs.gaia.feature.derived.neighbor;

import java.util.Set;

import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.IteratorUtils;

/**
 * An extension of the basic {@link Neighbor} class which
 * allows the neighborhood to be computed given some set of
 * graph items to ignore.  This extension allows for neighbor
 * implementations in which some nodes or edges may
 * have special characteristics (e.g., their existence maybe uncertain)
 * and need to be ignored during computation.
 * 
 * @author namatag
 *
 */
public abstract class NeighborWithOmmission extends Neighbor {
	private static final long serialVersionUID = 1L;

	/**
	 * For a given GraphItem, return the collection of GraphItems for use
	 * with generating the relational feature.
	 * 
	 * @param gi GraphItem to consider
	 * @param ignoreset Set of graph items to ignore when computing neighborhood
	 * @return Collection of the neighboring GraphItems of the specified GraphItem
	 */
	public Iterable<GraphItem> getNeighbors(GraphItem gi, Set<GraphItem> ignoreset) {
		initializeNeighbor();
		
		if(this.isCaching() && this.depcache.containsKey(gi)){
			return this.depcache.get(gi);
		}
		
		Iterable<GraphItem> deps = calcNeighbors(gi, ignoreset);
		if(this.isCaching()){
			this.depcache.put(gi, deps);
		}
		
		return deps;
	}
	
	/**
	 * For a given GraphItem, return the collection of GraphItems for use
	 * with generating the relational feature.
	 * 
	 * @param gi GraphItem to consider
	 * @param ignoregi Graph item to ignore when computing neighborhood
	 * @return Collection of the neighboring GraphItems of the specified GraphItem
	 */
	public Iterable<GraphItem> getNeighbors(GraphItem gi, GraphItem ignoregi) {
		initializeNeighbor();
		
		if(this.isCaching() && this.depcache.containsKey(gi)){
			return this.depcache.get(gi);
		}
		
		Iterable<GraphItem> deps = calcNeighbors(gi, ignoregi);
		if(this.isCaching()){
			this.depcache.put(gi, deps);
		}
		
		return deps;
	}
	
	/**
	 * Return the number of neighbors for item
	 * 
	 * @param gi Graph item to count neighbors for
	 * @param ignoreset Set of graph items to ignore when computing neighborhood
	 * @return Number of neighbors
	 */
	public int numNeighbors(GraphItem gi, Set<GraphItem> ignoreset) {
		Iterable<GraphItem> itrbl = getNeighbors(gi, ignoreset);
		return IteratorUtils.numIterable(itrbl);
	}
	
	/**
	 * Return the number of neighbors for item
	 * 
	 * @param gi Graph item to count neighbors for
	 * @param ignoregi Graph item to ignore when computing neighborhood
	 * @return Number of neighbors
	 */
	public int numNeighbors(GraphItem gi, GraphItem ignoregi) {
		Iterable<GraphItem> itrbl = getNeighbors(gi, ignoregi);
		return IteratorUtils.numIterable(itrbl);
	}
	
	/**
	 * Calculate dependency for the Graph Item.
	 * 
	 * @param gi Graph Item to get dependencies for
	 * @param ignoreset Set of graph items to ignore when computing neighborhood
	 * @return Set of dependencies
	 */
	protected abstract Iterable<GraphItem> calcNeighbors(GraphItem gi, Set<GraphItem> ignoreset);
	
	/**
	 * Calculate dependency for the Graph Item.
	 * 
	 * @param gi Graph Item to get dependencies for
	 * @param ignoregi Graph item to ignore when computing neighborhood
	 * @return Set of dependencies
	 */
	protected abstract Iterable<GraphItem> calcNeighbors(GraphItem gi, GraphItem ignoregi);
}
