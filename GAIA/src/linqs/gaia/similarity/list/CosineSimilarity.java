package linqs.gaia.similarity.list;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.util.Numeric;

/**
 * Calculate cosine similarity between two list of numeric objects.
 * The general formula for cosine similarity is given 
 * <a href="https://secure.wikimedia.org/wikipedia/en/wiki/Cosine_similarity">here</a>.
 * 
 * @author namatag
 *
 */
public class CosineSimilarity extends BaseConfigurable implements NormalizedListSimilarity {
	
	private static final long serialVersionUID = 1L;

	public double getSimilarity(List<? extends Object> item1, List<? extends Object> item2) {
		return this.getNormalizedSimilarity(item1, item2);
	}

	public double getNormalizedSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		if(item1.size()!=item2.size()) {
			throw new InvalidStateException("Lists must be the same length: "
					+item1.size()+"!="+item2.size());
		}
		
		double i1sqrsum = 0;
		double i2sqrsum = 0;
		double numerator = 0;
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			// Object must be some sort of numeric feature
			double dval1 = Numeric.parseDouble(i1val);
			double dval2 = Numeric.parseDouble(i2val);
			
			i1sqrsum += dval1*dval1;
			i2sqrsum += dval2*dval2;
			numerator += (dval1*dval2);
		}
		
		i1sqrsum = Math.sqrt(i1sqrsum);
		i2sqrsum = Math.sqrt(i2sqrsum);
		double denominator = i1sqrsum * i2sqrsum;
		
		return numerator/denominator;
	}
}
