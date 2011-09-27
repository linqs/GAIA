package linqs.gaia.similarity.list;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.similarity.ListDistance;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.util.Numeric;

/**
 * Returns the euclidean distance between the vectors created
 * by the list (i.e., ((x1-y1)^2+(x2-y2)^2+...+(xn-yn)^2)^(1/2)).
 * Objects are parsed as values as defined in Numeric.ParseDouble.
 * <p>
 * We calculate normalized similarity as follows: 
 * ((x1-y1)^2+(x2-y2)^2+...+(xn-yn)^2)^(1/2))/n^(1/2) where all the
 * x and y values are normalized.  An exception is thrown if
 * x and y are not normalized.
 * For similarity, we return the normalized similarity value.
 * 
 * @see linqs.gaia.util.Numeric
 * 
 * @author namatag
 *
 */
public class Euclidean extends BaseConfigurable implements NormalizedListSimilarity,
	ListDistance {
	
	private static final long serialVersionUID = 1L;

	public double getSimilarity(List<? extends Object> item1, List<? extends Object> item2) {
		return this.getNormalizedSimilarity(item1, item2);
	}

	public double getNormalizedSimilarity(List<? extends Object> item1,
			List<? extends Object> item2) {
		double similarity = this.getDistance(item1, item2);
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			// Object must be some sort of numeric feature
			double dval1 = Numeric.parseDouble(i1val);
			double dval2 = Numeric.parseDouble(i2val);
			
			if(dval1 < 0 || dval1 > 1 || dval2 < 0 || dval2 > 1) {
				throw new InvalidOperationException("Values not normalized: "
						+"item1="+dval1+ " item2="+dval2);
			}
		}
		
		return (1-similarity)/Math.sqrt(item1.size());
	}

	public double getDistance(List<? extends Object> item1,
			List<? extends Object> item2) {
		double distance = 0;
		if(item1.size()!=item2.size()){
			throw new InvalidOperationException("Incomparable feature lists of varying sizes: item1="
					+item1.size()+ " item2="+item2.size());
		}
		
		for(int i=0; i<item1.size(); i++){
			Object i1val = item1.get(i);
			Object i2val = item2.get(i);
			
			if(i1val == null || i2val == null) {
				throw new InvalidOperationException("Values cannot be null: "
						+"item1="+i1val+ " item2="+i2val);
			}
			
			// Object must be some sort of numeric feature
			double dval1 = Numeric.parseDouble(i1val);
			double dval2 = Numeric.parseDouble(i2val);
			
			distance += Math.pow((dval1-dval2), 2);
		}
		
		return Math.sqrt(distance);
	}
}
