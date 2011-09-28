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
package linqs.gaia.graph.io.test;

import java.util.Iterator;

import junit.framework.TestCase;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.SparseTabDelimIO;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;

/**
 * Test cases for TabDelimIO implementation
 * 
 * @author namatag
 *
 */
public class SparseTabDelimIOTestCase extends TestCase {
	public SparseTabDelimIOTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testOnSample() {
		// Specify IO method
		IO io = new SparseTabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("filedirectory", 
				"resource/SampleFiles/SparseTabDelimIOSample/SimpleExample");
		io.setParameter("graphclass",DataGraph.class.getCanonicalName());

		// Load Graph
		Graph g = io.loadGraph();
		
		assertTrue(g != null);
		
		// Test MultiID
		Node n4 = g.getNode((GraphItemID) ID.parseID("SocialNetwork.sn1.Person.N4"));
		Node n5 = g.getNode((GraphItemID) ID.parseID("SocialNetwork.sn1.Person.N5"));
		FeatureValue n4family = n4.getFeatureValue("family");
		FeatureValue n5family = n5.getFeatureValue("family");
		
		// Test closed default value
		assertTrue(n4family.getStringValue().equals("SocialNetwork.sn1.Person.N1"));
		
		// Test regular value
		assertTrue(n5family.getStringValue().contains("SocialNetwork.sn1.Person.N3")
				&& n5family.getStringValue().contains("SocialNetwork.sn1.Person.N4"));
		
		g.destroy();
	}
	
	public void testOnWebKB() {
		// Specify IO method
		IO io = new SparseTabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("filedirectory", 
				"resource/SampleFiles/SparseTabDelimIOSample/WebKB/cornell");
		io.setParameter("graphclass",DataGraph.class.getCanonicalName());

		// Load Graph
		Graph g = io.loadGraph();
		
		// Verify that all the features are defined
		Iterator<GraphItem> gitems = g.getGraphItems("webpage");
		Schema schema = g.getSchema("webpage");
		while(gitems.hasNext()) {
			GraphItem gi = gitems.next();
			Iterator<String> fitr = schema.getFeatureIDs();
			
			while(fitr.hasNext()) {
				String fid = fitr.next();
				if(gi.getFeatureValue(fid).equals(FeatureValue.UNKNOWN_VALUE)) {
					throw new RuntimeException("All values should be defined: "+fid+" for "+gi);
				}
			}
		}
		
		// Verify feature count
		int numnodefeatures = g.getSchema("webpage").numFeatures();
		int numedgefeatures = g.getSchema("hyperlink").numFeatures();
		
		assertTrue(numnodefeatures == 1704 && numedgefeatures == 0);
		
		g.destroy();
	}
}
