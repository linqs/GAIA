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
package linqs.gaia.example;

import java.util.Arrays;
import java.util.Iterator;

import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.MultiCategFeature;
import linqs.gaia.feature.NumFeature;
import linqs.gaia.feature.StringFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.structural.IncidentCount;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.TabDelimIO;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.OutputUtils;
import linqs.gaia.util.UnmodifiableSet;

/**
 * This is an example for accessing the features of items in GAIA.
 * All features of GAIA which can contain features (i.e., Nodes, Edges, Graphs)
 * implement the Decorable interface.  All Decorable items have a schema
 * signifying what types of features it can hold (e.g., a node with a person schema
 * may have age, height, and gender as features).  There are two main types
 * of features (Explicit and Derived) where the former are features whose
 * values you specify and the latter are features whose values are derived/computed
 * on the fly.  There are also subtypes to the features which specify what type of feature
 * is (e.g., is it a numerical or string feature?).
 * 
 * @see linqs.gaia.feature
 * @see linqs.gaia.feature.decorable
 * @see linqs.gaia.feature.schema.Schema
 * 
 * @author namatag
 *
 */
public class FeatureAccess {
	private static IO io;

	public static Graph getSampleGraph() {
		// Specify IO method
		io = new TabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("files", 
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/graph-SocialNetwork.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/node-Person.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/node-School.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/undirected-Family.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/directed-Friend.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/directed-Attends.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/undirected-Enemy.tab");

		// Load Graph
		return io.loadGraph();
	}

	public static void main(String[] args) throws Exception {
		// Set logging level to show all
		Log.showAllLogging();
		
		Graph g = getSampleGraph();
		Log.INFO("Loading Graph Successful!");
		Log.INFO("Loaded graph with "+g.numNodes()+" nodes and "+g.numEdges()+" edges\n\n");
		
		Log.INFO(OutputUtils.separator("#", 40));
		Log.INFO("Example for how to access schemas");
		// Get the list of schemas defined in this graph
		Iterator<String> schemaids = g.getAllSchemaIDs();
		while(schemaids.hasNext()) {
			String schemaid = schemaids.next();
			
			Schema schema = g.getSchema(schemaid);
			String output = "Information for Schema ID: "+schemaid;
			output+="\nType="+schema.getType();
			output+="\nNumber of features="+schema.numFeatures();
			output+="\nDefined features="+IteratorUtils.iterator2string(schema.getFeatureIDs(),",");
			output+="\n";
			
			Log.INFO(output);
		}
		
		// Get features for a decorable item
		Log.INFO(OutputUtils.separator("#", 40));
		
		// Select first node as an example.
		// You can do the same thing for all other Decorable items (i.e., Edges, Graph)
		Node n = g.getNodes().next();
		String schemaid = n.getSchemaID();
		Log.INFO("Node "+n+" is of schema "+schemaid+" with the following features: ");
		FeatureAccess.printFeatures(n);
		
		// How to change schema
		Log.INFO(OutputUtils.separator("#", 40));
		
		Log.INFO("We will now modify the schema "+schemaid);
		
		// Add derived feature
		DerivedFeature df = new IncidentCount();
		df.setCache(true);
		Schema schema = g.getSchema(schemaid);
		schema.addFeature("SimpleDegree", df);
		
		// Add explicit feature
		Feature ef = new ExplicitCateg(Arrays.asList(new String[]{"cat1", "cat2"}));
		schema.addFeature("SomeFeature", ef);
		
		// Update the graph with the modified schema
		g.updateSchema(schemaid, schema);
		
		// Now we can set the value of the new explicit feature
		n.setFeatureValue("SomeFeature", new CategValue("cat1"));
		
		Log.INFO("After update, Node "+n+" is of schema "+schemaid+" with the following features: ");
		FeatureAccess.printFeatures(n);
	}
	
	/**
	 * Example for accessing the features and feature values
	 * given a decorable item.
	 * 
	 * @param d Decorable Item
	 */
	private static void printFeatures(Decorable d) {
		Schema schema = d.getSchema();
		Iterator<String> featureids = schema.getFeatureIDs();
		while(featureids.hasNext()) {
			String featureid = featureids.next();
			Feature f = schema.getFeature(featureid);
			FeatureValue fvalue = d.getFeatureValue(featureid);
			
			String value = null;
			// If a feature value is unknown for a given feature,
			// a feature value of class UnknownValue is returned.
			if(fvalue.equals(FeatureValue.UNKNOWN_VALUE)) {
				value = "Unknown";
			}
			
			// Check the type of the feature
			// Feature types are defined under
			// linqs.gaia.feature and the corresponding values
			// are under linqs.gaia.feature.values
			String type = null;
			if(f instanceof CategFeature) {
				type = "Categorical";
				CategValue cv = (CategValue) fvalue;
				value = cv.getCategory();
				// Note: To get the probability distribution
				// double[] probs = cv.getProbs();
			} else if(f instanceof NumFeature) {
				type = "Numerical";
				NumValue nv = (NumValue) fvalue;
				value = ""+nv.getNumber();
			} else if(f instanceof StringFeature) {
				type = "String";
				StringValue sv = (StringValue) fvalue;
				value = sv.getString();
			} else if(f instanceof MultiCategFeature) {
				type = "Multi-Categorical";
				MultiCategValue mcv = (MultiCategValue) fvalue;
				UnmodifiableSet<String> cats = mcv.getCategories();
				value = IteratorUtils.iterator2string(cats.iterator(), ",");
			} else {
				type = f.getClass().getCanonicalName();
				value = fvalue.getStringValue();
			}
			
			Log.INFO(featureid+"["+type+"]="+value);
		}
	}
}
