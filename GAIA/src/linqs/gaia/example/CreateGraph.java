package linqs.gaia.example;

import java.util.Iterator;

import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.explicit.ExplicitNum;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;

/**
 * Simple example for creating a GAIA Graph
 * 
 * @author namatag
 *
 */
public class CreateGraph {
	public static void main(String[] args) {
		// Note: A GRAPH schema (with no features)
		// will automatically be added to the graph
		// with schemaID social_network.
		Graph g = new DataGraph(new GraphID("social_network","umd"));
		
		// Add schemas
		Schema people = new Schema(SchemaType.NODE);
		people.addFeature("name", new ExplicitString());
		people.addFeature("age", new ExplicitNum());
		g.addSchema("person", people);
		
		Schema classes = new Schema(SchemaType.NODE);
		classes.addFeature("section", new ExplicitNum(new NumValue(1)));
		g.addSchema("class", classes);
		
		Schema enrolled = new Schema(SchemaType.DIRECTED);
		enrolled.addFeature("type", new ExplicitCateg(new String[]{"regular","audit"}));
		g.addSchema("enrolled", enrolled);
		
		// Add person nodes
		Node p_alice = g.addNode(new GraphItemID(g.getID(), "person", "alice"));
		p_alice.setFeatureValue("name", new StringValue("Alice Smith"));
		p_alice.setFeatureValue("age", new NumValue(18));
		
		Node p_bob = g.addNode(new GraphItemID("person", "bob"));
		p_bob.setFeatureValue("name", new StringValue("Robert Jones"));
		p_bob.setFeatureValue("age", new NumValue(32));
		
		// Add enrolled edges
		Node c_srl = g.addNode(new GraphItemID("class", "srl"));
		
		Edge e1 = g.addDirectedEdge(new GraphItemID("enrolled","1"), p_alice, c_srl);
		e1.setFeatureValue("type", new CategValue("regular"));
		
		Edge e2 = g.addDirectedEdge(new GraphItemID("enrolled","2"), p_bob, c_srl);
		e2.setFeatureValue("type", new CategValue("audit"));
		
		// Example: Who is enrolled in the srl class?
		Log.INFO("Who is enrolled in the srl class?");
		Iterator<GraphItem> itr = c_srl.getAdjacentGraphItems("enrolled");
		while(itr.hasNext()) {
			GraphItem gi = itr.next();
			String name = gi.getFeatureValue("name").getStringValue();
			double age = ((NumValue) gi.getFeatureValue("age")).getNumber();
			Log.INFO(name+" (age "+age+") is enrolled in "+c_srl);
		}
	}
}
