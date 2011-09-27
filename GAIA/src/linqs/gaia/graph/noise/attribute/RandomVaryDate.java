package linqs.gaia.graph.noise.attribute;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.noise.AttributeNoise;
import linqs.gaia.log.Log;

/**
 * Randomly add variance to the date values specified
 * 
 * Required Parameters:
 * <UL>
 * <LI> format-Date format, as specified by @{Calendar}, of the string value to parse.
 * (e.g., "yyyy'-'MM'-'dd'T'HH':'mm:ss.S'Z'")
 * <LI> schemaid-Schema ID of items whose date value we want to value
 * <LI> featureids-Feature IDs of string valued items whose value we want to parse as a date
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> probvary-Probability of varying the value of an item.  Default is 25%.
 * <LI> monthrange-If specified, a number is randomly chosen between 0 and this value
 * where the number is used to add or subtract from the month value.  Default is 0.
 * <LI> dayrange-If specified, a number is randomly chosen between 0 and this value
 * where the number is used to add or subtract from the day value.  Default is 0.
 * <LI> yearrange-If specified, a number is randomly chosen between 0 and this value
 * where the number is used to add or subtract from the year value.  Default is 0.
 * <LI> seed-Random number generator seed to use.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class RandomVaryDate extends AttributeNoise {
	private boolean initialize = true;
	private String schemaid = null;
	private String[] featureids = null;
	private SimpleDateFormat sdf = null;
	private Random rand = null;
	private double probvary = 0;
	private int monthrange = 0;
	private int dayrange = 0;
	private int yearrange = 0;
	
	private void initialize() {
		initialize = false;
		
		String format = this.getStringParameter("format");
		sdf = new SimpleDateFormat(format);
		
		// Get parameters
		schemaid = this.getStringParameter("schemaid");
		featureids = this.getStringParameter("featureids").split(",");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
		
		probvary = .25;
		if(this.hasParameter("probvary")) {
			probvary = this.getDoubleParameter("probvary");
		}
		
		if(this.hasParameter("monthrange")) {
			this.monthrange = this.getIntegerParameter("monthrange");
		}
		
		if(this.hasParameter("dayrange")) {
			this.dayrange = this.getIntegerParameter("dayrange");
		}

		if(this.hasParameter("yearrange")) {
			this.yearrange = this.getIntegerParameter("yearrange");
		}
	}
	
	@Override
	public void addNoise(Graph g) {
		if(initialize) {
			this.initialize();
		}
		
		try {
			Iterator<GraphItem> gitr = g.getGraphItems(this.schemaid);
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();
				for(String fid:featureids) {
					if(rand.nextDouble()<probvary) {
						continue;
					}
					
					FeatureValue fv = gi.getFeatureValue(fid);
					if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
						continue;
					}
					
					String value = fv.getStringValue();
					String newvalue = this.varydate(value);
					gi.setFeatureValue(fid, newvalue);
				}
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String varydate(String datestring) throws ParseException {
		Date date = sdf.parse(datestring);
		Calendar c1 = Calendar.getInstance(); 
		c1.setTime(date);
		
		// Month
		if(this.monthrange!=0) {
			int sign = rand.nextInt(2)==0 ? -1 : 1;
			int diff = rand.nextInt(this.monthrange);
			c1.add(Calendar.MONTH, sign * diff);
		}
		
		// Day
		if(this.dayrange!=0) {
			int sign = rand.nextInt(2)==0 ? -1 : 1;
			int diff = rand.nextInt(this.dayrange);
			c1.add(Calendar.DAY_OF_MONTH, sign * diff);
		}
		
		// Year
		if(this.yearrange!=0) {
			int sign = rand.nextInt(2)==0 ? -1 : 1;
			int diff = rand.nextInt(this.yearrange);
			c1.add(Calendar.YEAR, sign * diff);
		}
		
		// Time is all set to 0
		c1.set(Calendar.HOUR, 0);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);
		c1.set(Calendar.MILLISECOND, 0);
		
		return sdf.format(c1.getTime());
	}
	
	@Override
	public void addNoise(Decorable d) {
		if(initialize) {
			this.initialize();
		}
		
		try {
			for(String fid:featureids) {
				if(rand.nextDouble()<probvary) {
					continue;
				}
				
				FeatureValue fv = d.getFeatureValue(fid);
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					continue;
				}
				
				String value = fv.getStringValue();
				String newvalue = this.varydate(value);
				d.setFeatureValue(fid, newvalue);
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm:ss.S'Z'");
		try {
			Date date = sdf.parse("2011-01-18T15:52:47.330Z");
			Calendar c1 = Calendar.getInstance(); 
			c1.setTime(date);
			Log.DEBUG(date);
			c1.add(Calendar.DAY_OF_MONTH, 1);
			Log.DEBUG(c1.getTime());
			
			Log.DEBUG(sdf.format(date));
			
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
