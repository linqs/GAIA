package linqs.gaia.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import linqs.gaia.exception.InvalidOperationException;

/**
 * Utilites to simplify handling lists.
 * 
 * @author namatag
 *
 */
public class ListUtils {
	/**
	 * Shuffle list given the seed
	 * 
	 * @param list List to shuffle
	 * @param seed Seed to use
	 * @return Copy of list where the order is shuffled
	 * @deprecated  When possible, use Collections.shuffle instead
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<?> shuffleList(List<?> list, int seed){
		Random rand = new Random(seed);
		List oldlist = new ArrayList(list);
		List newlist = new ArrayList();

		int listsize = oldlist.size();
		for(int i=0; i<listsize; i++){
			int index = rand.nextInt(oldlist.size());
			newlist.add(oldlist.get(index));
			oldlist.remove(index);
		}

		return newlist;
	}

	/**
	 * Return a shuffled order of the specified list indices
	 * 
	 * @param list List whose indices to shuffle
	 * @param rand Random number generator object
	 * @return List of shuffled indices
	 */
	public static List<Integer> shuffledIndices(List<?> list, Random rand){
		int listsize = list.size();
		List<Integer> indices = new ArrayList<Integer>();
		for(int i=0; i<listsize; i++){
			indices.add(i);
		}

		Collections.shuffle(indices, rand);

		return indices;
	}

	/**
	 * Return String representation of list
	 * 
	 * @param list List
	 * @return String representation
	 */
	public static String list2string(List<?> list){
		if(list==null){
			return null;
		}

		return ArrayUtils.array2String(list.toArray());
	}

	/**
	 * Return String representation of list
	 * 
	 * @param list List
	 * @param delimiter Delimiter to use between items
	 * @return String representation
	 */
	public static String list2string(List<?> list, String delimiter){
		if(list==null){
			return null;
		} else if(list.isEmpty()) {
			return "";
		}

		return ArrayUtils.array2String(list.toArray(), delimiter);
	}

	/**
	 * Create a double array given the list of Double objects.
	 * 
	 * @param list List of Double objects
	 * @return Array of double
	 */
	public static double[] doubleList2array(List<Double> list){
		double array[] = new double[list.size()];
		int size = array.length;
		for(int i=0; i<size; i++){
			array[i] = list.get(i);
		}

		return array;
	}

	/**
	 * Given a list and a number K,
	 * we return a new list with K items
	 * randomly chosen from the input list.
	 * 
	 * @param list List of Items
	 * @param K Number of items to pick
	 * @param rand Random number generator
	 * @return List of size K
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<?> pickKAtRandom(List<?> list, int K, Random rand) {
		if(K > list.size() || K<=0) {
			throw new InvalidOperationException("Invalid value for K: "+K
					+" given list of size "+list.size());
		}
		
		// Get a random ordering
		// From http://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
		int n = list.size();
		int i,j;
		int x[] = new int[n];
		for (i = 0; i < n; i++) {
			x[i] = i;
		}
		
		for (i = 0; i < K; i++) {
			j = i + rand.nextInt(n-i);
			// Randomly shuffle values to the first K indices
			int t = x[i];
			x[i] = x[j];
			x[j] = t;
		}
		
		List newlist = new ArrayList(K);
		for (i = 0; i < K; i++) {
			newlist.add(list.get(x[i]));
		}
		
		return newlist;
	}
}
