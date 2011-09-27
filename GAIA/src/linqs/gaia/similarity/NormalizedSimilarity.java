package linqs.gaia.similarity;

/**
 * Normalized similarity is an interface for all Similarity
 * measures which are always between 0 and 1 where 1
 * means exactly similar and 0 is not.
 * 
 * @author namatag
 *
 */
public interface NormalizedSimilarity<O> extends Similarity<O> {
	/**
	 * Return normalized similarity between two items
	 * 
	 * @param item1 First Item
	 * @param item2 Second Item
	 * 
	 * @return Unnormalized similarity
	 */
	double getNormalizedSimilarity(O item1, O item2);
}
