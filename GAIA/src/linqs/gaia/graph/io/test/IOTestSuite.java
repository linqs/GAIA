package linqs.gaia.graph.io.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IOTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each IO format
		suite.addTestSuite(TabDelimIOTestCase.class);
		suite.addTestSuite(SparseTabDelimIOTestCase.class);
		suite.addTestSuite(DottyIOTestCase.class);
		
		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}