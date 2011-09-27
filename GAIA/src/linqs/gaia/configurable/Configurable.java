package linqs.gaia.configurable;

import java.util.Map;

/**
 * Base interface for all interfaces which can be configured
 * by some set of parameters.  Parameters can be set by a function call
 * or using a configuration file.
 * <p>
 * The format for the configuration file is as follows:
 * <p>
 * <code>
 * # Comments<br>
 * parametername1=parametervalue1<br>
 * parametername2=parametervalue2<br>
 * parametername3=parametervalue3;;parametername4=parametervalue4
 * </code>
 * <p>
 * Notes:
 * <ul>
 * <li>Assume that the configurations are loaded and set at the beginning of
 * the first method call on the Configurable item.  Make sure all the parameters
 * you want to set are initialized prior to the first, non parameter related, method call.
 * <li>Parameter key-value pairs can be separated by a newline character or with separator ";;"
 * <li>Environment variables can be set in the configuration using parametername=${envvar}
 * <li>A previous set parameter can be accessed in the configuration using the format:
 * parametername=@{envvar}.
 * For example, if you have key1=val1 and key2=@{key1}, key2 will be set with the value val1.
 * <li>Another configuration file can be loaded within a configuration file
 * using "loadfile configurationfile" (i.e., "loadfile file.cfg").
 * The parameters in the configuration file will be loaded at that time prior to
 * loading the rest of the current configuration file.
 * <li>You can comment out a line by beginning it with the "#" character.  You can also
 * comment out the rest of a line by placing a "#" before the part you want to comment out
 * (e.g., key=val#This is not processed).  If your parameter needs the "#" character,
 * you can escape it using "\#".
 * <li>You can comment out multiple lines of the configuration
 * by placing "###COMMENT" at the beginning of the first line you want to comment out
 * and placing "COMMENT###" at the end of the last line you want to comment out.
 * <li>To span a line in a configuration file (e.g., for long values), end the line with a '\'.
 * When encountered, the line following the '\' is assumed part of the current line.
 * </ul>
 * <p>
 * Configurable items can have a dot delimited configurable id, cid, which can be used to specify
 * a parameter for a specific configurable item. For example:
 * <p>
 * <code>
 * shouldprint=true
 * B.shouldprint=true
 * C.shouldprint=false
 * </code>
 * <p>
 * If an object has a cid of B, the most specific match is B.shouldprint=true.
 * If an object has a cid of C, the most specific match is C.shouldprint=false.
 * If a cid is not specified or there is no match for that cid,
 * the next most specific match is shouldprint.
 * If no match is found, an expection is thrown.
 * 
 * @author namatag
 *
 */
public interface Configurable {
	/**
	 * Create a parameter with the given name and value
	 * 
	 * @param name Name of parameter
	 * @param value Value for this configuration
	 */
    void setParameter(String name,String value);
    
    /**
	 * Create a parameter with the given name and value
	 * 
	 * @param name Name of parameter
	 * @param value Value for this configuration
	 */
    void setParameter(String name,Number value);
    
    /**
     * Can load multiple attributes using the format: [attributename]=[attributevalue]
     * 
     * @param args String with multiple configurations specified
     */
    void setParameters(String args);
    
    /**
     * Loads attributes specified in the given file with
     * one configuration per line of the format: [attributename]=[attributevalue].
     * 
     * To include environment variables, use ${envvar} inside [attributevalue].
     * This will replace all instances of ${envvar} with the value of the environment
     * variable when this function is called.
     * 
     * To set the attribute to the value of a previously set parameter,
     * use @{envar} inside [attributevalue].  This will replace all instances
     * of @{envar} with the value of the parameter when this function is called.
     * 
     * i.e. path=${PATH}/somepath/${FOLDER}
     * 
     * @param filename File with multiple parameters specified
     */
    void loadParametersFile(String filename);
    
    /**
     * Loads attributes specified in the given file.
     * Use loadParametersFile instead.
     * 
     * @deprecated Method has been renamed.  Use loadParametersFile() instead.
     * @param filename File with multiple parameters specified
     */
    void setParametersFile(String filename);
    
    /**
     * Get the String representation of the parameter specified.
     * If a Configurable Item ID is set, we try to return a parameter
     * with a name "cid.name".  Otherwise, we return a parameter with just the
     * given "name". If called and the parameter is undefined, a panic is called.
     * 
     * @param name Name of parameter
     * @return String representation of the value
     */
    String getStringParameter(String name);
    
    /**
     * Get the String representation of the parameter specified where
     * the value has to be one of the strings specified in cases.
     * Parameter name defined as in {@link #getStringParameter(String)}.
     * 
     * @param name Name of parameter
     * @param cases Possible values this parameter can hold
     * @return String representation of the value
     */
    String getCaseParameter(String name, String[] cases);
    
    /**
     * Get the numeric representation of the parameter specified.
     * An error is thrown and program execution stopped if the String
     * value cannot be converted to a Double or if the parameter is not defined.
     * Parameter name defined as in {@link #getStringParameter(String)}.
     * 
     * @param name Name of parameter
     * @return Double representation of the value
     */
    double getDoubleParameter(String name);
    
    /**
     * Get the numeric representation of the parameter specified.
     * An error is thrown and program execution stopped if the String
     * value cannot be converted to a Integer or if the parameter is not defined.
     * Parameter name defined as in {@link #getStringParameter(String)}.
     * 
     * @param name Name of parameter
     * @return Double representation of the value
     */
    int getIntegerParameter(String name);
    
    /**
     * Check to see if the parameter is defined and has the values "yes".
     * Return true if it has the value "yes" and false if it has the value "no".
     * An exception is thrown if the value is defined and
     * the value for this parameter is neither "yes" or "no"
     * or if the parameter is not defined.
     * Parameter name defined as in {@link #getStringParameter(String)}.
     * <p>
     * Note: The yes or no value is case insensitive.
     * 
     * @param name Name of parameter
     * @return True if the parameter is defined and the value is yes
     */
    boolean getYesNoParameter(String name);
    
    /**
     * Get the String representation of the parameter specified.
     * If a Configurable Item ID is set, we try to return a parameter
     * with a name "cid.name".  Otherwise, we return a parameter with just the
     * given "name". If the parameter is not specified, the default value is used.
     * 
     * @param name Name of parameter
     * @param defaultvalue Default string value to use when the parameter not specified
     * @return String representation of the value
     */
    String getStringParameter(String name, String defaultvalue);
    
    /**
     * Get the String representation of the parameter specified where
     * the value has to be one of the strings specified in cases.
     * If the parameter is not specified, the default value is returned.
     * 
     * Parameter name defined as in {@link #getStringParameter(String)}.
     * 
     * @param name Name of parameter
     * @param cases Possible values this parameter can hold
     * @param defaultvalue Default string value to use when the parameter is not specified
     * @return String representation of the value
     */
    String getCaseParameter(String name, String[] cases, String defaultvalue);
    
    /**
     * Get the numeric representation of the parameter specified.
     * An error is thrown if the string value cannot be converted to a Double.
     * If the parameter is not specified, the default value is returned.
     * 
     * @param name Name of parameter
     * @param defaultvalue Default double value to use when the parameter if not specified
     * @return Double representation of the value
     */
    double getDoubleParameter(String name, double defaultvalue);
    
    /**
     * Get the numeric representation of the parameter specified.
     * An error is thrown if the String value cannot be converted to a Integer.
     * If the parameter is not specified, the default value is returned.
     * 
     * @param name Name of parameter
     * @param defaultvalue Default integer value to use when the parameter is not specified
     * @return Double representation of the value
     */
    int getIntegerParameter(String name, int defaultvalue);
    
    /**
     * Check to see if the parameter is defined and has the values "yes".
     * Return true if it has the value "yes" and false if it has the value "no".
     * An exception is thrown if the value is defined and
     * the value for this parameter is neither "yes" or "no".
     * If the parameter is not specified, the default value is returned.
     * <p>
     * Note: The yes or no value is case insensitive.
     * 
     * @param name Name of parameter
     * @param defaultvalue Default string value to use when the parameter is not specified
     * @return True if the parameter is defined and the value is yes
     */
    boolean getYesNoParameter(String name, String defaultvalue);
    
    /**
     * Remove the parameter value with the given name.
     * 
     * @param name Name of parameter
     */
    void removeParameter(String name);
    
    /**
     * Check to see if the parameter is defined.
     * 
     * @param name Name of parameter
     * @return True if the parameter is defined and false otherwise
     */
    boolean hasParameter(String name);
    
    /**
     * Check to see if the parameter is defined and has the value defined
     * 
     * @param name Name of parameter
     * @param value Value of parameter
     * @return True if the parameter is defined with the specified value and false otherwise
     */
    boolean hasParameter(String name, String value);
    
    /**
     * Check to see if the parameter is defined and return false if not defined
     * or if the value does not match.  If the parameter is defined and has the value
     * specified value, return true.  An exception is thrown if the value is defined and
     * the value for this parameter is neither "yes" or "no".
     * 
     * @param name Name of parameter
     * @param value Value of parameter ("yes" or "no")
     * @return True if the parameter is defined with the specified value and false otherwise
     */
    boolean hasYesNoParameter(String name, String value);
    
    /**
     * Return all set parameters.
     * 
     * @return Unmodifiable map of all parameters
     */
    Map<String, String> getAllParameters();
    
    /**
     * Copy all the configurations from the specified configurable object
     * @param conf
     */
    void copyParameters(Configurable conf);
    
    /**
     * Return all parameters as a single string of the form: 
     * <param1>=<val1>,<param2>=<val2>
     * 
     * @return String representation of all parameters
     */
    String allParameters2String();
    
    /**
     * Get ID for configurable item
     * 
     * @return Configurable ID
     */
    String getCID();
    
    /**
     * Set ID for configurable item.
     * When defined, the a request for the parameter A will be filled
     * by returning the value cid.A. Otherwise, a parameter A is returned.
     * 
     * @param cid Configurable ID
     */
    void setCID(String cid);
    
    /**
     * Save the parameters of the configurable item
     * to the specified file.
     * 
     * @param filename File to save configurations to
     */
    void saveParametersFile(String filename);
    
    /**
     * Check to see if the specified item has
     * the same exact configuration as the current item.
     * 
     * @param c Configurable item to compare to
     * @return True if the same, false otherwise
     */
    boolean hasSameConfiguration(Configurable c);
}
