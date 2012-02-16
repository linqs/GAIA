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
package linqs.gaia.feature.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.util.SimplePair;

/**
 * <p>Class which stores the features for a given {@link Decorable} object.
 * A {@link Schema} can only be applied to one {@link SchemaType} which corresponds
 * to the type of {@link Decorable} object in the {@link Graph}
 * it can represent.</p>
 * 
 * <p>{@link Schema} contains a list of {@link Feature} objects keyed by a unique
 * feature id.</p>
 * 
 * @see Feature
 * @see Decorable
 * 
 * @author namatag
 *
 */
public class Schema implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static Pattern featureidpattern = Pattern.compile("^[a-zA-Z_0-9\\-:]+$");
	private Map<String,Feature> features;
	private List<String> fids;
	private SchemaType type;
	
	/**
	 * Schema constructor
	 * 
	 * @param type Type of schema
	 */
	public Schema(SchemaType type) {
		this.type = type;
		this.features = new ConcurrentHashMap<String,Feature>();
		this.fids = Collections.synchronizedList(new ArrayList<String>());
	}
	
	/**
	 * Internal method to handle tasks common to addFeature and replaceFeatue
	 * 
	 * @param fid Feature id
	 * @param feature Feature
	 * @param updatefids If true, add fid to fids.  Otherwise, assume fid is already in fids.
	 */
	private synchronized void setFeature(String fid, Feature feature, boolean updatefids) {
		fid = fid.intern();
		this.features.put(fid,feature);
		
		// Throw an exception if trying to add feature without
		// adding it to the ordered list (assuming its not already there.
		// Note:  For use mainly with replace feature.
		if(!updatefids && !this.fids.contains(fid)) {
			throw new InvalidStateException("Trying to add a feature without"
					+" updating ordered list of features");
		}
		
		if(updatefids) {
			this.fids.add(fid);
		}
	}
	
	/**
	 * Add new feature.  If a feature with the id is already
	 * defined, an exception is thrown.
	 * <p>
	 * Note: Schema ID must match the Java Regex "^[a-zA-Z_0-9\\-:]+$".
	 * 
	 * @param fid Feature id
	 * @param feature Feature
	 */
	public synchronized void addFeature(String fid, Feature feature) {
		if(fid==null) {
			throw new InvalidStateException("Attempting to add a null feature id");
		}
		
		fid = fid.intern();
		if(this.hasFeature(fid)){
			throw new InvalidOperationException("Feature was previously defined: "+fid);
		}
		
		if(!featureidpattern.matcher(fid).matches()) {
			throw new InvalidOperationException("Invalid feature id: "+fid);
		}
		
		if(feature == null) {
			throw new InvalidStateException("Attempting to add a null feature: "+fid);
		}
		
		this.setFeature(fid, feature, true);
	}
	
	/**
	 * Replace current feature.  If a feature was not already defined,
	 * an exception is thrown.
	 * <p>
	 * Note:  The order of the feature ids are returned must remain the same.
	 * 
	 * @param fid Feature id
	 * @param feature Feature
	 */
	public synchronized void replaceFeature(String fid, Feature feature) {
		fid = fid.intern();
		if(!this.hasFeature(fid)){
			throw new InvalidOperationException("Feature was not previously defined: "+fid);
		}
		
		if(feature == null) {
			throw new InvalidStateException("Attempting to replace a feature with a null feature: "
					+fid);
		}
		
		this.setFeature(fid, feature, false);
	}
	
	/**
	 * Check if a feature with the given id is defined
	 * 
	 * @param fid Feature id to check
	 * @return True if the feature is defined, False otherwise
	 */
	public boolean hasFeature(String fid) {
		return this.features.containsKey(fid.intern());
	}
	
	/**
	 * Return the feature with the given id
	 * 
	 * @param fid Feature id
	 * @return Feature
	 */
	public Feature getFeature(String fid) {
		if(!this.hasFeature(fid)){
			throw new InvalidOperationException("Feature undefined in schema: "+fid);
		}
		
		return this.features.get(fid.intern());
	}

	/**
	 * Remove all features from schema
	 */
	public synchronized void removeAllFeatures(){
		Iterator<String> itr = this.getFeatureIDs();
		while(itr.hasNext()){
			this.removeFeature(itr.next());
		}
	}
	
	/**
	 * Remove feature with the given id
	 * 
	 * @param fid Feature id
	 */
	public synchronized void removeFeature(String fid) {
		fid = fid.intern();
		if(!this.hasFeature(fid)){
			throw new InvalidOperationException("Cannot remove undefined feature "+fid);
		}
		
		this.features.remove(fid);
		this.fids.remove(fid);
	}
	
	/**
	 * Get an iterator over all the feature ids
	 * 
	 * @return Iterator over the feature ids
	 */
	public Iterator<String> getFeatureIDs() {
		return (new LinkedList<String>(this.fids)).iterator();
	}
	
	/**
	 * Get an iterator over all the features defined in the schema
	 * 
	 * @return Iterator over feature id and feature pairs
	 */
	public Iterator<SimplePair<String,Feature>> getAllFeatures(){
		ArrayList<SimplePair<String,Feature>> allpairs =
			new ArrayList<SimplePair<String,Feature>>(this.numFeatures());
		Iterator<String> itr = this.getFeatureIDs();
		while(itr.hasNext()) {
			String fid = itr.next();
			allpairs.add(new SimplePair<String,Feature>(fid, this.features.get(fid)));
		}
		
		return allpairs.iterator();
	}
	
	/**
	 * Return the number of features defined in the schema
	 * 
	 * @return Number of features
	 */
	public int numFeatures(){
		return this.fids.size();
	}
	
	/**
	 * Return the type of schema
	 * 
	 * @return SchemaType
	 */
	public SchemaType getType(){
		return this.type;
	}
	
	/**
	 * Return a string with a summary of the schema of the form:<br>
	 * Schema Type: [SchemaType]<br>
	 * Number of Features: [Number of Features]<br>
	 * Features:<br>
	 * f1id(f1)<br>
	 * f2id(f2)<br>
	 * 
	 * @return String summary
	 */
	public String getSummaryString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Schema Type: "+this.getType().name()+"\n");
		buf.append("Number of Features: "+this.numFeatures()+"\n");
		buf.append("Features:\n");
		Iterator<SimplePair<String, Feature>> itr = this.getAllFeatures();
		while(itr.hasNext()){
			SimplePair<String,Feature> pair = itr.next();
			buf.append(pair.getFirst() +" ("
					+ pair.getSecond().getClass().getCanonicalName() + ")\n");
		}
		
		return buf.toString();
	}
	
	/**
	 * Return a string representation of schema of the form:<br>
	 * Schema:[SchemaType]
	 */
	public String toString() {
		String output = "Schema:"+this.getType().name();
		return output;
	}
	
	/**
	 * Create copy of schema
	 * <p>
	 * Note:  It makes a copy of the schema so schema changes must still
	 * be committed.  However, the original features, in particular
	 * derived features, are returned in the copy.  If you want a schema
	 * where all the features are also copies of the original features,
	 * use copyWithFeatures.
	 * 
	 * @return Copy of schema
	 */
	public Schema copy() {
		Schema copy = new Schema(this.getType());
		Iterator<SimplePair<String,Feature>> itr = this.getAllFeatures();
		while(itr.hasNext()) {
			SimplePair<String,Feature> pair = itr.next();
			copy.addFeature(pair.getFirst(), pair.getSecond());
		}
		
		return copy;
	}
	
	/**
	 * Create copy of schema
	 * <p>
	 * Note:  It makes a copy of the schema so schema changes must still
	 * be committed.  The copied schema also contains copies of the
	 * features therein (not pointers to the original object as with derived features).
	 * 
	 * @return Copy of schema
	 */
	public Schema copyWithFeatures() {
		Schema copy = new Schema(this.getType());
		Iterator<SimplePair<String,Feature>> itr = this.getAllFeatures();
		while(itr.hasNext()) {
			SimplePair<String,Feature> pair = itr.next();
			copy.addFeature(pair.getFirst(), pair.getSecond().copy());
		}
		
		return copy;
	}
	
	/**
	 * Generate a feature id which is not already defined in this schema.
	 * This feature id can be used in cases where a temporary feature needs to be
	 * created.  This should not be used whenever a more natural, interpretable
	 * feature id can be set for a given feature.
	 * 
	 * @return Feature ID
	 */
	public String generateRandomFeatureID() {
		Random rand = new Random(System.currentTimeMillis());
		String fid = "feature-"+rand.nextInt();
		while(this.hasFeature(fid)) {
			fid = "feature-"+rand.nextInt();
		} 
		
		return fid;
	}
	
	public static String generateRandomSchemaID(SchemaManager manager) {
		Random rand = new Random(System.currentTimeMillis());
		String sid = "schema-"+rand.nextInt();
		while(manager.hasSchema(sid)) {
			sid = "schema-"+rand.nextInt();
		} 
		
		return sid;
	}
}
