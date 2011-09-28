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

import java.util.Iterator;
import java.util.regex.Pattern;

import linqs.gaia.feature.decorable.Decorable;

/**
 * General interface for storing and accessing all {@link Schema} objects.
 * 
 * @author namatag
 *
 */
public interface SchemaManager {
	/**
	 * All schema ids must match this pattern
	 */
	static Pattern schemaidpattern = Pattern.compile("^[a-zA-Z_0-9\\-]+$");
	
	/**
	 * Add schema with the given schema id.
	 * If the schema is already defined, an exception is thrown.
	 * <p>
	 * Note: Schema ID must match the Java Regex "^[a-zA-Z_0-9\\-]+$".
	 * 
	 * @param schemaID Schema ID
	 * @param schema Schema to add
	 */
	void addSchema(String schemaID, Schema schema);

	/**
	 * Replace the schema currently defined with the schema id
	 * with a new schema.  If the schema is not already defined,
	 * an exception is thrown.
	 * <p>
	 * Note: Replacing a schema will remove all values previously
	 * defined for {@link Decorable} items with the corresponding schema.
	 * 
	 * @param schemaID Schema ID
	 * @param schema Schema
	 */
	void replaceSchema(String schemaID, Schema schema);
	
	/**
	 * Update the schema currently defined with the schema id
	 * with a new schema.  If the schema is not already defined,
	 * an exception is thrown.
	 * <p>
	 * Note: Updating a schema means for all explicit features whose declaration is unchanged
	 * (i.e., given a feature id, the class, configuration,
	 * and caching status (for ExplicitFeatures) of the feature is unchanged),
	 * the values for the corresponding {@link Decorable} objects are not removed.
	 * Any values for features not in the updated schema will be removed for
	 * {@link Decorable} items with the corresponding schema.
	 * <p>
	 * For Derived features, the old feature, and any cached information therein,
	 * are only saved if the class of the derived item and
	 * configuration is the same.
	 * 
	 * @param schemaID Schema ID
	 * @param schema Schema
	 */
	void updateSchema(String schemaID, Schema schema);

	/**
	 * Return a copy of the schema, created with schema.copy, with the given schema id
	 * 
	 * @param schemaID Schema id
	 * @return Schema
	 */
	Schema getSchema(String schemaID);
	
	/**
	 * Return the type of the schema with the given schema id
	 * 
	 * @param schemaID Schema id
	 * @return Schema Type
	 */
	SchemaType getSchemaType(String schemaID);
	
	/**
	 * Remove schema with the given schema id
	 * 
	 * 
	 * @param schemaID Schema id
	 */
	void removeSchema(String schemaID);
	
	/**
	 * Remove all schemas
	 */
	void removeAllSchemas();

	/**
	 * Check to see if a schema with the given schema id was already defined.
	 * 
	 * @param schemaID Schema id
	 * @return True if a schema with the given schema id is already defined.  False otherwise.
	 */
	boolean hasSchema(String schemaID);

	/**
	 * Get all defined schema ids
	 * 
	 * @return Iterator over the schema ids.
	 */
	Iterator<String> getAllSchemaIDs();
	
	/**
	 * Return all schema ids with the given schemaType
	 * 
	 * @param schemaType Schema Type of schemas you want
	 * @return Iterator over schema ids
	 */
	Iterator<String> getAllSchemaIDs(SchemaType schemaType);
}