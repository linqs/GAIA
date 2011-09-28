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
package linqs.gaia.graph.datagraph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaManager;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitCateg;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitMVCateg;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitMultiID;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitNum;
import linqs.gaia.graph.datagraph.feature.explicit.DGExplicitString;

/**
 * Manager which stores a schema by its unique schema ID.
 * <p>
 * In {@link DataGraph}, the features contain the values themselves.
 * To do that, the schema is processed to create a new schema where
 * the explicit features are replaced with implementations of
 * those features that support DGFeature.  All feature values
 * are stored in memory, as a result, accessible by using the
 * Decorable item as the key.
 * 
 * @author namatag
 *
 */
public abstract class DGSchemaManager extends BaseConfigurable implements SchemaManager {
	/**
	 * There are two maps to schemas.  One contains the internal representation,
	 * specifically for use in storing the explicit features.
	 * The second is a schema which is meant to hide the internal representation.
	 * Note that the only difference there should be between the schemas of the
	 * two is that the internal one replaces all the explicit features
	 * with some internal representation while the regular schema stores
	 * them as regular explicit features.  Both schemas also point to the
	 * the same DerivedFeature objects.
	 * 
	 * Note: Make sure the information in both schemas are consistent.
	 */
	protected Map<String,Schema> id2internalschema = new ConcurrentHashMap<String,Schema>();
	protected Map<String,Schema> id2schema = new ConcurrentHashMap<String,Schema>();

	public Iterator<String> getAllSchemaIDs(){
		return this.id2internalschema.keySet().iterator();
	}
	
	public Iterator<String> getAllSchemaIDs(SchemaType schemaType) {
		Set<Entry<String,Schema>> schemaset = id2internalschema.entrySet();
		List<String> schemaids = new LinkedList<String>();
		for(Entry<String, Schema> e:schemaset) {
			if(e.getValue().getType().equals(schemaType)) {
				schemaids.add(e.getKey());
			}
		}
		
		return schemaids.iterator();
	}

	public Schema getSchema(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		return this.id2schema.get(schemaID.intern()).copy();
	}
	
	public SchemaType getSchemaType(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		return this.id2internalschema.get(schemaID.intern()).getType();
	}
	
	/**
	 * Since we are storing the true value in the schema
	 * and getSchema just returns a copy which don't have these
	 * values defined, we add this function to let the
	 * DGGraphItems and Graph access the schema directly.
	 * 
	 * @param schemaID Schema ID of schema to get
	 * @return The internal schema object
	 */
	protected Schema getInternalSchema(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		return this.id2internalschema.get(schemaID.intern());
	}

	public boolean hasSchema(String schemaID) {
		return this.id2internalschema.containsKey(schemaID.intern());
	}

	public abstract void removeSchema(String schemaID);
	protected abstract String getGraphSchemaID();
	
	public void removeAllSchemas() {
		Set<String> schemaids = this.id2internalschema.keySet();
		for(String sid:schemaids){
			if(sid.equals(this.getGraphSchemaID())) {
				// Cannot delete the schema of the containing graph
				continue;
			}
			
			this.removeSchema(sid);
		}
	}

	/**
	 * Internal method used to do task common to adding or replacing schema
	 * 
	 * @param schemaID Schema id
	 * @param schema Schema
	 * @param oldschema Schema to update.  Set to null if not updating schema.
	 */
	protected void setSchema(String schemaID, Schema schema, Schema oldschema){
		Schema internalschema = schema.copy();
		
		if(oldschema != null && !internalschema.getType().equals(oldschema.getType())) {
			throw new InvalidStateException("Cannot replace a current schema id " +
					schemaID+" of type "+oldschema.getType()+" to "+internalschema.getType());
		}
		
		Iterator<String> itr = internalschema.getFeatureIDs();
		List<String> fids = new LinkedList<String>();
		while(itr.hasNext()){
			fids.add(itr.next());
		}
		
		// Go through all defined features
		for(String fid:fids){
			Feature f = internalschema.getFeature(fid);
			
			// If schema is being updated from an
			// old schema, replace the feature
			// in the new schema with the feature in the old schema.
			if(oldschema != null && oldschema.hasFeature(fid)){
				Feature newfeature = internalschema.getFeature(fid);
				Feature oldfeature = oldschema.getFeature(fid);
				
				// Only replace a Explicit feature in the new schema with an
				// Explicit feature from the old schema if the feature in the new
				// schema has the same class or equivalent DGFeature class.
				// Similarly, only replace a Derived feature in the new schema
				// with a Derived feature from the old schema if the feature
				// in the new same has the same class and if the configurations
				// for the two features are the same.
				if(
					(newfeature instanceof ExplicitString && oldfeature instanceof DGExplicitString)
					|| (newfeature instanceof ExplicitNum && oldfeature instanceof DGExplicitNum)	
					|| (newfeature instanceof ExplicitCateg && oldfeature instanceof DGExplicitCateg)	
					|| (newfeature instanceof ExplicitMultiCateg && oldfeature instanceof DGExplicitMVCateg)
					|| (newfeature instanceof ExplicitMultiID && oldfeature instanceof DGExplicitMultiID) 
				){
					// Update internal schema with the special feature object.
					// The regular schema should retain the old feature values.
					ExplicitFeature oldef = (ExplicitFeature) oldfeature;
					ExplicitFeature newef = (ExplicitFeature) f;
					
					// If the is closed status is changed in the feature,
					// replace the feature.  Note that this will
					// remove all previously known values for the feature.
					if(oldef.isClosed() != newef.isClosed()) {
						oldef = this.getEquivalentDGFeature(newef);
					}
					
					internalschema.replaceFeature(fid, oldef);
					continue;
				} else if(
						newfeature instanceof DerivedFeature && oldfeature instanceof DerivedFeature
						&& newfeature.getClass().equals(oldfeature.getClass())
						&& ((DerivedFeature) newfeature).hasSameConfiguration((DerivedFeature) oldfeature)
				){
					DerivedFeature olddf = (DerivedFeature) oldfeature;
					DerivedFeature newdf = (DerivedFeature) newfeature;
					
					// Handle case of derived feature cache being changed
					if(olddf.isCaching() != newdf.isCaching()) {
						olddf.setCache(newdf.isCaching());
					}
					
					internalschema.replaceFeature(fid, olddf);
					schema.replaceFeature(fid, olddf);
					continue;
				}
			}
			
			// Convert features to our DGFeatures in internal schema
			// Only convert explicit features
			if(!(f instanceof ExplicitFeature)){
				continue;
			}
			
			ExplicitFeature ef = (ExplicitFeature) f;
			internalschema.replaceFeature(fid, this.getEquivalentDGFeature(ef));
		}
		
		this.id2internalschema.put(schemaID.intern(), internalschema);
		this.id2schema.put(schemaID.intern(), schema);
	}
	
	private ExplicitFeature getEquivalentDGFeature(ExplicitFeature ef) {
		ExplicitFeature dgf = null;
		if(ef instanceof ExplicitNum){
			dgf = new DGExplicitNum((ExplicitNum) ef);
		} else if(ef instanceof ExplicitString){
			dgf = new DGExplicitString((ExplicitString) ef);
		} else if(ef instanceof ExplicitCateg){				
			dgf = new DGExplicitCateg((ExplicitCateg) ef);
		} else if(ef instanceof ExplicitMultiID){
			dgf = new DGExplicitMultiID((ExplicitMultiID) ef);
		} else if(ef instanceof ExplicitMultiCateg){
			dgf = new DGExplicitMVCateg((ExplicitMultiCateg) ef);
		} else {
			throw new UnsupportedTypeException("Unsupported explicit feature: "
					+ef.getClass().getCanonicalName());
		}
		
		return dgf;
	}
	
	public abstract void addSchema(String schemaID, Schema schema);
	
	public void replaceSchema(String schemaID, Schema schema) {
		if(!this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was not previously defined: "+schemaID);
		}
		
		Schema oldschema = this.getInternalSchema(schemaID);
		this.setSchema(schemaID, schema, oldschema);
	}
	
	public void updateSchema(String schemaID, Schema schema) {
		if(!this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was not previously defined: "+schemaID);
		}
		
		Schema oldschema = this.getInternalSchema(schemaID);
		this.setSchema(schemaID, schema, oldschema);
	}
}
