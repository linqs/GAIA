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
package linqs.gaia.prediction.existence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.prediction.CategoricalValuedGroup;
import linqs.gaia.prediction.PositiveOnlyGroup;
import linqs.gaia.prediction.Prediction;
import linqs.gaia.util.FileIterable;
import linqs.gaia.util.UnmodifiableList;

/**
 * Created to handle existence (e.g., link existence) predictions.
 * An important difference between this and standard categorical predictions
 * is that this is designed to be used in cases where there is a huge skew
 * between those that exist and those that do not.
 * In such instances, its often common to use "blocking" techniques so only
 * a subset of the overall number is actually explicitly predicted.
 * This is a positive only group which means only those predictions which are predicted to EXIST
 * are explicitly stored, and those which are not are assumed to have a value
 * of NOTEXIST.
 * <br>
 * Note: There is support for adding negative predictions separately
 * (i.e., {@link #addNegativePrediction(ExistencePred)}) for use in things like
 * {@link linqs.gaia.prediction.statistic.AUC} where its important
 * to use the probability of the negative prediction for whatever subset
 * of the negative instances you explicity predicted over.
 * 
 * @author namatag
 *
 */
public class ExistencePredGroup implements PositiveOnlyGroup, CategoricalValuedGroup {
	public static final String EXIST = "EXIST";
	public static final String NOTEXIST = "NOTEXIST";
	public static final UnmodifiableList<String> EXISTENCE =
		new UnmodifiableList<String>(Arrays.asList(NOTEXIST,EXIST));
	public static final double[] EXISTPROB = new double[]{0,1};
	public static final double[] NOTEXISTPROB = new double[]{1,0};
	
	private long numpositive = 0;
	private long numtotal = 0;
	private List<ExistencePred> predictions = new LinkedList<ExistencePred>();
	private List<ExistencePred> partialnegpredictions = new LinkedList<ExistencePred>();
	
	public ExistencePredGroup() {
		// Do nothing
	}
	
	/**
	 * Constructor
	 * 
	 * @param numtotal Number of total items (including those not explicitly defined)
	 * @param numpositive Number of total positive items (including those positive items not explicitly defined)
	 */
	public ExistencePredGroup(long numtotal, long numpositive) {
		this.numtotal = numtotal;
		this.numpositive = numpositive;
	}
	
	public void setNumPositive(long numpositive) {
		this.numpositive = numpositive;
	}
	
	public void setNumTotal(long numtotal) {
		this.numtotal = numtotal;
	}

	public long getNumPositive() {
		return this.numpositive;
	}

	public long getNumTotal() {
		return this.numtotal;
	}
	
	public void addPrediction(ExistencePred ep) {
		ep.setPredValue(ExistencePredGroup.EXIST);
		if(ep.getProbs()==null) {
			ep.setProbs(ExistencePredGroup.EXISTPROB);
		}
		
		predictions.add(ep);
	}

	public Iterator<? extends Prediction> getAllPredictions() {
		return this.predictions.iterator();
	}

	public long numPredictions() {
		long size = this.predictions.size();
		return size;
	}

	public void removeAllPredictions() {
		this.predictions.clear();
	}
	
	/**
	 *  Handle explicitly considered negative predictions
	 *  
	 * @param ep Negative prediction
	 */
	public void addNegativePrediction(ExistencePred ep) {
		ep.setPredValue(ExistencePredGroup.NOTEXIST);
		if(ep.getProbs()==null) {
			ep.setProbs(ExistencePredGroup.NOTEXISTPROB);
		}
		
		this.partialnegpredictions.add(ep);
	}

	public Iterator<? extends Prediction> getPartialNegativePredictions() {
		return this.partialnegpredictions.iterator();
	}
	
	public long numPartialPredictions() {
		return this.partialnegpredictions.size();
	}

	public void removeAllPartialNegativePredictions() {
		this.partialnegpredictions.clear();
	}

	public String getNegativeValue() {
		return NOTEXIST;
	}

	public String getPositiveValue() {
		return EXIST;
	}

	public UnmodifiableList<String> getCategories() {
		return EXISTENCE;
	}
	
	/**
	 * Save predictions into the specified file where
	 * the first line is tab delimited with total number and positive number, consecutively,
	 * and the subsequent lines correspond to the
	 * string representation of a single {@link ExistencePred}
	 * object.
	 * 
	 * @param filename File to save predictions into
	 */
	public void savePredictions(String filename) {
		try {
			FileWriter fstream = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(fstream);
			
			out.write(numtotal+"\t"+numpositive);
			out.write("\n");
			
			// Print positive predictions
			Iterator<? extends Prediction> itr = getAllPredictions();
			while(itr.hasNext()) {
				ExistencePred ep = (ExistencePred) itr.next();
				out.write(ep.toString());
				out.write("\n");
			}
			
			// Print negative predictions
			itr = getPartialNegativePredictions();
			while(itr.hasNext()) {
				ExistencePred ep = (ExistencePred) itr.next();
				out.write(ep.toString());
				out.write("\n");
			}
			
			out.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ExistencePredGroup loadPredictions(String filename) {
		ExistencePredGroup epg = null;
		FileIterable fitrbl = new FileIterable(filename);
		boolean isfirst = true;
		for(String line:fitrbl) {
			// Skip empty lines
			if(line.trim().isEmpty()) {
				continue;
			}
			
			if(isfirst) {
				isfirst = false;
				
				// Process counts from first line
				String[] parts = line.split("\t");
				if(parts.length!=2) {
					throw new InvalidStateException("Invalid tab delimited first line: "+line);
				}
				
				long numtotal = Long.parseLong(parts[0]);
				long numpositive = Long.parseLong(parts[1]);
				
				epg = new ExistencePredGroup();
				epg.setNumTotal(numtotal);
				epg.setNumPositive(numpositive);
			} else {
				ExistencePred ep = ExistencePred.parseString(line);
				if(ep.getPredValue().equals(ExistencePredGroup.EXIST)) {
					epg.addPrediction(ep);
				} else {
					epg.addNegativePrediction(ep);
				}
			}
		}
		
		return epg;
	}
}
