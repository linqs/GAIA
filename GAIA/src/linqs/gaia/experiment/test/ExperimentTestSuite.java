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
package linqs.gaia.experiment.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExperimentTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each experiment class
		suite.addTestSuite(OCExperimentTestCase.class);
		suite.addTestSuite(GraphAnalysisExperimentTestCase.class);
		suite.addTestSuite(GraphVisualizationExperimentTestCase.class);
		
		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}