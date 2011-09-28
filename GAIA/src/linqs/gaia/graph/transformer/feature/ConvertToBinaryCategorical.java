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
package linqs.gaia.graph.transformer.feature;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.feature.Feature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.log.Log;

/**
 * Create a binary categorical feature for the specified features
 * using the string representation of the feature values
 * as the categories.  The binary feature is {@link Constants#TRUE} for all items
 * whose value matches the select string and {@link Constants#FALSE} for all else.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> selectstring-String representation of value whose value to consider TRUE
 * <LI> features-Comma delimited list of features in the form SCHEMAID.FEATUREID
 * (i.e., person.color,person.gender,dog.breed).
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConvertToBinaryCategorical extends Transformer {

	@Override
	public void transform(Graph graph) {
		String[] pairs = this.getStringParameter("features").split(",");
		String value = this.getStringParameter("selectstring");
		
		for(String p:pairs) {
			String[] sfpair = p.split("\\.");
			
			String sid = sfpair[0];
			String fid = sfpair[1];
			
			convertToBinary(graph, sid, fid, value);
		}
	}
	
	/**
	 * Convert the specified feature for the specified schema id and graph
	 * to an explicit binary valued categorical feature whose
	 * value is {@link Constants#TRUE} if the string representation of the
	 * value matches the specified value and {@link Constants#FALSE} otherwise.
	 * 
	 * @param graph Graph
	 * @param sid Schema ID
	 * @param fid Feature ID
	 * @param value Value 
	 */
	public static void convertToBinary(Graph graph, String sid, String fid, String value) {
		String tmpfid = "gaiatemp-"+fid;
		Schema schema = graph.getSchema(sid);
		
		// Add temporary feature
		Feature tempf = new ExplicitString();
		schema.addFeature(tmpfid, tempf);
		graph.updateSchema(sid, schema);
		
		// Copy values to temporary feature
		Set<String> categories = new HashSet<String>();
		Iterator<GraphItem> gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(fid);
			
			// Get the set of categories
			if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				categories.add(fv.getStringValue());
				
				// Copy feature value to temporary feature
				gi.setFeatureValue(tmpfid, new StringValue(fv.getStringValue()));
			}
		}
		
		// Handle the case when there are no values known
		// by undoing any changes so far and printing a warning
		if(categories.isEmpty() || !categories.contains(value)) {
			schema = graph.getSchema(sid);
			schema.removeFeature(tmpfid);
			graph.updateSchema(sid, schema);
			Log.WARN("All values unknown.  Unable to convert to binary category:" +
					" Schema ID="+sid+" Feature ID="+fid+" Value="+value);
			return;
		}
		
		// Remove old feature
		schema.removeFeature(fid);
		graph.updateSchema(sid, schema);
		
		// Create categorical feature
		Feature categf = new ExplicitCateg(Constants.FALSETRUE);
		schema.addFeature(fid, categf);
		graph.updateSchema(sid, schema);
		
		// Set the value for the categorical feature
		gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(tmpfid);
			
			// Skip unknown values
			if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			}
			
			// Copy feature value to temporary feature
			if(fv.getStringValue().equals(value)) {
				gi.setFeatureValue(fid, new CategValue(Constants.TRUE));
			} else {
				gi.setFeatureValue(fid, new CategValue(Constants.FALSE));
			}
		}
		
		// Remove temporary feature
		schema.removeFeature(tmpfid);
		graph.updateSchema(sid, schema);
	}

}
