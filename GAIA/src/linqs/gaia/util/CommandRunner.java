package linqs.gaia.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import linqs.gaia.log.Log;

/**
 * Utility for doing a system call to run and parse or save
 * the output resulting from that call
 * 
 * @author namatag
 *
 */
public class CommandRunner {
	/**
	 * Thread to handle emptying the stream
	 * provided.  Based on work around described in:
	 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.htm
	 * 
	 * @author namatag
	 *
	 */
	private class StreamGobbler extends Thread {
		InputStream is;
		boolean iserror = false;
		BufferedWriter writer = null;

		StreamGobbler(InputStream is, BufferedWriter writer) {
			this.is = is;
			this.writer = writer;
		}
		
		StreamGobbler(InputStream is, boolean iserror) {
			this.is = is;
			this.iserror = iserror;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null) {
					if(writer!=null) {
						this.writer.write(line);
					}
					
					if(iserror) {
						Log.WARN(line);
					} else {
						Log.INFO(line);
					}
				}
				
				if(writer!=null) {
					writer.close();
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}
	
	/**
	 * Run the command specified and save the output and errors into
	 * the specified files.
	 * 
	 * @param command Command to run
	 * @param outputfile Output file
	 * @param errorfile Input file
	 */
	public static void runCommand(String command, String outputfile, String errorfile) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(outputfile));
			BufferedWriter error = new BufferedWriter(new FileWriter(errorfile));
			CommandRunner.runCommand(command, output, error);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String[] commandParser(String command) {
		// Split by whitespace
		if(!command.contains("\"")) {
			return command.split("\\s");
		}
		
		// Handle quotation marks
		// Note:  This may need to change if run across platform.
		// This was implemented specifically with Linux in mind.
		List<String> parsecommandlist = new ArrayList<String>();
		String[] commandarray = command.split("\\s");
		String quoted = null;
		for(String c:commandarray) {
			if(c.startsWith("\"") && quoted==null ) {
				quoted = c;
			} else if(quoted!=null) {
				quoted = quoted +" "+ c;
				if(c.endsWith("\"")) {
					quoted = quoted.replaceAll("\"", "");
					parsecommandlist.add(quoted);
					quoted = null;
				}
			} else {
				parsecommandlist.add(c);
			}
		}
		
		// Handle any remaining quoted stuff that wasn't added
		if(quoted != null) {
			parsecommandlist.add(quoted);
		}
		
		return parsecommandlist.toArray(new String[parsecommandlist.size()]);
	}
	
	/**
	 * Run the command specified and save the output and errors into
	 * the specified files.
	 * 
	 * @param command Command to run
	 * @param output Output file
	 * @param error Input file
	 */
	public static void runCommand(String command, BufferedWriter output, BufferedWriter error) {
		CommandRunner cr = new CommandRunner();
		
		try {
			String[] parsedcommand = commandParser(command);
			Log.DEBUG("Executing command: "+command+" parsed as \n"+ArrayUtils.array2String(parsedcommand));
			
			// Run command using the Runtime exec method:
			Process p = Runtime.getRuntime().exec(parsedcommand);	

			// Print error using Log.WARN
			CommandRunner.StreamGobbler errorGobbler = cr.new 
				StreamGobbler(p.getErrorStream(), error);            
            
            // Print output using Log.INFO
			CommandRunner.StreamGobbler outputGobbler = cr.new 
				StreamGobbler(p.getInputStream(), output);
            
            // Start the streams to process the output
			// Note:  This is needed since waitFor will block
			// if the output and error streams get full
            errorGobbler.start();
            outputGobbler.start();

			p.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Run the command specified.  The errors are printed
	 * using {@link Log#WARN} and output using {@link Log#INFO}.
	 * 
	 * @param command Command to run
	 */
	public static int runCommand(String command) {
		CommandRunner cr = new CommandRunner();
		
		try {
			String[] parsedcommand = commandParser(command);
			Log.DEBUG("Executing command: "+command+" parsed as \n"+ArrayUtils.array2String(parsedcommand));
			
			// Run command using the Runtime exec method:
			Process p = Runtime.getRuntime().exec(parsedcommand);
			
			// Print error using Log.WARN
			CommandRunner.StreamGobbler errorGobbler = cr.new 
				StreamGobbler(p.getErrorStream(), true);            
            
            // Print output using Log.INFO
			CommandRunner.StreamGobbler outputGobbler = cr.new 
				StreamGobbler(p.getInputStream(), false);
            
            // Start the streams to process the output
			// Note:  This is needed since waitFor will block
			// if the output and error streams get full
            errorGobbler.start();
            outputGobbler.start();

			return p.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
