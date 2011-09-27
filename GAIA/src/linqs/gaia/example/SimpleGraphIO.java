package linqs.gaia.example;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.TabDelimIO;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;

/**
 * Example of how to load a graph using an IO.
 * The example uses {@link TabDelimIO} as the input format
 * and the {@link linqs.gaia.graph.datagraph.DataGraph} implementation
 * for the underlying graph.
 * 
 * @author namatag
 *
 */
public class SimpleGraphIO {
	private static IO io;

	public static Graph getSampleGraph() {
		// Specify IO method
		io = new TabDelimIO();
		
		// Set required parameters for loading
		io.setParameter("files", 
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/graph-SocialNetwork.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/node-Person.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/node-School.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/undirected-Family.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/directed-Friend.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/directed-Attends.tab," +
				"resource/SampleFiles/TabDelimIOSample/SimpleExample/undirected-Enemy.tab");
		io.setParameter("graphclass",DataGraph.class.getCanonicalName());

		// Load Graph
		return io.loadGraph();
	}

	public static void main(String[] args) throws Exception {
		// Set logging level to show all
		Log.showAllLogging();
		
		Graph g = getSampleGraph();
		Log.INFO("Loading Graph Successful!");
		Log.INFO("Loaded graph with "+g.numNodes()+" nodes and "+g.numEdges()+" edges");
		
		// Print graph information
		//Log.INFO(GraphUtils.printFullData(g, true));
		
		// Remove node
		GraphItemID gid = new GraphItemID((GraphID) g.getID(), "Person", "N1");
		g.removeNode(gid);
		Log.INFO(GraphUtils.printFullData(g, true));

		// Set required parameters for saving
		io.setParameter("filedirectory", "/tmp/SampleSave/");
		io.setParameter("fileprefix", "test");
		
		// Save Graph
		io.saveGraph(g);
		Log.INFO("Writing Graph Successful!");
	}
}
