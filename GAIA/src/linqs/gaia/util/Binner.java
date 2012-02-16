package linqs.gaia.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.log.Log;

/**
 * Utility for binning numerical values.
 * This utility takes as argument a set of numerical values
 * and the number of bins, k, you went to separate them into.
 * This utility computes a range such that all numbers below
 * some value v<=v1 is in bin 1, v1<v<=v2 is in bin 2,
 * v2<v<=v3 is in bin 3, and so on until v > vk is in bin k.
 * This is done one of two ways.
 * The first uses only the minimum and maximum values given the sample
 * set and creates k evenly sized bins between those to values.
 * The second looks at the different values in the sample
 * and tries to construct bins where the number of sampled
 * instances are evenly distributed among the bins.
 * 
 * @author namatag
 *
 */
public class Binner {
	private List<Double> borders = null;
	private int numbins;
	
	/**
	 * Constructor
	 * 
	 * @param values Values to bin
	 * @param numbins Number of bins
	 * @param userange Compute the bins using the minimum and maximum values only 
	 */
	public Binner(List<Double> values, int numbins, boolean userange) {
		if(numbins<2) {
			throw new InvalidStateException("Minimum two bins: "+numbins);
		}
		
		this.numbins = numbins;
		
		if(userange) {
			MinMax mm = new MinMax();
			for(Double d:values) {
				mm.addValue(d);
			}
			
			Double min = mm.getMin();
			Double max = mm.getMax();
			Double range = max - min;
			Double interval = (Double) range/numbins;
			this.borders = new ArrayList<Double>();
			for(int i=0; i<numbins; i++) {
				this.borders.add(min+(i*interval));
			}
		} else {
			KeyedCount<Double> counts = new KeyedCount<Double>();
			for(Double d:values) {
				counts.increment(d);
			}
			
			// Sort the unique values
			List<Double> uniquevalues = new ArrayList<Double>(counts.getKeys());
			Collections.sort(uniquevalues);
			
			int totalcount = (int) counts.totalCounted();
			int idealperbin = numbins>totalcount ? 1 : totalcount/numbins;
			int numuniquevalues = uniquevalues.size();
			Log.DEBUG("Total count: "+totalcount
					+" Requested number of bins: "+this.numbins
					+" Ideal per bin: "+idealperbin
					+" Number unique: "+numuniquevalues);
			
			// If the number of unique values is less than the number of
			// bins, just use those values as the bin values
			if(numuniquevalues<=numbins) {
				this.borders = new ArrayList<Double>(uniquevalues);
			} else {
				// Compute the border for the bins
				this.borders = new ArrayList<Double>();
				int currbincount = 0;
				int totalbincount = 0;
				for(int i=0; i<numuniquevalues; i++) {
					Double currvalue = uniquevalues.get(i);
					int currcount = (int) counts.getCount(currvalue);
					currbincount += currcount;
					if(currbincount>=idealperbin) {
						// See if putting this value in this bin
						// versus the next bin causes more imbalance
						borders.add(currvalue);
						totalbincount+=currbincount;
						currbincount=0;
						
						// Recompute ideal per bin to
						// adjust for large bins
						int numbinsremaining = numbins-borders.size();
						if(numbinsremaining!=0) {
							idealperbin = numbins>totalcount ?
								1 : (totalcount-totalbincount)/numbinsremaining;
						}
					}
				}
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Bin value borders: "+ListUtils.list2string(borders, ","));
		}
	}
	
	/**
	 * Get the bin for the current value
	 * 
	 * @param value Value
	 * @return Bin
	 */
	public int getBin(Double value) {
		int size = borders.size();
		for(int i=0; i<size; i++) {
			Double rangemin = i==0 ? Double.NEGATIVE_INFINITY : borders.get(i-1);
			Double rangemax = borders.get(i);
			if(value>rangemin && value<=rangemax) {
				return i+1;
			}
		}
		
		return size;
	}
	
	/**
	 * Return the number of bins
	 * 
	 * @return Number of bins
	 */
	public int getNumBins() {
		return this.numbins;
	}
	
	public static void main(String[] args) {
		Log.SHOWDEBUG=true;
		List<Double> values =
			Arrays.asList(new Double[]{0.0,0.0,0.0,1.0,2.0,3.0});
		Binner binner = new Binner(values,3,false);
		
		List<Double> testvalues =
			Arrays.asList(new Double[]{0.0,1.0,1.0,2.0,2.0,3.0,3.0,6.0,7.0,100.0,-1.0,1.2});
		for(Double d:testvalues) {
			Log.INFO(d+" in bin "+binner.getBin(d));
		}
	}
}
