package linqs.gaia.graph.converter.test;

import junit.framework.TestCase;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;

/**
 * Test cases for JUNG Converter implementation
 * 
 * @author namatag
 *
 */
public class JungConverterTestCase extends TestCase {
	private Graph g = null;
	
	public JungConverterTestCase() {
		
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
		g = null;
	}
	
	public void testCounts() {
		JungConverter exporter = new JungConverter();
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = exporter.exportGraph(g);
		
		assertEquals(2, jungg.getEdgeCount());
		assertEquals(8, jungg.getVertexCount());
	}
}
