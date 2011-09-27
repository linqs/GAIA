package linqs.gaia.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * General utilities to simplify downloading and processing content from urls
 */
public class URLUtils {
	/**
	 * Return the string representation of the content located at the
	 * specific URL.  All line breaks are replaced with '\n' for easy
	 * parsing.
	 * 
	 * @param url URL
	 * @return String representation of URL contents
	 */
	public static String getURLContents(String url) {
		InputStream is = null;
		StringBuffer content = new StringBuffer();

		try {
			// Create the URL
			URL u = new URL(url);

			// Open an input stream from the url
			is = u.openStream();
			
			// Convert the stream to a string, separating lines with a newline
			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
			String s;
			while ((s = bf.readLine()) != null) {
				content.append(s + "\n");
			}
			
		} catch (MalformedURLException mue) {
			throw new RuntimeException("Malformed URL encountered: "+url, mue);
		} catch (IOException ioe) {
			throw new RuntimeException("IO Exception occurred accessing: "+url, ioe);
		} finally {
			// Close the InputStream no matter what
			try {
				if(is != null) {
					is.close();
				}
			} catch (IOException ioe) {
				// Do nothing
			}
		}

		return content.toString();
	}
}
