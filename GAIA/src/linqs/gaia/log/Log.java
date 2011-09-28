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
package linqs.gaia.log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.configurable.Configurable;

/**
 * <p>
 * General logging class to help with debugging and to allow meaningful
 * messages to be printed from the code when needed.
 * <p>
 * We have 4 levels:
 * <UL>
 * <LI> DEBUG: Show for minor debugging messages that maybe kept in the code, even after release.
 * <LI> INFO: Use for general information that you may want to periodically print from fully functional code.
 * <LI> WARN: For use with printing messages for recoverable errors
 * <LI> PANIC: Use for when an error has occurred that cannot be recovered from
 * <LI> DEV: Use for messages used during development.  Aside from the messages, it prints where the message was called.
 * <LI> DEVSTOP: Same as DEV except execution is halted.  Use instead of PANIC in debugging
 * <LI> DEVPAUSE: Same as DEV except execution is paused until a key is pressed.
 * </UL>
 * <p>
 * Notes:
 * <UL>
 * <LI> DEV should NEVER be in release code.
 * <LI> INFO and WARN messages are printed by default.
 * <LI> Passing an Exception as the message to PANIC will print the stack trace for the exception.
 * </UL>
 * <p>
 * There are flags, named correspondingly, you can set for whether or not the
 * messages should be printed.  PANIC messages are always printed and will
 * always result in the program printing a stack trace and stopping execution.
 * You can either just print a specified message or print a message when some condition
 * is met.
 * 
 * @author namatag
 *
 */
public class Log extends BaseConfigurable {
	public static boolean SHOWDEV = true;
	public static boolean SHOWDEBUG = false;
	public static boolean SHOWINFO = true;
	public static boolean SHOWWARN = true;
	public static boolean SHOWSOURCE = false;
	
	/**
	 * Specify a message prefix if you need to quickly identify
	 * message lines from a log file from other output.
	 */
	public static String MESSAGEPREFIX = "";
	
	/**
	 * Hide all logging levels except PANIC
	 *
	 */
	public static void showAllLogging(){
		Log.updateAllLogging(true);
	}
	
	/**
	 * Hide all logging levels except PANIC
	 *
	 */
	public static void hideAllLogging(){
		Log.updateAllLogging(false);
	}
	
	private static void updateAllLogging(boolean state) {
		Log.SHOWDEV=state;
		Log.SHOWDEBUG=state;
		Log.SHOWINFO=state;
		Log.SHOWWARN=state;
	}
	
	/**
	 * Function to print message
	 * 
	 * @param logtype
	 * @param message
	 * @param sourceinfoalways
	 */
	private static void printLog(String logtype, Object message, boolean sourceinfoalways){
		String source="";
		if(Log.SHOWSOURCE || sourceinfoalways){
			source = Log.getSourceInfo();
		}
		
		System.out.println(Log.MESSAGEPREFIX+logtype+source+": "+message);
	}
	
	/**
	 * Use for minor debug messages
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 */
	public static void DEBUG(boolean check, Object message){
		if(check && SHOWDEBUG){
			printLog("DEBUG",message,false);
		}
	}
	
	public static void DEBUG(Object message){
		DEBUG(true, message);
	}
	
	/**
	 * Use for informative messages you may
	 * want to look at periodically but not all
	 * the time
	 */
	public static void INFO(boolean check, Object message){
		if(check && SHOWINFO){
			printLog("INFO",message,false);
		}
	}
	
	public static void INFO(Object message){
		INFO(true, message);
	}
	
	/**
	 * Use for when an error has occurred that
	 * needs to be resolved.
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 */
	public static void WARN(boolean check, Object message){
		if(check && SHOWWARN){
			printLog("WARN",message,true);
		}
	}
	
	public static void WARN(Object message){
		WARN(true, message);
	}
	
	/**
	 * Use for when a serious error has occurred so that
	 * you shouldn't continue
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 * @deprecated Use exceptions to allow the end user to handle what to do under certain types of errors.
	 */
	public static void PANIC(boolean check, Object message){
		if(check){
			// Process exceptions by printing the stack trace and stopping execution
			if(message instanceof Exception){
				Exception ex = ((Exception) message);
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				printLog("PANIC","Handling exception: "+ex.getLocalizedMessage()+"\n"+sw.toString(),true);
			} else {
				
				// Print stack trace
				StringWriter sw = new StringWriter();
				new Throwable().printStackTrace(new PrintWriter(sw));
				
				printLog("PANIC",message+"\n"+sw.toString(),true);
			}
			
			System.exit(1);
		}
	}
	
	/**
	 * Print Panic message
	 * 
	 * @param message Message to print
	 * @deprecated Use exceptions to allow the end user to handle what to do under certain types of errors.
	 */
	public static void PANIC(Object message){
		PANIC(true, message);
	}
	
	/**
	 * Use for debugging messages used in development
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 */
	public static void DEV(boolean check, Object message){
		if(check && SHOWDEV){
			printLog("DEV",message,true);
		}
	}
	
	public static void DEV(Object message){
		DEV(true, message);
	}
	
	/**
	 * Use for when an error has occurred that
	 * needs to be resolved.  Use this in place
	 * of PANIC for stopping execution in debugging.
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 */
	public static void DEVSTOP(boolean check, Object message){
		if(check && SHOWDEV){
			printLog("DEVSTOP:",message,true);
		}
		
		System.exit(0);
	}
	
	public static void DEVSTOP(Object message){
		DEVSTOP(true, message);
	}
	
	public static void DEVSTOP(){
		DEVSTOP(true, "Stopping");
	}
	
	/**
	 * Use to pause execution for debugging
	 * 
	 * @param check Condition to check
	 * @param message Message to print
	 */
	public static void DEVPAUSE(boolean check, Object message){
		if(check && SHOWDEV){
			printLog("DEVPAUSE",message+"\n(Press Any Key to Continue ...)",true);
			
			pause();
			printLog("DEVPAUSE","(Continuing Execution ...)",true);
		}
	}
	
	public static void DEVPAUSE(Object message){
		DEVPAUSE(true, message);
	}
	
	public static void DEVPAUSE(){
		DEVPAUSE(true, "Pausing");
	}
	
	private static String getSourceInfo(){
		int linenum = (new Throwable()).getStackTrace()[4].getLineNumber();
		String filename = (new Throwable()).getStackTrace()[4].getFileName();
		filename = filename.replaceAll(".java", "");
		return "["+filename+":"+linenum+"]";
	}
	
	/**
	 * Pauses execution in program until a key is pressed
	 */
	public static void pause() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			br.readLine();
		} catch (Exception e) {
			// Do nothing on purpose
		}
	}
	
	/**
	 * Set the logging level file.  The logging level file is in the
	 * same format as all the configurable files with
	 * the following optional parameters defined:
	 * 
	 * Optional Parameters:
	 * <UL>
	 * <LI>SHOWDEV-If yes, show DEV level messages.  If no, turn off.  Default is yes.
	 * <LI>SHOWDEBUG-If yes, show DEBUG level messages.  If no, turn off.  Default is no.
	 * <LI>SHOWINFO-If yes, show INFO level messages.  If no, turn off.  Default is yes.
	 * <LI>SHOWWARN-If yes, show WARN level messages.  If no, turn off.  Default is yes.
	 * <LI>SHOWSOURCE-If yes, show the java class and line where the log message is called
	 * for all messages.  If no, turn off.  Default is no.
	 * <LI>SHOWALL-If yes, show all log messages.
	 * </UL>
	 * @param configfile Configuration file with log level settings 
	 */
	public static void setLoggingLevel(String configfile) {
		Log log = new Log();
		log.loadParametersFile(configfile);
		setLoggingLevel(log);
	}
	
	/**
	 * Set the logging level file using the configurations
	 * of the specified Configurable object.
	 * 
	 * @param c Configurable object
	 */
	public static void setLoggingLevel(Configurable c) {
		if(c.hasParameter("SHOWDEV")) {
			Log.SHOWDEV = c.getYesNoParameter("SHOWDEV");
		}
		
		if(c.hasParameter("SHOWDEBUG")) {
			Log.SHOWDEBUG = c.getYesNoParameter("SHOWDEBUG");
		}
		
		if(c.hasParameter("SHOWINFO")) {
			Log.SHOWINFO = c.getYesNoParameter("SHOWINFO");;
		}
		
		if(c.hasParameter("SHOWWARN")) {
			Log.SHOWWARN = c.getYesNoParameter("SHOWWARN");
		}
		
		if(c.hasParameter("SHOWSOURCE")) {
			Log.SHOWSOURCE = c.getYesNoParameter("SHOWSOURCE");
		}
		
		if(c.hasYesNoParameter("SHOWALL","yes")) {
			Log.showAllLogging();
		}
	}
	
	/**
	 * Sample use of Logs
	 * 
	 * @param args Arguments to use in main class
	 */
	public static void main(String[] args) {
		Log.DEBUG("DEBUG");
		Log.INFO("INFO");
		Log.WARN("WARN");
		Log.DEV("DEV");
		//Log.PANIC(1<2,"PANIC");
		
		Log.SHOWDEBUG=true;
		Log.SHOWWARN=true;
		Log.SHOWINFO=true;
		Log.SHOWSOURCE=true;
		
		Log.INFO("*****************************************");
		Log.INFO("After flags:");
		Log.DEBUG("DEBUG");
		Log.INFO(1==3,"INFO");
		Log.WARN("ERROR");
		Log.DEV("DEV");
		Log.PANIC(1>2,"PANIC");
		Log.INFO("Done");
		Exception ex = new Exception("something went wrong");
		Log.PANIC(ex);
	}
}
