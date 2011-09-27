package linqs.gaia.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import linqs.gaia.exception.InvalidStateException;

/**
 * An iterable object that iterates over lines of a file
 * as you would any other iterable object.
 * <p>
 * Note: The implementation uses a file stream which
 * is close automatically once the last line is read
 * for a given iterator.  If you are reading files,
 * but not all the way to the last line, you must close
 * the file stream manually (i.e., ((FileIterator) itr).close());
 * 
 * @author namatag
 *
 */
public class FileIterable implements Iterable<String> {
	private String filename = null;
	private String charsetName = null;
	
	/**
	 * Creates an iterable object over a file
	 * 
	 * @param filename Filename to iterate over
	 */
	public FileIterable(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Creates an iterable object over a file
	 * using the specified character encoding.
	 * 
	 * @param filename Filename to iterate over
	 * @param charsetName
     *        The name of a supported
     *        {@link java.nio.charset.Charset </code>charset<code>}
	 */
	public FileIterable(String filename, String charsetName) {
		this.filename = filename;
		this.charsetName = charsetName;
	}
	
	public Iterator<String> iterator() {
		BufferedReader br = null;
		try {
			if(charsetName==null) {
				br = new BufferedReader(new FileReader(filename));
			} else {
				br = new BufferedReader(
						new InputStreamReader(new FileInputStream(filename), charsetName));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return new FileIterator(br);
	}
	
	/**
	 * Iterator for use with FileIterable.
	 * 
	 * Note: The implementation uses a file stream which
	 * is close automatically once the last line is read
	 * for a given iterator.  If you are reading files,
	 * but not all the way to the last line, you must close
	 * the file stream manually (i.e., ((FileIterator) itr).close());
	 * 
	 * @author namatag
	 *
	 */
	public static class FileIterator implements Iterator<String> {
		private BufferedReader br;
		String nextline = null;
		
		public FileIterator(BufferedReader br) {
			this.br = br;
			this.getNextLine();
		}
		
		private void getNextLine() {
			try {
				this.nextline = br.readLine();
				
				// Close reader once the last line is read
				if(this.nextline == null) {
					br.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		/**
		 * Close the file stream
		 */
		public void close() {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public boolean hasNext() {
			return this.nextline!=null;
		}

		public String next() {
			String next = this.nextline;
			this.getNextLine();
			
			return next;
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}	
	}
}
