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
