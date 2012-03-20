/*
 * This file is part of the GAIA software.
 * Copyright 2012 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package linqs.gaia.graph.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.FileFormatException;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

/**
 * 
 * 
 * @author bert
 *
 */
public class PredicateTabDelimIO extends BaseConfigurable implements IO, DirectoryBasedIO {
	public final static String COMMENT_LINE="#";
	public final static String DELIMITER = "\t";
	private final static String DEFAULT_GRAPH_CLASS = DataGraph.class.getCanonicalName();
	public final static String DEFAULT_GRAPH_SCHEMAID="GRAPH";
	public final static String DEFAULT_NODE_SCHEMAID="NODE";
	public final static String DEFAULT_EDGE_SCHEMAID="EDGE";

	public Graph loadGraphFromDir(String directory) {
		return this.loadGraphFromDir(directory, null);
	}

	public Graph loadGraphFromDir(String directory, String objid) {
		String filenames[] = null;
		// Get the appropriate files
		if(directory!=null) {
			filenames = this.getFilesFromDir(directory);
		} else if(this.hasParameter("files")) {
			filenames = this.getStringParameter("files").split(",");
		} else if(this.hasParameter("filedirectory")) {
			filenames = this.getFilesFromDir(this.getStringParameter("filedirectory"));
		} else {
			throw new ConfigurationException("Neither files nor filedirectory was specified");
		}

		Graph g = this.createGraph(objid);

		// first set node attribute schema

		String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);

		Schema schema = new Schema(SchemaType.NODE);

		for (String f : filenames) {
			String [] fsplit = f.split(File.separator);

			String [] segments = (fsplit[fsplit.length-1]).split("\\.");

			if (segments.length > 0 && segments[0].equals("attribute"))
				schema.addFeature(segments[1], new ExplicitString());
		}

		g.addSchema(nodesid, schema);



		for (String f : filenames) {
			String [] fsplit = f.split(File.separator);

			String [] segments = (fsplit[fsplit.length-1]).split("\\.");

			if (segments.length > 0 && segments[0].equals("attribute"))
				this.loadAttributeFile(g, f, segments[1]);
			else if (segments.length > 0 && segments[0].equals("edge"))
				this.loadEdgeFile(g, f, segments[1]);
			else
				Log.INFO("Warning: unable to load predicates from file "+f);
		}

		return g;
	}

	private Graph createGraph(String objid) {
		String graphclass = DEFAULT_GRAPH_CLASS;
		if(this.hasParameter("graphclass")){
			graphclass = this.getStringParameter("graphclass");
		}

		// Create Graph
		String schemaID = DEFAULT_GRAPH_SCHEMAID;
		String objID = objid==null ? GraphID.generateGraphID(schemaID).getObjID() : objid;
		GraphID id = new GraphID(schemaID, objID);
		Class<?>[] argsClass = new Class[]{GraphID.class};
		Object[] argValues = new Object[]{id};

		Graph g = (Graph) Dynamic.forName(Graph.class,
				graphclass,
				argsClass,
				argValues);

		g.copyParameters(this);

		return g;
	}

	private void loadAttributeFile(Graph g, String filename, String attribute) {
		String currline = null;
		try {

			String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);
			BufferedReader br = new BufferedReader(new FileReader(filename));


			currline = this.getNonCommentLine(br);

			while (currline!=null){
				String nodeline[] = currline.split(DELIMITER);

				String nodeid = nodeline[0];
				GraphItemID gid = new GraphItemID((GraphID) g.getID(), nodesid, nodeid);
				Node node = g.getNode(gid);
				if (node == null)
					node = g.addNode(gid);

				node.setFeatureValue(attribute, nodeline[1]);

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private void loadEdgeFile(Graph g, String filename, String edgeType) {
		String currline = null;
		try {
			String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);

			BufferedReader br = new BufferedReader(new FileReader(filename));
			currline = this.getNonCommentLine(br);
			if(currline.trim().equals("UNDIRECTED")) {
				// Skip the first line
				currline = this.getNonCommentLine(br);
			}

			if(!g.hasSchema(edgeType)) {
				Schema schema = new Schema(SchemaType.UNDIRECTED);
				g.addSchema(edgeType, schema);
			} else if(!g.getSchemaType(edgeType).equals(SchemaType.UNDIRECTED)) {
				throw new FileFormatException("This file format only supports one type of edge. " +
						"Encountered conflicting files indicated the defined edge" +
						" to be both directed and undirected.");
			}

			while(currline!=null){
				String edgeline[] = currline.split(DELIMITER);
				int length = edgeline.length;
				if(length!=2) {
					throw new FileFormatException("Each line of the edge file should be of length 2." +
							"  Encountered line: "+currline);
				}

				GraphItemID gid = GraphItemID.generateGraphItemID(g, edgeType);
				Node n1 = g.getNode(new GraphItemID(nodesid, edgeline[0]));
				Node n2 = g.getNode(new GraphItemID(nodesid, edgeline[1]));
				if(n1.equals(n2)) {
					g.addUndirectedEdge(gid, n1);
				} else {
					g.addUndirectedEdge(gid, n1, n2);
				}

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load graph from specified node and edge files
	 * 
	 * @param nodefile Node file
	 * @param edgefile Edge file
	 * @return Graph
	 */
	public Graph loadNodeEdgeFile(String nodefile, String edgefile) {
		Graph g = this.createGraph(null);
		this.loadAttributeFile(g, nodefile, "name");
		this.loadEdgeFile(g, nodefile, "edge");

		Log.INFO("Calling loadNodeEdgeFile. This probably doesn't do what you want it to do.");

		return g;
	}

	/**
	 * Return next non comment line (non-empty and doesn't start with #).
	 * 
	 * @param br Buffered Reader to read from
	 * @return Next non-comment line
	 * @throws Exception
	 */
	private String getNonCommentLine(BufferedReader br) throws Exception {
		String line = br.readLine();

		while(line != null && (line.startsWith(COMMENT_LINE) || line.trim().length()==0)){
			line = br.readLine();
		}

		return line;
	}

	private String[] getFilesFromDir(String dirpath) {
		File dir = new File(dirpath);

		// Skip files that start with `.'.
		FileFilter filefilter = new FileFilter() {
			public boolean accept(File file) {
				return !file.isDirectory() && !file.isHidden();
			}
		};

		File[] children = dir.listFiles(filefilter);
		if (children == null) {
			throw new ConfigurationException("Either dir does not exist or is not a directory: "+dirpath);
		}

		List<String> filelist = new ArrayList<String>();
		for (int i=0; i<children.length; i++) {
			try {
				// Get filename of file or directory
				File file = children[i];				
				filelist.add(file.getAbsolutePath());
			} catch (Exception e) {
				Log.WARN("Skipping: "+children[i].getAbsolutePath());
			}    
		}

		return filelist.toArray(new String[filelist.size()]);
	}

	public void saveGraphToDir(String directory, Graph g) {
		// Create directory if it doesn't already exist
		FileIO.createDirectories(directory);

		String nodesid = null;
		if(this.hasParameter("nodesid")){
			nodesid = this.getStringParameter("nodesid");
		} else {
			Iterator<String> itr = g.getAllSchemaIDs(SchemaType.NODE);
			while(itr.hasNext()) {
				if(nodesid==null) {
					nodesid = itr.next();
				} else {
					throw new FileFormatException(PredicateTabDelimIO.class.getCanonicalName()
							+ " does not support multiple node types");
				}
			}
		}

		Set<String> edgeTypes = new HashSet<String>();

		Iterator<String> directed = g.getAllSchemaIDs(SchemaType.DIRECTED);
		while (directed.hasNext())
			edgeTypes.add(directed.next());

		Iterator<String> undirected = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
		while (undirected.hasNext())
			edgeTypes.add(undirected.next());

		for (String edgeType : edgeTypes) {

			String edgeFile = directory + File.separator + "edge." + edgeType + ".tsv";

			try {
				FileWriter fstream = new FileWriter(edgeFile);
				BufferedWriter out = new BufferedWriter(fstream);
				Iterator<Edge> eitr = g.getEdges(edgeType);
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					if(e.numNodes()>2) {
						throw new FileFormatException(SimpleCSVIO.class.getCanonicalName()
								+ " only supports binary edges");
					}

					Node n1 = null;
					Node n2 = null;
					if (e instanceof DirectedEdge) {
						DirectedEdge de = (DirectedEdge) e;
						n1 = de.getSourceNodes().next();
						n2 = de.getTargetNodes().next();
					} else {
						UndirectedEdge ue = (UndirectedEdge) e;
						Iterator<Node> nitr = ue.getAllNodes();
						n1 = nitr.next();
						n2 = nitr.hasNext() ? nitr.next() : n1;
					}

					out.write(n1.getID().getObjID()+DELIMITER+n2.getID().getObjID()+"\n");
				}

				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// Get node attributes

		// print attributes

		Iterator<String> attributes = g.getSchema(nodesid).getFeatureIDs();
		while (attributes.hasNext()) {
			String feature = attributes.next();

			String attrFile = directory + File.separator + "attribute." + feature + ".tsv";

			try {
				FileWriter fstream = new FileWriter(attrFile);
				BufferedWriter out = new BufferedWriter(fstream);
				Iterator<Node> nitr = g.getNodes();
				while(nitr.hasNext()) {
					Node n = nitr.next();

					out.write(n.getID().getObjID()+DELIMITER+n.getFeatureValue(feature)+"\n");
				}

				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// if nodes have no attributes, print flat node file

		if (g.getSchema(nodesid).numFeatures() == 0) {
			String nodeFile = directory + File.separator + "attribute.dummy.tsv";

			try {
				FileWriter fstream = new FileWriter(nodeFile);
				BufferedWriter out = new BufferedWriter(fstream);
				Iterator<Node> nitr = g.getNodes();
				while(nitr.hasNext()) {
					Node n = nitr.next();

					out.write(n.getID().getObjID()+DELIMITER+"1\n");
				}

				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	public Graph loadGraph() {
		String filedirectory = this.getStringParameter("filedirectory");

		return loadGraphFromDir(filedirectory);
	}

	public Graph loadGraph(String objid) {
		String filedirectory = this.getStringParameter("filedirectory");

		return loadGraphFromDir(filedirectory, objid);
	}

	public void saveGraph(Graph g) {
		String filedirectory = this.getStringParameter("filedirectory");
		saveGraphToDir(filedirectory, g);
	}

	public static void main(String[] args) {
		DirectoryBasedIO io = new SimpleCSVIO();
		Graph g = io.loadGraphFromDir("resource/SampleFiles/SimpleCSVIOSample");
		GraphUtils.printFullData(g, true);

		DirectoryBasedIO io2 = new PredicateTabDelimIO();

		io2.saveGraphToDir("/Users/bert/Desktop/testpsl", g);
		g.destroy();

		Log.INFO("\n\n\n\n\n\n\n\n**************************\n******Reloaded Graph\n*******\n\n\n\n\n");

		Graph g2 = io2.loadGraphFromDir("/Users/bert/Desktop/testpsl");
		GraphUtils.printFullData(g2, true);
	}
}
