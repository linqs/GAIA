package linqs.gaia.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import linqs.gaia.exception.ConfigurationException;

/**
 * Utilities to simplify writing to files
 * 
 * @author namatag
 *
 */
public class FileIO {
	/**
	 * Write text to file
	 * 
	 * @param file Filename of file to write to
	 * @param text Text to write to file
	 * @param append If true, append to file.  Otherwise, overwrite.
	 */
	public static void write2file(String file, String text, boolean append) {
		try{
			// Create necessary dirs
			String dirs = file.substring(0,file.lastIndexOf(File.separator));
			if(dirs != null && dirs.trim().length() != 0 && !(new File(dirs)).exists()){
				createDirectories(dirs);
			}

			// Create file 
			FileWriter fstream = new FileWriter(file, append);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(text);

			//Close the output stream
			out.close();
		} catch (Exception e){//Catch exception if any
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Write text to file
	 * 
	 * @param file Filename of file to write to
	 */
	public static void write2file(String file, String text) {
		FileIO.write2file(file, text, false);
	}

	/**
	 * Create directories specified by the path or do nothing
	 * if the directory already exists.
	 * 
	 * @param path Path containing only directory names
	 */
	public static void createDirectories(String path) {
		File file = (new File(path));
		if(file.exists()){
			return;
		}

		boolean success = file.mkdirs();
		if(!success){
			throw new RuntimeException("Unable to create directories for "+path);
		}
	}

	/**
	 * Get a system temporary directory path to use.
	 * This not only returns the path but creates the directory
	 * so it is available for immediate use.
	 * 
	 * @return Directory path for temporary directory
	 */
	public static String getTemporaryDirectory() {
		String dirname = System.getProperty("java.io.tmpdir")+File.separator+System.nanoTime();
		createDirectories(dirname);

		return dirname;
	}
	
	/**
	 * Check to see if a file with the given name exists
	 * @param filename Name of file
	 * @return True if the file exists, false otherwise
	 */
	public static boolean fileExists(String filename) {
		File file = (new File(filename));
		if(file.exists()){
			return true;
		}

		return false;
	}
	
	/**
	 * Copy the contents of the source file to the target file.
	 * If the target file exists, it is overwritten.
	 * 
	 * @param source Source file
	 * @param target Target file
	 */
	public static void copyFile(String source, String target) {
		try{
			File f1 = new File(source);
			File f2 = new File(target);
			InputStream in = new FileInputStream(f1);

			//For Overwrite the file.
			OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			
			in.close();
			out.close();
		} catch(Exception e){
			throw new RuntimeException(e);   
		}
	}
	
	/**
	 * Return a list containing the string representation of
	 * the absolute file names of files in the directory
	 * 
	 * @param directory Directory
	 * @return List of filenames
	 */
	public static List<String> getFilesFromDir(String directory) {
		File dir = new File(directory);

		// Skip files that start with `.'.
		FileFilter filefilter = new FileFilter() {
			public boolean accept(File file) {
				return !file.isDirectory() && !file.isHidden();
			}
		};

		File[] children = dir.listFiles(filefilter);
		if (children == null) {
			throw new ConfigurationException(
					"Either dir does not exist or is not a directory: "
					+directory);
		}
		
		List<String> filenames = new ArrayList<String>();
		for(File c:children) {
			filenames.add(c.getAbsolutePath());
		}
		
		return filenames;
	}
}
