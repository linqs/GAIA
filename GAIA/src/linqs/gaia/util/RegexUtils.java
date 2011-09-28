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

