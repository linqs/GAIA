/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
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
import java.util.Iterator;
import java.util.List;

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
 * Implementation of a very simple file format stored as comma separated values (CSV).
 * This format assumes that there is only one node and one edge type,
 * all edges are binary, and all attributes are string valued.
 * The format is defined by files containing comma separated values
 * corresponding to either a node or edge (directed or undirected) file.
 * All empty lines or lines which begin with the character "#" is ignored.
 * A file is processed as a node file if its first line has the value "NODE"
 * or if its filename ends with ".node".  Next, a file is processed as a directed edge file
 * if its first line has the value "DIRECTED" or if its filename ends with ".directed".
 * Finally, a file is processed as an undirected edge file
 * if its first line has the value "UNDIRECTED" or if its filename ends with ".undirected".
 * <p>
 * A node file can contain an optional first line with the text "NODE"
 * (Note: This line maybe ommitted if the node file is identified by the suffix of its filename).
 * The next line contains a comma delimited list of feature ids which
 * defines the string valued features defined for each node.
 * The remaining lines consists of a comma delimited list of values where
 * the first value is used as the object id for each node and the
 * remaining values are the values of the feature (given in the same order they were
 * defined in the feature definition line).  Unknown values are specified as "?"
 * and an exception is thrown if a value is not specified for each feature
 * (i.e., each node line should have k+1 comma delimited values where k is the number of features define
 * in the feature definition line).
 * </p>
 * <p>
 * The edge files can contain an optional first line with the text "DIRECTED" or "UNDIRECTED".
 * (Note: This line maybe ommitted if the node file is identified by the suffix of its filename).
 * The remaining lines consists of two comma delimited values which indicate the
 * object ids (specified in the node files) for the nodes incident this edge.
 * For directed edges, the first value is the object id of the source node and the second
 * value is the object id of the target node.
 * </p>
 * <p>
 * Note: Examples of this format can be found under resource/SampleFiles/SimpleCSVIOSample.
 * </p>
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> filedirectory-Directory to store or load the graph
 * Note: Parameter not required if using {@link DirectoryBasedIO} methods.
 * <LI> files-Comma delimited list of the files to use.
 * This parameter is used over filedirectory if both are specified.
 * Not required if filedirectory is specified or if using {@link DirectoryBasedIO} methods.
 * </UL>
 * </p>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> graphclass-Full java class for the graph,
 * instantiated using {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * <LI> nodesid-Schema ID of node.  If specified during save operation, only save
 * the nodes with this schema ID.  If specified during load operation, load
 * the nodes with this schema ID.
 * <LI> edgesid-Schema ID of edge.  If specified during save operation, only save
 * the edges with this schema ID.  If specified during load operation, load
 * the edges with this schema ID.
 * </UL>
 * 
 * 
 * @author namatag
 *
 */
public class SimpleCSVIO extends BaseConfigurable implements IO, DirectoryBasedIO {
	public final static String COMMENT_LINE="#";
	public final static String DELIMITER = ",";
	private final static String DEFAULT_GRAPH_CLASS = DataGraph.class.getCanonicalName();
	public final static String DEFAULT_GRAPH_SCHEMAID="CSVGRAPH";
	public final static String DEFAULT_NODE_SCHEMAID="CSVNODE";
	public final static String DEFAULT_EDGE_SCHEMAID="CSVEDGE";
	
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
			throw new ConfigurationException("Neither files, nor filedirectory was specified");
		}
		
		Graph g = this.createGraph(objid);
		List<String> nonnodefiles = new ArrayList<String>();
		for(String f:filenames) {
			String firstline;
			try {
				// Check to see what type of file it is
				BufferedReader in = new BufferedReader(new FileReader(f));
				firstline = getNonCommentLine(in);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if(f.endsWith(".node") || firstline.trim().equals("NODE")) {
				this.loadNodeFile(g, f);
			} else {
				nonnodefiles.add(f);
			}
		}
		
		for(String f:nonnodefiles) {
			String firstline;
			try {
				// Check to see what type of file it is
				BufferedReader in = new BufferedReader(new FileReader(f));
				firstline = getNonCommentLine(in);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if(f.endsWith(".directed") || firstline.trim().equals("DIRECTED")) {
				this.loadDirectedEdgeFile(g, f);
			} else if(f.endsWith(".undirected") || firstline.trim().equals("UNDIRECTED")) {
				this.loadUndirectedEdgeFile(g, f);
			} else {
				throw new FileFormatException("Encountered file of unknown format: "+f);
			}
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
	
	private void loadNodeFile(Graph g, String filename) {
		String currline = null;
		try {
			String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			currline = this.getNonCommentLine(br);
			if(currline.trim().equals("NODE")) {
				// Skip the first line
				currline = this.getNonCommentLine(br);
			}
			
			// Process features line
			String[] fids = currline.split(DELIMITER);
			if(!g.hasSchema(nodesid)) {
				Schema schema = new Schema(SchemaType.NODE);
				for(String f:fids) {
					schema.addFeature(f, new ExplicitString());
				}
				
				g.addSchema(nodesid, schema);
			}
			currline = this.getNonCommentLine(br);
			
			while(currline!=null){
				String nodeline[] = currline.split(DELIMITER);
				int length = nodeline.length;
				if(length!=(fids.length+1)) {
					throw new FileFormatException("Insufficient length for node entry:" +
							" Expected "+(fids.length+1)+" but encountered "+nodeline.length
							+" in "+currline);
				}
				
				String nodeid = nodeline[0];
				GraphItemID gid = new GraphItemID((GraphID) g.getID(), nodesid, nodeid);
				Node node = g.addNode(gid);
				
				for(int i=1; i<length; i++) {
					String value = nodeline[i];
					if(value.equals("?")) {
						// Leave as unknown
					} else {
						node.setFeatureValue(fids[i-1], nodeline[i]);
					}
				}

				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void loadDirectedEdgeFile(Graph g, String filename) {
		String currline = null;
		try {
			String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);
			String edgesid = this.getStringParameter("edgesid", DEFAULT_EDGE_SCHEMAID);
			
			BufferedReader br = new BufferedReader(new FileReader(filename));
			currline = this.getNonCommentLine(br);
			if(currline.trim().equals("DIRECTED")) {
				// Skip the first line
				currline = this.getNonCommentLine(br);
			}
			
			if(!g.hasSchema(edgesid)) {
				Schema schema = new Schema(SchemaType.DIRECTED);
				g.addSchema(edgesid, schema);
			} else if(!g.getSchemaType(edgesid).equals(SchemaType.DIRECTED)) {
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
				
				GraphItemID gid = GraphItemID.generateGraphItemID(g, edgesid);
				Node n1 = g.getNode(new GraphItemID(nodesid, edgeline[0]));
				Node n2 = g.getNode(new GraphItemID(nodesid, edgeline[1]));
				
				g.addDirectedEdge(gid, n1, n2);
				
				currline = this.getNonCommentLine(br);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void loadUndirectedEdgeFile(Graph g, String filename) {
		String currline = null;
		try {
			String nodesid = this.getStringParameter("nodesid", DEFAULT_NODE_SCHEMAID);
			String edgesid = this.getStringParameter("edgesid", DEFAULT_EDGE_SCHEMAID);
			
			BufferedReader br = new BufferedReader(new FileReader(filename));
			currline = this.getNonCommentLine(br);
			if(currline.trim().equals("UNDIRECTED")) {
				// Skip the first line
				currline = this.getNonCommentLine(br);
			}
			
			if(!g.hasSchema(edgesid)) {
				Schema schema = new Schema(SchemaType.UNDIRECTED);
				g.addSchema(edgesid, schema);
			} else if(!g.getSchemaType(edgesid).equals(SchemaType.UNDIRECTED)) {
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
				
				GraphItemID gid = GraphItemID.generateGraphItemID(g, edgesid);
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
		this.loadNodeFile(g, nodefile);
		this.loadDirectedEdgeFile(g, nodefile);
		
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
					throw new FileFormatException(SimpleCSVIO.class.getCanonicalName()
							+ " does not support multiple node types");
				}
			}
		}
		
		String edgesid = null;
		if(this.hasParameter("edgesid")){
			edgesid = this.getStringParameter("edgesid");
		} else {
			Iterator<String> itr = g.getAllSchemaIDs(SchemaType.DIRECTED);
			while(itr.hasNext()) {
				if(edgesid==null) {
					edgesid = itr.next();
				} else {
					throw new FileFormatException(SimpleCSVIO.class.getCanonicalName()
							+ " does not support multiple node types");
				}
			}
			
			itr = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
			while(itr.hasNext()) {
				if(edgesid==null) {
					edgesid = itr.next();
				} else {
					throw new FileFormatException(SimpleCSVIO.class.getCanonicalName()
							+ " does not support multiple node types");
				}
			}
		}
		
		// Save nodes
		String nodefile = directory+"/"+nodesid+".node";
		try {
			FileWriter fstream = new FileWriter(nodefile);
			BufferedWriter out = new BufferedWriter(fstream);
			Schema schema = g.getSchema(nodesid);
			
			// Save feature ids
			Iterator<String> fitr = schema.getFeatureIDs();
			List<String> fids = new ArrayList<String>();
			boolean first = true;
			while(fitr.hasNext()) {
				if(first) {
					first = false;
				} else {
					out.write(DELIMITER);
				}
				
				String fid = fitr.next();
				fids.add(fid);
				out.write(fid);
			}
			out.write("\n");
			
			// Save each node
			Iterator<Node> nitr = g.getNodes(nodesid);
			while(nitr.hasNext()) {
				Node n = nitr.next();
				out.write(n.getID().getObjID());
				
				for(String f:fids) {
					out.write(DELIMITER);
					if(n.hasFeatureValue(f)) {
						String value = n.getFeatureValue(f).getStringValue();
						out.write(value);
					} else {
						out.write("?");
					}
				}
				out.write("\n");
			}
			
			out.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// Save edges
		boolean isdirected = g.getSchemaType(edgesid).equals(SchemaType.DIRECTED);
		String edgefile = directory+"/"+edgesid+
			(isdirected ? ".directed" : ".undirected");
		try {
			FileWriter fstream = new FileWriter(edgefile);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<Edge> eitr = g.getEdges(edgesid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				if(e.numNodes()>2) {
					throw new FileFormatException(SimpleCSVIO.class.getCanonicalName()
							+ " only supports binary edges");
				}
				
				Node n1 = null;
				Node n2 = null;
				if(isdirected) {
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
		Log.INFO(GraphUtils.getSimpleGraphOverview(g));
		io.saveGraphToDir("/Users/namatag/Desktop/testcsv", g);
		g.destroy();
		Graph g2 = io.loadGraphFromDir("/Users/namatag/Desktop/testcsv");
		Log.INFO(GraphUtils.getSimpleGraphOverview(g2));
	}
}
