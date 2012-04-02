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
package linqs.gaia.prediction.feature;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.util.FileIterable;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Collection of category valued feature value predictions.
 * 
 * @author namatag
 *
 */
public class CategValuePredGroup implements CategoricalValuedGroup {
	UnmodifiableList<String> categories;
	List<CategValuePred> predictions;
	Set<String> categoriesset;
	
	/**
	 * Constructor
	 * 
	 * @param categories Unmodifiable list of categories
	 */
	public CategValuePredGroup(UnmodifiableList<String> categories) {
		this.categories = categories;
		this.categoriesset = new HashSet<String>(categories.copyAsList());
		
		this.predictions = new LinkedList<CategValuePred>();
	}
	
	/**
	 * Constructor
	 * 
	 * @param categories List of categories
	 */
	public CategValuePredGroup(List<String> categories) {
		this(new UnmodifiableList<String> (categories));
	}
	
	/**
	 * Constructor
	 * 
	 * @param categories Iterator over all categories
	 */
	public CategValuePredGroup(Iterator<String> categories) {
		this(IteratorUtils.iterator2stringlist(categories));
	}
	
	/**
	 * Add prediction
	 * 
	 * @param cvp Prediction to add
	 */
	public void addPrediction(CategValuePred cvp) {
		String truevalue = cvp.getTrueValue();
		String predvalue = cvp.getPredValue();
		
		if(!this.categoriesset.contains(truevalue)) {
			throw new InvalidStateException("Invalid true value: "+truevalue);
		}
		
		if(!this.categoriesset.contains(predvalue)) {
			throw new InvalidStateException("Invalid predicted value: "+predvalue);
		}
		
		this.predictions.add(cvp);
	}

	/**
	 * Get Categories
	 */
	public UnmodifiableList<String> getCategories() {
		return this.categories;
	}

	/**
	 * Get iterator over all predictions
	 */
	public Iterator<? extends Prediction> getAllPredictions() {
		return this.predictions.iterator();
	}

	/**
	 * Return total number of predictions
	 */
	public long numPredictions() {
		return this.predictions.size();
	}

	/**
	 * Remove all defined predictions
	 */
	public void removeAllPredictions() {
		this.predictions.clear();
	}
	
	/**
	 * Save predictions into the specified file where
	 * the first line is a comma delimited list of categories
	 * and the subsequent lines correspond to the
	 * string representation of a single {@link CategValuePred}
	 * object.
	 * 
	 * @param filename Filename
	 */
	public void savePredictions(String filename) {
		try {
			FileWriter fstream = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(fstream);
			
			out.write(ListUtils.list2string(this.categories.copyAsList(), ","));
			out.write("\n");
			Iterator<? extends Prediction> itr = getAllPredictions();
			while(itr.hasNext()) {
				CategValuePred cvp = (CategValuePred) itr.next();
				out.write(cvp.toString());
				out.write("\n");
			}
			
			out.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Load predictions from the specified file where
	 * the first line is a comma delimited list of categories
	 * and the subsequent lines correspond to the
	 * string representation of a single {@link CategValuePred} object.
	 * 
	 * @param filename Filename
	 * @return {@link CategValuePredGroup} saved into file
	 */
	public static CategValuePredGroup loadPredictions(String filename) {
		CategValuePredGroup cvpg = null;
		FileIterable fitrbl = new FileIterable(filename);
		boolean isfirst = true;
		for(String line:fitrbl) {
			// Skip empty lines
			if(line.trim().isEmpty()) {
				continue;
			}
			
			if(isfirst) {
				isfirst = false;
				
				// Process categories from first line
				String[] parts = line.split(",");
				List<String> cats = new ArrayList<String>(parts.length);
				for(String p:parts) {
					cats.add(p);
				}
				
				cvpg = new CategValuePredGroup(cats);
			} else {
				CategValuePred cvp = CategValuePred.parseString(line);
				cvpg.addPrediction(cvp);
			}
		}
		
		return cvpg;
	}
}
