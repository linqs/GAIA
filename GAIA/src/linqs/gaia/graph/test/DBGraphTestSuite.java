package linqs.gaia.graph.test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

public class DBGraphTestSuite {
	public static Test suite() {
		// Add DBGraph tests
		TestSuite suite = new TestSuite();
		suite.addTestSuite(DBGraphTestCase.class);
		
		TestSetup setup = new TestSetup(suite) {
			protected void setUp( ) throws Exception {
				
			}
			
			protected void tearDown( ) throws Exception {
				
			}
		};

		return setup;
	}
}