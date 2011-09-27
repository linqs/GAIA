package linqs.gaia.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.exception.InvalidStateException;

/**
 * General utilities for use with Map objects
 * 
 * @author namatag
 *
 */
public class MapUtils {
	/**
	 * Convert map entries to string
	 * 
	 * @param map Map to print
	 * @param keydelimiter Delimiter to separate key from value
	 * @param entrydelimiter Delimiter to separate entries in map
	 * 
	 * @return String representation
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String map2string(Map map, String keydelimiter, String entrydelimiter){
		String output = null;
		
		Set<Entry> entries = map.entrySet();
		for(Entry e:entries) {
			if(output==null) {
				output = "";
			} else {
				output += entrydelimiter;
			}
			
			output+=e.getKey()+keydelimiter+e.getValue();
		}
		
		return output;
	}
	
	/**
	 * Convert map to a map with key and value of string
	 * 
	 * @param map Map to convert
	 * 
	 * @return Converted Map
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Map<String,String> map2stringmap(Map map){
		return MapUtils.map2stringmap(map, new HashMap<String, String>());
	}
	
	/**
	 * Convert map to a map with key and value of string
	 * 
	 * @param map Map to convert
	 * @param stringmap Map to add values to
	 * 
	 * @return Converted Map
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String,String> map2stringmap(Map map, Map<String,String> stringmap){
		Map<String,String> output = stringmap;
		
		Set<Entry> entries = map.entrySet();
		for(Entry e:entries) {
			if(output.containsKey(e.getKey().toString())) {
				throw new InvalidStateException(
						"Two keys from source map to the same key in converted map: "
						+e.getKey().toString());
			}
			
			output.put(e.getKey().toString(), e.getValue().toString());
		}
		
		return output;
	}
}
