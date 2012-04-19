package linqs.gaia.model.util.itemorder;


import java.io.Serializable;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.util.SimplePair;

/**
 * Utility which takes an iterable object of items (or list of iterable objects)
 * and returns their items in a particular order.  This can be used, for example,
 * in iterative algorithms to test the sensitivity of the inference to the order
 * the test instances are evaluation.
 * 
 * @author namatag
 *
 */
public abstract class ItemOrder extends BaseConfigurable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Initialize the object with the set of items you want returned
	 * in some order.
	 * 
	 * @param items Iterable object of items
	 */
	public abstract void initialize(Iterable<? extends Object> items);
	
	/**
	 * Initialize the object with the set of items you want returned
	 * in some order.
	 * 
	 * @param items List which contains iterable objects of items.
	 */
	public abstract void initializeItemLists(List<? extends Iterable<?>> items);
	
	/**
	 * Get the next item defined by this order.
	 * 
	 * @return Next item
	 */
	public abstract Object getNextItem();
	
	/**
	 * Get the next index-item in the order (where the index is list index where
	 * the Iterable with this item was found or "0" if a single iterable object was provided)
	 * or null if all the items have been returned.
	 * 
	 * @return Next in order of the Index-Item pairs
	 */
	public abstract SimplePair<Integer, Object> getNextPair();
	
	/**
	 * Reset the order starts from the beginning, assuming no items were previously
	 * returned, and return items until the full set of items is return.
	 */
	public abstract void reset();
}
