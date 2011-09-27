package linqs.gaia.util;

import java.text.DecimalFormat;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.values.NumValue;

public class Numeric {
	/**
	 * Return true if the string is numeric, false otherwise
	 * 
	 * @param value String value to check
	 * @return True if the String can be parsed as a numeric value.  False otherwise.
	 */
	public static boolean isNumeric(String value){
		try{
			Double.parseDouble(value);
		} catch(NumberFormatException e){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns a string representation of a double with
	 * the specified number of decimal places.
	 * 
	 * @param value Double value to print
	 * @param numdecimal Number of decimal places
	 * @return String representation of double
	 */
	public static String decimal(double value, int numdecimal){
		StringBuffer buf = new StringBuffer();
		buf.append("#0");
		for(int i=0; i<numdecimal;i++){
			if(i==0){
				buf.append(".");
			}
			
			buf.append("0");
		}
		
        DecimalFormat dformat = new DecimalFormat(buf.toString());
		
		return dformat.format(value);
	}
	
	/**
	 * Returns a string representation of the given
	 * number where "0"'s are added to the front of
	 * the string until the length of the string
	 * is the same as the length specified by numpadding.
	 * For example, give 2 with a padding of 3, we return
	 * 002.
	 * 
	 * @param value Integer value
	 * @param numpadding Length of target string
	 * @return Padded string
	 */
	public static String padded(int value, int numpadding) {
		String stringvalue = ""+value;
		while(stringvalue.length()<numpadding) {
			stringvalue = "0"+stringvalue;
		}
		
		return stringvalue;
	}
	
	/**
	 * Return factorial of a number i.e.: num!
	 * 
	 * @param num Number to take factorial
	 * @return Return n!
	 */
	public static int factorial(int num){
		return multrange(num, 1);
	}
	
	/**
	 * Return multiplication of a range of numbers
	 * i.e. (top)*(top-1)*(top-2)...(bottom+1)
	 * 
	 * @param top Top value in range
	 * @param bottom Bottom value in range
	 * @return Multiplied value
	 */
	public static int multrange(int top, int bottom) {
		int factorial = 1;
		while(top>bottom){
			factorial = factorial*top;
			top--;
		}
		
		return factorial;
	}
	
	/**
	 * Return the combination value of n choose r.
	 * 
	 * @param n Number of items to choose from
	 * @param r Number to choose per choice
	 * @return Value of n choose r
	 */
	public static int combination(int n, int r){
		int comb = multrange(n, n-r)/multrange(r,1);
		return comb;
	}
	
	/**
	 * Return the double value of the object.
	 * The double value is defined as follows:
	 * <UL>
	 * <LI>If o is a Double or Integer object, return the value.
	 * <LI>If o is a NumValue, return the numeric value.
	 * <LI>If o is a String or StringValue, parse string as a double.  Throw
	 * an exception if it cannot be parsed.
	 * </UL>
	 * 
	 * @param o Object to get double value of
	 * @return Double value
	 */
	public static double parseDouble(Object o) {
		double value = 0;
		if(o instanceof Double) {
			value = (Double) o;
		} else if(o instanceof Integer) {
			value = (Integer) o;
		} else if(o instanceof String) {
			String ostring = (String) o;
			if(isNumeric(ostring)) {
				value = Double.parseDouble(ostring);
			} else {
				throw new UnsupportedTypeException("Unable to parse string: "+
						ostring);
			}
		} else if(o instanceof NumValue) {
			value = ((NumValue) o).getNumber();
		} else {
			throw new UnsupportedTypeException("Unsupported type: "+
				o.getClass().getCanonicalName());
		}
		
		return value;
	}
}
