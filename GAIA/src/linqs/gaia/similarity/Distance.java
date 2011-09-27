package linqs.gaia.similarity;

/**
 * Base interface of all implementations of unnormalized distance measures.
 * By definition, distance is symmetric i.e., dist(a,b) == dist(b,a)
 * where the HIGHER the returned similarity value, the LESS similar
 * the two items are.
 * 
 * @author namatag
 *
 */
public interface Distance<O> {
	/**
	 * Return distance between two items
	 * 
	 * @param item1 First Item
	 * @param item2 Second Item
	 * 
	 * @return Unnormalized distance
	 */
	double getDistance(O item1, O item2);
}
