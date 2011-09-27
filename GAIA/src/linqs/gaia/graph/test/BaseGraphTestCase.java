package linqs.gaia.graph.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitMultiCateg;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiCategValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.TabDelimIO;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;

public abstract class BaseGraphTestCase extends TestCase {
	public BaseGraphTestCase() {
		
	}
	
	protected void setUp() {
		
	}

	protected void tearDown() {
		
	}
	
	protected Graph getSimpleGraph() {
		GraphID gid = new GraphID("gschemaA","g1");
		Graph g = this.getGraph(gid);
		
		// Add nodes
		g.addSchema("nschemaA", new Schema(SchemaType.NODE));
		Node n1 = g.addNode(new GraphItemID(gid, "nschemaA", "n1"));
		Node n2 = g.addNode(new GraphItemID(gid, "nschemaA", "n2"));
		Node n3 = g.addNode(new GraphItemID(gid, "nschemaA", "n3"));
		
		g.addSchema("nschemaB", new Schema(SchemaType.NODE));
		Node n4 = g.addNode(new GraphItemID(gid, "nschemaB", "n4"));
		g.addNode(new GraphItemID(gid, "nschemaB", "n5"));
		g.addNode(new GraphItemID(gid, "nschemaB", "n6"));
		g.addNode(new GraphItemID(gid, "nschemaB", "n7"));
		
		g.addSchema("nschemaC", new Schema(SchemaType.NODE));
		Node n8 = g.addNode(new GraphItemID(gid, "nschemaC", "n8"));
		g.addNode(new GraphItemID(gid, "nschemaC", "n9"));
		g.addNode(new GraphItemID(gid, "nschemaC", "n10"));
		
		// Add directed edges
		g.addSchema("dedgeschemaA", new Schema(SchemaType.DIRECTED));
		g.addDirectedEdge(new GraphItemID(gid,"dedgeschemaA","d1"),
				Arrays.asList(new Node[]{n1}).iterator(),
				Arrays.asList(new Node[]{n2}).iterator());
		g.addDirectedEdge(new GraphItemID(gid,"dedgeschemaA","d2"),
				Arrays.asList(new Node[]{n2}).iterator(),
				Arrays.asList(new Node[]{n1}).iterator());
		
		g.addSchema("dedgeschemaB", new Schema(SchemaType.DIRECTED));
		
		// Add undirected edges
		g.addSchema("uedgeschemaA", new Schema(SchemaType.UNDIRECTED));
		g.addUndirectedEdge(new GraphItemID(gid,"uedgeschemaA","u1"),
				Arrays.asList(new Node[]{n2,n3}).iterator());
		g.addUndirectedEdge(new GraphItemID(gid,"uedgeschemaA","u2"),
				Arrays.asList(new Node[]{n1,n4}).iterator());
		g.addUndirectedEdge(new GraphItemID(gid,"uedgeschemaA","u3"),
				Arrays.asList(new Node[]{n8}).iterator());
		
		return g;
	}
	
	/**
	 * Test that the graph can handle multiple graph instantiations.
	 */
	public void testMultiGraphs() {
		// Create one graph
		GraphID gid1 = new GraphID("gchemaA","g1");
		Graph g1 = this.getGraph(gid1);
		g1.addSchema("nschemaA", new Schema(SchemaType.NODE));
		Node n1 = g1.addNode(new GraphItemID(gid1, "nschemaA", "n1"));
		Node n2 = g1.addNode(new GraphItemID(gid1, "nschemaA", "n2"));
		Node n3 = g1.addNode(new GraphItemID(gid1, "nschemaA", "n3"));
		
		g1.addSchema("dedgeschemaA", new Schema(SchemaType.DIRECTED));
		g1.addDirectedEdge(new GraphItemID(gid1,"dedgeschemaA","d1"),
				Arrays.asList(new Node[]{n1}).iterator(),
				Arrays.asList(new Node[]{n2}).iterator());
	
		g1.addSchema("dedgeschemaB", new Schema(SchemaType.DIRECTED));
		
		g1.addSchema("uedgeschemaA", new Schema(SchemaType.UNDIRECTED));
		g1.addUndirectedEdge(new GraphItemID(gid1,"uedgeschemaA","u1"),
				Arrays.asList(new Node[]{n2,n3}).iterator());
		
		// Create a second graph
		GraphID gid2 = new GraphID("testgraphschema","g2");
		Graph g2 = this.getGraph(gid2);
		g2.addSchema("nschemaA", new Schema(SchemaType.NODE));
		g2.addNode(new GraphItemID(gid2, "nschemaA", "n1"));
		g2.addNode(new GraphItemID("nschemaA", "n4"));
		g2.addNode(new GraphItemID(gid2, "nschemaA", "n5"));
		
		assertEquals(3, g1.numNodes());
		assertEquals(3, g1.numGraphItems("nschemaA"));
		assertEquals(1, g1.numGraphItems("dedgeschemaA"));
		assertEquals(1, g1.numGraphItems("uedgeschemaA"));
		assertEquals(1, g1.numGraphItems("uedgeschemaA"));
		assertEquals(3, g2.numGraphItems("nschemaA"));
		assertEquals(2, g1.numEdges());
		assertEquals(3, g2.numNodes());
		assertEquals(0, g2.numEdges());
		assertTrue(!g1.equals(g2));
		
		// Verify GraphID uniqueness/checking
		assertTrue(g2.getNode(new GraphItemID(gid2, "nschemaA", "n1")).equals(
				g2.getNode(new GraphItemID("nschemaA", "n1"))));
		assertTrue(g2.getNode(new GraphItemID(gid2, "nschemaA", "n1")).getID().getGraphID().equals(
				g2.getNode(new GraphItemID("nschemaA", "n1")).getID().getGraphID()));
		assertNull(g1.getNode(new GraphItemID(gid2, "nschemaA", "n1")));
		assertNotNull(g1.getNode(new GraphItemID("nschemaA", "n1")));
		
		g1.destroy();
		g2.destroy();
	}
	
	public void testAllNodesRemoval() {
		Graph g = this.getSimpleGraph();
		g.removeAllNodes();
		
		assertTrue(g.numNodes()==0 && !g.getNodes().hasNext());
		g.destroy();
	}
	
	public void testAllEdgesRemoval() {
		Graph g = this.getSimpleGraph();
		g.removeAllEdges();
		
		assertTrue(g.numEdges()==0 && !g.getEdges().hasNext());
		g.destroy();
	}
	
	public void testGraphItemsRemoval() {
		Graph g = this.getSimpleGraph();
		g.removeAllGraphItems("nschemaA");
		
		assertTrue(g.numGraphItems("nschemaA")==0
				&& !g.getGraphItems("nschemaA").hasNext());
		g.destroy();
	}
	
	public void testGraphItemAccessability() {
		Graph g = this.getSimpleGraph();
		
		Set<GraphItemID> nids = new HashSet<GraphItemID>();
		Iterator<Node> nitr = g.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			nids.add(n.getID());
		}
		
		for(GraphItemID id:nids) {
			// Just verify you can re-acquire node given an id
			Node n = g.getNode(id);
			
			if(n == null || !n.getID().equals(id)) {
				throw new RuntimeException("Unable to reacquire Node: "+id+" got "+n);
			}
		}
		
		Set<GraphItemID> eids = new HashSet<GraphItemID>();
		Iterator<Edge> eitr = g.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			eids.add(e.getID());
		}
		
		for(GraphItemID id:eids) {
			// Just verify you can re-acquire edge given an id
			Edge e = g.getEdge(id);
			
			if(e == null || !e.getID().equals(id)) {
				throw new RuntimeException("Unable to reacquire Edge: "+id);
			}
		}
		
		// Test id uniqueness
		assertTrue(g.numEdges()==eids.size() && g.numNodes()==nids.size());
		g.destroy();
	}
	
	public void testCounts() {
		Graph g = this.getSimpleGraph();
		
		assertTrue(g.numEdges()==5
				&& g.numNodes()==10
				&& g.numGraphItems("nschemaA")==3
				&& g.numGraphItems("nschemaB")==4
				&& g.numGraphItems("nschemaC")==3
				&& g.numGraphItems("dedgeschemaA")==2
				&& g.numGraphItems("uedgeschemaA")==3);
		
		g.destroy();
	}
	
	public void testIncidentAndAdjacent() {
		Graph g = this.getSimpleGraph();
		GraphID gid = g.getID();
		
		Node n1 = g.getNode(new GraphItemID(gid, "nschemaA", "n1"));
		Node n2 = g.getNode(new GraphItemID(gid, "nschemaA", "n2"));
		Node n3 = g.getNode(new GraphItemID(gid, "nschemaA", "n3"));
		
		Edge ed1 = g.getEdge(new GraphItemID(gid, "dedgeschemaA", "d1"));
		Edge ed2 = g.getEdge(new GraphItemID(gid, "dedgeschemaA", "d2"));
		
		Edge eu1 = g.getEdge(new GraphItemID(gid, "uedgeschemaA", "u1"));
		Edge eu2 = g.getEdge(new GraphItemID(gid, "uedgeschemaA", "u2"));
		Edge eu3 = g.getEdge(new GraphItemID(gid, "uedgeschemaA", "u3"));
		
		// Test Incident
		assertEquals(3,n1.numIncidentGraphItems());
		assertEquals(2,n1.numIncidentGraphItems("dedgeschemaA"));
		assertEquals(3,n2.numIncidentGraphItems());
		assertEquals(0,n3.numIncidentGraphItems("dedgeschemaA"));
		assertEquals(2,ed1.numIncidentGraphItems());
		assertEquals(2,ed2.numIncidentGraphItems());
		assertEquals(2,eu1.numIncidentGraphItems());
		assertEquals(2,eu2.numIncidentGraphItems());
		assertEquals(1,eu3.numIncidentGraphItems());
		assertEquals(1,eu2.numIncidentGraphItems("nschemaA"));
		
		// Test Adjacent
		assertEquals(2,n1.numAdjacentGraphItems());
		assertEquals(1,n1.numAdjacentGraphItems("dedgeschemaA"));
		assertEquals(1,n1.numAdjacentSources());
		assertEquals(1,n1.numAdjacentSources("dedgeschemaA"));
		assertEquals(1,n1.numAdjacentTargets("dedgeschemaA"));
		assertEquals(true,n1.isAdjacent(n2));
		assertEquals(false,n1.isAdjacent(n3));
		assertEquals(true,n1.isAdjacentSource(n2));
		assertEquals(true,n1.isAdjacentTarget(n2));
		assertEquals(true,n1.isAdjacentTarget(n2, "dedgeschemaA"));
		assertEquals(false,n1.isAdjacentTarget(n2, "dedgeschemaB"));
		assertEquals(2,n2.numAdjacentGraphItems());
		assertEquals(3,ed1.numAdjacentGraphItems());
		assertEquals(3,ed1.numAdjacentGraphItems("nschemaA"));
		assertEquals(0,ed1.numAdjacentGraphItems("nschemaB"));
		assertEquals(0,eu1.numAdjacentGraphItems("nschemaB"));
		assertEquals(2,eu1.numAdjacentGraphItems("nschemaA"));

		g.destroy();
	}
	
	public void testAttributes() {
		Graph g = this.getSimpleGraph();
		
		// Add features to schemas, one for each feature type
		Schema schema = g.getSchema("nschemaA");
		schema.addFeature("string", new ExplicitString());
		schema.addFeature("num", new ExplicitNum());
		schema.addFeature("cat", new ExplicitCateg(new String[]{"cat1","cat2"}));
		schema.addFeature("mcat", new ExplicitMultiCateg(new String[]{"mcat1","mcat2","mcat3"}));
		schema.addFeature("mid", new ExplicitMultiID());
		g.updateSchema("nschemaA", schema);
		
		// Test explicit features
		GraphItemID giid = new GraphItemID(g.getID(), "nschemaA", "n1");
		Node n1 = g.getNode(giid);
		n1.setFeatureValue("string", new StringValue("sval1"));
		n1.setFeatureValue("num", new NumValue(1));
		n1.setFeatureValue("cat", new CategValue("cat1", new double[]{1,0}));
		Set<String> mcats1 = new HashSet<String>();
		mcats1.add("mcat1");
		n1.setFeatureValue("mcat", new MultiCategValue(mcats1, new double[]{.5,1,0}));
		n1.setFeatureValue("mid", new MultiIDValue(g.getID()));
		
		giid = new GraphItemID(g.getID(), "nschemaA", "n2");
		Node n2 = g.getNode(giid);
		n2.setFeatureValue("string", new StringValue("sval2"));
		n2.setFeatureValue("num", new NumValue(2));
		n2.setFeatureValue("cat", new CategValue("cat2"));
		Set<String> mcats2 = new HashSet<String>();
		mcats2.add("mcat2");
		mcats2.add("mcat1");
		n2.setFeatureValue("mcat", new MultiCategValue(mcats2));
		n2.setFeatureValue("mid", new MultiIDValue(n1.getID()));
		
		// Test getting value back
		assertTrue(n1.getFeatureValue("string").equals(new StringValue("sval1")));
		assertTrue(n1.getFeatureValue("num").equals(new NumValue(1)));
		assertTrue(n1.getFeatureValue("cat").equals(new CategValue("cat1", new double[]{1,0})));
		assertTrue(n1.getFeatureValue("mcat").equals(new MultiCategValue(mcats1, new double[]{.5,1,0})));
		assertTrue(n1.getFeatureValue("mid").equals(new MultiIDValue(g.getID())));
		
		// Verify automatically setting the probability of Categ features
		assertTrue(n2.getFeatureValue("cat").equals(new CategValue("cat2", new double[]{0,1})));
		assertTrue(n2.getFeatureValue("mcat").equals(new MultiCategValue(mcats2, new double[]{1,1,1})));
		
		// Test setFeatureValues and getFeatureValues
		List<String> batchfids = Arrays.asList(new String[]{"string","cat","mcat"});
		List<FeatureValue> batchvals = Arrays.asList(new FeatureValue[]{
				new StringValue("sval2"),
				new CategValue("cat1", new double[]{1,0}),
				FeatureValue.UNKNOWN_VALUE});
		n2.setFeatureValues(batchfids, batchvals);
		
		assertEquals(batchvals, n2.getFeatureValues(batchfids));
		
		g.destroy();
	}
	
	public void testSystemData() {
		Graph g = this.getSimpleGraph();
		g.setSystemData("key1", "val1");
		g.setSystemData("key2", "val2");
		g.setSystemData("key3", "val3");
		g.setSystemData("key4", "val4");
		
		assertEquals("val1", g.getSystemData("key1"));
		assertEquals("val2", g.getSystemData("key2"));
		assertEquals("val3", g.getSystemData("key3"));
		assertEquals("val4", g.getSystemData("key4"));
		
		g.removeSystemData("key1");
		assertNull(g.getSystemData("key1"));
		
		GraphItemID giid = new GraphItemID(g.getID(), "nschemaA", "n1");
		Node n1 = g.getNode(giid);
		giid = new GraphItemID(g.getID(), "nschemaA", "n2");
		Node n2 = g.getNode(giid);
		
		g.setSystemData(n1.getID(), "key1", "n1key1val1");
		g.setSystemData(n1.getID(), "key2", "n1key2val1");
		g.setSystemData(n2.getID(), "key1", "n2key1val1");
		assertEquals("n1key1val1", g.getSystemData(n1.getID(), "key1"));
		assertEquals("n2key1val1", g.getSystemData(n2.getID(), "key1"));
		
		g.removeSystemData(n1.getID(), "key1");
		assertNull(g.getSystemData(n1.getID(), "key1"));
		assertEquals("n1key2val1", g.getSystemData(n1.getID(), "key2"));
		assertEquals("n2key1val1", g.getSystemData(n2.getID(), "key1"));
		
		g.removeAllSystemData();
		
		g.destroy();
	}
	
	public void testLoadWebKB() {
		Graph g = this.getSimpleGraph();
		
		// Specify IO method
		IO io = new TabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("files", 
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.graph," +
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.nodes," +
				"resource/SampleFiles/TabDelimIOSample/WebKB/texas/webkb-texas.edges");
		io.setParameter("graphclass",g.getClass().getCanonicalName());

		// Load Graph
		Graph webkbgraph = io.loadGraph();
		
		webkbgraph.destroy();
		g.destroy();
	}
	
	public void testLoadSimpleExample() {
		Graph g = this.getSimpleGraph();
		
		// Specify IO method
		IO io = new TabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("filedirectory", 
				"resource/SampleFiles/TabDelimIOSample/SimpleExample");
		io.setParameter("graphclass",g.getClass().getCanonicalName());

		// Load Graph
		Graph simplegraph = io.loadGraph();
		
		simplegraph.destroy();
		g.destroy();
	}
	
	protected abstract Graph getGraph(GraphID gid);
}
