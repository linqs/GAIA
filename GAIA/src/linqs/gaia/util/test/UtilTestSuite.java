package linqs.gaia.util.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class UtilTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each potential link generator
		suite.addTestSuite(UtilTestCase.class);

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}