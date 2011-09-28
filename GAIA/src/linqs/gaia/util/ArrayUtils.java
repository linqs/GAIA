/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.util;

public class ArrayUtils {
	/**
	 * Return index of minimum value
	 * 
	 * @param array Array of doubles
	 * @return Index of minimum value
	 */
	public static int minValueIndex(double[] array){
		if(array==null || array.length==0){
			return -1;
		}
		
		double min = Double.POSITIVE_INFINITY;
		int minindex = -1;
		for(int i=0; i<array.length; i++){
			if(array[i] < min){
				minindex = i;
				min = array[i];
			}
		}
		
		return minindex;
	}
	
	/**
	 * Return minimum value
	 * 
	 * @param array Array of doubles
	 * @return Minimum value in array
	 */
	public static double minValue(double[] array){
		return array[ArrayUtils.minValueIndex(array)];
	}
	
	/**
	 * Return index of maximum value
	 * 
	 * @param array Array of doubles
	 * @return Index of maximum value
	 */
	public static int maxValueIndex(double[] array){
		if(array==null || array.length==0){
			return -1;
		}
		
		double max = Double.NEGATIVE_INFINITY;
		int maxindex = -1;
		for(int i=0; i<array.length; i++){
			if(array[i] > max){
				maxindex = i;
				max = array[i];
			}
		}
		
		return maxindex;
	}
	
	/**
	 * Return maximum value
	 * 
	 * @param array Array of doubles
	 * @return Maximum value in array
	 */
	public static double maxValue(double[] array) {
		return array[ArrayUtils.maxValueIndex(array)];
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(Object[] array){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Array size: "+array.length+"\n");
		int counter=0;
		for(Object obj: array){
			buf.append("["+counter+"]="+obj+"\n");
			counter++;
		}
		
		return buf.toString();
	}
	
	/**
	 * Return string representation of array
	 * 
	 * @param array Array to print
	 * @param delimiter Delimiter to separate values
	 * @return Single line representation separating values by the delimiter
	 */
	public static String array2String(Object[] array, String delimiter){
		if(array==null) {
			return "Array is Null";
		}
		
		String output=null;
		int counter=0;
		for(Object obj: array){
			if(output==null){
				output = "";
			} else {
				output += delimiter;
			}
			
			output += obj;
			counter++;
		}
		
		return output;
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(double[] array){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Array size: "+array.length+"\n");
		int counter=0;
		for(double obj: array){
			buf.append("["+counter+"]="+obj+"\n");
			counter++;
		}
		
		return buf.toString();
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @param delimiter Delimiter to separate values
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(double[] array, String delimiter){
		if(array==null) {
			return "Array is Null";
		}
		
		String output=null;
		int counter=0;
		for(double obj: array){
			if(output==null){
				output = "";
			} else {
				output += delimiter;
			}
			
			output += obj;
			counter++;
		}
		
		return output;
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(int[] array){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Array size: "+array.length+"\n");
		int counter=0;
		for(double obj: array){
			buf.append("["+counter+"]="+obj+"\n");
			counter++;
		}
		
		return buf.toString();
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @param delimiter Delimiter to separate values
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(int[] array, String delimiter){
		if(array==null) {
			return "Array is Null";
		}
		
		String output=null;
		int counter=0;
		for(int obj: array){
			if(output==null){
				output = "";
			} else {
				output += delimiter;
			}
			
			output += obj;
			counter++;
		}
		
		return output;
	}
	
	/**
	 * Convert the delimited string into an array of type double.
	 * An exception is throw if any delimited portion of the string
	 * cannot be cast into a double
	 * 
	 * @param string Delimited string
	 * @param delimiter Delimiter
	 * @return Array of type double
	 */
	public static double[] string2ArrayDouble(String string, String delimiter) {
		String[] parts = string.split(delimiter);
		double[] output = new double[parts.length];
		for(int i=0; i<parts.length; i++) {
			output[i] = Double.parseDouble(parts[i]);
		}
		
		return output;
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(double[][] array){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Array size: "+array.length+"\n");
		for(int i=0; i<array.length; i++) {
			for(int j=0; j<array.length; j++){
				buf.append("["+i+","+j+"]="+array[i][j]+"\n");
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Return string representation of array
	 * 
	 * @param array Array to print
	 * @param coldelim Column delimiter to separate values
	 * @param rowdelim Row delimiter to separate values
	 * @return Single line representation separating values by the delimiter
	 */
	public static String array2String(double[][] array, String coldelim, String rowdelim){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<array.length; i++) {
			if(i!=0) {
				buf.append(rowdelim);
			}
			
			for(int j=0; j<array.length; j++){
				if(j!=0) {
					buf.append(coldelim);
				}
				
				buf.append(array[i][j]);
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Print array entries to screen
	 * 
	 * @param array Array to print
	 * @return String representation in multiple lines and showing index
	 */
	public static String array2String(Object[][] array){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Array size: "+array.length+"\n");
		for(int i=0; i<array.length; i++) {
			for(int j=0; j<array.length; j++){
				buf.append("["+i+","+j+"]="+array[i][j]+"\n");
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Return string representation of array
	 * 
	 * @param array Array to print
	 * @param coldelim Column delimiter to separate values
	 * @param rowdelim Row delimiter to separate values
	 * @return Single line representation separating values by the delimiter
	 */
	public static String array2String(Object[][] array, String coldelim, String rowdelim){
		if(array==null) {
			return "Array is Null";
		}
		
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<array.length; i++) {
			if(i!=0) {
				buf.append(rowdelim);
			}
			
			for(int j=0; j<array.length; j++){
				if(j!=0) {
					buf.append(coldelim);
				}
				
				buf.append(array[i][j]);
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Return index of the provided value in the array.
	 * A -1 is returned if the value is not found in the array.
	 * 
	 * @param array String array to check
	 * @param value String value whose index we're interested in
	 * @return Index of value in array
	 */
	public static int getIndex(String[] array, String value){
		for(int i=0; i<array.length; i++){
			if(array[i].equals(value)){
				return i;
			}
		}

		return -1;
	}
	
	/**
	 * Calculate standard deviation
	 * 
	 * @param vals Values to take the standard deviation of
	 * @return Standard deviation value
	 */
	public static double stddev(double[] vals){
		double avg = ArrayUtils.average(vals);
		double diff = 0;
		for(Double val : vals){
			diff+=Math.pow(val-avg,2);
		}
		
		return (Math.sqrt(diff))/(double) vals.length;
	}
	
	/**
	 * Calculate average
	 * 
	 * @param vals Values to take the average of
	 * @return Average value
	 */
	public static double average(double[] vals){
		double sum = 0;
		for(Double val : vals){
			sum += val;
		}
		
		double avg = sum/(double) vals.length;
		
		return avg;
	}
	
	/**
	 * Return a portion of the array
	 * 
	 * @param orig Original array
	 * @param startindex Start index to include
	 * @param stopindex Stop index to include
	 * @return Sub array
	 */
	public static double[] subarray(double[] orig, int startindex, int stopindex){
		double[] sub = new double[stopindex-startindex+1];
		for(int i=0; i<sub.length; i++){
			sub[i] = orig[startindex+i];
		}
		
		return sub;
	}
	
	/**
	 * Return a portion of the array
	 * 
	 * @param orig Original array
	 * @param startindex Start index to include
	 * @return Sub array
	 */
	public static double[] subarray(double[] orig, int startindex){
		return subarray(orig, startindex, orig.length-1);
	}
	
	/**
	 * Return a portion of the array.  If the start or stop value is outside
	 * the range of the array, a null is returned.
	 * 
	 * @param orig Original array
	 * @param startindex Start index to include
	 * @param stopindex Stop index to include
	 * @return Sub array
	 */
	public static String[] subarray(String[] orig, int startindex, int stopindex){
		if( startindex < 0 ||
			startindex >= orig.length ||
			stopindex < 0 ||
			stopindex >= orig.length) {
			return null;
		}
		
		String[] sub = new String[stopindex-startindex+1];
		for(int i=0; i<sub.length; i++){
			sub[i] = orig[startindex+i];
		}
		
		return sub;
	}
	
	/**
	 * Return a portion of the array
	 * 
	 * @param orig Original array
	 * @param startindex Start index to include
	 * @return Sub array
	 */
	public static String[] subarray(String[] orig, int startindex){
		return subarray(orig, startindex, orig.length-1);
	}
}
