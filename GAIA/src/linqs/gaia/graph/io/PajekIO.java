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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.FileIterable;

/**
 * Implements basic support for loading and saving a GAIA graph
 * from the Pajek data format.  This format can load the standard
 * Pajek format where "Vertices", "Edges", and "Arcs" are defined.
 * When loading, information like color or position are ignored.
 * When saving, this saves the nodes and edges in a single file.
 * No attributes are saved but the "label" of the nodes,
 * which is the second column for nodes,
 * contains the string representation of the id.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> filedirectory-Directory to store or load the graph
 * Note: Parameter not required if using {@link DirectoryBasedIO} methods.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> parseid-If yes, when loading, parse the "label" (the second column in the graph),
 * as the string representation of the IDs of the nodes and edges.
 * Default is no.
 * <LI> graphclass-Full java class for the graph,
 * instantiated using {@link Dynamic#forConfigurableName}, used when loading.
 * Defaults is {@link linqs.gaia.graph.datagraph.DataGraph}.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
	 
public class PajekIO extends BaseConfigurable implements IO, DirectoryBasedIO {
	private final static String DEFAULT_GRAPH_CLASS = DataGraph.class.getCanonicalName();
	private final static String DEFAULTGRAPHSCHEMA = "pajekgraph";
	private final static String DEFAULTGRAPHOBJID = "1";
	private final static String DEFAULTNODESCHEMA = "pajeknode";
	private final static String DEFAULTDIRECTEDSCHEMA = "pajekdirected";
	private final static String DEFAULTUNDIRECTEDSCHEMA = "pajekundirected";
	private final static String DEFAULTFILENAME = "pajekgraph.net";

	public Graph loadGraph() {
		String filedirectory = this.getStringParameter("filedirectory");
		
		return internalLoadGraph(filedirectory, null);
	}
	
	public Graph loadGraph(String objid) {
		if(objid==null) {
			throw new InvalidStateException("Graph object ID cannot be null");
		}
		
		String filedirectory = this.getStringParameter("filedirectory");
		
		return internalLoadGraph(filedirectory, objid);
	}
	
	public Graph loadGraphFromDir(String directory) {
		return  internalLoadGraph(directory, null);
	}
	
	public Graph loadGraphFromDir(String directory, String objid) {
		if(objid==null) {
			throw new InvalidStateException("Graph object ID cannot be null");
		}
		
		return internalLoadGraph(directory, objid);
	}
	
	private Graph internalLoadGraph(String filedirectory, String objid) {
		String filename = filedirectory+File.separator+DEFAULTFILENAME;
		boolean parseid = this.hasYesNoParameter("parseid","yes");
		
		// Create Graph
		String schemaID = DEFAULTGRAPHSCHEMA;
		String objID = objid==null ? DEFAULTGRAPHOBJID : objid;
		GraphID id = new GraphID(schemaID, objID);
		Class<?>[] argsClass = new Class[]{GraphID.class};
		Object[] argValues = new Object[]{id};

		String graphclass = PajekIO.DEFAULT_GRAPH_CLASS;
		if(this.hasParameter("graphclass")){
			graphclass = this.getStringParameter("graphclass");
		}
		Graph g = (Graph) Dynamic.forName(Graph.class,
				graphclass,
				argsClass,
				argValues);
		
		g.copyParameters(this);
		
		// Process line
		Map<Integer,Node> int2node = new HashMap<Integer,Node>();
		int ecounter = 0;
		String type = null;
		FileIterable fitrbl = new FileIterable(filename);
		for(String line:fitrbl) {
			line = line.trim();
			if(line.length()==0 || line.startsWith("/*")) {
				continue;
			} else if(line.startsWith("*Vertices")) {
				type = "V";
				continue;
			} else if(line.startsWith("*Edges")) {
				type = "E";
				continue;
			} else if(line.startsWith("*Arcs")) {
				type = "A";
				continue;
			} else if(line.startsWith("*Arcslist")) {
				type = "AL";
				continue;
			}
			
			// Processed based on type
			if(type==null) {
				throw new InvalidStateException("Unable to parse file: "+filename);
			} else if(type.equals("V")) {
				String parts[] = line.split("\\s+");
				GraphItemID gid = null;
				if(parseid && parts.length>1) {
					gid = (GraphItemID) GraphItemID.parseID(parts[1]);
				} else {		
					gid = new GraphItemID(DEFAULTNODESCHEMA,parts[0]);
				}
				
				// Add schema, if needed
				if(!g.hasSchema(gid.getSchemaID())) {
					g.addSchema(gid.getSchemaID(), new Schema(SchemaType.NODE));
				}
				
				Node n = g.addNode(gid);
				int2node.put(Integer.parseInt(parts[0]), n);
			} else if(type.equals("E")) {
				String parts[] = line.split("\\s+");
				GraphItemID gid = new GraphItemID(DEFAULTUNDIRECTEDSCHEMA,""+(ecounter++));
				
				// Add schema, if needed
				if(!g.hasSchema(gid.getSchemaID())) {
					g.addSchema(gid.getSchemaID(), new Schema(SchemaType.UNDIRECTED));
				}
				
				Node n1 = int2node.get(Integer.parseInt(parts[0]));
				if(n1==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[0]
					                                + " in line "+line);
				}
				
				Node n2 = int2node.get(Integer.parseInt(parts[1]));
				if(n2==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[1]
					                                + " in line "+line);
				}
				
				g.addUndirectedEdge(gid, n1, n2);
			} else if(type.equals("A")) {
				String parts[] = line.split("\\s+");
				GraphItemID gid = new GraphItemID(DEFAULTDIRECTEDSCHEMA,""+(ecounter++));
				
				// Add schema, if needed
				if(!g.hasSchema(gid.getSchemaID())) {
					g.addSchema(gid.getSchemaID(), new Schema(SchemaType.DIRECTED));
				}
				
				Node n1 = int2node.get(Integer.parseInt(parts[0]));
				if(n1==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[0]
					                                + " in line "+line);
				}
				
				Node n2 = int2node.get(Integer.parseInt(parts[1]));
				if(n2==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[1]
					                                + " in line "+line);
				}
				
				g.addDirectedEdge(gid, n1, n2);
			} else if(type.equals("AL")) {
				String parts[] = line.split("\\s+");
				GraphItemID gid = new GraphItemID(DEFAULTDIRECTEDSCHEMA,""+(ecounter++));
				
				// Add schema, if needed
				if(!g.hasSchema(gid.getSchemaID())) {
					g.addSchema(gid.getSchemaID(), new Schema(SchemaType.DIRECTED));
				}
				
				Node n1 = int2node.get(Integer.parseInt(parts[0]));
				if(n1==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[0]
					                                + " in line "+line);
				}
				
				Node n2 = int2node.get(Integer.parseInt(parts[1]));
				if(n2==null) {
					throw new InvalidStateException("Node not previously encountered: "+parts[1]
					                                + " in line "+line);
				}
				g.addDirectedEdge(gid, n1, n2);
				
				if(parts.length>2) {
					for(int i=2; i<parts.length; i++) {
						Node prev = int2node.get(Integer.parseInt(parts[i-1]));	
						if(prev==null) {
							throw new InvalidStateException("Node not previously encountered: "+parts[i-1]
							                                + " in line "+line);
						}
						
						Node next = int2node.get(Integer.parseInt(parts[i]));
						if(next==null) {
							throw new InvalidStateException("Node not previously encountered: "+parts[i]
							                                + " in line "+line);
						}
						
						g.addDirectedEdge(gid, prev, next);
					}
				}
			} else {
				throw new InvalidStateException("Unvalid type: "+type);
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph Loaded: "+GraphUtils.getSimpleGraphOverview(g));
		}
		
		return g;
	}

	public void saveGraphToDir(String directory, Graph g) {
		String filedirectory = this.getStringParameter("filedirectory");
		FileWriter fstream;
		try {
			// Create directory if it doesn't already exist
			FileIO.createDirectories(filedirectory);
			fstream = new FileWriter(filedirectory+File.separatorChar+DEFAULTFILENAME);
			BufferedWriter out = new BufferedWriter(fstream);
			
			Map<Node,Integer> node2int = new HashMap<Node,Integer>();
			
			int ncounter = 0;
			int numnodes = g.numNodes();
			if(numnodes<1) {
				out.write("/* Empty Graph */");
				out.close();
			}
			
			out.write("*Vertices "+numnodes+"\n");
			// Save nodes
			Iterator<Node> nitr = g.getNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				
				// Map int id to node
				int id = ++ncounter;
				node2int.put(n, id);
				
				// Write line, with the label set to the string representation of the id
				out.write(id+" \""+n.getID().toString()+"\"\n");
			}
			
			// Save undirected edges
			Iterator<String> sitr = g.getAllSchemaIDs(SchemaType.UNDIRECTED);
			boolean printuline = true;
			while(sitr.hasNext()) {
				String sid = sitr.next();
				if(g.numGraphItems(sid)>0 && printuline) {
					out.write("*Edges\n");
				}
				
				Iterator<Edge> eitr = g.getEdges(sid);
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					// Only binary edges supported
					if(e.numNodes()!=2) {
						throw new UnsupportedTypeException("Only binary edges supported: "
								+e+" has "+e.numNodes());
					}
					
					nitr = e.getAllNodes();
					Node n1 = nitr.next();
					Node n2 = nitr.next();
					out.write(node2int.get(n1)+" "+node2int.get(n2)+"\n");
				}
			}
			
			// Save directed edges
			sitr = g.getAllSchemaIDs(SchemaType.DIRECTED);
			printuline = true;
			while(sitr.hasNext()) {
				String sid = sitr.next();
				if(g.numGraphItems(sid)>0 && printuline) {
					out.write("*Arcs\n");
				}
				
				Iterator<Edge> eitr = g.getEdges(sid);
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					// Only binary edges supported
					if(e.numNodes()!=2) {
						throw new UnsupportedTypeException("Only binary edges supported: "
								+e+" has "+e.numNodes());
					}
					
					Node n1 = ((DirectedEdge) e).getSourceNodes().next();
					Node n2 = ((DirectedEdge) e).getTargetNodes().next();
					out.write(node2int.get(n1)+" "+node2int.get(n2)+"\n");
				}
			}
			
			out.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void saveGraph(Graph g) {
		String filedirectory = this.getStringParameter("filedirectory");
		this.saveGraphToDir(filedirectory, g);
	}
}
