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
package linqs.gaia.graph.dbgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaManager;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.util.IteratorUtils;

/**
 * Database schema manager
 * 
 * @author namatag
 *
 */
public abstract class DBSchemaManager extends DBSystemManager implements SchemaManager {
	Map<String,Schema> id2schema = new HashMap<String,Schema>();
	
	protected String getTableName(String sid) {
		return "schema_"+sid;
	}

	public Iterator<String> getAllSchemaIDs() {
		return this.id2schema.keySet().iterator();
	}

	public Schema getSchema(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		return this.id2schema.get(schemaID).copy();
	}

	public Iterator<String> getAllSchemaIDs(SchemaType schemaType) {
		Set<Entry<String,Schema>> schemaset = id2schema.entrySet();
		List<String> schemaids = new LinkedList<String>();
		for(Entry<String, Schema> e:schemaset) {
			if(e.getValue().getType().equals(schemaType)) {
				schemaids.add(e.getKey());
			}
		}
		
		return schemaids.iterator();
	}

	public SchemaType getSchemaType(String schemaID) {
		if(!this.hasSchema(schemaID)) {
			throw new InvalidOperationException("No schema with the given ID defined: "+schemaID);
		}
		
		return this.id2schema.get(schemaID.intern()).getType();
	}

	public boolean hasSchema(String schemaID) {
		// Called to initialize the graph, if necessary
		this.getConnection();
		
		return this.id2schema.containsKey(schemaID.intern());
	}

	public abstract void removeSchema(String schemaID);
	
	public void addSchema(String schemaID, Schema schema) {
		if(this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was previously defined: "+schemaID);
		}
		
		if(!schemaidpattern.matcher(schemaID).matches()) {
			throw new InvalidOperationException("Invalid feature id: "+schemaID);
		}
		
		// Drop schema table if it exists
		String stable = this.getTableName(schemaID);
		this.dropTableIfExists(stable);
		
		// Add to list
		// Create table corresponding to schema
		String sql = "CREATE TABLE "+stable
			+" (dbid int, fid varchar(255), value varchar(255), prob varchar(255), "
			+" PRIMARY KEY (dbid, fid)";
		
		if(schema.getType().equals(SchemaType.NODE)) {
			sql += ", FOREIGN KEY (dbid) REFERENCES nodes(dbid) ON DELETE CASCADE";
		} else if(schema.getType().equals(SchemaType.DIRECTED) || schema.getType().equals(SchemaType.UNDIRECTED)) {
			sql += ", FOREIGN KEY (dbid) REFERENCES edges(dbid) ON DELETE CASCADE";
		} else if(schema.getType().equals(SchemaType.GRAPH)) {
			// Do nothing
		} else {
			throw new UnsupportedTypeException("Unsupported schema type: "+schema.getType());
		}
		sql +=")";
		
		this.executeSQL(sql);
		
		// Create index for table
		this.executeSQL("CREATE INDEX "+stable+"index ON "+stable+" (fid)");
		id2schema.put(schemaID, schema);
	}
	
	public void replaceSchema(String schemaID, Schema schema) {
		if(!this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was not previously defined: "+schemaID);
		}
		
		Schema oldschema = this.getSchema(schemaID);
		this.setSchema(schemaID, schema, oldschema);
	}
	
	public void updateSchema(String schemaID, Schema schema) {
		if(!this.hasSchema(schemaID)){
			throw new InvalidOperationException("Schema was not previously defined: "+schemaID);
		}
		
		Schema oldschema = this.getSchema(schemaID);
		this.setSchema(schemaID, schema, oldschema);
	}
	
	protected void setSchema(String schemaID, Schema schema, Schema oldschema){
		// Replacement or updated schemas must be the
		// same SchemaType as the old schema
		if(oldschema != null && !schema.getType().equals(oldschema.getType())) {
			throw new InvalidStateException("Cannot replace a current schema id " +
					schemaID+" of type "+oldschema.getType()+" to "+schema.getType());
		}
		
		if(oldschema==null) {
			throw new InvalidStateException("Old schema cannot be null");
		}
		
		String stable = this.getTableName(schemaID);
		
		// Get list of features from new schema
		Iterator<String> itr = schema.getFeatureIDs();
		Set<String> newfids = new HashSet<String>(IteratorUtils.iterator2stringlist(itr));
		itr = oldschema.getFeatureIDs();
		Set<String> oldfids = new HashSet<String>(IteratorUtils.iterator2stringlist(itr));
		
		Set<String> same = new HashSet<String>(newfids);
		same.retainAll(oldfids);
		Set<String> removed = new HashSet<String>(oldfids);
		removed.removeAll(newfids);
		
		String sql = null;
		// Remove values for removed explicit features
		for(String fid:removed) {
			Feature f = oldschema.getFeature(fid);
			
			if(f instanceof ExplicitFeature) {
				sql = "DELETE FROM "+stable+" WHERE fid='"+fid+"'";
				this.executeSQL(sql);
			}
		}
		
		// Remove values for current features whose type has changed
		for(String fid:same) {
			Feature f = schema.getFeature(fid);
			Feature oldf = oldschema.getFeature(fid);
			if(f instanceof ExplicitFeature && !f.equals(oldf)) {
				sql = "DELETE FROM "+stable+" WHERE fid='"+fid+"'";
				this.executeSQL(sql);
			}
		}
		
		this.id2schema.put(schemaID.intern(), schema);
	}
	
}
