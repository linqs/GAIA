package linqs.gaia.feature.derived.aggregate;

import java.util.LinkedList;
import java.util.List;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.util.KeyedSum;
import linqs.gaia.util.SimplePair;

/**
 * This is a multi-valued derived feature which, for every
 * possible category of a given categorical feature,
 * return the proportion of neighbors for that value.
 * This feature only applies to graph items.
 * Neighborhood is defined by the specified Neighbor class.
 * <p>
 * Note:  Since this code shares the same characteristics
 * as {@link NeighborValueCount}, it uses
 * the same code base.  See {@link NeighborValueCount}
 * for the parameters.
 * 
 * @see linqs.gaia.feature.derived.aggregate.NeighborValueCount
 * 
 * @author namatag
 *
 */
public class NeighborValuePercent extends NeighborValueCount {
	private static NumValue numvalue0 = new NumValue(0.0);
	
	public FeatureValue calcFeatureValue(Decorable di) {
		if(features==null) {
			this.initialize();
		}
		
		// Get counts of features from neighbors
		KeyedSum<String> count = this.getCount(di, this.weightbyprob);
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(SimplePair<String, CVFeature> pair:features) {
			// Get percent of neighbors with a given value
			double value = count.getPercent(pair.getFirst());
			if(value==0) {
				fvalues.add(numvalue0);
			} else {
				fvalues.add(new NumValue(value));
			}
		}
		
		return (FeatureValue) new CompositeValue(fvalues);
	}
}
