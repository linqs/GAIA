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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tartarus.snowball.SnowballStemmer;

/**
 * Utility class for creating a bag of words dictionary.
 * Documents are added as strings and these documents are parsed
 * (delimited by whitespace, tab, or new line) and the
 * words stemmed using a Snowball Stemmer defined by libstemmer.
 * Infrequent words, as well as stop words, can be removed
 * using the given functions.  The resulting set of words
 * are the dictionary.
 * <p>
 * Note: This utility requires the Stemmer Library to be in the
 * class path (i.e., snowball.jar).  See Snowball stemming library
 * (http://snowball.tartarus.org) for details.
 * 
 * @author namatag
 *
 */
public class Dictionary {
	private KeyedCount<String> numappear = new KeyedCount<String>();
	private KeyedCount<String> numdocuments = new KeyedCount<String>();
	private KeyedSum<String> word2tfsum = new KeyedSum<String>();
	private SnowballStemmer stemmer = null;
	private int numdocsadded = 0;
	
	/**
	 * Initialize dictionary to use the specified Snowball stemmer
	 * 
	 * @param stemmerclass Class of Snowball stemmer to use
	 * (e.g., org.tartarus.snowball.ext.englishStemmer) 
	 */
	public Dictionary(String stemmerclass) {
		if(stemmerclass != null) {
			try {
				stemmer = (SnowballStemmer) Dynamic.forName(SnowballStemmer.class, stemmerclass);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Initialize dictionary.<br>
	 * Note: If using you want to perform stemming, use {@link #Dictionary(String)} constructor.
	 */
	public Dictionary() {
		this(null);
	}
	
	/**
	 * Add document to list of possible
	 * 
	 * @param document String containing the whole document
	 */
	public void addDocument(String document) {
		// Preprocess document
		document = this.preprocess(document);
		
		// Split the document by whitespace, tab, or newline
		KeyedCount<String> numappearindoc = new KeyedCount<String>();
		String[] words = document.split("\\s+");
		for(String w:words) {
			String currw = stemWord(w);
			
			// Do not add 0 length words
			if(currw.trim().length()==0) {
				continue;
			}
			
			// Count word as appearing in this paper
			// Note: Only count once
			if(!numappearindoc.hasKey(currw)) {
				numdocuments.increment(currw);
			}
			
			// Count how often the word appears in this paper
			numappear.increment(currw);
			numappearindoc.increment(currw);
		}
		
		// Compute term frequency of term in document
		Set<String> docwords = numappearindoc.getKeys();
		for(String w:docwords) {
			word2tfsum.add(w, numappearindoc.getPercent(w));
		}
		
		numdocsadded++;
	}
	
	/**
	 * Apply preprocessing to document
	 * 
	 * @param document Document to preprocess
	 * @return Preprocessed document
	 */
	private String preprocess(String document) {
		// Reduce all words to lower case
		document = document.toLowerCase();
		
		// Remove all non alphanumeric characters
		// Remove apostrophe, dash, and underscore
		document = document.replaceAll("'|-|_", "");
		
		// Replace all other characters with space
		document = document.replaceAll("[^\\w\\s]"," ");
		
		// Replace all multiple spaces with a single space
		document = document.replaceAll("\\s+"," ");
		
		return document;
	}
	
	/**
	 * Get the counts for this document
	 * 
	 * @param document String representing the document
	 * @return Counts of the words in the dictionary
	 */
	public KeyedCount<String> getCounts(String document) {
		KeyedCount<String> counts = new KeyedCount<String>();
		
		// Preprocess document
		document = this.preprocess(document);
		
		// Split the document by whitespace, tab, or newline
		String[] words = document.split("\\s+");
		for(String w:words) {
			String currw = stemWord(w);
			
			if(shouldInclude(currw)) {
				counts.increment(currw);
			}
		}
		
		return counts;
	}
	
	/**
	 * Get the TF-IDF score of each dictionary word in the document.
	 * This assumes the current document is part of the set of documents
	 * added to this dictionary.
	 * 
	 * @param document Document to compute TFIDF values from
	 * @return Map of the string to its TF-IDF score in this document
	 */
	public Map<String,Double> getTFIDF(String document) {
		KeyedCount<String> counts = this.getCounts(document);
		Set<String> words = counts.getKeys();
		Map<String,Double> alltfidf = new HashMap<String,Double>(words.size());
		for(String w:words) {
			// (Number of times this word appears in document/Length of document) *
			// log((Number of documents)/(Number of documents which has this word))
			alltfidf.put(w, ((double) counts.getCount(w) / (double) counts.totalCounted())
				* Math.log((double) this.numdocsadded/(double) this.numdocuments.getCount(w)));
		}
		
		
		return alltfidf;
	}
	
	/**
	 * Get the set of words defined in this dictionary
	 * 
	 * @return Set of words in the dictionary
	 */
	public Set<String> getDictionary() {
		// Get the list of words in the dictionary
		return this.numdocuments.getKeys();
	}
	
	/**
	 * Get the size of the dictionary
	 * 
	 * @return Dictionary size
	 */
	public int getDictionarySize() {
		return this.getDictionary().size();
	}
	
	/**
	 * Number of documents added
	 * 
	 * @return Number of documents added
	 */
	public int getNumDocumentsAdded() {
		return this.numdocsadded;
	}
	
	/**
	 * Saves the dictionary words into the specified file.
	 * The first line stores the number of documents
	 * used to create this dictionary.
	 * Subsequent lines are comma delimited lines, one per word,
	 * of the form word,#wordappearances,#worddocs
	 * 
	 * @param filename File to save dictionary to
	 */
	public void saveDictionary(String filename) {
		BufferedWriter file = null;
		try {
			file = new BufferedWriter(new FileWriter(filename));
			
			// Write the number of of documents in the first line
			file.write(this.numdocsadded+"\n");
			
			Set<String> words = this.getDictionary();
			for(String w:words) {
				file.write(w+","+this.numappear.getCount(w)
						+","+this.numdocuments.getCount(w)
						+","+this.word2tfsum.getSum(w)+"\n");
			}
			
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Loads a dictionary from a file generated by saveDictionary.
	 * Each line is loaded as a word.
	 * 
	 * @param filename File to load dictionary words from
	 */
	public void loadDictionary(String filename) {
		BufferedReader file = null;
		try {
			this.numappear.clearCounts();
			this.numdocuments.clearCounts();
			this.word2tfsum.clearSum();
			
			file = new BufferedReader(new FileReader(filename));
			String line = file.readLine();
			boolean firstline = true;
			while(line!=null) {
				if(line.trim().length()==1) {
					line = file.readLine();
					continue;
				}
				
				if(firstline) {
					this.numdocsadded = Integer.parseInt(line);
					firstline = false;
					line = file.readLine();
					continue;
				}
				
				String[] parts=line.split(",");
				String word = parts[0];
				int numappear = Integer.parseInt(parts[1]);
				int numdocs = Integer.parseInt(parts[2]);
				double tfsum = Double.parseDouble(parts[3]);
				
				this.numappear.setCount(word, numappear);
				this.numdocuments.setCount(word, numdocs);
				this.word2tfsum.add(word, tfsum);
				
				line = file.readLine();
			}
			
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
 	
	/**
	 * Remove the specified word from the dictionary.
	 * 
	 * @param word Word to remove
	 */
	public void removeWord(String word) {
		this.numdocuments.removeKey(word);
		this.numappear.removeKey(word);
		this.word2tfsum.removeKey(word);
	}
	
	/**
	 * Remove words which appears in less documents than the specified
	 * number of documents
	 * 
	 * @param mindocs Minimum number of documents
	 */
	public void removeMinDocs(int mindocs) {
		Set<String> words = new HashSet<String>(this.numdocuments.getKeys());
		for(String w:words) {
			if(this.numdocuments.getCount(w)<mindocs) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which appears in less documents than the specified
	 * percentage of documents
	 * 
	 * @param minpctdocs Minimum percentage of documents
	 */
	public void removeMinPctDocs(double minpctdocs) {
		Set<String> words = new HashSet<String>(this.numdocuments.getKeys());
		for(String w:words) {
			if(((double) this.numdocuments.getCount(w)/this.numdocsadded)<minpctdocs) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which appears less than the specified
	 * number of appearances
	 * 
	 * @param minapp Minimum number of appearances
	 */
	public void removeMinAppearances(int minapp) {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(this.numappear.getCount(w)<minapp) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words whose length is below (exclusive) the specified length
	 * 
	 * @param minlength Minimum length of word
	 */
	public void removeMinWordLength(int minlength) {
		Set<String> words = new HashSet<String>(this.numdocuments.getKeys());
		for(String w:words) {
			if(w.length()<minlength) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which appears in more documents than the specified
	 * number of documents
	 * 
	 * @param maxdocs Maximum percentage of documents
	 */
	public void removeMaxDocs(int maxdocs) {
		Set<String> words = new HashSet<String>(this.numdocuments.getKeys());
		for(String w:words) {
			if(this.numdocuments.getCount(w)>maxdocs) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which appears in more documents than the specified
	 * percentage of documents
	 * 
	 * @param maxpctdocs Maximum number of documents
	 */
	public void removeMaxPctDocs(double maxpctdocs) {
		Set<String> words = new HashSet<String>(this.numdocuments.getKeys());
		for(String w:words) {
			if(((double) this.numdocuments.getCount(w)/this.numdocsadded)>maxpctdocs) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which appears more than the specified
	 * number of appearances
	 * 
	 * @param maxapps Maximum number of appearances
	 */
	public void removeMaxAppearances(int maxapps) {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(this.numappear.getCount(w)>maxapps) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove the words which appear in the stop list file
	 * where the file has one word per line.
	 * Comments can be included by prefixing the line with
	 * the pound symbol (i.e., #).
	 * <p>
	 * An example stop words file is available at
	 * lib/libstemmer-20090108/stopwords.txt.
	 * 
	 * @param file File of stop words
	 */
	public void removeStopWords(String file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String word = null;
			while((word = reader.readLine()) != null) {
				// Remove words in stop list
				word = word.toLowerCase().trim();
				
				// Skip comment lines
				if(word.startsWith("#")) {
					continue;
				}

				this.removeWord(word);
			}
			
			reader.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Remove words which are just numbers
	 */
	public void removeNumeric() {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(Numeric.isNumeric(w)) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words which are contain at least one numbers or special characters
	 */
	public void removeNonAlpha() {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(w.matches(".*\\d.*")) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words whose word length is below the specified length
	 * 
	 * @param minlength Minimum word length
	 */
	public void removeMinLength(int minlength) {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(w.length()<minlength) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Remove words whose word length is above the specified length
	 * 
	 * @param maxlength Maximum word length
	 */
	public void removeMaxLength(int maxlength) {
		Set<String> words = new HashSet<String>(this.numappear.getKeys());
		for(String w:words) {
			if(w.length()>maxlength) {
				this.removeWord(w);
			}
		}
	}
	
	/**
	 * Filter the words using the sum of the TF-IDF
	 * values of that word across all documents.
	 * 
	 * @param k Maximum number of words to keep
	 */
	public void filterTopKByTFIDFSum(int k) {
		TopK<String> topk = new TopK<String>(k);
		Set<String> wordset = new HashSet<String>(word2tfsum.getKeys());
		for(String w:wordset) {
			double tfidfsum = word2tfsum.getSum(w)
				* Math.log((double) this.numdocsadded/(double) this.numdocuments.getCount(w));
			List<String> toremove = topk.add(tfidfsum, w);
			for(String rw:toremove) {
				removeWord(rw);
			}
		}
	}
	
	/**
	 * Return the stemmed version of this word
	 * 
	 * @param word Word to stem
	 * @return Stem of word
	 */
	private String stemWord(String word) {
		// If not using a stemmer, return original word
		if(stemmer==null) {
			return word;
		}
		
		stemmer.setCurrent(word);
		stemmer.stem();
		String stemmed = stemmer.getCurrent();
		
		return stemmed;
	}
	
	/**
	 * Check to see whether or not to include this word
	 * 
	 * @param word Word to check
	 * @return True if the word is in the dictionary and false otherwise
	 */
	private boolean shouldInclude(String word) {
		return this.numdocuments.hasKey(word);
	}
}
