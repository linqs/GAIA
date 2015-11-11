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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * General utilities to simplify downloading and processing content from urls
 */
public class URLUtils {
    /**
     * Return the string representation of the content located at the specific URL. All line breaks
     * are replaced with '\n' for easy parsing.
     * 
     * @param url URL
     * @return String representation of URL contents
     */
    public static String getURLContents(String url) {
        return getURLContents(url, 5000, 5000);
    }

    /**
     * Return the string representation of the content located at the specific URL. All line breaks
     * are replaced with '\n' for easy parsing.
     * 
     * @param url URL
     * @return String representation of URL contents
     */
    public static String getURLContents(String url, int connectionTimeout, int readTimeOut) {
        InputStream is = null;
        StringBuffer content = new StringBuffer();

        try {
            // Create the URL
            URL u = new URL(url);

            // Open an input stream from the url
            URLConnection con = u.openConnection();
            con.setConnectTimeout(connectionTimeout);
            con.setReadTimeout(readTimeOut);
            is = con.getInputStream();

            //is = u.openStream();

            // Convert the stream to a string, separating lines with a newline
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            String s;
            while ((s = bf.readLine()) != null) {
                content.append(s + "\n");
            }
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed URL encountered: " + url, mue);
        } catch (FileNotFoundException fnfe) {
            // Return null if file not found
            content = null;
        } catch (IOException ioe) {
            throw new RuntimeException("IO Exception occurred accessing: " + url, ioe);
        } finally {
            // Close the InputStream no matter what
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // Do nothing
            }
        }

        return content == null ? null : content.toString();
    }

    public static void writeURLContents(String fileName, String url, int connectionTimeout, int readTimeOut) throws Exception {
        InputStream is = null;

        // Create file 
        BufferedWriter out = new BufferedWriter
                (new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));

        try {
            // Create the URL
            URL u = new URL(url);

            // Open an input stream from the url
            URLConnection con = u.openConnection();
            con.setConnectTimeout(connectionTimeout);
            con.setReadTimeout(readTimeOut);
            is = con.getInputStream();

            // Convert the stream to a string, separating lines with a newline
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            String s;
            while ((s = bf.readLine()) != null) {
                out.write(s + "\n");
            }
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed URL encountered: " + url, mue);
        } catch (FileNotFoundException fnfe) {
            // Return nothing if file not found;
        } catch (IOException ioe) {
            throw new RuntimeException("IO Exception occurred accessing: " + url, ioe);
        } finally {
            // Close the InputStream no matter what
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // Do nothing
            }

            out.close();
        }
    }

    /**
     * Return the page redirected to by the specified URI
     * 
     * @param uri Location
     * @return Pages redirected into
     */
    public static String getRedirectURL(String uri)
            throws Exception
    {
        String header;
        try {
            URL url = new java.net.URL(uri);
            HttpURLConnection httpURLConnection =
                    (java.net.HttpURLConnection) url.openConnection();
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.connect();
            header = httpURLConnection.getHeaderField("Location");
        } catch (Exception e) {
            return null;
        }

        return header;
    }
}
