package linqs.gaia.identifiable.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IdentifiableTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each IO format
		suite.addTestSuite(IdentifiableTestCase.class);

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}