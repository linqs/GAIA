package linqs.gaia.global;

/**
 * Class containing all references to static variables used in the code.
 * 
 * @author namatag
 *
 */
public class Global {
	/** Stem name for creation of anonymous identifiers */
	private static final String anonymous_stem = "gaia_anon:";

	/** Count of number of anonymous items requested */
	private static int anonymous_count = 0;
	
	/**
	 * Request a new feature name for an input feature that is anonymous.
	 *
	 * @return A name to use for the anonymous feature.
	 */
	public static String requestAnonymousID() {
		return anonymous_stem+(anonymous_count++);
	}
	
	/**
	 * Request the next number in the local counter
	 *
	 * @return A name to use for the anonymous feature.
	 */
	public static int requestGlobalCounterValue() {
		return anonymous_count++;
	}
	
	/**
	 * Resets all global variables
	 */
	public static void reset(){
		anonymous_count = 0;
	}
}
