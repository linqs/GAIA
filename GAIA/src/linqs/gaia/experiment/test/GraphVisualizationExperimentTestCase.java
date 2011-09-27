package linqs.gaia.experiment.test;

import junit.framework.TestCase;
import linqs.gaia.experiment.Experiment;
import linqs.gaia.experiment.GraphVisualizationExperiment;

public class GraphVisualizationExperimentTestCase extends TestCase {
	public GraphVisualizationExperimentTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testExperiment() {
		Experiment e = new GraphVisualizationExperiment();
		e.loadParametersFile("resource/SampleFiles/GraphVisualizationExperimentSample/experiment.cfg");
		e.runExperiment();
		
		assertNotNull(e);
	}
}
