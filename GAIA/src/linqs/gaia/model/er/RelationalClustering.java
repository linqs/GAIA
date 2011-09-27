package linqs.gaia.model.er;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UndefinedFunctionException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.global.Global;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.BootstrapModel;
import linqs.gaia.model.er.bootstrap.ERBootstrap;
import linqs.gaia.model.er.merger.Merger;
import linqs.gaia.model.er.merger.feature.FeatureMerger;
import linqs.gaia.model.er.merger.node.IncidentEdgeMerger;
import linqs.gaia.model.util.plg.NodeCentricPLG;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.similarity.node.NaiveRelationalSim;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedSet;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimpleTimer;

/**
 * Collective relational clustering algorithm as proposed in:
 * <p>
 * Bhattacharya, I. & Getoor, L.,
 * Collective Entity Resolution in Relational Data,
 * ACM Transactions on Knowledge Discovery from Data, 2007, 1, 1-36 
 * <p>
 * Note: Attributes and edges are merged pairwise with the resulting
 * merged node only containing the merged feature and edge values.
 * Choose the feature and edge merger appropriately.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodesimclass-Node similarity measure to use to calculate similarity.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> threshold-Value between 0 and 1, inclusive.  Anything equal to and
 * above the threshold is predicted to exist.  Default is .5.
 * <LI> fvalue-If learning, this specifies what f value to optimize for
 * (i.e., f1, f2, etc.).  Default is 1.
 * <LI> erbootstrapclass-{@link ERBootstrap} implementation,
 * instantiated using in {@link Dynamic#forConfigurableName}.  If specified,
 * all pairs predicted true by {@link ERBootstrap#isSameEntity} is
 * predicted as referring to the same entity.
 * <LI> maxiterations-Maximum number of iterations to run for relational
 * clustering.  Default is to run until the threshold is met.
 * <LI> learnalphaandthreshold-If yes, learn the optimal alpha and threshold pair
 * with the assumption we are using {@link linqs.gaia.similarity.node.NaiveRelationalSim}
 * as the node similarity measure.  Default is no. Parameter not valid when training
 * from a specific set of co-reference edges.
 * <LI> learnthreshold-If yes, learn the optimal threshold given the node similarity
 * measure.  Default is no.  Parameter not valid when training
 * from a specific set of co-reference edges.
 * <LI>fmergerclass-Class of the {@link FeatureMerger} to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * <LI>iemergerclass-Class of the {@link IncidentEdgeMerger} to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * <LI> sysdatakey-The algorithm stores the comma delimited ids of the merged references in system data using
 * this system data key.  You can access the same data, during the merging process,
 * by accessing this system data value using this system data key and the entity.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class RelationalClustering extends BaseConfigurable
	implements EntityResolution, BootstrapModel {
	private static final long serialVersionUID = 1L;
	
	private double threshold = .5;
	
	// Value set only if alpha is learned
	private Double alpha = null;
	private double fvalue = 1;
	private int maxiterations = -1;
	private NormalizedNodeSimilarity nodesim;
	
	private String refschemaid;
	private String edgeschemaid;
	
	private boolean shouldbootstrap = false;
	private ERBootstrap erboostrap = null;
	
	private FeatureMerger fmerger = null;
	private IncidentEdgeMerger iemerger = null;
	
	private String sysdatakey = "tmp_reffid";
	private boolean initialize = true;
	
	/**
	 * Initialize the model
	 * 
	 * @param edgeschemaid
	 */
	private void initialize(String refschemaid, String edgeschemaid) {
		this.initialize=false;
		this.edgeschemaid = edgeschemaid;
		this.refschemaid = refschemaid;
		
		// Initialize node similarity
		String nodesimclass = this.getStringParameter("nodesimclass");
		nodesim = (NormalizedNodeSimilarity) 
			Dynamic.forConfigurableName(NormalizedNodeSimilarity.class,
			nodesimclass, this);
		
		// Get threshold
		if(this.hasParameter("threshold")) {
			this.threshold = this.getDoubleParameter("threshold");
		}
		
		// Get max iterations
		if(this.hasParameter("maxiterations")) {
			this.maxiterations = this.getIntegerParameter("maxiterations");
		}
		
		// Get ER bootstrap class
		if(this.hasParameter("erbootstrapclass")) {
			String erbootstrapclass = this.getStringParameter("erbootstrapclass");
			this.erboostrap = (ERBootstrap)
				Dynamic.forConfigurableName(ERBootstrap.class, erbootstrapclass);
			this.erboostrap.copyParameters(this);
		}
		
		// Get feature merger
		if(this.hasParameter("fmergerclass")) {
			String fmergerclass = this.getStringParameter("fmergerclass");
			fmerger = (FeatureMerger) Dynamic.forConfigurableName(FeatureMerger.class,
					fmergerclass);
			fmerger.copyParameters(this);
		}
		
		// Get Incident Edge merger
		if(this.hasParameter("iemergerclass")) {
			String iemergerclass = this.getStringParameter("iemergerclass");
			iemerger = (IncidentEdgeMerger) Dynamic.forConfigurableName(IncidentEdgeMerger.class,
					iemergerclass);
			iemerger.copyParameters(this);
		}
		
		// Get system data key used to store references per entity
		if(this.hasParameter("sysdatakey")) {
			this.sysdatakey = this.getStringParameter("sysdatakey");
		}
	}
	
	/**
	 * Remove the ER edges but save them in the system data.
	 * We need to do this prior to training so we don't use
	 * the er edges in learning our model.
	 * 
	 * @param g Graph
	 */
	private void removeEREdges(Graph g) {
		// Save and remove the ER edges of the graph
		Iterator<Edge> eitr = g.getEdges(this.edgeschemaid);
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			// All edges should be binary and undirected
			if(!(e instanceof UndirectedEdge) || e.numNodes()!=2) {
				throw new InvalidStateException("Invalid edge encountered: "+e);
			}
			
			Iterator<Node> nitr = e.getAllNodes();
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			String coref = g.getSystemData(n1.getID(), "rc-coref");
			if(coref==null) {
				coref = "";
			} else {
				coref += ",";
			}
			coref += n2.getID().toString();
			
			g.setSystemData(n1.getID(), "rc-coref", coref);
		}
		
		g.removeAllGraphItems(this.edgeschemaid);
	}
	
	/**
	 * Restore previously removed er edges.
	 * 
	 * @param g Graph
	 */
	private void restoreEREdges(Graph g) {
		// Restore previously removed er edges
		Iterator<Node> nitr = g.getNodes(this.refschemaid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			String sysdata = g.getSystemData(n.getID(), "rc-coref");
			
			if(sysdata==null) {
				continue;
			}
			
			String[] ids = sysdata.split(",");
			for(String id:ids) {
				g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, this.edgeschemaid), 
						n, g.getNode(GraphItemID.parseGraphItemID(id)));
			}
			
			g.removeSystemData(n.getID(),"rc-coref");
		}
	}
	
	public void learn(Graph graph, String refschemaid, String edgeschemaid,
			PotentialLinkGenerator generator) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		// Set f-value of the f-measure to optimize for
		fvalue = 1;
		if(this.hasParameter("fvalue")) {
			fvalue = this.getDoubleParameter("fvalue");
		}
		
		if(this.hasYesNoParameter("learnalphaandthreshold", "yes")) {
			// Learn the alpha and threshold for when we
			// use NodeRelationalSim.  We support this since
			// this is what Relational Clustering was defined with.
			this.learnAlphaAndThreshold(graph, generator); 
		} else if(this.hasYesNoParameter("learnthreshold", "yes")) {
			// Learn the optimal threshold given any similarity measure
			this.learnThreshold(graph, generator);
		}
	}
	
	public void learn(Graph graph, String edgeschemaid, String entityschemaid, String refschemaid,
			String referstoschemaid, PotentialLinkGenerator generator) {
		
		Graph copygraph = graph.copy("gaiatmp-"+graph.getID().getObjID());
		ERUtils.addCoRefFromRefersToEdges(copygraph, entityschemaid, referstoschemaid, edgeschemaid);
		this.learn(copygraph, refschemaid, edgeschemaid, generator);
		
		copygraph.destroy();
	}
	
	/**
	 * Learn the optimal threshold, optimizing for f-measure,
	 * from the training graph.
	 * 
	 * @param graph Training graph
	 * @param generator Proposes all pairs of nodes which may refer to the same entity.
	 */
	private void learnThreshold(Graph graph, PotentialLinkGenerator generator) {
		ThresholdTester tt = new ThresholdTester(graph);
		this.removeEREdges(graph);
		
		SimpleTimer timer = new SimpleTimer();
		this.predictAsLink(graph, generator, 0, tt);
		Log.DEBUG("Learned threshold="+tt.getThreshold()
				+" for an expected f-measure="+tt.getExpectedF1()
				+" in time "+timer.timeLapse());
		this.threshold = tt.getThreshold();
		
		graph.removeAllGraphItems(this.edgeschemaid);
		
		this.restoreEREdges(graph);
	}
	
	/**
	 * Learn alpha and threshold.  This is only applicable
	 * for certain similarity measures.
	 * 
	 * @param graph Training graph
	 * @param generator Proposes all pairs of nodes which may refer to the same entity.
	 */
	private void learnAlphaAndThreshold(Graph graph, PotentialLinkGenerator generator) {
		double maxthreshold = 0.0;
		double maxalpha = 0;
		double maxf1 = 0;
		
		// Can only learn alpha for some similarity measures
		if(!(nodesim instanceof NaiveRelationalSim)) {
			throw new ConfigurationException("Can only train alpha for NaiveRelationalSim: "
					+this.nodesim.getClass().getCanonicalName());
		}
		
		NaiveRelationalSim nrsim = (NaiveRelationalSim) nodesim;
		ThresholdTester tt = new ThresholdTester(graph);
		this.removeEREdges(graph);
		
		// Go over all possible alphas and start it
		SimpleTimer allatimer = new SimpleTimer();
		double[] alphas = new double[]{0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
		for(double alpha:alphas) {
			nrsim.setAlpha(graph, alpha);
			
			SimpleTimer atimer = new SimpleTimer();
			this.predictAsLink(graph, generator, 0, tt);
			Log.DEBUG("Learned threshold="+tt.getThreshold()
					+" for alpha="+alpha
					+" with an expected f-measure="+tt.getExpectedF1()
					+" in time "+atimer.timeLapse());
			
			// Check to see which threshold performs the best
			double currf1 = tt.getExpectedF1();
			if(currf1 > maxf1) {
				maxf1 = currf1;
				maxthreshold = tt.getThreshold();
				maxalpha = alpha;
			}
			
			graph.removeAllGraphItems(this.edgeschemaid);
			
			tt.reset();
		}
		
		this.restoreEREdges(graph);
		nrsim.setAlpha(graph, maxalpha);
		this.alpha=maxalpha;
		this.threshold = maxthreshold;
		
		Log.DEBUG("Optimal learned threshold="+maxthreshold
				+" for alpha="+maxalpha
				+" with an expected f-measure="+maxf1
				+" in time "+allatimer.timeLapse());
	}
	
	/**
	 * Learn the ideal threshold for the given graph
	 * 
	 * @author namatag
	 *
	 */
	private class ThresholdTester {
		private Double threshold = null;
		private boolean firstdec = false;
		private double maxf1threshold = 1;
		private double maxf1 = 0;
		private double numpos = 0;
		private double numtp = 0;
		private double numfp = 0;
		private Set<String> pospairs = new HashSet<String>();
		
		protected ThresholdTester(Graph g) {
			if(!g.hasSchema(edgeschemaid)) {
				throw new InvalidStateException("Schema undefined: "+edgeschemaid);
			}
			
			// Remember all positive pairs
			numpos = g.numGraphItems(edgeschemaid);
			
			Iterator<Edge> eitr = g.getEdges(edgeschemaid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				if(e.numNodes() != 2) {
					throw new UnsupportedTypeException("Only binary edges supported:"
							+" Number of nodes: "+e.numNodes()+" for "+e);
				}
				
				// Compute the feature and relational similarity of the features
				// Note: Assumes the nodes are unmerged.
				Iterator<Node> itr = e.getAllNodes();
				Node n1 = itr.next();
				Node n2 = itr.next();
				pospairs.add(this.pair2string(n1.getID(), n2.getID()));
			}
		}
		
		private String pair2string(ID id1, ID id2) {
			List<String> ids = new ArrayList<String>();
			ids.add(id1.getSchemaID()+"."+id1.getObjID());
			ids.add(id2.getSchemaID()+"."+id2.getObjID());
			Collections.sort(ids);
			return ListUtils.list2string(ids, ",");
		}
		
		private List<ID> getRefs(Graph g, Node n) {
			List<ID> refs = new ArrayList<ID>();
			String refvalue = g.getSystemData(n.getID(), sysdatakey);
			if(refvalue == null) {
				// Assume unmerged node
				refs.add(n.getID());
			} else {
				// Get obj ids
				String[] refids = refvalue.split(",");
				for(String idstring:refids) {
					refs.add(GraphItemID.parseID(idstring));
				}
			}
			
			return refs;
		}
		
		protected boolean addPositivePrediction(Graph g, Node n1, Node n2, double similarity) {
			// Store the lowest threshold encountered thus far
			if(threshold == null) {
				threshold = similarity;
			} else if(similarity < threshold) {
				// Compute f-measure for last minimum
				// Also, if applicable, recompute the maxf1
				// for the same threshold.
				double p = numtp/(numtp+numfp);
				double r = numtp/(numpos);
				double n = fvalue;
				double currf1 = ((1+(n*n))*p*r)/((n*n*p)+r);
				
				// If f1 of last similarity is better, use that
				if(currf1>=maxf1) {
					maxf1 = currf1;
					maxf1threshold = threshold;
				}
				
				// Update to new minimum
				threshold = similarity;
				
				// Check whether or not an inflection point occurred
				firstdec = true;
			}
			
			// If n1 and n2 are from a merge,
			// process accordingly
			List<ID> n1refs = this.getRefs(g, n1);
			List<ID> n2refs = this.getRefs(g, n2);
			for(ID n1ref:n1refs) {
				for(ID n2ref:n2refs) {
					boolean istp = isSame(n1ref, n2ref);
					if(istp) {
						numtp++;
					} else {
						numfp++;
					}
				}
			}
			
			// Break out sooner by recognizing we can't get a better f1.
			boolean shouldbreak = firstdec && (numtp == numpos);
			
			return shouldbreak;
		}
		
		private boolean isSame(ID id1, ID id2) {
			return this.pospairs.contains(this.pair2string(id1, id2));
		}
		
		public double getThreshold() {
			return this.maxf1threshold;
		}
		
		public double getExpectedF1() {
			return this.maxf1;
		}
		
		public void reset() {
			// Note:  Do not reset the positive pairs or the counts for positive pairs.
			threshold = null;
			maxf1threshold = 1;
			maxf1 = 0;
			numtp = 0;
			numfp = 0;
		}
	}

	public void predictAsLink(Graph graph, PotentialLinkGenerator generator) {
		this.predictAsLink(graph, generator, this.threshold, null);
	}
	
	/**
	 * This function is not supported for this model.
	 * An {@link UndefinedFunctionException} will be thrown if called.
	 */
	public void predictAsLink(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		throw new UndefinedFunctionException("Relational Clustering is unable to return" +
				" meaningful probability distributions for potentially co-referent pairs.");
	}
	
	/**
	 * This function is not supported for this model.
	 * An {@link UndefinedFunctionException} will be thrown if called.
	 */
	public void predictAsLink(Graph graph, Iterable<Edge> unknownedges,
			boolean removenotexist, String existfeature) {
		throw new UndefinedFunctionException("Relational Clustering is unable to return" +
			" meaningful probability distributions for potentially co-referent pairs.");
	}
	
	public void predictAsNode(Graph graph, PotentialLinkGenerator generator,
			String entitysid, String referstosid) {
		this.predictAsLink(graph, generator);
		ERUtils.addEntityNodesFromCoRef(graph, graph, edgeschemaid, entitysid, refschemaid, referstosid, null, true);
	}
	
	public void predictAsNode(Graph refgraph, Graph entitygraph, PotentialLinkGenerator generator,
			String entitysid, String reffeatureid) {
		this.predictAsLink(refgraph, generator);
		ERUtils.addEntityNodesFromCoRef(refgraph, entitygraph, edgeschemaid, entitysid, refschemaid, null, reffeatureid, true);
	}
	
	/**
	 * Predict as link.  This method is also used in training
	 * to consider all possible thresholds.
	 * 
	 * @param graph
	 * @param generator
	 * @param threshold
	 * @param tt
	 */
	private void predictAsLink(Graph graph, PotentialLinkGenerator generator,
			double threshold, ThresholdTester tt) {
		// Reset counter since this is used when merging
		// Note: Done to make CRC more consistent since ordering in it
		// can also depend on the ID of the node.
		Global.reset();
		
		SimpleTimer timer = new SimpleTimer();
		Log.DEBUG("Graph Before Predicting As Link: "+GraphUtils.getSimpleGraphOverview(graph));
		
		if(nodesim instanceof NaiveRelationalSim && this.alpha!=null) {
			NaiveRelationalSim nrsim = (NaiveRelationalSim) nodesim;
			nrsim.setAlpha(graph, alpha);
		}
		
		if(!(generator instanceof NodeCentricPLG)) {
			throw new InvalidStateException("Potential link generator must be node centric: "+
					generator.getClass().getCanonicalName());
		}
		
		NodeCentricPLG ncplg = (NodeCentricPLG) generator;
		
		if(!graph.hasSchema(this.edgeschemaid)) {
			graph.addSchema(this.edgeschemaid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Create the cluster nodes.  We'll be resolving these nodes instead
		// of the actual nodes.
		Graph copygraph = graph.copy("copy-"+graph.getID().getObjID());
		
		// Handle bootstrapping by creating an eredge between all positive pairs
		if(this.shouldbootstrap) {
			if(this.erboostrap==null) {
				throw new InvalidStateException("Unable to bootstrap, bootstrap class" +
						" not specified.");
			}
			
			Iterator<Edge> eitr = generator.getLinksIteratively(copygraph, edgeschemaid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				
				// Only support binary edges
				if(e.numNodes()!=2) {
					throw new ConfigurationException("Invalid number of edges: "+e.numNodes());
				}
				
				Iterator<Node> nitr = e.getAllNodes();
				Node n1 = nitr.next();
				Node n2 = nitr.next();
				
				boolean issame = this.erboostrap.isSameEntity(n1, n2);
				
				if(!issame) {
					// Remove edges between all pairs not same
					copygraph.removeGraphItem(e);
				}
			}
			
			Log.DEBUG("Graph After Bootstrapping: "+GraphUtils.getSimpleGraphOverview(copygraph));
		}
		
		// Merge nodes from bootstrapping
		// Note: Assume transitivity and merged appropriate nodes
		List<Set<Node>> entitysets = ERUtils.getTransitiveEntity(copygraph, refschemaid, edgeschemaid);
		for(Set<Node> eset:entitysets) {
			// Merge all the nodes one by one
			Iterator<Node> nitr = eset.iterator();
			Node lastn = nitr.next();
			while(nitr.hasNext()) {
				Node currn = nitr.next();
				lastn = this.mergeNodes(copygraph, currn, lastn);
			}
		}
		
		KeyedSet<Node,CPair> node2entry = new KeyedSet<Node,CPair>();
		TreeSet<CPair> clusterqueue = new TreeSet<CPair>();
		
		// Remove "co-reference" edges, if available, up to this point
		copygraph.removeAllGraphItems(edgeschemaid);
		
		Log.DEBUG("Graph After Bootstrap Merging: "+GraphUtils.getSimpleGraphOverview(copygraph));
		
		// Generate all edges between the cluster elements and add to priority queue
		Iterator<Edge> eitr = generator.getLinksIteratively(copygraph, edgeschemaid);
		this.addPotentialClusters(copygraph, eitr, clusterqueue, node2entry);
		
		// Repeat and merge contents with similarity past some threshold, as needed
		if(!clusterqueue.isEmpty()) {
			int nummerged = 0;
			// Get the most similar item, add an edge for it, update the similarities and repeat
			CPair topsim = clusterqueue.first();
			Double topsimval = topsim.sim;
			while(topsimval >= threshold) {
				nummerged++;
				
				// Remove from queue
				clusterqueue.remove(topsim);
				
				Node n1 = topsim.node1;
				Node n2 = topsim.node2;
				
				// Handle learning threshold
				boolean ttbreak = false;
				if(tt!=null) {
					ttbreak = tt.addPositivePrediction(copygraph, n1, n2, topsimval);
				}
				
				// Get neighbors of the nodes being merged
				List<Node> n1neighbors = IteratorUtils.iterator2nodelist(n1.getAdjacentGraphItems());
				List<Node> n2neighbors = IteratorUtils.iterator2nodelist(n2.getAdjacentGraphItems());
				
				Set<Node> neighbors = new HashSet<Node>();
				neighbors.addAll(n1neighbors);
				neighbors.addAll(n2neighbors);
				neighbors.remove(n1);
				neighbors.remove(n2);
				
				// Get all the cluster entries that n1 was part of
				Set<CPair> n1clusters = new HashSet<CPair>(node2entry.getCollection(n1));
				for(CPair c:n1clusters) {		
					node2entry.removeItem(c.node1, c);
					node2entry.removeItem(c.node2, c);
					clusterqueue.remove(c);
				}
				
				// Get all the cluster entries that n2 was part of
				Set<CPair> n2clusters = new HashSet<CPair>(node2entry.getCollection(n2));
				for(CPair c:n2clusters) {
					node2entry.removeItem(c.node1, c);
					node2entry.removeItem(c.node2, c);
					clusterqueue.remove(c);
				}
				
				node2entry.remove(n1);
				node2entry.remove(n2);
				
				// Merge nodes
				Node mergednode = this.mergeNodes(copygraph, n1, n2);
				
				// Add clusters to cluster queue for all nodes a merged node
				// is similar to
				Iterator<Edge> nceitr = ncplg.getLinksIteratively(copygraph, mergednode, edgeschemaid);
				this.addPotentialClusters(copygraph, nceitr, clusterqueue, node2entry);
				
				// Update neighbor similarity in queue
				for(Node n:neighbors) {
					Set<CPair> nclusterstmp = new HashSet<CPair>(node2entry.getSet(n));
					
					Set<CPair> nclusters = new HashSet<CPair>(nclusterstmp);
					
					if(!n.isAdjacent(mergednode)) {
						throw new InvalidStateException("After merging, these two should be adjacent: "
								+n+" and "+mergednode);
					}
					
					for(CPair c:nclusters) {
						clusterqueue.remove(c);
						node2entry.removeItem(c.node1,c);
						node2entry.removeItem(c.node2,c);
						
						c.sim = this.getSimilarity(c.node1, c.node2);
						
						clusterqueue.add(c);
						node2entry.addItem(c.node1, c);
						node2entry.addItem(c.node2, c);
					}
				}
				
				// Get the next highest
				// If cluster queue empty by this point
				// or if the maximum number of iterations is reached, break.
				if(ttbreak
					|| clusterqueue.isEmpty()
					|| (this.maxiterations > 0 && nummerged >= this.maxiterations)) {
					Log.DEBUG("Breaking with number of clusters remaining: "
							+clusterqueue.size());
					break;
				}
				
				topsim = clusterqueue.first();
				topsimval = topsim.sim;
			}
		}
		
		// Now go over the merged nodes in the copied graph and add links between
		// the corresponding true nodes in the original graph
		Iterator<Node> nitr = copygraph.getNodes(this.refschemaid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			String refvalue = copygraph.getSystemData(n.getID(), sysdatakey);
			if(refvalue == null) {
				// Skip nodes who aren't the result of merging
				continue;
			}
			
			// Get obj ids
			String[] refids = refvalue.split(",");
			List<ID> objidslist = new ArrayList<ID>();
			for(String idstring:refids) {
				objidslist.add(GraphItemID.parseID(idstring));
			}
			
			// Get all pairwise
			for(int i=0; i<objidslist.size(); i++) {
				for(int j=i+1; j<objidslist.size(); j++) {
					Node n1 = (Node) graph.getEquivalentGraphItem((GraphItemID) objidslist.get(i));
					Node n2 = (Node) graph.getEquivalentGraphItem((GraphItemID) objidslist.get(j));
					
					if(n1 == n2 || n1 == null || n2 == null) {
						throw new InvalidStateException("Unable to get pairs for edge "+n+" with nodes"
								+" n1="+n1+" with id "+new GraphItemID(graph.getID(),
										this.refschemaid,objidslist.get(i).getObjID())
								+" n2="+n2+" with id "+new GraphItemID(graph.getID(),
										this.refschemaid,objidslist.get(j).getObjID()));
					}
					
					graph.addUndirectedEdge(
							GraphItemID.generateGraphItemID(graph, edgeschemaid),
							Arrays.asList(new Node[]{n1,n2}));
				}
			}
		}
		
		// Remove copy
		copygraph.destroy();
		
		Log.DEBUG("Graph After Predicting As Link: "
				+GraphUtils.getSimpleGraphOverview(graph)
				+timer.timeLapse());
	}
	
	/**
	 * Merge nodes in the copy graph.  This was added
	 * since we not only have to do this in the iterative part
	 * but also bootstrapping, if requested.
	 * 
	 * @param graph Graph
	 * @param n1 First node to merge
	 * @param n2 Second node to merge
	 * @return Merged node
	 */
	private Node mergeNodes(Graph graph, Node n1, Node n2) {
		// Merge reffeatureids, as appropriate
		// Get the reference ids stored in the System Data
		String refids = null;
		String currrefids = graph.getSystemData(n1.getID(), sysdatakey);
		if(currrefids==null) {
			// Handle case where the node is not one of the merged nodes
			currrefids = n1.getID().toString();
		}
		refids = currrefids;
		
		currrefids = graph.getSystemData(n2.getID(), sysdatakey);
		if(currrefids==null) {
			// Handle case where the node is not one of the merged nodes
			currrefids = n2.getID().toString();
		}
		refids += ","+currrefids;
		
		// Remove old merge system data
		graph.removeSystemData(n1.getID(), sysdatakey);
		graph.removeSystemData(n2.getID(), sysdatakey);
		
		// Merge cluster
		Node mergednode = Merger.mergeNodes(graph,
				Arrays.asList(new Node[]{n1,n2}),
				fmerger,
				iemerger);
		
		// Set system data of merged node to consist
		// of the references of the nodes it was merged from
		graph.setSystemData(mergednode.getID(), sysdatakey, refids);
		
		return mergednode;
	}
	
	/**
	 * Add clusters that may potentially exists
	 * 
	 * @param copygraph Graph to add clusters for
	 * @param eitr Edge Iterator
	 * @param clusterqueue Queue containing the clusters
	 * @param node2entry Mapping of the nodes to their entries
	 */
	private void addPotentialClusters(Graph copygraph, Iterator<Edge> eitr,
			TreeSet<CPair> clusterqueue, KeyedSet<Node,CPair> node2entry) {
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			
			// Only support binary edges
			if(e.numNodes() != 2) {
				throw new InvalidStateException("Only binary edges supported: "+e
						+" has #nodes="+e.numNodes());
			}
			
			// Get nodes
			Iterator<Node> nitr = e.getAllNodes();
			Node n1 = nitr.next();
			Node n2 = nitr.next();
			
			if(!n1.getSchemaID().equals(this.refschemaid) 
					|| !n2.getSchemaID().equals(this.refschemaid)) {
				throw new InvalidStateException(
						"Iterator should only return edges between nodes of schema id: "
						+this.refschemaid+" not "+n1.getSchemaID()+" or "+n2.getSchemaID());
			}
			
			double sim = this.getSimilarity(n1, n2);
			
			CPair cpair = new CPair(n1,n2,sim);
			clusterqueue.add(cpair);
			node2entry.addItem(n1, cpair);
			node2entry.addItem(n2, cpair);
			
			copygraph.removeEdge(e);
		}
	}
	
	/**
	 * Get the similarity of the nodes in an edge.
	 * 
	 * @param e Edge whose nodes to get similarity of
	 * @return Similarity of the nodes of an edge
	 */
	private double getSimilarity(Node n1, Node n2) {
		return this.nodesim.getNormalizedSimilarity(n1, n2);
	}
	
	/**
	 * Internal class for storing the node pairs in a queue
	 * 
	 * @author namatag
	 *
	 */
	public static class CPair implements Comparable<CPair> {
		public Node node1 = null;
		public Node node2 = null;
		public double sim = Double.NEGATIVE_INFINITY;
		
		public CPair(Node node1, Node node2, double sim) {
			this.node1 = node1;
			this.node2 = node2;
			this.sim = sim;
		}
		
		public int compareTo(CPair obj) {
			if(this == obj) {
				return 0;
			}
			
			CPair oc = (CPair) obj;
			
			if(this.sim < oc.sim) {
				return 1;
			} else if (this.sim > oc.sim) {
				return -1;
			} else {
				String thisobjid = this.node1.getID().getObjID()+"-"+this.node2.getID().getObjID();
				String ocobjid = oc.node1.getID().getObjID()+"-"+oc.node2.getID().getObjID();
				
				return thisobjid.compareTo(ocobjid);
			}
		}	
		
		/**
		 * Objects are equal if their first and second values are equal.
		 */
		public boolean equals(Object obj) {
			// Not strictly necessary, but often a good optimization
		    if (this == obj) {
		      return true;
		    }
		    
		    if (obj == null || !(obj instanceof CPair)) {
		      return false;
		    }
		    
		    CPair oc = (CPair) obj;
		    
		    return this.node1.equals(oc.node1) && this.node2.equals(oc.node2) && this.sim==oc.sim;
		}
		
		public int hashCode() {
		  int hash = 1;
		  hash = hash * 31 + this.node1.hashCode();
		  hash = hash * 31 + this.node2.hashCode();
		  hash = hash * 31 + (int) this.sim;
		  
		  return hash;
		}
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		if(this.hasParameter("saved-alpha")) {
			this.alpha = this.getDoubleParameter("saved-alpha");
		}
		
		String edgeschemaid = this.getStringParameter("saved-edgeschemaid");
		String refschemaid = this.getStringParameter("saved-refschemaid");
		this.initialize(refschemaid, edgeschemaid);
		
		// Override threshold from configuration with whatever was saved
		this.threshold = this.getDoubleParameter("saved-threshold");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.setParameter("saved-refschemaid", this.refschemaid);
		this.setParameter("saved-threshold", this.threshold);
		if(this.alpha!=null) {
			this.setParameter("saved-alpha", this.alpha);
		}
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}

	public void shouldBootstrap(boolean bootstrap) {
		this.shouldbootstrap = bootstrap;
	}

	public void learn(Graph graph, Iterable<Edge> knownedges,
			String edgeschemaid, String refschemaid, String existfeature) {
		if(initialize) {
			this.initialize(refschemaid, edgeschemaid);
		}
		
		// Set f-value of the f-measure to optimize for
		fvalue = 1;
		if(this.hasParameter("fvalue")) {
			fvalue = this.getDoubleParameter("fvalue");
		}
		
		if(this.hasYesNoParameter("learnalphaandthreshold", "yes") 
				|| this.hasYesNoParameter("learnthreshold", "yes")) {
			throw new UndefinedFunctionException("Relational Clustering is unable to learn" +
			" thresholds using only co-referent pairs.");
		}
	}
}
