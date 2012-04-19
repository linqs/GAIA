package linqs.gaia.model.util.itemorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.util.SimplePair;

/**
 * Return items in a random order.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> seed-Seed integer to use to randomly order items (using {@link Collections#shuffle(List, Random)}).
 * <LI>
 * 
 * @author namatag
 *
 */
public class RandomOrder extends ItemOrder {
	private static final long serialVersionUID = 1L;
	private boolean initialized = false;
	
	Iterator<SimplePair<Integer, Object>> spitr = null;
	List<SimplePair<Integer, Object>> allitems =
		new ArrayList<SimplePair<Integer, Object>>();
	
	@Override
	public void initializeItemLists(List<? extends Iterable<?>> items) {
		allitems.clear();
		
		for(int i=0; i<items.size(); i++) {
			Iterator<? extends Object> ditr = items.get(i).iterator();
			while(ditr.hasNext()) {
				allitems.add(new SimplePair<Integer,Object>(i, ditr.next()));
			}
		}
		
		int seed = this.getIntegerParameter("seed",0);
		Random rand = new Random(seed);
		Collections.shuffle(allitems, rand);
		spitr = allitems.iterator();
		
		initialized = true;
	}

	@Override
	public SimplePair<Integer, Object> getNextPair() {
		if(!initialized) {
			throw new InvalidStateException("Items have not yet been initialized.");
		}
		
		if(spitr.hasNext()){
			return spitr.next();
		} else {
			return null;
		}
	}

	@Override
	public void reset() {
		if(!initialized) {
			throw new InvalidStateException("Items have not yet been initialized.");
		}
		
		spitr = allitems.iterator();
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
