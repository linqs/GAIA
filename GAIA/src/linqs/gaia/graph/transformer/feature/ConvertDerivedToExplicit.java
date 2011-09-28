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

import java.util.Iterator;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.transformer.Transformer;

/**
 * Convert the specified derived features into explicit features
 * with the same value.  Currently only supports numeric, categorical and string
 * features.
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
public class ConvertDerivedToExplicit extends Transformer {

	@Override
	public void transform(Graph graph) {
		String[] pairs = this.getStringParameter("features").split(",");
		
		for(String p:pairs) {
			String[] sfpair = p.split("\\.");
			
			String sid = sfpair[0];
			String fid = sfpair[1];
			this.convertDerivedToExplicit(graph, sid, fid);
		}
	}
	
	private void convertDerivedToExplicit(Graph graph, String sid, String fid) {
		Schema schema = graph.getSchema(sid);
		
		Feature df = schema.getFeature(fid);
		if(!(df instanceof DerivedFeature)) {
			throw new UnsupportedTypeException("Specified feature is not derived: "
					+sid+"."+fid+" is "+df.getClass().getCanonicalName());
		}
		
		ExplicitFeature ef = null;
		if(df instanceof StringFeature) {
			ef = new ExplicitString();
		} else if(df instanceof CategFeature) {
			ef = new ExplicitCateg(((CategFeature) df).getAllCategories());
		} else if(df instanceof NumFeature) {
			ef = new ExplicitNum();
		} else if(df instanceof MultiIDFeature) {
			ef = new ExplicitMultiID();
		} else {
			throw new UnsupportedTypeException("Unsupported Feature type: "
					+sid+"."+fid+" is "+df.getClass().getCanonicalName());
		}
		
		String tempfid = fid+"-gaiatmp";
		schema.addFeature(tempfid, ef);
		graph.updateSchema(sid, schema);
		
		Iterator<GraphItem> gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(fid);
			gi.setFeatureValue(tempfid, fv);
		}
		
		// Remove derived feature
		schema.removeFeature(fid);
		graph.updateSchema(sid, schema);
		
		// Add explicit feature with old name
		schema.addFeature(fid, ef);
		graph.updateSchema(sid, schema);
		
		// Copy the values from the temporary feature to this one
		gitr = graph.getGraphItems(sid);
		while(gitr.hasNext()) {
			GraphItem gi = gitr.next();
			FeatureValue fv = gi.getFeatureValue(tempfid);
			gi.setFeatureValue(fid, fv);
		}
		
		// Remove temporary feature
		schema.removeFeature(tempfid);
		graph.updateSchema(sid, schema);
	}
}
