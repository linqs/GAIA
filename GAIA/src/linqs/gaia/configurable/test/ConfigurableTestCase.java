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
package linqs.gaia.configurable.test;

import java.util.Map;

import junit.framework.TestCase;
import linqs.gaia.configurable.Configurable;
import linqs.gaia.log.Log;

/**
 * Test cases for configurable implementation
 * 
 * @author namatag
 *
 */
public class ConfigurableTestCase extends TestCase {
	public ConfigurableTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testValueParsing() {
		Configurable conf = new TestConfigurableObject();
		
		// Load configuration
		conf.loadParametersFile("resource/SampleFiles/ConfigurationSample/examplefile.cfg");
		
		// Print parameters
		Map<String,String> parameters = conf.getAllParameters();
		Log.INFO(parameters);
		
		// Verify comment lines
		assertFalse(conf.hasParameter("commentval1"));
		assertFalse(conf.hasParameter("commentval2"));
		
		// Verify multiple inline
		assertEquals(conf.getStringParameter("inline1"),"val1");
		assertEquals(conf.getStringParameter("inline2"),"val2");
		assertEquals(conf.getStringParameter("inline3"),"val3");
		
		// Verify multiline
		assertEquals(conf.getStringParameter("multiline"),"a,b,c,d,e,f,g");
		
		// Verify values
		assertTrue(conf.hasParameter("name"));
		assertFalse(conf.hasParameter("testnonval"));
		String name = conf.getStringParameter("name");
		
		// Verify String
		assertEquals(name, "gaia");
		assertEquals(conf.getStringParameter("name"), conf.getStringParameter("namecopy"));
		assertEquals(conf.getStringParameter("project"), "gaia-project");
		
		// Verify double
		assertTrue(conf.getDoubleParameter("age")==5);
		
		// Verify yes no
		assertTrue(conf.getYesNoParameter("checkyesno1"));
		assertFalse(conf.getYesNoParameter("checkyesno2"));
		assertFalse(conf.hasYesNoParameter("checkyesno1","no"));
		assertTrue(conf.hasYesNoParameter("checkyesno1","yes"));
		assertFalse(conf.hasYesNoParameter("checkyesno3","no"));
	}
	
	public void testCIDParsing() {
		Configurable conf = new TestConfigurableObject();
		Configurable conf1 = new TestConfigurableObject("cid1");
		Configurable conf2 = new TestConfigurableObject("cid2");
		
		// Load configuration
		conf.loadParametersFile("resource/SampleFiles/ConfigurationSample/examplefile.cfg");
		conf1.loadParametersFile("resource/SampleFiles/ConfigurationSample/examplefile.cfg");
		conf2.loadParametersFile("resource/SampleFiles/ConfigurationSample/examplefile.cfg");
		
		// Print parameters
		Map<String,String> parameters = conf.getAllParameters();
		Log.INFO("No CID: "+parameters);
		parameters = conf1.getAllParameters();
		Log.INFO("CID="+conf1.getCID()+": "+parameters);
		parameters = conf2.getAllParameters();
		Log.INFO("CID="+conf2.getCID()+": "+parameters);
		
		// Verify differ on CID specific values
		assertEquals(conf.getStringParameter("name"),"gaia");
		assertEquals(conf1.getStringParameter("name"),"cid1name");
		assertEquals(conf2.getStringParameter("name"),"cid2name");
		
		// Verify same on global values
		assertEquals(conf.getStringParameter("height"),"5'4");
		assertEquals(conf1.getStringParameter("height"),"5'4");
		assertEquals(conf2.getStringParameter("height"),"5'4");
	}
	
	public void testLoadFile() {
		Configurable conf = new TestConfigurableObject();
		
		// Load configuration
		conf.loadParametersFile("resource/SampleFiles/ConfigurationSample/exampleloader.cfg");
		
		// Print parameters
		Map<String,String> parameters = conf.getAllParameters();
		Log.INFO(parameters);
		
		// Verify before and after loading values to ensure they're still accessible
		assertEquals((int) conf.getDoubleParameter("before"), 100);
		assertEquals((int) conf.getDoubleParameter("after"), 200);
		
		// Verify name was overwritten
		assertEquals(conf.getStringParameter("name"),"replacename");
	}
}
