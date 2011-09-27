package linqs.gaia.identifiable.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;

/**
 * Test cases for configurable implementation
 * 
 * @author namatag
 *
 */
public class IdentifiableTestCase extends TestCase {
	public IdentifiableTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	public void testUniqueness() {
		// Create sample graphs
		GraphID g1 = new GraphID("sn","friendster");
		GraphItemID gid1a = new GraphItemID(g1,"person","mark");
		GraphItemID gid1b = new GraphItemID(g1,"person","mark");
		GraphItemID gid1c = new GraphItemID(g1,"person","ivy");
		GraphItemID gid1d = new GraphItemID(g1,"plant","ivy");
		
		GraphID g2 = new GraphID("sn","facebook");
		GraphItemID gid2a = new GraphItemID(g2,"person","mark");
		GraphItemID gid2b = new GraphItemID(g2,"person","ivy");
		
		GraphID g3 = new GraphID("sn-full","friendster");
		
		// Test uniqueness
		Set<ID> unique = new HashSet<ID>();
		unique.add(g1);
		unique.add(gid1a);
		unique.add(gid1b);
		unique.add(gid1c);
		unique.add(gid1d);
		unique.add(g2);
		unique.add(gid2a);
		unique.add(gid2b);
		unique.add(g3);
		
		// Check that the unique set was appropriately done
		assertEquals(8, unique.size());
	}
}
