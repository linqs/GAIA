package linqs.gaia.graph.io;

import java.io.File;
import java.io.FileWriter;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.io.GraphMLReader;
import edu.uci.ics.jung.io.GraphMLWriter;
import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

/**
 * Implements limited support for loading and saving a GAIA graph
 * from the GraphML data format.  This implementation uses the JUNG
 * Graph ML IO utilities ({@link GraphMLWriter} and {@link GraphMLReader})
 * and is limited to saving only the structure of the network.
 * Note: This exporter requires the Jung Library to be in the classpath
 * (i.e., jung-api-2.0-beta1.jar).
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> filedirectory-Directory to store or load the graph
 * Note: Parameter not required if using {@link DirectoryBasedIO} methods.
 * </UL>
 * <p>
 * Optional Parameters"
 * <UL>
 * <LI> jcclass-Full java class for the {@link JungConverter},
 * instantiated using {@link Dynamic#forConfigurableName}, to use.
 * Defaults is {@link linqs.gaia.graph.converter.jung.JungConverter}
 * with default parameters.
 * </UL>
 * @author namatag
 *
 */
public class GraphMLIO extends BaseConfigurable implements IO, DirectoryBasedIO {
	private boolean initialize = true;
	private JungConverter jc = null;
	private final static String DEFAULTGRAPHSCHEMA = "graphmlgraph";
	public static String DEFAULTFILENAME = "graph.graphml";
	
	private void initialize() {
		initialize = false;
		
		String jcclass = this.getStringParameter("jcclass", JungConverter.class.getCanonicalName());
		jc = (JungConverter) Dynamic.forConfigurableName(JungConverter.class, jcclass, this);
	}

	public Graph loadGraphFromDir(String directory) {
		return this.loadGraphFromDir(directory,GraphID.generateGraphID(DEFAULTGRAPHSCHEMA).getObjID());
	}

	public Graph loadGraphFromDir(String directory, String objid) {
		try {
			GraphMLReader<edu.uci.ics.jung.graph.Graph<Object, Object>, Object,Object> gmlreader =
				new GraphMLReader<edu.uci.ics.jung.graph.Graph<Object, Object>, Object,Object>
					(null,
					 new JungFactory());
			
			// Create jung graph
			edu.uci.ics.jung.graph.Graph<Object, Object> jungg
				= new SparseMultigraph<Object, Object>();
			
			gmlreader.load(directory+File.separatorChar+DEFAULTFILENAME, jungg);
			
			return jc.importGraph(jungg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private class JungFactory implements org.apache.commons.collections15.Factory<Object> {
		int counter = 0;
		
		public String create() {
			counter++;
			return ""+counter;
		}
	}

	public void saveGraphToDir(String directory, Graph g) {
		if(initialize) {
			initialize();
		}
		
		edu.uci.ics.jung.graph.Graph<Object,Object> jungg = jc.exportGraph(g);
		GraphMLWriter<Object,Object> gmlwriter = new GraphMLWriter<Object,Object>();
		
		FileWriter fstream;
		try {
			// Create directory if it doesn't already exist
			FileIO.createDirectories(directory);
			fstream = new FileWriter(directory+File.separatorChar+DEFAULTFILENAME);
			gmlwriter.save(jungg, fstream);
			fstream.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Graph loadGraph() {
		String filedirectory = this.getStringParameter("filedirectory");
		return this.loadGraphFromDir(filedirectory,GraphID.generateGraphID(DEFAULTGRAPHSCHEMA).getObjID());
	}

	public Graph loadGraph(String objid) {
		String filedirectory = this.getStringParameter("filedirectory");
		return this.loadGraphFromDir(filedirectory,objid);
	}

	public void saveGraph(Graph g) {
		String filedirectory = this.getStringParameter("filedirectory");
		this.saveGraphToDir(filedirectory, g);
	}
}
