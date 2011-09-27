package linqs.gaia.sampler.decorable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.feature.decorable.Decorable;

/**
 * Sampler to perform leave one out cross validation.
 * In this sampler, each subset consists of exactly one
 * of the items.
 * 
 * @author namatag
 *
 */
public class LeaveOneOutSampler extends DecorableSampler {
	@Override
	public void generateSampling(Iterator<? extends Decorable> items) {
		while(items.hasNext()) {
			Decorable d = items.next();
			List<Decorable> split = new ArrayList<Decorable>(1);
			split.add(d);
			this.subsets.add(split);
			
			// Store list of all items
			this.allitems.add(d);
		}
		
		this.numsubsets = this.subsets.size();
	}
}
