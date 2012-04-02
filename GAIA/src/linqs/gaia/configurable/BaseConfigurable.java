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
package linqs.gaia.configurable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.FileIO;

/**
 * Class to support the common functionality of {@link Configurable} items.
 * 
 * @author namatag
 *
 */
public abstract class BaseConfigurable implements Configurable {
	public static final String PARAMDELIM = ";;";
	public static final String VALDELIM = "=";
	public static final String CIDDELIM = ".";
	public static final String COMMENT_CHAR = "#";
	public static final String COMMENT_START = "###COMMENT";
	public static final String COMMENT_STOP = "COMMENT###";
	public static final char ESCAPE_CHAR = '\\';
	public static final String LOADFILE = "loadfile ";
	
	public static final Pattern envpattern = Pattern.compile("\\$\\{[^\\}\\n]+\\}");
	public static final Pattern varpattern = Pattern.compile("@\\{[^\\}\\n]+\\}");
	
	private HashMap<String, String> parameters = new HashMap<String, String>();
	private String cid = null;
	
	public double getDoubleParameter(String name) {
		String value = this.getStringParameter(name);
		double dvalue;
		try {
			dvalue = Double.parseDouble(value);
		} catch(Exception e) {
			throw new ConfigurationException("Value is not a double: "+name+VALDELIM+value);
		}
		
		return dvalue;
	}
	
	public int getIntegerParameter(String name) {
		String value = this.getStringParameter(name);
		int dvalue;
		try {
			dvalue = Integer.parseInt(value);
		} catch(Exception e) {
			throw new ConfigurationException("Value is not an integer: "+name+VALDELIM+value);
		}
		
		return dvalue;
	}

	public String getStringParameter(String name) {
		
		String currname = this.findMostSpecificMatch(name);
		String value = this.parameters.get(currname);
		
		// Note: This will handle all cases where
		// we are trying to get a parameter but
		// the parameter is not defined.
		if(value==null){
			String message = "Parameter is not defined: "+currname;
			if(this.getCID()!=null){
				message += " or "+this.getCID()+CIDDELIM+name;
			}
			
			message += " (Defined are: "+this.allParameters2String()+")";
			
			throw new ConfigurationException(message);
		}
		
		return value;
	}
	
	public String getCaseParameter(String name, String[] cases) {
		String val = this.getStringParameter(name);
		
		List<String> currcases = Arrays.asList(cases);
		if(currcases.contains(val)) {
			return val;
		} else {
			throw new ConfigurationException("Parameter does not have a valid value: "
					+ name +"="+val+" where valid are "+ArrayUtils.array2String(cases, ","));
		}
	}
	
	public boolean getYesNoParameter(String name) {
		String val = this.getStringParameter(name);
		
		if(val.equalsIgnoreCase("yes")) {
			return true;
		} else if(val.equalsIgnoreCase("no")) {
			return false;
		} else {
			throw new ConfigurationException("Parameter does not have a \"yes\" or \"no\" value: "
					+ name +"="+val);
		}
	}
	
	public double getDoubleParameter(String name, double defaultvalue) {
		String value = this.getStringParameter(name, ""+defaultvalue);
		double dvalue;
		try {
			dvalue = Double.parseDouble(value);
		} catch(Exception e) {
			throw new ConfigurationException("Value is not a double: "+name+VALDELIM+value);
		}
		
		return dvalue;
	}
	
	public int getIntegerParameter(String name, int defaultvalue) {
		String value = this.getStringParameter(name, ""+defaultvalue);
		int dvalue;
		try {
			dvalue = Integer.parseInt(value);
		} catch(Exception e) {
			throw new ConfigurationException("Value is not an integer: "+name+VALDELIM+value);
		}
		
		return dvalue;
	}

	public String getStringParameter(String name, String defaultvalue) {
		
		String currname = this.findMostSpecificMatch(name);
		String value = this.parameters.get(currname);
		
		// Note: This will handle all cases where
		// we are trying to get a parameter but
		// the parameter is not defined.
		if(value==null){
			value = defaultvalue;
		}
		
		return value;
	}
	
	public String getCaseParameter(String name, String[] cases, String defaultvalue) {
		String val = this.getStringParameter(name, defaultvalue);
		
		List<String> currcases = Arrays.asList(cases);
		if(currcases.contains(val)) {
			return val;
		} else {
			throw new ConfigurationException("Parameter does not have a valid value: "
					+ name +"="+val+" where valid are "+ArrayUtils.array2String(cases, ","));
		}
	}
	
	public boolean getYesNoParameter(String name, String defaultvalue) {
		String val = this.getStringParameter(name, defaultvalue);
		
		if(val.equalsIgnoreCase("yes")) {
			return true;
		} else if(val.equalsIgnoreCase("no")) {
			return false;
		} else {
			throw new ConfigurationException("Parameter does not have a \"yes\" or \"no\" value: "
					+ name +"="+val);
		}
	}
	
	public void removeParameter(String name) {
		// See if value is defined and throw away if it isn't.
		this.getStringParameter(name);
		
		// Remove the matching parameter value
		name = this.findMostSpecificMatch(name);
		this.parameters.remove(name);
	}

	public void setParameter(String name, String value) {
		if(value == null) {
			throw new InvalidStateException("Parameter value cannot be null: "+name+"=null");
		}
		
		// Trim name and value to deal with extra spaces
		name = name.trim();
		value = value.trim();
		
		// Update environment variables in value
		String newvalue = value;
		Matcher matcher = envpattern.matcher(value);
        while (matcher.find()) {
        	String group = matcher.group();
        	int start = matcher.start();
        	int end = matcher.end();
        	
        	String replacement = group.substring(2, group.length()-1);
        	replacement = System.getenv(replacement);
        	if(replacement==null){
				throw new ConfigurationException("No variable is defined for "
						+group
						+" from configuration "+name+"="+value);
			}
        	
        	newvalue = newvalue.substring(0, start)+replacement+newvalue.substring(end);
        	matcher = envpattern.matcher(newvalue);
        }
		
		// Update internal variable in value
		matcher = varpattern.matcher(newvalue);
        while (matcher.find()) {
        	String group = matcher.group();
        	int start = matcher.start();
        	int end = matcher.end();
        	
        	String replacement = group.substring(2, group.length()-1);
        	replacement = this.getStringParameter(replacement, null);
        	if(replacement==null){
				throw new ConfigurationException("No variable is defined for "
						+group
						+" from configuration "+name+"="+value);
			}
        	
        	newvalue = newvalue.substring(0, start)+replacement+newvalue.substring(end);
        	matcher = varpattern.matcher(newvalue);
        }
        value = newvalue;
		
		parameters.put(name.intern(), value);
	}
	
	public void setParameter(String name, Number value) {
		this.setParameter(name, value.toString());
	}

	public void setParameters(String args) {
		String parts[] = args.split(PARAMDELIM);

		for(String arg: parts){
			if(arg.length()==0) {
				continue;
			}
			
			int index = arg.indexOf(VALDELIM);
			
			if(index == -1) {
				throw new ConfigurationException("Malformed parameter: "+args);
			}
			
			String key = arg.substring(0, index);
			String value = arg.substring(index+1);
			
			this.setParameter(key, value);
		}
	}
	
	public void loadParametersFile(String filename) {
		this.loadParametersFile(filename, new ArrayList<String>());
	}
	
	public void setParametersFile(String filename) {
		this.loadParametersFile(filename, new ArrayList<String>());
	}
	
	/**
	 * Helper function to prevent a file from loading itself
	 * 
	 * @param filename
	 * @param loadedfiles
	 */
	private void loadParametersFile(String filename, List<String> loadedfiles) {
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String line;
			while ((line = this.getNonCommentLine(br)) != null) {
				// Load files referred to in this configuration file
				if(line.startsWith(LOADFILE)){
					String filenames[] = line.split("\\s+");
					for(int i=1; i<filenames.length; i++){
						String otherfile = filenames[i];
						
						// Done to prevent loops
						if(loadedfiles.contains(otherfile)){
							throw new ConfigurationException("File was previously loaded: "+otherfile);
						}
						
						loadedfiles.add(otherfile);
						this.loadParametersFile(otherfile, loadedfiles);
					}
				} else {
					setParameters(line);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Return next non comment line.
	 * 
	 * @param br
	 * @return
	 * @throws Exception
	 */
	private String getNonCommentLine(BufferedReader br) throws Exception {
		
		// Skip lines starting with COMMENT_CHAR and lines which consists of whitespace
		String line = br.readLine();
		boolean incomment = false;
		while(line!=null) {
			// Remove leading and trailing whitespaces
			line = line.trim();
			if(line.startsWith(COMMENT_START)) {
				incomment = true;
			} else if(line.endsWith(COMMENT_STOP)) {
				incomment = false;
			} else if(line.startsWith(COMMENT_CHAR) || line.trim().length()==0) {
				// Still don't have a non-comment line
			} else {
				// If this is a non-comment line, non-whitespace,
				// and not in a comment region, break.
				if(!incomment) {
					break;
				}
			}
			
			// Get next line
			line = br.readLine();
		}
		
		if(line==null) {
			return null;
		}
		
		// Support splitting configuration over multiple lines
		if(line.endsWith(""+ESCAPE_CHAR)) {
			line = line.substring(0,line.length()-1);
			String append = this.getNonCommentLine(br);
			if(append!=null) {
				line += append;
			}
		}
		
		// Remove anything in the line past the comment character
		int index = line.indexOf(COMMENT_CHAR);
		while(index!=-1) {
			// Allow for escape character \#
			if(line.charAt(index-1)==ESCAPE_CHAR) {
				if(index >= line.length()) {
					break;
				}
				
				index = line.indexOf(COMMENT_CHAR, index + 1);
				continue;
			} else {
				line = line.substring(0, index);
				break;
			}
		}
		
		// Remove all escape symbols from the escaped characters
		line = line.replaceAll("\\\\"+COMMENT_CHAR, COMMENT_CHAR);
		
		return line;
	}

	public boolean hasParameter(String name) {
		name = this.findMostSpecificMatch(name);
		
		if(name == null){
			return false;
		}
		
		return parameters.containsKey(name);
	}
	
	public boolean hasParameter(String name, String value) {
		if(!hasParameter(name)){
			return false;
		}
		
		name = this.findMostSpecificMatch(name);
		
		return parameters.get(name).equals(value);
	}
	
	public boolean hasYesNoParameter(String name, String value) {
		if(!hasParameter(name)){
			return false;
		} else {
			if(!value.equalsIgnoreCase("yes") && !value.equalsIgnoreCase("no")) {
				throw new InvalidStateException("Requesting an invalid value for yesno parameter: "+
						name +" = "+value);
			}
			
			String currvalue = getYesNoParameter(name) ? "yes" : "no";
			return currvalue.equals(value);
		}
	}
	
	public Map<String, String> getAllParameters(){
		return Collections.unmodifiableMap(this.parameters);
	}

	public void copyParameters(Configurable conf) {
		this.parameters.putAll(conf.getAllParameters());
	}

	public String allParameters2String() {
		String output=null;
		Map<String, String> allparams = this.getAllParameters();
		Set<Entry<String,String>> entries = allparams.entrySet();
		for(Entry<String,String> entry:entries){
			if(output==null){
				output=entry.getKey()+VALDELIM+entry.getValue();
			} else {
				output+=PARAMDELIM+entry.getKey()+VALDELIM+entry.getValue();
			}
		}

		return output;
	}

	public String getCID() {
		return cid;
	}

	public void setCID(String cid) {
		this.cid = cid==null ? null : cid.trim();
	}
	
	/**
	 * Returns the most specific matching attribute or the original
	 * name if no more specific match is found.
	 * 
	 * @param name Parameter name
	 * @return Most specific form of name given CID
	 */
	private String findMostSpecificMatch(String name) {
		String newname = name;
		
		// Match the most specific cid first then loosen match
		// i.e. If name=val and CID=A, match A.val.
		// If a match can't be find, look for val.
		if(this.getCID()!=null){
			String cidkey = this.getCID();
			String paramname = cidkey+CIDDELIM+name;
			if(this.parameters.containsKey(paramname)){
				newname = paramname;
			}
		}
		
		return newname;
	}

	public void saveParametersFile(String filename) {
		FileIO.write2file(filename, this.allParameters2String());
	}

	public boolean hasSameConfiguration(Configurable c) {
		return this.getAllParameters().equals(c.getAllParameters());
	}
}
