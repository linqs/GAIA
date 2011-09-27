package linqs.gaia.util;

import linqs.gaia.exception.InvalidStateException;

/**
 * A simple pair of values
 * 
 * @author namatag
 *
 * @param <K> Type of first value
 * @param <V> Type of second value
 */
public class SimplePair<K,V> implements Comparable<SimplePair<K,V>> {
	private K first;
	private V second;
	
	/**
	 * Constructor
	 * 
	 * @param first Value of first item
	 * @param second Value of second item
	 */
	public SimplePair(K first, V second){
		this.setFirst(first);
		this.setSecond(second);
	}
	
	/**
	 * Get first value
	 * 
	 * @return First value
	 */
	public K getFirst() {
		return first;
	}
	
	/**
	 * Set first value
	 * 
	 * @param first First value
	 */
	public void setFirst(K first) {
		this.first = first;
	}
	
	/**
	 * Get second value
	 * 
	 * @return Second value
	 */
	public V getSecond() {
		return second;
	}
	
	/**
	 * Set second value
	 * 
	 * @param second Second value
	 */
	public void setSecond(V second) {
		this.second = second;
	}
	
	/**
	 * Print string representation in the form first.toString():second.toString().
	 */
	public String toString() {
		return this.first.toString() + ":" + this.second.toString();
	}
	
	/**
	 * Objects are equal if their first and second values are equal.
	 */
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
	    if (this == obj) {
	      return true;
	    }
	    
	    if (obj==null || !(obj instanceof SimplePair<?,?>)) {
	      return false;
	    }
	    
	    @SuppressWarnings("unchecked")
		SimplePair<K,V> p = (SimplePair<K, V>) obj;
	    
	    return this.getFirst().equals(p.getFirst()) && this.getSecond().equals(p.getSecond());
	}
	
	public int hashCode() {
	  int hash = 1;
	  hash = hash * 31 + this.first.hashCode();
	  hash = hash * 31 + this.second.hashCode();

	  return hash;
	}
	
	/**
	 * Simple pairs are ordered by the defined ordering of the second object.
	 * If the values are tied, ordering is then done on the defined ordering of the first object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int compareTo(SimplePair<K,V> o) {
		if(this == o || this.equals(o)) {
			return 0;
		}
		
		Object osec = o.getSecond();
		Object currsec = this.getSecond();
		
		if(osec.getClass() != currsec.getClass()
			|| !(osec instanceof Comparable)
			|| !(currsec instanceof Comparable)) {
			throw new ClassCastException("Identical comparable classes of second item expected: "
					+currsec.getClass().getCanonicalName()
					+" compared to object "
					+osec.getClass().getCanonicalName()
					);
		}
		
		int scomp = ((Comparable) currsec).compareTo(osec);
		if(scomp == 0) {
			// Compare first if they second value are tied
			Object ofirst = o.getFirst();
			Object currfirst = this.getFirst();
			
			if(ofirst.getClass() != currfirst.getClass()
				|| !(ofirst instanceof Comparable)
				|| !(currfirst instanceof Comparable)) {
				throw new ClassCastException("Identical comparable classes of first item expected: "
						+currfirst.getClass().getCanonicalName()
						+" compared to object "
						+ofirst.getClass().getCanonicalName()
						);
			}
			
			int firstiscomp = ((Comparable) currfirst).compareTo(ofirst);
			if(firstiscomp==0) {
				throw new InvalidStateException("Two unequal pairs being ordered as equal: "
						+this+" and "+o);
			}
			
			return firstiscomp;
		} else {
			return scomp;
		}
	}
}
