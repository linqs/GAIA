package linqs.gaia.feature.derived.neighbor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * 
 * Required Parameters:
 * <UL>
 * <LI> neighborclasses-Comma delimited set of neighbor classes where each
 * delimited value uses the format defined in Dynamic.forConfigurable.
 * </UL>
 * 
 * @author namatag
 *
 */
public class NeigborsOfNeighbors extends Neighbor {
	private static final long serialVersionUID = 1L;
	
	private boolean initialize = true;
	private List<Neighbor> neighbors = null;
	
	private void initialize() {
		initialize = false;
		
		String[] nclasses = this.getStringParameter("neighborclasses").split(",");
		neighbors = new ArrayList<Neighbor>(nclasses.length);
		for(String n:nclasses) {
			Neighbor currn = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, n, this);
			neighbors.add(currn);
		}
	}
	
	@Override
	protected Iterable<GraphItem> calcNeighbors(GraphItem gi) {
		if(initialize) {
			this.initialize();
		}
		
		Set<GraphItem> lastset = new HashSet<GraphItem>();
		lastset.add(gi);
		for(Neighbor n:neighbors) {
			Set<GraphItem> currset = new HashSet<GraphItem>();
			for(GraphItem currgi:lastset) {
				Iterable<GraphItem> itrbl = n.getNeighbors(currgi);
				for(GraphItem ngi:itrbl) {
					currset.add(ngi);
				}
			}
			
			lastset=currset;
		}
		
		lastset.remove(gi);
		
		return lastset;
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
