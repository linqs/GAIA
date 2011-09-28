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
package linqs.gaia.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import linqs.gaia.log.Log;

/**
 * Primary JUnit test function.  This will run all other
 * test JUnit test in GAIA.
 * 
 * @author srhuang
 * @author namatag
 *
 */
public class AllTests {

	public static Test suite() {
		Log.hideAllLogging();
		TestSuite suite = new TestSuite();
		
		suite.addTest(linqs.gaia.configurable.test.ConfigurableTestSuite.suite());
		suite.addTest(linqs.gaia.graph.io.test.IOTestSuite.suite());
		suite.addTest(linqs.gaia.graph.test.GraphTestSuite.suite());
		suite.addTest(linqs.gaia.graph.test.DBGraphTestSuite.suite());
		suite.addTest(linqs.gaia.identifiable.test.IdentifiableTestSuite.suite());
		suite.addTest(linqs.gaia.experiment.test.ExperimentTestSuite.suite());
		suite.addTest(linqs.gaia.model.lp.test.LPTestSuite.suite());
		suite.addTest(linqs.gaia.util.test.UtilTestSuite.suite());

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
