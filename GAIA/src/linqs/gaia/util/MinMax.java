package linqs.gaia.util;

/**
 * Data structure to simplify task of getting the minimum
 * or maximum value of something.
 * 
 * @author namatag
 *
 */
public class MinMax {
	private double max = Double.NEGATIVE_INFINITY;
	private double min = Double.POSITIVE_INFINITY;
	private int numconsidered = 0;
	private double sum = 0;
	
	/**
	 * Consider new value as max or min
	 * 
	 * @param val Value
	 */
	public void addValue(double val){
		if(val<min) {
			min = val;
		}
		
		if(val>max){
			max = val;
		}
		
		numconsidered++;
		sum+=val;
	}
	
	/**
	 * Reset object
	 */
	public void reset() {
		max = Double.NEGATIVE_INFINITY;
		min = Double.POSITIVE_INFINITY;
		numconsidered = 0;
		sum = 0;
	}

	/**
	 * Get max value
	 * 
	 * @return Max value
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Get min value
	 * 
	 * @return Min value
	 */
	public double getMin() {
		return min;
	}
	
	/**
	 * Get mean value
	 * 
	 * @return Mean value
	 */
	public double getMean() {
		return sum/(double) this.numconsidered;
	}
	
	/**
	 * Get number of values considered
	 * 
	 * @return Number considered
	 */
	public int getNumConsidered() {
		return this.numconsidered;
	}
	
	/**
	 * Get min value
	 * 
	 * @return Min value
	 */
	public double getSumTotal() {
		return sum;
	}
	
	/**
	 * Return the string representation of this object.
	 * Format: "Num Considered=XX SumTotal=XX Min=XX Max=XX Mean=XX".
	 */
	public String toString() {
		return "Num Considered="+this.getNumConsidered()
			+" SumTotal="+this.getSumTotal()
			+" Min="+this.getMin()
			+" Max="+this.getMax()
			+" Mean="+this.getMean();
	}
}
