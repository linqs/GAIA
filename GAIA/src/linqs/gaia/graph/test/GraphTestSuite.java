package linqs.gaia.graph.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class GraphTestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each IO format
		suite.addTestSuite(DataGraphTestCase.class);

		return suite;
	}
}