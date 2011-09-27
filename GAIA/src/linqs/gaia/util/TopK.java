package linqs.gaia.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import linqs.gaia.log.Log;

/**
 * Return the Top K items where the top K is defined
 * as those with the K highest values.
 * <p>
 * Note:  This implementation stores a pointer
 * over the nodes of the top K.
 * 
 * @author namatag
 *
 * @param <O> Comparable object class to order
 */
public class TopK<O> {
	private int k;
	private PriorityQueue<Pair> topK;
	private Double borderval = Double.POSITIVE_INFINITY;
	private boolean savedups = false;
	private boolean reverse = false;

	/**
	 * Constructor
	 * 
	 * @param k Value of K
	 * @param savedups If true, you can return more than K objects
	 * if there are ties for values.
	 * If false, if there are multiple values with the same score and
	 * keeping them all will result in returning more than K,
	 * one of those instances will be arbitrarily removed.
	 */
	public TopK(int k, boolean savedups) {
		this.k = k;
		this.savedups = savedups;
		topK = new PriorityQueue<Pair>(k);
	}
	
	/**
	 * Constructor
	 * 
	 * @param k Value of K
	 * @param savedups If true, you can return more than K objects
	 * if there are ties for values.
	 * If false, if there are multiple values with the same score and
	 * keeping them all will result in returning more than K,
	 * one of those instances will be arbitrarily removed.
	 * @param reverse If true, use the natural reverse ordering to
	 * get the bottom K instead.
	 */
	public TopK(int k, boolean savedups, boolean reverse) {
		this.k = k;
		this.savedups = savedups;
		this.reverse = reverse;
		if(this.reverse) {
			topK = new PriorityQueue<Pair>(k,Collections.reverseOrder());
		} else {
			topK = new PriorityQueue<Pair>(k);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param k Value of K
	 */
	public TopK(int k) {
		this(k, false);
	}

	/**
	 * Get K value
	 * 
	 * @return K value
	 */
	public int getK() {
		return this.k;
	}

	/**
	 * Get top K objects
	 * 
	 * @return List of top K objects
	 */
	@SuppressWarnings("unchecked")
	public Set<O> getTopK() {
		Set<O> vals = new HashSet<O>();
		for(Pair pair: this.topK) {
			vals.add((O) pair.o);
		}

		return vals;
	}
	
	/**
	 * Get top K objects in decreasing order,
	 * or, if reverse and you want bottom K, in ascending order
	 * 
	 * @return List of top K objects
	 */
	@SuppressWarnings("unchecked")
	public List<O> getTopKSorted() {
		List<Pair> pairlist = new ArrayList<Pair>(this.topK);
		if(reverse) {
			Collections.sort(pairlist);
		} else {
			Collections.sort(pairlist,Collections.reverseOrder());
		}
		
		List<O> vals = new ArrayList<O>();
		for(Object pair: pairlist) {
			vals.add((O) ((Pair) pair).o);
		}

		return vals;
	}
	
	/**
	 * Get top K objects with scores in decreasing order,
	 * or, if reverse and you want bottom K, in ascending order
	 * 
	 * @return List of top K objects with scores
	 */
	@SuppressWarnings("unchecked")
	public List<SimplePair<O,Double>> getTopKSortedWithScores() {
		List<Pair> pairlist = new ArrayList<Pair>(this.topK);
		if(reverse) {
			Collections.sort(pairlist);
		} else {
			Collections.sort(pairlist,Collections.reverseOrder());
		}
		
		List<SimplePair<O,Double>> vals = new ArrayList<SimplePair<O,Double>>();
		for(Object pair: pairlist) {
			Pair p = (Pair) pair;
			vals.add(new SimplePair<O,Double>((O) p.o,p.d));
		}

		return vals;
	}

	/**
	 * Add item
	 * 
	 * @param value Value of object
	 * @param obj Object
	 * @return Items removed as a result of this insertion
	 * or the obj itself, if it was not added.
	 */
	@SuppressWarnings("unchecked")
	public List<O> add(Double value, O obj) {
		List<O> removed = new ArrayList<O>();
		
		if(topK.size() < this.k) {
			// Fill K until full
			this.topK.add(new Pair(value, obj));
			this.borderval = this.topK.peek().d;
		} else if((reverse && value > this.borderval) || (!reverse && value < this.borderval)) {
			// If adding something whose value is lower
			// than the lowest current value, do not add obj.
			// Instead, return obj as removed.
			removed.add(obj);
		} else if(topK.contains(new Pair(value, obj))) {
			// Handle saving duplicates, if requested
			if(savedups) {
				topK.add(new Pair(value, obj));
			} else {
				// If not saving duplicates,
				// and this item has a duplicate score,
				// do not add.  Instead, return obj as removed.
				removed.add(obj);
			}
		} else if((reverse && value < this.borderval) || (!reverse && value > this.borderval)) {
			topK.add(new Pair(value, obj));

			// If adding something higher than the lowest value,
			// remove the lowest value and insert the new value.
			List<Pair> dups = new ArrayList<Pair>();
			while(this.topK.peek().d.equals(this.borderval)) {
				dups.add(this.topK.poll());
			}

			// Least common value is a duplicate
			// and the removal results in less than K
			if(this.topK.size()<this.k) {
				if(this.savedups) {
					// add it all back if saving duplicates
					this.topK.addAll(dups);
					dups.clear();
				} else if(!this.savedups) {
					// add just enough back to get back to K
					for(Pair p:dups) {
						if(this.topK.size()<this.k) {
							this.topK.add(p);
						} else {
							removed.add((O) p.o);
						}
					}
				}
			} else {
				// All of these were removed
				for(Pair p:dups) {
					removed.add((O) p.o);
				}
			}

			// Update minval with the current lowest value.
			this.borderval = topK.peek().d;
		}
		
		return removed;
	}

	public List<O> add(double value, O obj) {
		return this.add(new Double(value), obj);
	}
	
	public void clear() {
		this.topK.clear();
		this.borderval = Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Pair object to store the object within
	 * 
	 * @author namatag
	 *
	 */
	private class Pair implements Comparable<Pair> {
		public Double d = null;
		public Object o = null;

		public Pair(double d, Object o) {
			this.d = d;
			this.o = o;
		}

		/**
		 * Objects are equal if d is equal
		 */
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			// Not strictly necessary, but often a good optimization
			if (this == obj) {
				return true;
			}

			if (obj==null || !(obj instanceof TopK.Pair)) {
				return false;
			}

			Pair p = (Pair) obj;

			return this.d.equals(p.d);
		}

		public int hashCode() {
			return this.d.hashCode();
		}
		
		/**
		 * Comparison is done solely on d
		 */
		public int compareTo(Pair o) {
			return this.d.compareTo(o.d);
		}
		
		public String toString() {
			return this.o+"="+this.d;
		}
	}

	public static void main(String[] args) {
		Log.showAllLogging();
		
		TopK<String> topk = new TopK<String>(3, true, false);
		Log.DEBUG("Removed="+topk.add(10.0, "10a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(3.0, "3")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(4.0, "4")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(5.0, "5")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10d")+" TopK="+topk.getTopK());
		Log.DEBUG("Sorted: "+topk.getTopKSorted());
		Log.DEBUG("Sorted with scores: "+topk.getTopKSortedWithScores());
		
		topk = new TopK<String>(3, false, true);
		Log.DEBUG("Removed="+topk.add(10.0, "10a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(3.0, "3")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(4.0, "4")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(5.0, "5")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10d")+" TopK="+topk.getTopK());
		Log.DEBUG("Sorted: "+topk.getTopKSorted());
		Log.DEBUG("Sorted with scores: "+topk.getTopKSortedWithScores());
		
		topk = new TopK<String>(3, false, false);
		Log.DEBUG("Removed="+topk.add(10.0, "10a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(3.0, "3")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(4.0, "4")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(5.0, "5")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10d")+" TopK="+topk.getTopK());
		Log.DEBUG("Sorted: "+topk.getTopKSorted());
		Log.DEBUG("Sorted with scores: "+topk.getTopKSortedWithScores());
		
		topk = new TopK<String>(3, true, true);
		Log.DEBUG("Removed="+topk.add(10.0, "10a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(1.0, "1b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(2.0, "2c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(3.0, "3")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(4.0, "4")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(5.0, "5")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6a")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(6.0, "6b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10b")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10c")+" TopK="+topk.getTopK());
		Log.DEBUG("Removed="+topk.add(10.0, "10d")+" TopK="+topk.getTopK());
		Log.DEBUG("Sorted: "+topk.getTopKSorted());
		Log.DEBUG("Sorted with scores: "+topk.getTopKSortedWithScores());
	}
}
