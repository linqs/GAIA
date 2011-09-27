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
