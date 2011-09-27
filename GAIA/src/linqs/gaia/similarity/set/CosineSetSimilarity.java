package linqs.gaia.similarity.set;

import java.util.HashSet;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.similarity.NormalizedSetSimilarity;

/**
 * Calculate cosine similarity between two sets of objects.
 * The feature is computed by creating a vector, for each set, where the entries
 * in the vector is an 0/1 indicator of whether (1) or not (0) an item
 * appears in a given set.  The general formula for cosine
 * similarity is given 
 * <a href="https://secure.wikimedia.org/wikipedia/en/wiki/Cosine_similarity">here</a>.
 * When comparing items in a set, the equation can be simplified to:
 * <p>
 * CosineSimilarity(si,sj)=(Number of items in both si and sj) /
 * ((Square root of the number of elements in si) * (Square root of the number of elements in sj)).
 * 
 * @author namatag
 */
public class CosineSetSimilarity extends BaseConfigurable implements NormalizedSetSimilarity {
	private static final long serialVersionUID = 1L;
	
	public double getSimilarity(Set<? extends Object> item1, Set<? extends Object> item2) {
		double sim = 0;
		
		Set<Object> isect = new HashSet<Object>(item1);
		isect.retainAll(item2);
		
		sim = isect.size() / (Math.sqrt(item1.size()) * Math.sqrt(item2.size()));
		
		return sim;
	}
	
	public double getNormalizedSimilarity(Set<? extends Object> item1,
			Set<? extends Object> item2) {
		return this.getSimilarity(item1, item2);
	}
}
