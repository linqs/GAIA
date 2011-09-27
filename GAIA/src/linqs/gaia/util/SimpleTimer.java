package linqs.gaia.util;

import java.text.DateFormat;
import java.util.Date;

/**
 * Simple timer to identify how much time has lapsed.
 * Elapsed time can be return in milliseconds, seconds,
 * minutes, hours or day units
 * 
 * Reference:
 * http://exampledepot.com/egs/java.lang/ElapsedTime.html
 * 
 * @author namatag
 *
 */
public class SimpleTimer {
	private long starttime;
	private long lastpointtime;
	
	/**
	 * Constructor.<br>
	 * Note: The start time is set during construction.
	 */
	public SimpleTimer() {
		start();
	}
	
	/**
	 * Set start time to current time
	 */
	public void start(){
		starttime = System.currentTimeMillis();
		lastpointtime = starttime;
	}
	
	/**
	 * Return time lapsed since start in milliseconds
	 * 
	 * @return Time lapsed
	 */
	public double msecLapse(){
		return System.currentTimeMillis()-starttime;
	}
	
	/**
	 * Return time lapsed since start in seconds
	 * 
	 * @return Time lapsed
	 */
	public double secLapse(){
		return msecLapse()/1000F;
	}
	
	/**
	 * Return time lapsed since start in minutes
	 * 
	 * @return Time lapsed
	 */
	public double minLapse(){
		return msecLapse()/(60*1000F);
	}
	
	/**
	 * Return time lapsed since start in hours
	 * 
	 * @return Time lapsed
	 */
	public double hourLapse(){
		return msecLapse()/(60*60*1000F);
	}
	
	/**
	 * Return time lapsed since start in days
	 * 
	 * @return Time lapsed
	 */
	public double dayLapse(){
		return msecLapse()/(24*60*60*1000F);
	}
	
	/**
	 * Return the time lapsed in the following format:
	 * <p>
	 * If readable==false, "[#days]:[#hours]:[#mins]:[#secs]:[#msecs]".<br>
	 * If readable==true, "[#days] days [#hours] hours [#mins] mins [#secs] secs [#msecs] msecs".
	 * 
	 * @param readable Return human readable string
	 * @return Time elapsed
	 */
	public String timeLapse(boolean readable){
		double msecLapse = this.msecLapse();
		
		return SimpleTimer.msec2string(msecLapse, readable);
	}
	
	/**
	 * Return human readable version of elapsed time
	 * @return Time elapsed
	 */
	public String timeLapse(){
		return timeLapse(true);
	}
	
	/**
	 * Return the time since the last time this function was called.
	 * The checkpoint time is initialized to the start time.
	 * 
	 * @return Time elapsed
	 */
	public String checkpointTime() {
		return this.checkpointTime(true);
	}
	
	/**
	 * Return the time since the last time this function was called.
	 * The checkpoint time is initialized to the start time.
	 * 
	 * @param readable Return human readable string
	 * @return Time elapsed
	 */
	public String checkpointTime(boolean readable) {
		long previous = lastpointtime;
		lastpointtime = System.currentTimeMillis();
		return SimpleTimer.msec2string(lastpointtime-previous, true);
	}
	
	/**
	 * Return a string version of the current date and time
	 * 
	 * @return Current date and time
	 */
	public String now(){
		DateFormat longTimestamp = 
			DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
		
		return longTimestamp.format(new Date());
	}
	
	public static String msec2string(double msecLapse, boolean readable) {
		String output = "";
		
		int days = (int) (msecLapse/(24*60*60*1000F));
		msecLapse -= days*(24*60*60*1000F);
		
		int hours = (int) (msecLapse/(60*60*1000F));
		msecLapse -= hours*(60*60*1000F);
		
		int mins = (int) (msecLapse/(60*1000F));
		msecLapse -= mins*(60*1000F);
		
		int secs = (int) (msecLapse/1000F);
		msecLapse -= secs*1000F;
		
		int msec = (int) msecLapse;
		
		if(readable){
			output = days+" days "+hours+" hours "+mins+" mins "+secs+" secs "+msec+" msecs";
		} else {
			output = days+":"+hours+":"+mins+":"+secs+":"+msec;
		}
		
		return output;
	}
}
