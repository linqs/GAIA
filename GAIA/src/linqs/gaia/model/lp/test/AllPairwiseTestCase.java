package linqs.gaia.model.lp.test;

import java.util.Iterator;

import junit.framework.TestCase;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.model.util.plg.AllPairwise;

/**
 * Test cases for TabDelimIO implementation
 * 
 * @author namatag
 *
 */
public class AllPairwiseTestCase extends TestCase {
	private Graph g = null;
	
	public AllPairwiseTestCase() {
		
	}
	
	protected void setUp() {
		g = new DataGraph(new GraphID("g", "g1"));
		g.addSchema("people", new Schema(SchemaType.NODE));
		g.addSchema("email", new Schema(SchemaType.NODE));
		g.addSchema("sent_to", new Schema(SchemaType.DIRECTED));
		g.addSchema("friends_with", new Schema(SchemaType.UNDIRECTED));
		
		Node p1 = g.addNode(new GraphItemID(g.getID(),"people","p1"));
		Node p2 = g.addNode(new GraphItemID(g.getID(),"people","p2"));
		g.addNode(new GraphItemID(g.getID(),"people","p3"));
		g.addNode(new GraphItemID(g.getID(),"people","p4"));
		g.addNode(new GraphItemID(g.getID(),"people","p5"));
		
		Node e1 = g.addNode(new GraphItemID(g.getID(),"email","e1"));
		Node e2 = g.addNode(new GraphItemID(g.getID(),"email","e2"));
		g.addNode(new GraphItemID(g.getID(),"email","e3"));
		
		g.addUndirectedEdge(new GraphItemID(g.getID(),"friends_with","f1"), p1, p2);
		g.addDirectedEdge(new GraphItemID(g.getID(),"sent_to","s1"), e1, e2);
	}

	protected void tearDown() {
		g.destroy();
	}
	
	public void testDirected() {
		// Test directed
		AllPairwise plgd = new AllPairwise();
		plgd.setParameter("sourceschemaid", "email");
		plgd.setParameter("targetschemaid", "people");
		
		Iterator<Edge> eitr = null;
		eitr = plgd.getLinksIteratively(g, "sent_to");
		int counter = 0;
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			Log.DEBUG("Edge: "+e+" "+GraphItemUtils.getEdgeNodesString(e));
			counter++;
		}
		
		assertEquals(counter, 15);
	}
	
	public void testUndirected() {
		// Test undirected
		AllPairwise plgu = new AllPairwise();
		plgu.setParameter("nodeschemaid", "people");
		
		Iterator<Edge> eitr = plgu.getLinksIteratively(g, "friends_with");
		int counter = 0;
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			Log.DEBUG("Edge: "+e+" "+GraphItemUtils.getEdgeNodesString(e));
			counter++;
		}
		
		assertEquals(counter, 9);
	}
	
	public void testNCDirected() {
		// Test directed
		AllPairwise plgd = new AllPairwise();
		plgd.setParameter("sourceschemaid", "email");
		plgd.setParameter("targetschemaid", "email");
		
		Node e3 = g.getNode(new GraphItemID(g.getID(),"email","e3"));
		Iterator<Edge> eitr = plgd.getLinksIteratively(g, e3, "sent_to");
		int counter = 0;
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			Log.DEBUG("Edge: "+e+" "+GraphItemUtils.getEdgeNodesString(e));
			counter++;
		}
		
		assertEquals(counter, 4);
	}
	
	public void testNCUndirected() {
		// Test undirected
		AllPairwise plgu = new AllPairwise();
		plgu.setParameter("nodeschemaid", "people");
		
		Node p3 = g.getNode(new GraphItemID(g.getID(),"people","p3"));
		Iterator<Edge> eitr = plgu.getLinksIteratively(g, p3, "friends_with");
		int counter = 0;
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			Log.DEBUG("Edge: "+e+" "+GraphItemUtils.getEdgeNodesString(e));
			counter++;
		}
		
		assertEquals(counter, 4);
	}
}
