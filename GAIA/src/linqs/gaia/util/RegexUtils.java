package linqs.gaia.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Utilities to aid in testing Java regular expressions
 * 
 * @author namatag
 *
 */
public class RegexUtils {
	/**
	 * Simple Java program which provides a command line interface
	 * to enter java regex and text to apply it on.
	 * The program will return whether or not the text matches
	 * the expression.
	 * 
	 * @param args Arguments to main call
	 */
    public static void main(String[] args) {
    	try {
			while(true) {
				// Prompt for pattern
			    System.out.print("Enter regex pattern: ");
			    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			    String regex = br.readLine();
			    
			    // Prompt for pattern
			    System.out.print("Enter text: ");
			    String input = br.readLine();
			    
			    // Print if match or fail
			    if(Pattern.matches(regex, input)) {
			    	System.out.println("Match!");
			    } else {
			    	System.out.println("Fail!");
			    }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}

