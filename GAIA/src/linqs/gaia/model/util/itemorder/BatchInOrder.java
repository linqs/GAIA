package linqs.gaia.model.util.itemorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.util.SimplePair;

/**
 * Return items in the order they were given (i.e., order returned by iterable objects).
 * If multiple iterables are provided, return items
 * in all iterables in the order they are in the list.
 * 
 * @author namatag
 *
 */
public class BatchInOrder extends ItemOrder {
	private static final long serialVersionUID = 1L;
	private boolean initialized = false;
	private int index = 0;
	private List<? extends Iterable<?>> items;
	private Iterator<?> ditr;
	
	@Override
	public void initializeItemLists(List<? extends Iterable<?>> items) {
		this.items = items;
		this.index = 0;
		ditr = this.items.get(index).iterator();
		
		initialized = true;
	}
	
	@Override
	public SimplePair<Integer, Object> getNextPair() {
		if(!initialized) {
			throw new InvalidStateException("Items have not yet been initialized.");
		}
		
		Object di = null;
		while(!ditr.hasNext()) {
			index++;
			if(index < items.size()) {
				ditr = this.items.get(index).iterator();
			} else {
				return null;
			}
		}
		
		if(ditr.hasNext()) {
			di = ditr.next();
			return new SimplePair<Integer, Object>(index, di);
		} else {
			return null;
		}
	}

	@Override
	public void reset() {
		if(!initialized) {
			throw new InvalidStateException("Items have not yet been initialized.");
		}
		
		this.index = 0;
		ditr = this.items.get(index).iterator();
	}
	
	@Override
	public Object getNextItem() {
		return this.getNextPair().getSecond();
	}

	@Override
	public void initialize(Iterable<? extends Object> items) {
		List<Iterable<? extends Object>> listitems = new ArrayList<Iterable<? extends Object>>();
		listitems.add(items);
		this.initializeItemLists(listitems);
	}
}
