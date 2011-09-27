package linqs.gaia.model.lp.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class LPTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test each potential link generator
		suite.addTestSuite(AllPairwiseTestCase.class);

		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}