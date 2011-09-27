package linqs.gaia.configurable.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConfigurableTestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each IO format
		suite.addTestSuite(ConfigurableTestCase.class);

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
