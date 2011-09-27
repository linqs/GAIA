package linqs.gaia.graph.converter.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConverterTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each converter
		suite.addTestSuite(JungConverterTestCase.class);

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}