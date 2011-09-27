package linqs.gaia.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import linqs.gaia.log.Log;

/**
 * A JUnit test function similar to AllTest.
 * This test, however, will not run the most time expensive tests
 * in GAIA.  Use this test as a way to do quick verifications.
 * <p>
 * Note: This is not a full substitute to AllTest.
 * Changes must still be tested against all test cases for submission.
 * 
 * @author srhuang
 * @author namatag
 *
 */
public class QuickTests {

	public static Test suite() {
		Log.hideAllLogging();
		TestSuite suite = new TestSuite();
		
		suite.addTest(linqs.gaia.configurable.test.ConfigurableTestSuite.suite());
		suite.addTest(linqs.gaia.graph.io.test.IOTestSuite.suite());
		suite.addTest(linqs.gaia.graph.test.GraphTestSuite.suite());
		suite.addTest(linqs.gaia.identifiable.test.IdentifiableTestSuite.suite());
		suite.addTest(linqs.gaia.util.test.UtilTestSuite.suite());

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}