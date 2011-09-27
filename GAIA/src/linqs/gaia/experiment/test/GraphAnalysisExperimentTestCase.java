package linqs.gaia.experiment.test;

import junit.framework.TestCase;
import linqs.gaia.experiment.Experiment;
import linqs.gaia.experiment.GraphAnalysisExperiment;

public class GraphAnalysisExperimentTestCase extends TestCase {
	public GraphAnalysisExperimentTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testExperiment() {
		Experiment e = new GraphAnalysisExperiment();
		e.loadParametersFile("resource/SampleFiles/GraphAnalysisExperimentSample/experiment.cfg");
		e.runExperiment();
		
		assertNotNull(e);
	}
}
