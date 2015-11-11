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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

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
     * @param append If true, append to file. Otherwise, overwrite.
     */
    public static void write2file(String file, String text, boolean append) {
        try {
            // Create necessary dirs
            String dirs = file.substring(0, file.lastIndexOf(File.separator));
            if (dirs != null && dirs.trim().length() != 0 && !(new File(dirs)).exists()) {
                createDirectories(dirs);
            }

            // Create file 
            BufferedWriter out = new BufferedWriter
                    (new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
            out.write(text);

            //Close the output stream
            out.close();
        } catch (Exception e) {//Catch exception if any
            throw new RuntimeException(e);
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
     * Return contents of file as a string
     * 
     * @param file Filename of file to read from
     * @return String representing file contents
     */
    public static String readFromFile(String file) {
        int len;
        char[] chr = new char[4096];
        StringBuffer buffer = new StringBuffer();
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            while ((len = reader.read(chr)) > 0) {
                buffer.append(chr, 0, len);
            }

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return buffer.toString();
    }

    /**
     * Return contents of file as a string
     * 
     * @param filename Name of file
     * @param charsetName Charset to use. Recommend "UTF-8"
     * @param isZipFile True if its a zip file and false otherwise.
     * @return String representation of file content
     */
    public static String readFromFile(String filename, String charsetName, boolean isZipFile) {
        StringBuffer buffer = new StringBuffer();
        BufferedReader br = null;
        try {
            if (isZipFile && charsetName != null) {
                br = new BufferedReader(
                        new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), charsetName));
            } else if (isZipFile) {
                br = new BufferedReader(
                        new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
            } else if (charsetName == null) {
                br = new BufferedReader(new FileReader(filename));
            } else {
                br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(filename), charsetName));
            }

            int len;
            char[] chr = new char[4096];
            try {
                while ((len = br.read(chr)) > 0) {
                    buffer.append(chr, 0, len);
                }

                br.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return buffer.toString();
    }

    /**
     * Create directories specified by the path or do nothing if the directory already exists.
     * 
     * @param path Path containing only directory names
     */
    public static void createDirectories(String path) {
        File file = (new File(path));
        if (file.exists()) {
            return;
        }

        boolean success = file.mkdirs();
        if (!success) {
            throw new RuntimeException("Unable to create directories for " + path);
        }
    }

    /**
     * Get a system temporary directory path to use. This not only returns the path but creates the
     * directory so it is available for immediate use.
     * 
     * @return Directory path for temporary directory
     */
    public static String getTemporaryDirectory() {
        String dirname = System.getProperty("java.io.tmpdir") + File.separator + System.nanoTime();
        createDirectories(dirname);

        return dirname;
    }

    /**
     * Check to see if a file with the given name exists
     * 
     * @param filename Name of file
     * @return True if the file exists, false otherwise
     */
    public static boolean fileExists(String filename) {
        File file = (new File(filename));
        if (file.exists()) {
            return true;
        }

        return false;
    }

    /**
     * Copy the contents of the source file to the target file. If the target file exists, it is
     * overwritten.
     * 
     * @param source Source file
     * @param target Target file
     */
    public static void copyFile(String source, String target) {
        try {
            File f1 = new File(source);
            File f2 = new File(target);
            InputStream in = new FileInputStream(f1);

            //For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a list containing the string representation of the absolute file names of files in the
     * directory
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
                            + directory);
        }

        List<String> filenames = new ArrayList<String>();
        for (File c : children) {
            filenames.add(c.getAbsolutePath());
        }

        return filenames;
    }

    /**
     * Compute the number of lines in a file
     * 
     * @param filename Name of file
     * @return Number of lines in file
     */
    public static int getNumLinesInFile(String filename) {
        int cnt = 0;
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(filename));
            while ((reader.readLine()) != null) {
            }

            cnt = reader.getLineNumber();
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return cnt;
    }
}
