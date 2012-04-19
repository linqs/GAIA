package linqs.gaia.model.util.itemorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.util.SimplePair;

/**
 * Return items in the order they were given (i.e., order returned by iterable objects).
 * Unline {@link BatchInOrder}, however, this allows for only the items in an iterable item
 * in the list to be returned multiple times prior to proceeding to the next iterable
 * object on the list.  For example, suppose you have two iterable items and you want
 * the items in the first iterable object to be returned 5 times and the items in the second
 * to be return 10 times.  When you call {@link #getNextItem()} or {@link #getNextPair()},
 * it will continue returning the items in the first iterable object and then return null
 * once you have iterated over all items.  When you call {@link #reset()}, rather than going
 * to the next iterable item (as in {@link BatchInOrder}) it will start from the beginning
 * of the iterable item in the current slot and repeat this 4 more times (for a total of 5 iterations).
 * After the fift iteration, after calling reset, {@link #getNextItem()} or {@link #getNextPair()}
 * will return items from the second item.  The procedure is repeated for the second
 * iterable item 10 times.  Once the iterable items have been repeated the specified number of times,
 * any subsequent calls to reset will start from the beginning with the first iterable item.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>persetcounter-Comma delimited list of iterations for each iterable item
 * in the provided list.  This must have a one-on-one correspondence with the list
 * provided when calling {@link #initializeItemLists(List)}.  For example,
 * if this has value "persetcounter=2,4,6" we will iterate the item
 * in the iterable item of the first position 2 times, the second position 4 times,
 * and the third position 6 times.
 * </UL>
 * 
 * @author namatag
 *
 */
public class PipelineOrder extends ItemOrder {
	private static final long serialVersionUID = 1L;
	
	private boolean initialized = false;
	private int index = 0;
	private List<? extends Iterable<?>> items;
	private Iterator<? extends Object> ditr;
	private int currsetcounter = 0;
	private int[] persetcounter = null;
	private boolean stopcurrent = false;
	
	@Override
	public void initializeItemLists(List<? extends Iterable<?>> items) {
		String[] pscstring = this.getStringParameter("persetcounter").split(",");
		if(pscstring.length!=items.size()) {
			throw new ConfigurationException("Item list size and iteration numbers do not match: "
					+items.size()+"!="+pscstring.length);
		}
		
		persetcounter = new int[pscstring.length];
		int nonzeroindex = -1;
		for(int i=0; i<persetcounter.length; i++) {
			persetcounter[i] = Integer.parseInt(pscstring[i]);
			if(nonzeroindex == -1 && persetcounter[i]>0) {
				nonzeroindex = i;
			}
		}
		
		this.items = items;
		this.stopcurrent = false;
		this.currsetcounter = 0;
		
		if(nonzeroindex != -1) {
			// Find the first non-zero persetcounter value
			this.index = nonzeroindex;
			this.currsetcounter=0;
			ditr = this.items.get(index).iterator();
		} else {
			this.index = persetcounter.length-1;
			ditr = null;
		}
		
		initialized = true;
	}
	
	@Override
	public SimplePair<Integer, Object> getNextPair() {
		if(!initialized) {
			throw new InvalidStateException("Items have not yet been initialized.");
		}
		
		if(ditr != null && ditr.hasNext()) {
			Object di = ditr.next();
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
		
		if(!stopcurrent && currsetcounter<this.persetcounter[this.index]) {
			currsetcounter++;
		} else {
			this.stopcurrent = false;
			this.currsetcounter = 1;
			this.index++;
		}
		
		if(this.index<this.items.size()) {
			ditr = this.items.get(index).iterator();
		} else {
			ditr = null;
		}
	}
	
	/**
	 * When specified, the next time {@link #reset()} is called,
	 * even if the number of iterations in the current iterable item
	 * has not be completed, continue to the next iterable item.
	 */
	public void stopCurrent() {
		this.stopcurrent = true;
	}
	
	/**
	 * Return true if the pipeline order is on the last iterable item.
	 * 
	 * @return True if on the last iterable item, false otherwise.
	 */
	public boolean isLastInList() {
		if((this.index+1)==items.size()) {
			return true;
		} else {
			return false;
		}
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
