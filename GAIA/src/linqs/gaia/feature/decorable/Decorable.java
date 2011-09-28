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
package linqs.gaia.feature.decorable;

import java.util.List;

import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.identifiable.ID;

/**
 * Base interface for all items that can contain features.
 * <p>
 * Note: All items which hold features must be identifiable since features
 * are keyed using both the schema ID it implements, as well as its
 * own unique identifier.
 * 
 * @author namatag
 *
 */
public interface Decorable {
	/**
	 * Get the feature with the given id.
	 * <p>
	 * For open explicit features, {@link FeatureValue#UNKNOWN_VALUE} is
	 * returned for items where a feature is not defined.
	 * For closed explicit features, the defined default
	 * value is returned.
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * 
	 * @param featureid String id of feature
	 * @return Feature value
	 */
	FeatureValue getFeatureValue(String featureid);

	/**
	 * Get the list of features with the given ids.
	 * <p>
	 * For open explicit features, {@link FeatureValue#UNKNOWN_VALUE} is
	 * returned for items where a feature is not defined.
	 * For closed explicit features, the defined default
	 * value is returned.
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * 
	 * @param featureids List of tring ids of feature
	 * @return List of feature values
	 */
	List<FeatureValue> getFeatureValues(List<String> featureids);

	/**
	 * Check to see if the feature value is known for the given feature.
	 * Return true if the value is known, and false if the value is FeatureValue.UNKNOWN_VALUE
	 * (i.e., if getFeatureValue returns FeatureValue.UNKNOWN_VALUE).
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * 
	 * @param featureid String id of feature
	 * @return True if the value is known, and false if the value is FeatureValue.UNKNOWN_VALUE.
	 */
	boolean hasFeatureValue(String featureid);

	/**
	 * For this object, set feature with the given id with the value.
	 * If the feature is not a {@link ExplicitFeature} whose value
	 * can be set, an exception is thrown.
	 * To set a feature value as unknown for this feature,
	 * you can either remove the value, or you
	 * can call it with the value object of
	 * {@link FeatureValue#UNKNOWN_VALUE}.
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * @see linqs.gaia.feature.values.FeatureValue
	 * 
	 * @param featureid Feature ID
	 * @param value Value of feature for this object
	 */
	void setFeatureValue(String featureid, FeatureValue value);
	
	/**
	 * For this object, set feature with the given id with the string converted
	 * to the equivalent value, as defined below.
	 * If the feature is not a {@link ExplicitFeature} whose value
	 * can be set, an exception is thrown.
	 * To set a feature value as unknown for this feature,
	 * you can either remove the value, or you
	 * can call it with the value object of
	 * {@link FeatureValue#UNKNOWN_VALUE}.
	 * The string values are parsed, based on the feature type, as follows:
	 * <p>
	 * <UL> StringFeature-Value is used directly as the string value
	 * <UL> CategFeature-Value is used as the category with probability of that category set to 1.0
	 * <UL> NumFeature-Value is parsed to its numeric equivalent.  An exception is thrown if it cannot be parsed.
	 * <UL> MultiCategFeature-Value is assumed to be a string delimited set of categories, all of probability 1.0
	 * <UL> MultiIDFeature-Value is assumed to be a string of delimited set of ID, as parsed by {@link ID#parseID}
	 * <LI> 
	 * </UL>
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * @see linqs.gaia.feature.values.FeatureValue
	 * 
	 * @param featureid Feature ID
	 * @param value String value of feature to parse for this object
	 */
	void setFeatureValue(String featureid, String value);
	
	/**
	 * For this object, set feature with the given id with the integer converted
	 * to the equivalent value, as defined below.
	 * If the feature is not a {@link ExplicitFeature} whose value
	 * can be set, an exception is thrown.
	 * To set a feature value as unknown for this feature,
	 * you can either remove the value, or you
	 * can call it with the value object of
	 * {@link FeatureValue#UNKNOWN_VALUE}.
	 * The string values are parsed, based on the feature type, as follows:
	 * <p>
	 * <UL> StringFeature-Value is used directly as the string value
	 * <UL> CategFeature-Value is used as the index of the category.  An exception is thrown if the value
	 * cannot be used as the index.
	 * <UL> NumFeature-Value is used directly as the numeric value
	 * <LI> 
	 * </UL>
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * @see linqs.gaia.feature.values.FeatureValue
	 * 
	 * @param featureid Feature ID
	 * @param value Numeric value of feature to parse for this object
	 */
	void setFeatureValue(String featureid, double value);
	
	/**
	 * For this object, set features with the given ids with the values.
	 * The list of feature ids and values must be the same size
	 * where the index of a feature id corresponds to the value it holds.
	 * If the feature is not a {@link ExplicitFeature} whose value
	 * can be set, an exception is thrown.
	 * To set a feature value as unknown for this feature,
	 * you can either not call this function, or you
	 * can call it with the value object of
	 * {@link FeatureValue#UNKNOWN_VALUE}.
	 * 
	 * @see linqs.gaia.feature.ExplicitFeature
	 * @see linqs.gaia.feature.values.FeatureValue
	 * 
	 * @param featureids List of Feature IDs
	 * @param values List of values of feature for this object
	 */
	void setFeatureValues(List<String> featureids, List<FeatureValue> values);

	/**
	 * For this object, set feature with the given id to have an unknown value.
	 * This is equivalent to setFeatureValue(featureid, FeatureValue.UnknownValue).
	 * 
	 * @param featureid
	 */
	void removeFeatureValue(String featureid);

	/**
	 * Get the schema ID for the item
	 * 
	 * @return Schema ID
	 */
	String getSchemaID();

	/**
	 * Get the schema for the item
	 * 
	 * @return Schema
	 */
	Schema getSchema();
}
