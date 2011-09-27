package linqs.gaia.model.lp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.registry.GraphRegistry;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.er.merger.MergeUtils;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimpleTimer;

/**
 * This is an oracle link prediction algorithm.
 * The model assumes that the set of existing edges are declared
 * in the system data or an output graph, with references connected to their
 * entity using a refers-to edge is provided.
 * For the former, a system data entry
 * with the value defined by LinkPredictor.EXIST is specified
 * for the string value returned by edge2nodestring(e) where
 * e is a true edge.  Edges for which the system data is not defined,
 * or with the value of LinkPredictor.NOTEXIST is predicted as non-existing.
 * For the latter, an edge exists if the corresponding
 * entity or node (if its not a reference), as specified by the refers-to sid, is linked.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> mergedidsfid-If specified, the nodes of the edge may be the result
 * of nodes being merged (i.e., entity resolution).  If so, we predict
 * the existence of a link for binary edges only.  A link existing
 * is predicted if the number of pairs of references which have a link,
 * from the two nodes, outnumber those without a link.
 * <LI> usesystemdata-If yes, the groundtruth is stored in system data as described above.
 * Either this is yes or outputgraphid must be specified.
 * <LI> outputgraphid-String representation of the output graph id where
 * the reference nodes are specified with refers-to edges to their entities.
 * If specified, a graph with this graph ID must be registered in {@link GraphRegistry} or
 * an exception will be thrown.
 * Either this is yes or usesystemdata must be specified.
 * <LI> referstosid-The schema id of the "refers-to" in output graph.
 * Only used when outputgraphid is specified.
 * <LI> checkpoint-If set, this the current number of links predicted over is printed
 * as an INFO message whenever (the current number)%(checkpoint) == 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class LPOracle extends BaseConfigurable implements LinkPredictor {
	private static final long serialVersionUID = 1L;
	
	private String mergedidsfid = null;
	private GraphID outputgraphid;
	private String referstosid;
	private boolean usesystemdata=false;
	private Double checkpoint = null;
	
	private String edgeschemaid = null;
	
	public void initialize(String edgeschemaid) {
		this.edgeschemaid = edgeschemaid;
		
		if(this.hasParameter("mergedidsfid")) {
			mergedidsfid = this.getStringParameter("mergedidsfid");
		}
		
		if(this.hasParameter("outputgraphid")) {
			this.outputgraphid = GraphID.parseGraphID(this.getStringParameter("outputgraphid"));
		}
		
		if(this.hasParameter("referstosid")) {
			this.referstosid = this.getStringParameter("referstosid");
		}
		
		if(this.hasParameter("checkpoint")) {
			this.checkpoint = this.getDoubleParameter("checkpoint");
		}
		
		usesystemdata = this.hasYesNoParameter("usesystemdata", "yes");
	}

	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String existfeature) {
		if(this.edgeschemaid==null) {
			this.initialize(edgeschemaid);
		}
	}

	public void learn(Graph graph, PotentialLinkGenerator generator,
			String edgeschemaid) {
		if(this.edgeschemaid==null) {
			this.initialize(edgeschemaid);
		}
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges) {
		this.predict(graph, unknownedges.iterator(), true, null);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator) {
		this.predict(graph, generator.getLinksIteratively(graph, edgeschemaid), true, null);
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		this.predict(graph, unknownedges.iterator(), removenotexist, existfeature);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		this.predict(graph, generator.getLinksIteratively(graph, edgeschemaid), removenotexist, existfeature);
	}
	
	/**
	 * Return a string representation of the nodes incident to the given edge.
	 * The nodes are represented using values of the form
	 * schemaid.objectid.  Undirected edges are stored as comma delimited
	 * values and Directed edges are stored as two comma delimited values
	 * separated by a "|" where all the nodes before the character are
	 * the source nodes and all the nodes after are the target nodes.
	 * The comma delimited values representing the nodes are ordered alphabetically.
	 * 
	 * @param e Edge
	 * @return String representation of the incident nodes.
	 */
	public static String edge2nodestring(Edge e) {
		if(e instanceof UndirectedEdge) {
			List<String> nodes = new ArrayList<String>();
			Iterator<Node> nitr = e.getAllNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				ID id = n.getID();
				nodes.add(id.getSchemaID()+"."+id.getObjID());
			}
			
			Collections.sort(nodes);
			return e.getSchemaID()+":"+ListUtils.list2string(nodes, ",");
		} else if(e instanceof DirectedEdge) {
			DirectedEdge de = (DirectedEdge) e;
			
			List<String> sources = new ArrayList<String>();
			Iterator<Node> nitr = de.getSourceNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				ID id = n.getID();
				sources.add(id.getSchemaID()+"."+id.getObjID());
			}
			
			List<String> targets = new ArrayList<String>();
			nitr = de.getTargetNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				ID id = n.getID();
				targets.add(id.getSchemaID()+"."+id.getObjID());
			}
			
			Collections.sort(sources);
			Collections.sort(targets);
			return e.getSchemaID()+":"+ListUtils.list2string(sources, ",")
					+"|"+ListUtils.list2string(targets, ",");
		} else {
			throw new UnsupportedTypeException("Unsupported Edge Type: "+e.getClass().getCanonicalName());
		}
	}
	
	/**
	 * Convert the set of node ids and edge schema id
	 * to a string representation for use as a key in the system data.
	 * 
	 * @param edgeschemaid Schema ID of edge
	 * @param id1 ID of first node
	 * @param id2 ID of second node
	 * @param isdirected True if the edge is a directed edge and false otherwise
	 * @return Key for system data
	 */
	public static String nodepair2nodestring(String edgeschemaid, ID id1, ID id2, boolean isdirected) {		
		// Return the string representation
		return edgeschemaid+":"
			+id1.getSchemaID()+"."+id1.getObjID()
			+ (isdirected ? "|" : ",")
			+id2.getSchemaID()+"."+id2.getObjID();
	}
	
	/**
	 * Process all predictions appropriately
	 * 
	 * @param graph Graph
	 * @param eitr Iterator over edges
	 * @param removenotexist If true, remove non existing edges
	 * @param existfeature If specified, save existence feature value
	 */
	private void predict(Graph graph, Iterator<Edge> eitr,
			boolean removenotexist, String existfeature) {
		Graph outputgraph = null;
		if(outputgraphid!=null) {
			outputgraph = GraphRegistry.getGraph(outputgraphid);
		}
		
		// Add exist feature, if not already defined
		Schema schema = graph.getSchema(edgeschemaid);
		if(existfeature!=null && !schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(edgeschemaid, schema);
		}
		
		int numpredover = 0;
		int numexist = 0;
		SimpleTimer timer = new SimpleTimer();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			numpredover++;
			KeyedCount<Boolean> counts = new KeyedCount<Boolean>();
			
			boolean isdirected = false;
			
			// Merged ids currently only supported for binary edges
			if(e.numNodes()!=2) {
				throw new UnsupportedTypeException("Only binary edges supported: "+
						e.numNodes());
			}
			
			Node n1, n2;
			if(e instanceof UndirectedEdge) {
				Iterator<Node> nitr = e.getAllNodes();
				n1 = nitr.next();
				n2 = nitr.next();
			} else if(e instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) e;
				n1 = de.getSourceNodes().next();
				n2 = de.getTargetNodes().next();
				
				isdirected=true;
			} else {
				throw new UnsupportedTypeException(
					"Unsupported Edge Type: "+e.getClass().getCanonicalName());
			}
			
			Set<GraphItemID> n1refids = this.getReferences(n1);
			Set<GraphItemID> n2refids = this.getReferences(n2);
			
			boolean n1noref = false;
			if(n1refids.isEmpty()) {
				n1noref = true;
				n1refids.add(n1.getID());
			}
			
			boolean n2noref = false;
			if(n2refids.isEmpty()) {
				n2noref = true;
				n2refids.add(n2.getID());
			}
			
			if(usesystemdata) {
				// If the groundtruth is stored in the system data,
				// process accordingly
				Set<String> sdkeys = new HashSet<String>();
				for(ID n1refid:n1refids) {
					for(ID n2refid:n2refids) {
						sdkeys.add(LPOracle.nodepair2nodestring(e.getSchemaID(), n1refid, n2refid, false));
					}
				}
				
				// Go over all possible sdkeys resulting from the merges, if applicable
				for(String sdkey:sdkeys) {
					String sdvalue = graph.getSystemData(sdkey);
					counts.increment((sdvalue != null && sdvalue.equals(LinkPredictor.EXIST)));
				}
			} else if(outputgraph!=null) {
				// If the groundtruth is stored in the output graph,
				// check to see if respective entities share an edge
				// of the specified type.
				for(GraphItemID n1refid:n1refids) {
					Node n1ref = (Node) outputgraph.getEquivalentGraphItem(n1refid);
					Node n1entity = n1noref ? n1ref : ERUtils.getRefersToEntity(n1ref, referstosid);
					
					for(GraphItemID n2refid:n2refids) {
						Node n2ref = (Node) outputgraph.getEquivalentGraphItem(n2refid);
						Node n2entity = n2noref ? n2ref : ERUtils.getRefersToEntity(n2ref, referstosid);
						
						if(isdirected) {
							counts.increment(n1entity.isAdjacentTarget(n2entity, edgeschemaid));
						} else {
							counts.increment(n1entity.isAdjacent(n2entity, edgeschemaid));
						}
					}
				}
			} else {
				throw new ConfigurationException("Either parameters usesystemdata is yes" +
						" or outputgraphid must be specified");
			}
			
			// Handle edge
			if(counts.highestCountKey()) {
				// Handle existing, if highest count is true
				if(existfeature!=null) {
					e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
				}
				
				numexist++;
			} else {
				// Handle not existing
				if(removenotexist) {
					graph.removeEdge(e);
					continue;
				}
				
				if(existfeature!=null) {
					e.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
				}
			}
			
			if(this.checkpoint != null && (numpredover % this.checkpoint) == 0) {
				Log.INFO("Existence Classifier model predicted: "
						+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
				timer.start();
			}
		}
	}
	
	private Set<GraphItemID> getReferences(Node n) {
		Set<GraphItemID> refids = new HashSet<GraphItemID>();
		
		// Support the two ways a new node may be added
		// 1) As a merge of reference nodes
		// 2) As a new node linke to its references by a refers-to edge
		if(mergedidsfid!=null) {
			// Get the ids of references merged
			refids.addAll(MergeUtils.getMergeIDs(n, mergedidsfid));
		} else if(referstosid!=null) {
			// Get the ids of references with refers-to edge to this entity
			Iterator<Node> itr = n.getAdjacentSources(referstosid);
			while(itr.hasNext()) {
				refids.add(itr.next().getID());
			}
		}
		
		return refids;
	}
	
	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		this.initialize(edgeschemaid);
	}
	
	public void saveModel(String directory) {
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
