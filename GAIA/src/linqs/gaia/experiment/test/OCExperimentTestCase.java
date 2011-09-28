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

import junit.framework.TestCase;
import linqs.gaia.experiment.Experiment;
import linqs.gaia.experiment.OCExperiment;

public class OCExperimentTestCase extends TestCase {
	public OCExperimentTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testExperiment() {
		Experiment e = new OCExperiment();
		e.loadParametersFile("resource/SampleFiles/OCExperimentSample/experiment.cfg");
		e.runExperiment();
		
		assertNotNull(e);
	}
}
