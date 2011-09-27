package linqs.gaia.graph.noise.reference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.ExplicitFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableSet;
import linqs.gaia.visualization.lightviz.LightVizVisualization;

/**
 * We add ambiguous references to the data
 * graph by adding random number of nodes (as specified)
 * for each (or a percentage) of the nodes in the information graph.
 * Each reference node has the same attributes as the corresponding node in the
 * entity node.  We also create edges ``similar'' to those of the information graph
 * by ensuring all reference nodes have an edge equivalent to the edges of the corresponding
 * entity nodes.  We do this by going through each reference node and
 * creating an edge from that reference node, referring to an entity node n1,
 * to a reference node, corresponding to an entity node n2,
 * if n1 and n2 share an edge and the reference nodes do not already
 * have a "similar" edge between them for that particular entity edge.
 * <p>
 * More specifically, you:
 * <OL>
 * <LI> Create references for each node of the specified nodesid (at least one).
 * Create references for an entity between 1 and the number of edges incident to that entity.
 * <LI> Randomly order references.  Given a reference, you look at the relationships
 * of the equivalent entity and ensure it has at least one equivalent edge.
 * An equivalent edge for a reference, nr1, is made by going to the entity of that reference, n1,
 * and looking at all edges for that node.  For each edge, if the edge hasn't been
 * processed for this reference, create a new edge where the entities are replaced
 * replaced randomly with one of its reference.
 * <LI> Repeat until all reference nodes have at least one equivalent edge.
 * <LI> Remove original entity nodes and edges.
 * </OL>
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodesid-Schema ID of nodes to create references for
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> degreetype-Type of degree to upperbound the number of references to.
 * Options are "indegree", "outdegree", and "inandoutdegree".  Default is "inandoutdegree".
 * <LI> refsid-Schema ID to assign references to have (e.g., email_address from an entity of schema person).
 * If not defined, the schema ID of the entity is used.
 * <LI> edgesidprefix-Prefix to add to the SchemaID of "equivalent" edges  created
 * (i.e., if an edge of schema "cites" exists for the entity,
 * the equivalent edge between references maybe "noisy-cites" given this prefix is "noisy-").
 * <LI> reffid-Feature ID of string feature to store the comma delimited string
 * representations of the references for a given entity.  If not specified, this feature is not created. 
 * <LI> entityfid-Feature ID of string feature to store a string representation
 * of entity.  If not specified, this feature is not created.
 * <LI> probcreaterefs-Probability of creating a reference for a given entity node.
 * If set, with a given probability, only create one reference.
 * <LI> minrefs-Minimum number of references which must be at least 1.  Default is 1.
 * <LI> maxrefs-Maximum number of references.  Default is no maximum.
 * <LI> saveentities-If specified, the original nodes and edges are not deleted.
 * Default is to delete them.
 * <LI> deriveobjids-If yes, the object ids of references are of the form
 * [entity object id]-[counter].  If no, the object id is a randomly selected number.
 * Default is no.
 * <LI> outlinksonly-If yes, only add outgoing links for a node.  This will ensure
 * that a node has no more than one equivalent link for all outgoing links
 * while it may have multiple equivalent links for incoming links. Default is no.
 * <LI> inlinksonly-If yes, only add incoming links for a node.  This will ensure
 * that a node has no more than one equivalent link for all incoming links
 * while it may have multiple equivalent links for outgoing links. Default is no.
 * <LI> nonrandom-If "yes", instead of choosing a random number of entities, use
 * the maximum amount as defined by the degree or other parameters.  Default is "no".
 * <LI> referstosid-If specified, create a directed refers-to edge from each reference to its entity
 * with this schema id.
 * <LI> addcorefsid-If specified, create an undirected edge between each reference which
 * with refers to a common entity using this value as the schema ID.
 * <LI> addallequivalent-If "yes", add all possible equivalent edges instead
 * of a random subset.  Default is "no".  Note:  The current implementation only
 * supports binary edges.
 * <LI> mappingfile-File name of file to save mapping to.  If specified, a file
 * is created or appended to where each line is of the form REFID\tENTITYID where
 * REFID is the string representation of the ID of a created reference
 * and ENTITYID is the string representation of the ID of the entity the reference was created for.
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class ReferenceBased extends Noise {
	private Random rand = null;
	private String nodesid = null;
	
	private String entityfid = null;
	private String reffid = null;
	private String procedgesfid = "tmp_procedgesfid";
	private String degreetype = "inandoutdegree";
	
	private boolean deriveobjids = false;
	private boolean saveentities = false;
	private boolean outlinksonly = false;
	private boolean inlinksonly = false;
	private boolean nonrandom = false;
	private boolean addallequivalent = false;
	
	private String refsid = null;
	private String edgesidprefix = null;
	private String mappingfile = null;
	
	private Double probcreaterefs = null;
	private int minrefs;
	private int maxrefs;
	
	/**
	 * Return the number of references to create for this node
	 * 
	 * @param n Node to add references for
	 * @return Number of references to add
	 */
	private int getNumReferences(Node n) {
		// If not below some probability, just create 1.
		if(probcreaterefs != null && rand.nextDouble() > probcreaterefs) {
			return 1;
		}
		
		// Choose a number between 1 and the minimum of degree of node or the
		// maximum value.
		int numedges = 0;
		if(degreetype.equals("indegree")) {
			numedges = n.numAdjacentSources();
		} else if(degreetype.equals("outdegree")) {
			numedges = n.numAdjacentTargets();
		} else {
			numedges = n.numAdjacentGraphItems();
		}
		
		int currmaxrefs = maxrefs < numedges ? maxrefs : numedges;
		if(nonrandom) {
			return currmaxrefs<minrefs ? minrefs : currmaxrefs;
		}
		
		int range = currmaxrefs - minrefs;
		
		// Ensure at least the minimum number of references is made per entity
		range = range > 0 ? range : 1;
		
		int numrefs = minrefs + rand.nextInt(range);
		
		return numrefs;
	}
	
	/**
	 * Create references for the specified node
	 * 
	 * @param g Graph to add references to
	 * @param n Node to make references for
	 */
	private void createReferences(Graph g, Node n) {
		// Get number of reference
		int numrefs = this.getNumReferences(n);
		MultiIDValue entityfidval = new MultiIDValue(n.getID());
		Set<ID> refset = new HashSet<ID>();
		for(int i=0; i<numrefs; i++) {
			// Make reference
			GraphItemID giid = null;
			if(deriveobjids) {
				ID nid = n.getID();
				giid = new GraphItemID(g.getID(), refsid, nid.getObjID()+"-"+i);
			} else {
				giid = GraphItemID.generateGraphItemID(g, refsid);
			}
			Node ref = g.addNode(giid);
			
			// Copy feature values of original entity
			copyFeatureValues(n, ref);
			
			refset.add(ref.getID());
			
			// For references, store the id of the entity
			ref.setFeatureValue(entityfid, entityfidval);
		}
		
		// For the entity, store the ids of references
		n.setFeatureValue(reffid, new MultiIDValue(refset));
		
		// Save mappings to a file, if requested
		if(mappingfile!=null) {
			StringBuffer buf = new StringBuffer();
			for(ID ref:refset) {
				buf.append("\n"+ref+"\t"+n.getID());
			}
			
			FileIO.write2file(mappingfile, buf.toString(), true);
		}
	}
	
	public void copyFeatureValues(Decorable entity, Decorable reference) {
		// Copy the explicit feature values from the original
		Schema schema = entity.getSchema();
		Iterator<SimplePair<String, Feature>> fitr = schema.getAllFeatures();
		while(fitr.hasNext()) {
			SimplePair<String, Feature> fpair = fitr.next();
			String fid = fpair.getFirst();
			Feature f = fpair.getSecond();
			
			// Don't copy reffid
			if(fid.equals(reffid)) {
				continue;
			}
			
			if(f instanceof ExplicitFeature) {
				reference.setFeatureValue(fid, entity.getFeatureValue(fid));
			}
		}
	}
	
	/**
	 * Get the node this reference was made from.
	 * Return null if this is an entity node.
	 * 
	 * @param g Graph to get reference from
	 * @param n Reference node
	 * @return Entity node of reference node
	 */
	private Node getEntity(Graph g, Node n) {
		if(!n.getSchemaID().equals(refsid)) {
			throw new InvalidStateException("Entity not defined for node: "+n);
		}
		
		FeatureValue fval = n.getFeatureValue(entityfid);
		if(fval.equals(FeatureValue.UNKNOWN_VALUE)) {
			return null;
		}
		
		ID eid = ((MultiIDValue) fval).getID();
		
		return g.getNode((GraphItemID) eid);
	}
	
	/**
	 * Check to see if the node is an entity
	 * 
	 * @param n Node to check
	 * @return True if n is an entity, false otherwise.
	 */
	private boolean isEntity(Node n) {
		if(!n.getSchemaID().equals(nodesid)) {
			return false;
		}
		
		if(refsid.equals(nodesid)) {
			// If refsid is the same as nodesid,
			// need to distinguish between the references and entities
			
			// Entities should not have the entityfid defined
			FeatureValue fval = n.getFeatureValue(entityfid);
			if(fval.equals(FeatureValue.UNKNOWN_VALUE)) {
				return true;
			} else {
				return false;
			}
		} else {
			// If refsid is not the same as nodesid,
			// then all nodes with nodesid must be entities
			return true;
		}
	}
	
	/**
	 * Randomly return a node from the set of references made with
	 * the given entity node.  If this is a reference node,
	 * and exception is thrown.  If this is a node which is not
	 * of the specified node schema, just return the node value.
	 * 
	 * @param g Graph
	 * @param n Entity node
	 * @return Randomly chosen reference node
	 */
	private Node getRandomReference(Graph g, Node n) {
		if(!n.getSchemaID().equals(nodesid)) {
			return n;
		}
		
		// Return a reference for entities and n for all others
		FeatureValue fval = n.getFeatureValue(reffid);
		if(fval.equals(FeatureValue.UNKNOWN_VALUE)) {
			throw new InvalidStateException("Expecting reference node: "+n);
		}
		
		// Randomly choose a reference
		UnmodifiableSet<ID> ids = ((MultiIDValue) fval).getIDs();
		ID[] parts = (ID[]) ids.toArray(new ID[]{});
		ID randrefid = parts[rand.nextInt(parts.length)];
		
		return g.getNode((GraphItemID) randrefid);
	}
	
	/**
	 * Return the set of references made with
	 * the given entity node.  If this is a reference node,
	 * and exception is thrown.  If this is a node which is not
	 * of the specified node schema, just return the node value.
	 * 
	 * @param g Graph
	 * @param n Entity node
	 * @return Randomly chosen reference node
	 */
	private Set<Node> getAllReferences(Graph g, Node n) {
		Set<Node> references = new HashSet<Node>();
		if(!n.getSchemaID().equals(nodesid)) {
			references.add(n);
			return references;
		}
		
		// Return a reference for entities and n for all others
		FeatureValue fval = n.getFeatureValue(reffid);
		if(fval.equals(FeatureValue.UNKNOWN_VALUE)) {
			throw new InvalidStateException("Expecting reference node: "+n);
		}
		
		// Randomly choose a reference
		UnmodifiableSet<ID> ids = ((MultiIDValue) fval).getIDs();
		ID[] parts = (ID[]) ids.toArray(new ID[]{});
		for(ID id:parts) {
			references.add(g.getNode((GraphItemID) id));
		}
		
		return references;
	}
	
	/**
	 * Utility function to return a list where the entities
	 * of the list are replaced with a randomly chosen function.
	 * All non-entities are left as is.
	 * 
	 * @param g Graph
	 * @param nitr Iterator over nodes
	 * @param entity Entity to replace in edge
	 * @param ref Reference to replace entity instance with
	 * @return List of nodes
	 */
	private List<Node> convertToReferences(Graph g, Iterator<Node> nitr, Node entity, Node ref) {
		List<Node> nodelist = new ArrayList<Node>();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			Node newref = null;
			if(n.equals(entity)) {
				newref = ref;
			} else {
				newref = this.getRandomReference(g, n);
			}
			
			nodelist.add(newref);
		}
		
		return nodelist;
	}
	
	private void annotProcEdges(List<Node> nodes, Edge e) {
		for(Node n:nodes) {
			// Don't add annotation to those already annotated for that edge
			// or for non reference nodes.
			if(!n.getSchemaID().equals(refsid) || isProcessed(n, e)) {
				continue;
			}
			
			// Add annotation
			FeatureValue value = n.getFeatureValue(procedgesfid);
			Set<ID> newval = null;
			if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
				newval = new HashSet<ID>();
			} else {
				newval = ((MultiIDValue) value).getIDs().copyAsSet();
			}
			
			newval.add(e.getID());
			n.setFeatureValue(procedgesfid, new MultiIDValue(newval));
		}
	}
	
	private boolean isProcessed(Node n, Edge e) {
		FeatureValue value = n.getFeatureValue(procedgesfid);
		
		if(value.equals(FeatureValue.UNKNOWN_VALUE)) {
			return false;
		}
		
		return ((MultiIDValue) value).getIDs().contains(e.getID());
	}
	
	/**
	 * Add the directed edge for the specified reference
	 * 
	 * @param g Graph
	 * @param entity Entity Node
	 * @param ref Reference made from the given entity node
	 * @param e Edge to add a copy of
	 */
	private void addDirectedEdgeCopy(Graph g, Node entity, Node ref, DirectedEdge e) {
		// Don't process reference again for this edge
		if(isProcessed(ref, e)) {
			return;
		}
		
		String esid = edgesidprefix+e.getSchemaID();
		if(!g.hasSchema(esid)) {
			g.addSchema(esid, e.getSchema());
		}
		
		// Get Sources
		Iterator<Node> nitr = e.getSourceNodes();
		List<Node> sources = this.convertToReferences(g, nitr, entity, ref);
		
		// Get Targets
		nitr = e.getTargetNodes();
		List<Node> targets = this.convertToReferences(g, nitr, entity, ref);
		
		// Add edge
		Edge copye = g.addDirectedEdge(GraphItemID.generateGraphItemID(g, esid),
				sources, targets);
		FeatureUtils.copyFeatureValues(e, copye);
		
		// Annotate edges as being processed for all entries
		this.annotProcEdges(sources, e);
		this.annotProcEdges(targets, e);
	}
	
	/**
	 * Add the undirected edge for the specified reference
	 * 
	 * @param g Graph
	 * @param entity Entity Node
	 * @param ref Reference made from the given entity node
	 * @param e Edge to add a copy of
	 */
	private void addUndirectedEdgeCopy(Graph g, Node entity, Node ref, UndirectedEdge e) {
		// Don't process reference again for this edge
		if(isProcessed(ref, e)) {
			return;
		}
		
		String esid = edgesidprefix+e.getSchemaID();
		if(!g.hasSchema(esid)) {
			g.addSchema(esid, e.getSchema());
		}
		
		// Get nodes
		Iterator<Node> nitr = e.getAllNodes();
		List<Node> nodes = this.convertToReferences(g, nitr, entity, ref);
		
		// Add edge
		Edge copye = g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, esid), nodes);
		FeatureUtils.copyFeatureValues(e, copye);
		
		// Annotate edges as being processed for all entries
		this.annotProcEdges(nodes, e);
	}
	
	private void initialize() {
		// Get parameters
		nodesid = this.getStringParameter("nodesid");
		
		// Create entityfid for references and reffid for original
		entityfid = "tmp_entityfid";
		if(this.hasParameter("entityfid")) {
			entityfid = this.getStringParameter("entityfid");
		}
		
		reffid = "tmp_reffid";
		if(this.hasParameter("reffid")) {
			reffid = this.getStringParameter("reffid");
		}
		
		minrefs = 1;
		if(this.hasParameter("minrefs")) {
			minrefs = (int) this.getDoubleParameter("minrefs");
			
			if(minrefs < 1) {
				throw new ConfigurationException("minrefs must be at least 1: "+minrefs);
			}
		}
		
		maxrefs = Integer.MAX_VALUE;
		if(this.hasParameter("maxrefs")) {
			maxrefs = (int) this.getDoubleParameter("maxrefs");
			
			if(maxrefs < minrefs) {
				throw new ConfigurationException("maxrefs must be greater than or equal to minref: "
						+"maxref="+maxrefs+" minref="+minrefs);
			}
		}
		
		probcreaterefs = null;
		if(this.hasParameter("probcreaterefs")) {
			probcreaterefs = this.getDoubleParameter("probcreaterefs");
		}
		
		edgesidprefix = "";
		if(this.hasParameter("edgesidprefix")) {
			edgesidprefix = this.getStringParameter("edgesidprefix");
		}
		
		refsid = nodesid;
		if(this.hasParameter("refsid")) {
			refsid = this.getStringParameter("refsid");
		}
		
		if(this.hasParameter("mappingfile")) {
			mappingfile = this.getStringParameter("mappingfile");
		}
		
		saveentities = this.hasYesNoParameter("saveentities","yes");
		deriveobjids = this.hasYesNoParameter("deriveobjids", "yes");
		outlinksonly = this.hasYesNoParameter("outlinksonly", "yes");
		inlinksonly = this.hasYesNoParameter("inlinksonly", "yes");
		
		degreetype = this.getCaseParameter("degreetype",
			new String[]{"indegree","outdegree","inandoutdegree"}, "inandoutdegree");
		
		nonrandom = this.hasYesNoParameter("nonrandom", "yes");
		addallequivalent = this.hasYesNoParameter("addallequivalent", "yes");
		
		int seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}
		rand = new Random(seed);
	}
	
	@Override
	public void addNoise(Graph g) {
		// Initialize once
		if(nodesid==null) {
			this.initialize();
		}
		
		// Add reference sid, if not defined
		if(!g.hasSchema(refsid)) {
			g.addSchema(refsid, g.getSchema(nodesid));
		}
		
		// Create attributes to temporarily store values for entities
		Schema schema = g.getSchema(nodesid);
		schema.addFeature(reffid, new ExplicitMultiID());
		g.updateSchema(nodesid, schema);
		
		// Create attributes to temporarily store values for references
		schema = g.getSchema(refsid);
		schema.addFeature(entityfid, new ExplicitMultiID());
		schema.addFeature(procedgesfid, new ExplicitMultiID());
		g.updateSchema(refsid, schema);
		
		// Create references for all specified nodes
		List<Node> entities = IteratorUtils.iterator2nodelist(g.getNodes(nodesid));
		for(Node n:entities) {
			this.createReferences(g, n);
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph after adding references: "+GraphUtils.getSimpleGraphOverview(g));
		}
		
		if(this.addallequivalent) {
			addAllEquivalentEdges(g);
		} else {
			// Add a random subset of the equivalent edges
			addEquivalentEdges(g);
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph after adding equivalent edges: "+GraphUtils.getSimpleGraphOverview(g));
		}
		
		// Add refers-to edges
		if(this.hasParameter("referstosid")) {
			String referstosid = this.getStringParameter("referstosid");
			ERUtils.addRefersToFromEntityFeature(g, referstosid, refsid, entityfid);
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Graph after adding refers-to edges: "+GraphUtils.getSimpleGraphOverview(g));
			}
		}
		
		// Ad coreference edges
		if(this.hasParameter("addcorefsid")) {
			String corefsid = this.getStringParameter("addcorefsid");
			ERUtils.addCoRefFromEntityFeature(g, corefsid, refsid, entityfid);
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Graph after coreference edges: "+GraphUtils.getSimpleGraphOverview(g));
			}
		}
		
		// Remove original entities and edges including them
		if(!saveentities) {
			Iterator<Node> nitr = g.getNodes(nodesid);
			while(nitr.hasNext()) {
				Node n = nitr.next();
				if(this.isEntity(n)) {
					// Remove edges for entity node
					n.removeIncidentEdges();
					
					// Remove entity node
					g.removeNode(n);
				}
			}
		}
		
		// Remove temporary attributes for entities
		schema = g.getSchema(nodesid);
		
		// Remove reference feature, if not requested
		if(!this.hasParameter("reffid")) {
			schema.removeFeature(reffid);
		}
		g.updateSchema(nodesid, schema);
		
		// Remove temporary attributes for references
		schema = g.getSchema(refsid);
		// Remove entity feature, if not requested
		if(!this.hasParameter("entityfid")) {
			schema.removeFeature(entityfid);
		}
		
		// Remove information about which edges were
		// processed for a given reference
		schema.removeFeature(procedgesfid);
		g.updateSchema(refsid, schema);
	}
	
	private void addAllEquivalentEdges(Graph g) {
		Set<Edge> edges = new HashSet<Edge>();
		
		// Get all edges incident at least an entity of one of our reference node
		Iterator<Node> nitr = g.getNodes(refsid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			// Process all non-entities
			if(!this.isEntity(n)) {
				// Get the entity for the reference
				Node entity = this.getEntity(g, n);
				
				if(entity == null) {
					throw new InvalidStateException("All non-entities should be references: "+n);
				}
				
				// Get edges
				Iterator<Edge> eitr = entity.getAllEdges();
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					edges.add(e);
				}
			}
		}
		
		// For each edge, add all the equivalent edge that could
		// exist for this reference.
		for(Edge e:edges) {
			String esid = edgesidprefix+e.getSchemaID();
			if(!g.hasSchema(esid)) {
				g.addSchema(esid, e.getSchema());
			}
			
			if(e instanceof DirectedEdge) {
				DirectedEdge de = (DirectedEdge) e;
				
				if(de.numNodes()!=2) {
					throw new UnsupportedTypeException("Functionality only defined for binary edges: "
							+de+" has "+ de.numNodes());
				}
				
				// Get all entities in source
				Set<Node> sources = getAllReferences(g, de.getSourceNodes().next());
				
				// Get all entities in target
				Set<Node> targets = getAllReferences(g, de.getTargetNodes().next());
				
				for(Node s:sources) {
					for(Node t:targets) {
						// Add edge
						Edge copye = g.addDirectedEdge(
								GraphItemID.generateGraphItemID(g, esid),
								s, t);
						FeatureUtils.copyFeatureValues(e, copye);
					}
				}
			} else if(e instanceof UndirectedEdge) {
				UndirectedEdge ue = (UndirectedEdge) e;
				if(ue.numNodes()!=2) {
					throw new UnsupportedTypeException("Functionality only defined for binary edges: "
							+ue+" has "+ ue.numNodes());
				}
				
				// Get all entities in source
				Iterator<Node> uenitr = ue.getAllNodes();
				Set<Node> nodes1 = getAllReferences(g, uenitr.next());
				
				// Get all entities in target
				Set<Node> nodes2 = getAllReferences(g, uenitr.next());
				
				for(Node n1:nodes1) {
					for(Node n2:nodes2) {
						// Add edge
						Edge copye = g.addUndirectedEdge(
								GraphItemID.generateGraphItemID(g, esid),
								n1, n2);
						FeatureUtils.copyFeatureValues(e, copye);
					}
				}
			} else {
				throw new UnsupportedTypeException("Unsupported edge type: "
						+e.getClass().getCanonicalName());
			}
		}
	}
	
	private void addEquivalentEdges(Graph g) {
		// Go through each node and for each
		// reference, add an equivalent edge for
		// all the edges of the original entity.
		// Note that the added edge will not point
		// to the original entities of incident nodes
		// but to their references (if applicable).
		Iterator<Node> nitr = g.getNodes(refsid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			// Process all non-entities
			if(!this.isEntity(n)) {
				// Get the entity for the reference
				Node entity = this.getEntity(g, n);
				
				if(entity == null) {
					throw new InvalidStateException("All non-entities should be references: "+n);
				}
				
				// Get edges
				Iterator<Edge> eitr = entity.getAllEdges();
				while(eitr.hasNext()) {
					Edge e = eitr.next();
					
					if(e instanceof DirectedEdge) {
						DirectedEdge de = (DirectedEdge) e;
						
						// Outlinks only
						if(outlinksonly && !de.isSource(entity)) {
							continue;
						}
						
						// Inlinks only
						if(inlinksonly && !de.isTarget(entity)) {
							continue;
						}
						
						// Process directed edges
						this.addDirectedEdgeCopy(g, entity, n, de);
					} else if(e instanceof UndirectedEdge) {
						// Process undirected edges
						this.addUndirectedEdgeCopy(g, entity, n, (UndirectedEdge) e);
					} else {
						throw new UnsupportedTypeException("Unsupported edge type: "
								+e.getClass().getCanonicalName());
					}
				}
				
			}
		}
	}
	
	// TODO Remove in final version
	public static void main(String[] args) {
		
		DataGraph g = new DataGraph(new GraphID("g","1"));
		g.updateSchema("g", new Schema(SchemaType.GRAPH));
		g.addSchema("n", new Schema(SchemaType.NODE));
		g.addSchema("n2", new Schema(SchemaType.NODE));
		g.addSchema("de", new Schema(SchemaType.DIRECTED));
		g.addSchema("ue", new Schema(SchemaType.UNDIRECTED));
		
		Node n1 = g.addNode(new GraphItemID(g.getID(), "n", "1"));
		Node n2 = g.addNode(new GraphItemID(g.getID(), "n", "2"));
		Node n3 = g.addNode(new GraphItemID(g.getID(), "n", "3"));
		Node n4 = g.addNode(new GraphItemID(g.getID(), "n", "4"));
		
		g.addNode(new GraphItemID(g.getID(), "n2", "21"));
		Node n22 = g.addNode(new GraphItemID(g.getID(), "n2", "22"));
		Node n23 = g.addNode(new GraphItemID(g.getID(), "n2", "23"));
		
		g.addNode(new GraphItemID(g.getID(), "n2", "24"));
		
		g.addDirectedEdge(new GraphItemID(g.getID(), "de", "1"), n1, n2);
		g.addDirectedEdge(new GraphItemID(g.getID(), "de", "2"), n1, n3);
		g.addDirectedEdge(new GraphItemID(g.getID(), "de", "3"), n1, n22);
		g.addDirectedEdge(new GraphItemID(g.getID(), "de", "4"), n1, n23);
		
		g.addUndirectedEdge(new GraphItemID(g.getID(), "ue", "1"), n2, n3);
		g.addUndirectedEdge(new GraphItemID(g.getID(), "ue", "2"), n3, n4);
		
		LightVizVisualization viz1 = new LightVizVisualization();
		viz1.visualize(g);
		
		ReferenceBased noise = new ReferenceBased();
		noise.setParameter("nodesid", "n");
		noise.setParameter("minrefs", "2");
		noise.setParameter("deriveobjids", "yes");
		noise.setParameter("refsid", "nref");
		noise.setParameter("edgesidprefix", "noisy-");
		noise.addNoise(g);
		
		Iterator<Edge> eitr = g.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(e instanceof DirectedEdge) {
				Log.INFO(e+" Sources="+IteratorUtils.iterator2nodelist(((DirectedEdge) e).getSourceNodes())
						+" Targets="+IteratorUtils.iterator2nodelist(((DirectedEdge) e).getTargetNodes()));
			} else {
				Log.INFO(e+" Nodes="+IteratorUtils.iterator2nodelist(((UndirectedEdge) e).getAllNodes()));
			}
		}
		
		LightVizVisualization viz2 = new LightVizVisualization();
		viz2.visualize(g);
	}
}