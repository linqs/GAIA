package linqs.gaia.identifiable.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;

public class IDTestCase extends TestCase {
	public IDTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	/**
	 * Test GraphID
	 */
	public void testGraphID() {
		GraphID gid11a = new GraphID("graphsid1","g1");
		GraphID gid11b = new GraphID("graphsid1","g1");
		GraphID gid21 = new GraphID("graphsid2","g1");
		GraphID gid22a = new GraphID("graphsid2","g2");
		GraphID gid22b = new GraphID("graphsid2","g2");
		GraphID gid22c = new GraphID("graphsid2","g2");
		GraphID gid23 = new GraphID("graphsid2","g3");
		
		Set<GraphID> idset = new HashSet<GraphID>();
		idset.add(gid11a);
		idset.add(gid11b);
		idset.add(gid21);
		idset.add(gid22a);
		idset.add(gid22b);
		idset.add(gid22c);
		idset.add(gid23);
		
		assertEquals(4, idset.size());
	}
	
	/**
	 * Test GraphID
	 */
	public void testGraphItemID() {
		GraphID gid11a = new GraphID("graphsid1","g1");
		GraphID gid11b = new GraphID("graphsid1","g1");
		GraphID gid22b = new GraphID("graphsid2","g2");
		
		GraphItemID gitid11a = new GraphItemID(gid11a,"s1","gi1");
		GraphItemID gitid11b = new GraphItemID(gid11a,"s1","gi1");
		GraphItemID gitid11c = new GraphItemID(gid11b,"s1","gi1");
		GraphItemID gitid21a = new GraphItemID(gid11a,"s2","gi1");
		GraphItemID gitid21b = new GraphItemID(gid11a,"s2","gi1");
		GraphItemID gitid23a = new GraphItemID(gid11a,"s2","gi3");
		GraphItemID gitid23b = new GraphItemID(gid22b,"s2","gi3");
		
		Set<GraphItemID> idset = new HashSet<GraphItemID>();
		idset.add(gitid11a);
		idset.add(gitid11b);
		idset.add(gitid11c);
		idset.add(gitid21a);
		idset.add(gitid21b);
		idset.add(gitid23a);
		idset.add(gitid23b);
		
		assertEquals(4, idset.size());
	}
}
