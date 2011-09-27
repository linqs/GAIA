package linqs.gaia.similarity;

import java.io.Serializable;

import linqs.gaia.configurable.Configurable;

/**
 * Base interface of all implementations of unnormalized similarity measures.
 * By definition, similarity is symmetric i.e., sim(a,b) == sim(b,a).
 * Also, the HIGHER the returned similarity value, the MORE similar
 * the two items are.
 * 
 * @author namatag
 *
 */
public interface Similarity<O> extends Configurable, Serializable {
	/**
	 * Return similarity between two items
	 * 
	 * @param item1 First Item
	 * @param item2 Second Item
	 * 
	 * @return Unnormalized similarity
	 */
	double getSimilarity(O item1, O item2);
}
