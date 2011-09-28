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

import junit.framework.TestCase;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.graph.io.DottyIO;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.TabDelimIO;
import linqs.gaia.util.FileIO;

/**
 * Test cases for TabDelimIO implementation
 * 
 * @author namatag
 *
 */
public class DottyIOTestCase extends TestCase {
	public DottyIOTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testOnWebKB() {
		// Specify IO method
		IO io = new TabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("files", 
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.graph," +
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.nodes," +
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.edges");
		io.setParameter("graphclass",DataGraph.class.getCanonicalName());

		// Load Graph
		Graph g = io.loadGraph();
		
		// Save in dotty format
		IO dottyio = new DottyIO();
		
		dottyio.setParameter("filedirectory", FileIO.getTemporaryDirectory());
		dottyio.setParameter("graphprops", "overlap=scale;");
		dottyio.setParameter("schemaprops", "webpage=label=\"\",shape=rectangle,style=filled");
		dottyio.setParameter("valueprops", "webpage.label.student=color=red,shape=triangle;"
				+"webpage.label.faculty=color=blue");
		dottyio.setParameter("heightprops", "webpage.w2");
		dottyio.setParameter("widthprops", "webpage.w3");
		
		dottyio.saveGraph(g);
		
		g.destroy();
	}
}
