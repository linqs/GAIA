package linqs.gaia.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Data structure for use with counting the number
 * of times a certain String value appears.
 * Useful for getting class distribution..
 * 
 * @author namatag
 *
 */
public class KeyedCount<K> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Map<K, Integer> counts;
	private Set<K> keys;
	private int totalCount;
	
	/**
	 * Constructor
	 */
	public KeyedCount(){
		counts = new LinkedHashMap<K, Integer>();
		keys = new LinkedHashSet<K>();
		totalCount = 0;
	}
	
	/**
	 * Increment the count for the given key.
	 * Note: Counts always start at 0.
	 * 
	 * @param key Key
	 */
	public void increment(K key){
		int newcount = 0;
		if(counts.containsKey(key)){
			newcount = counts.get(key);
		}
		
		newcount++;
		counts.put(key, newcount);
		keys.add(key);
		totalCount++;
	}
	
	/**
	 * Increment the count for the given key
	 * by the specified number of times.
	 * This is equivalent to calling {@link #increment}
	 * the same number of times as numincrement.
	 * Note: Counts always start at 0.
	 * 
	 * @param key Key
	 */
	public void increment(K key, int numincrement){
		int newcount = 0;
		if(counts.containsKey(key)){
			newcount = counts.get(key);
		}
		
		newcount+=numincrement;
		counts.put(key, newcount);
		keys.add(key);
		totalCount+=numincrement;
	}
	
	/**
	 * Decrement the count for the given key.
	 * Note: Counts always start at 0.
	 * 
	 * @param key Key
	 */
	public void decrement(K key){
		int newcount = 0;
		if(counts.containsKey(key)){
			newcount = counts.get(key);
		}
		
		newcount--;
		counts.put(key, newcount);
		keys.add(key);
		totalCount--;
	}
	
	/**
	 * Return true value of key was ever used i.e., decremented or incremented
	 * 
	 * @param key Key
	 * @return True if the Key was ever used, False otherwise.
	 */
	public boolean hasKey(K key){
		return keys.contains(key);
	}
	
	/**
	 * Get the list of keys counted
	 * 
	 * @return Set of String valued keys.
	 */
	public Set<K> getKeys(){
		return Collections.unmodifiableSet(keys);
	}
	
	/**
	 * Get the numeric count of the given key.
	 * This will return 0 for values not seen,
	 * as well as values seen but with a count 0
	 * (i.e., incremented and decremented to 0).
	 * 
	 * @param key String valued key
	 * @return Count of given key
	 */
	public int getCount(K key){
		if(counts.containsKey(key)){
			return counts.get(key);
		}
		
		return 0;
	}
	
	/**
	 * Get the percent count of the given key
	 * 
	 * @param key String valued key
	 * @return Percent of given key
	 */
	public double getPercent(K key){
		if(totalCount==0){
			return 0;
		}
		
		return (double) this.getCount(key)/(double) totalCount;
	}

	/**
	 * Return the number of counted values.
	 * 
	 * @return Total number of the counts
	 */
	public int totalCounted(){
		return this.totalCount;
	}
	
	/**
	 * Return key with highest count.
	 * If there are multiple with the same count,
	 * return a randomly chosen one from among them.
	 * A null is returned if no counts are given.
	 * 
	 * @return Key with highest count
	 */
	public K highestCountKey(){
		List<K> hck = this.highestCountKeys();
		
		if(hck == null || (hck != null && hck.isEmpty())) {
			return null;
		}
		
		return this.highestCountKeys().get(0);
	}
	
	/**
	 * Return keys with highest count.
	 * A null is returned if no counts are given.
	 * 
	 * @return Keys with highest count
	 */
	public List<K> highestCountKeys(){
		List<K> hckeys = null;
		
		Set<Entry<K, Integer>> entries = counts.entrySet();
		double maxcount = -1;
		
		for(Entry<K, Integer> e: entries){
			double val = e.getValue();
			K key = e.getKey();
			if(val>maxcount){
				maxcount = val;
				hckeys = new LinkedList<K>();
				hckeys.add(key);
			} else if(val == maxcount) {
				hckeys.add(key);
			}
		}
		
		return hckeys;
	}
	
	/**
	 * Return key with lowest count
	 * If there are multiple with the same value,
	 * return a randomly chosen one.
	 * 
	 * @return Key with lowest count
	 */
	public K lowestCountKey(){
		List<K> lck = this.lowestCountKeys();
		
		if(lck.isEmpty()) {
			return null;
		}
		
		return this.lowestCountKeys().get(0);
	}
	
	/**
	 * Return keys with lowest count
	 * 
	 * @return Keys with lowest count
	 */
	public List<K> lowestCountKeys(){
		List<K> lckeys = null;
		
		Set<Entry<K, Integer>> entries = counts.entrySet();
		double mincount = Integer.MAX_VALUE;
		
		for(Entry<K, Integer> e: entries){
			double val = e.getValue();
			K key = e.getKey();
			if(val<mincount){
				mincount = val;
				lckeys = new LinkedList<K>();
				lckeys.add(key);
			} else if(val == mincount) {
				lckeys.add(key);
			}
		}
		
		return lckeys;
	}
	
	/**
	 * Reset counts
	 */
	public void clearCounts(){
		this.counts.clear();
		this.keys.clear();
	}
	
	/**
	 * Remove the key such that if the value
	 * was previously defined, it will not
	 * be returned in the set of keys
	 * 
	 * @param key Key to remove
	 */
	public void removeKey(K key) {
		if(this.counts.containsKey(key)) {
			this.counts.remove(key);
			this.keys.remove(key);
		}
	}
	
	/**
	 * Return number of keys counted
	 * 
	 * @return Number of keys
	 */
	public int numKeys(){
		return this.keys.size();
	}
	
	/**
	 * Print keyed count
	 */
	public String toString(){
		if(counts.size()==0){
			return "Keyed Count Empty";
		}
		
		StringBuffer buf = new StringBuffer();
		int highest = this.counts.get(this.highestCountKey());
		int lowest = this.counts.get(this.lowestCountKey());
		List<Entry<K,Integer>> entries = new ArrayList<Entry<K,Integer>>();
		entries.addAll(this.counts.entrySet());
		
		// Sort the list using an anonymous inner class implementing Comparator for the compare method
        java.util.Collections.sort(entries, new Comparator<Map.Entry<K, Integer>>(){
            public int compare(Map.Entry<K, Integer> entry, Map.Entry<K, Integer> entry1)
            {
                // Return 0 for a match, -1 for less than and +1 for greater than
                return (entry.getValue().equals(entry1.getValue()) ? 0 : (entry.getValue() > entry1.getValue() ? 1 : -1));
            }
        });
        
		for(Entry<K, Integer> e:entries){
			double percent = (int) (this.getPercent(e.getKey())*100000);
			percent = (double) percent/1000;
			
			buf.append(e.getKey()+"="+e.getValue()+" ("+percent+"%)");
			if(highest==e.getValue()){
				buf.append(" MAX");
			} else if(lowest==e.getValue()){
				buf.append(" MIN");
			}
			
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	/**
	 * Return string representation of count
	 * 
	 * @param valseparator Separator of key-value pair (i.e., "=" means "key=value")
	 * @param countseparator Separator of count pairs (i.e., "," means "key1=value1,key2=value2")
	 * @return String representation
	 */
	public String toString(String valseparator, String countseparator) {
		String countsstring = null;
		Set<Entry<K, Integer>> entries = this.counts.entrySet();
		for(Entry<K, Integer> e:entries) {
			if(countsstring == null) {
				countsstring = "";
			} else {
				countsstring += countseparator;
			}
			
			countsstring += e.getKey() + valseparator + e.getValue();
		}
		
		return countsstring;
	}
	
	/**
	 * Override count for a specific key
	 * 
	 * @param key Key for count
	 * @param count Count value
	 */
	public void setCount(K key, int count) {
		int oldcount = this.getCount(key);
		int diff = oldcount - count;
		this.totalCount = this.totalCount - diff;
		
		this.counts.put(key, count);
		this.keys.add(key);
	}
	
	/**
	 * Update the total count counter.
	 * For use when you want to update
	 * the way percentage is computed.
	 * 
	 * @param totalcount Total count value to use
	 */
	public void setTotalCount(int totalcount) {
		this.totalCount = totalcount;
	}
	
	/**
	 * Return a KeyedCount where the number of a particular count
	 * in this KeyedCount is now being used as the key.
	 * i.e. Count the number of values which have a given count
	 * 
	 * @return Inverted KeyedCount
	 */
	public KeyedCount<Integer> invert(){
		KeyedCount<Integer> inverted = new KeyedCount<Integer>();
		Set<Entry<K,Integer>> entries = this.counts.entrySet();
        
		for(Entry<K, Integer> e:entries){
			inverted.increment(e.getValue());
		}
		
		return inverted;
	}
}
