package linqs.gaia.util;

/**
 * Class containing functions to make outputting Strings easier.
 * 
 * @author namatag
 *
 */
public class OutputUtils {
	/**
	 * Output a simple string N times.  For use in separating output.
	 * 
	 * @param val Value to Repeat
	 * @param numreps Number of repetitions
	 * @return String with val repeated the specified number of times
	 */
	public static String separator(String val, int numreps){
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<numreps; i++){
			buf.append(val);
		}
		
		return buf.toString();
	}
}
