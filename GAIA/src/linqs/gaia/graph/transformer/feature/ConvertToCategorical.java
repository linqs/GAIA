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

import java.util.ArrayList;
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
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.log.Log;

/**
 * Create a categorical feature for the specified features
 * using the string representation of the feature values
 * as the categories.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> features-Comma delimited list of features in the form SCHEMAID.FEATUREID
 * (i.e., person.color,person.gender,dog.breed).
 * </UL>
 * 
 * @author namatag
 *
 */
public class ConvertToCategorical extends Transformer {

	@Override
	public void transform(Graph graph) {
		String[] pairs = this.getStringParameter("features").split(",");
		
		for(String p:pairs) {
			String[] sfpair = p.split("\\.");
			
			String sid = sfpair[0];
			String fid = sfpair[1];
			
			convertToCateg(graph, sid, fid);
		}
	}
	
	/**
	 * Convert the specified feature for the specified schema id and graph
	 * to an explicit valued categorical feature.
	 * 
	 * @param graph Graph
	 * @param sid Schema ID
	 * @param fid Feature ID
	 */
	public static void convertToCateg(Graph graph, String sid, String fid) {
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
		if(categories.isEmpty()) {
			schema = graph.getSchema(sid);
			schema.removeFeature(tmpfid);
			graph.updateSchema(sid, schema);
			Log.WARN("All values unknown.  Unable to convert to categorical: Schema ID="+sid+" Feature ID="+fid);
			return;
		}
		
		// Remove old feature
		schema.removeFeature(fid);
		graph.updateSchema(sid, schema);
		
		// Create categorical feature
		Feature categf = new ExplicitCateg(new ArrayList<String>(categories));
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
			gi.setFeatureValue(fid, new CategValue(fv.getStringValue()));
		}
		
		// Remove temporary feature
		schema.removeFeature(tmpfid);
		graph.updateSchema(sid, schema);
	}

}
