package linqs.gaia.sampler;

import java.util.Iterator;

/**
 * Interfaces for dividing datasets into useable splits,
 * for such uses as distinguishing between training and testing data.
 *
 * @param <T> Set T split
 * @author Namata
 */
public interface Sampler<T> {
	/**
	 * Generate a sampling of the provided graph.
	 * 
	 * @param items Set of decorable items to sample
	 */
    public void generateSampling(Iterator<? extends T> items);
    
    /**
     * Return the number of training and test sample pairs generated.
     * 
     * @return Number of training and test sample pairs.
     */
    public int getNumSubsets();
    
    /**
     * Return the decorable items in the specified subset
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
    public Iterable<T> getSubset(int index);
    
    /**
     * Return the decorable items NOT in the specified subset
     * 
     * @param index Index of subset
     * @return Iterable of the decorable items
     */
	public Iterable<T> getNotInSubset(int index);
}
