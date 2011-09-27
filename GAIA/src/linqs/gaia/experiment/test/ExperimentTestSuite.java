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