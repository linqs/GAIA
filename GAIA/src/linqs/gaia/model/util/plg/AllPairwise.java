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
package linqs.gaia.model.util.plg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.similarity.NodeSimilarity;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.util.Dynamic;

/**
 * Generate all non-existent pairwise links for the given graph.
 * For undirected edges, this proposes all non-existent link between all
 * pairs of nodes with the given schema id.
 * For directed links, this proposes an edge for every possible pair of
 * nodes where one node is in the source and one is in the target.
 * Note that in directed edges, edges a to b and b to a are proposed.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> Required if the edge were generating is undirected:
 *   <UL>
 *   <LI> nodeschemaid-Schema ID of nodes in edge
 *   </UL>
 * <LI> Required if the edge were generating is directed:
 *   <UL>
 *   <LI> sourceschemaid-Schema ID of nodes in the source of the edge
 *   <LI> targetschemaid-Schema ID of nodes in the target of the edge
 *   </UL>
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> setnotexist-If set to yes, predicted links when using addAllLinks are initialized
 * with the value of NOTEXIST.
 * <LI> factorpotential-Factor of potential edges to return relative to known edges
 * (i.e., value of 2 for a graph with 10 known edges means return 20 potential edges)
 * <LI> numberpotential-Number of potential edges to return
 * <LI> numpotentialparam - Name for Java property which contains the number
 * of potential edges to return, as set earlier (e.g., number of removed edges by some noise generator).
 * <LI> randomsample-If yes, return a random subset of the potential edges.
 * NOTE: This may become very slow as nodes are randomly chosen and tested.
 * This also requires more memory since it needs to keep track of which
 * edges have also been proposed.
 *   <UL>
 *   <LI> seed-Seed for random sample.  Default is 0.
 *   </UL>
 * <LI> nodefvpair-Constraint on which nodes to use.  Given a comma delimited
 * set of feature=value pairs, only propose edges between nodes
 * which match the features and string value specified (i.e., label=red,length=5.0).
 * You can also specify GAIA-KNOWN (i.e., label=GAIA-KNOWN) to specify you want nodes where the value
 * of the feature is known and GAIA-UNKNOWN (i.e., label=GAIA-UNKNOWN) to specify
 * you want the nodes where the value of the feature is not known.
 * <LI> sourcefvpair-Same as nodefvpair but over the source nodes in directed edges.
 * <LI> targetfvpair-Same as nodefvpair but over the target nodes in directed edges.
 * <LI> selflink-If "yes", allow self links (i.e., allow node to link to self).  Default is "no".
 * <LI> homophilylinks-The links being generated are homophily links (link items
 * with the same value).  If set to "knownonly", only propose links between
 * nodes with the specified feature value known.  If set to "unknownonly", only propose
 * links between nodes where at least one node has the feature value unknown.
 *   <UL>
 *   <LI>homophilyfeatureid-Feature ID of feature to link homophily links over
 *   </UL>
 * <LI> binfeaturevalue-Feature ID of feature to bin by.  If the "binvaluedistance" is defined,
 * where the feature is a numeric feature, only nodes with where the absolute
 * difference between the values is less than the binvaluedistance is compared.
 * If not, only two nodes with the same bin value are ever returned as a possible edge. 
 * <LI> binvaluedistance-Distance to use for binfeaturevalue.  If this parameters
 * is set and binfeaturevalue is not numeric, throw an exception.
 * <LI> nodesimclass-{@link NodeSimilarity} class to compute similarity
 * of pairs of nodes.  Only nodes whose similarity is above some threshold,
 * specified by the "nodesimthreshold" parameter, are proposed as potential links.
 * <LI> usenormalizednsim-If "yes", use the normalized node similarity.
 * Otherwise, use the unnormalized node similarity.  Default is "yes".
 * <LI> nodesimthreshold-Threshold to use with "nodesimclass".  Only pairs of nodes
 * whose node similiarity is greater than this threshold can have a potential link.
 * <UL>
 * 
 * @author namatag
 *
 */
public class AllPairwise extends BaseConfigurable implements PotentialLinkGenerator, NodeCentricPLG {
	private boolean isInitialized = false;

	private Map<String, String> nodefv = null;
	private Map<String, String> sourcefv = null;
	private Map<String, String> targetfv = null;
	private String homophilylinks = null;
	private String homophilyfeatureid = null;
	private String binfeaturevalue = null;
	private Double binvaluedistance = null;
	private boolean selflink = false;
	private NodeSimilarity nnsim = null;
	private Double nodesimthreshold = null;
	private boolean usenormalizednsim = true;
	private Integer checkpoint = null;

	private boolean randomsample = false;
	private int seed = 0;

	int numberpotential = -1;
	double factorpotential = -1;

	public void initialize(Graph g, String edgeschemaid) {
		// Initialize only once
		isInitialized = true;

		// Get parameters
		if(this.hasParameter("numpotentialparam")) {
			String propname = this.getStringParameter("numpotentialparam");
			String propvalue = System.getProperty(propname);
			numberpotential = Integer.parseInt(propvalue);
		}else if(this.hasParameter("numberpotential")) {
			numberpotential = (int) this.getDoubleParameter("numberpotential");
		} else if(this.hasParameter("factorpotential")) {
			factorpotential = this.getDoubleParameter("factorpotential");
		}

		randomsample = false;
		if(this.hasParameter("randomsample", "yes")) {
			randomsample = true;
		}

		seed = 0;
		if(this.hasParameter("seed")) {
			seed = (int) this.getDoubleParameter("seed");
		}

		if(this.hasParameter("nodefvpair")) {
			this.nodefv = this.parseFVPairs(this.getStringParameter("nodefvpair"));
		}

		if(this.hasParameter("sourcefvpair")) {
			this.sourcefv = this.parseFVPairs(this.getStringParameter("sourcefvpair"));
		}

		if(this.hasParameter("targetfvpair")) {
			this.targetfv = this.parseFVPairs(this.getStringParameter("targetfvpair"));
		}

		if(this.hasParameter("selflink")) {
			String selflink = this.getStringParameter("selflink");
			if(selflink.equals("yes")) {
				this.selflink = true;
			} else if(selflink.equals("node")) {
				this.selflink = true;
			} else {
				throw new ConfigurationException("Unsupported type: "+selflink);
			}
		}

		if(this.hasParameter("homophilylinks")) {
			this.homophilylinks = this.getCaseParameter("homophilylinks",
					new String[]{"knownonly","unknownonly"});

			this.homophilyfeatureid = this.getStringParameter("homophilyfeatureid");
		}

		if(this.hasParameter("binfeaturevalue")) {
			this.binfeaturevalue = this.getStringParameter("binfeaturevalue");
			
			// If the bin value is less than something
			if(this.hasParameter("binvaluedistance")) {
				this.binvaluedistance = this.getDoubleParameter("binvaluedistance");
			}
		}

		if(this.hasParameter("nodesimclass")) {
			nnsim = (NormalizedNodeSimilarity) Dynamic.forConfigurableName(NormalizedNodeSimilarity.class,
					this.getStringParameter("nodesimclass"),
					this);
			
			nodesimthreshold = this.getDoubleParameter("nodesimthreshold");
			usenormalizednsim = this.getYesNoParameter("usenormalizednsim", "yes");
		}
		
		if(this.hasParameter("checkpoint")) {
			checkpoint = this.getIntegerParameter("checkpoint");
		}
	}

	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		if(!isInitialized) {
			initialize(g, edgeschemaid);
		}

		int maxreturn = Integer.MAX_VALUE;
		if(numberpotential != -1) {
			maxreturn = numberpotential;
		} else if(factorpotential != -1) {
			maxreturn = (int) (g.numGraphItems(edgeschemaid) * factorpotential);

			Log.DEBUG("Returning only "+maxreturn+" given "+g.numGraphItems(edgeschemaid)+
					" with a factor of "+factorpotential);
		}

		Schema schema = g.getSchema(edgeschemaid);

		// Create custom iterator
		if(schema.getType().equals(SchemaType.UNDIRECTED)) {
			String nsid = this.getStringParameter("nodeschemaid");

			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, nsid);

			List<Node> nodes = new LinkedList<Node>();
			Iterator<GraphItem> itr = g.getGraphItems(nsid);
			while(itr.hasNext()) {
				Node n = (Node) itr.next();
				if(this.matchfv(n, nodefv)) {
					// Check to see the bin value is defined for all items
					if(this.binfeaturevalue != null
							&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
						throw new InvalidStateException("Bin feature "+this.binfeaturevalue
								+" not defined for "+n);
					}

					nodes.add(n);
				}
			}

			return new UndirectedIterator(g, edgeschemaid, nodes, maxreturn);
		} else if(schema.getType().equals(SchemaType.DIRECTED)) {
			String snsid = this.getStringParameter("sourceschemaid");
			String tnsid = this.getStringParameter("targetschemaid");

			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, snsid);
			checkForBinFeature(g, tnsid);

			List<Node> sources = new LinkedList<Node>();
			Iterator<GraphItem> itr = g.getGraphItems(snsid);
			while(itr.hasNext()) {
				Node n = (Node) itr.next();
				if(this.matchfv(n, sourcefv)) {
					// Check to see the bin value is defined for all items
					if(this.binfeaturevalue != null
							&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
						throw new InvalidStateException("Bin feature "+this.binfeaturevalue
								+" not defined for "+n);
					}

					sources.add(n);
				}
			}

			List<Node> targets = new LinkedList<Node>();
			itr = g.getGraphItems(tnsid);
			while(itr.hasNext()) {
				Node n = (Node) itr.next();
				if(this.matchfv(n, targetfv)) {
					// Check to see the bin value is defined for all items
					if(this.binfeaturevalue != null
							&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
						throw new InvalidStateException("Bin feature "+this.binfeaturevalue
								+" not defined for "+n);
					}

					targets.add(n);
				}
			}

			return new DirectedIterator(g, edgeschemaid, sources, targets, maxreturn);
		} else {
			throw new ConfigurationException("Unsupported edge schema type: "
					+edgeschemaid+" of type "+schema.getType());
		}
	}

	private void checkForBinFeature(Graph g, String schemaid) {
		if(this.binfeaturevalue == null){
			return;
		}

		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(this.binfeaturevalue)) {
			throw new ConfigurationException("Bin value not defined for schema: "
					+this.binfeaturevalue+" from "+schemaid);
		}
	}

	private Map<String, String> parseFVPairs(String fvpair) {
		String[] parts = fvpair.split(",");
		Map<String, String> pairs = new HashMap<String, String>(parts.length);

		for(String part:parts) {
			String[] pair = part.split("=");
			pairs.put(pair[0], pair[1]);
		}

		return pairs;
	}

	private boolean matchfv(Node n, Map<String, String> fvpairs) {
		if(fvpairs==null) {
			return true;
		}

		Set<Entry<String, String>> pairs = fvpairs.entrySet();
		for(Entry<String, String> p: pairs) {
			String val = p.getValue();
			FeatureValue currfval = n.getFeatureValue(p.getKey());

			if(val.equals("GAIA-KNOWN") && currfval.equals(FeatureValue.UNKNOWN_VALUE)) {
				return false;
			} else if(val.equals("GAIA-UNKNOWN") && !currfval.equals(FeatureValue.UNKNOWN_VALUE)) {
				return false;
			} else if(!currfval.getStringValue().equals(val)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Iterator for handling directed edge prediction.
	 * 
	 * @author namatag
	 *
	 */
	private class DirectedIterator implements Iterator<Edge> {
		private int sindex = 0;
		private int tindex = 0;
		private int ssize = 0;
		private int tsize = 0;
		private List<Node> nextpair = null;
		private int numreturned = 0;

		private String edgeschemaid = null;
		private Graph graph = null;
		private List<Node> sources = null;
		private List<Node> targets = null;

		private int maxreturn = Integer.MAX_VALUE;

		private Random rand = null;
		private HashSet<String> considered = new HashSet<String>();

		public DirectedIterator(Graph g, String edgeschemaid,
				List<Node> sources, List<Node> targets, int maxreturn) {
			this.graph = g;
			this.edgeschemaid = edgeschemaid;
			this.sources = sources;
			this.targets = targets;

			this.maxreturn = maxreturn;
			this.rand = new Random(seed);

			this.ssize = sources.size();
			this.tsize = targets.size();

			if(this.ssize == 0 || this.tsize == 0 || maxreturn<1) {
				this.nextpair = null;
			} else {
				this.nextpair = this.nextPair();
			}
		}

		public boolean hasNext() {
			// If there is no available next item, return false.
			boolean hasnext = nextpair != null;

			if(!hasnext) {
				this.graph = null;
				this.sources = null;
				this.targets = null;
				this.considered = null;
			}

			return hasnext;
		}

		public Edge next() {
			// Return null if there are no edges left
			if(this.nextpair == null) {
				return null;
			}

			// Save last value
			List<Node> lastpair = this.nextpair;
			this.numreturned++;

			// Get next value
			// Note: Do "<" instead of "<=" since at least one edge is added
			// by the initialization.
			if(this.numreturned<maxreturn) {
				this.nextpair = this.nextPair();
			} else {
				this.nextpair = null;
			}
			
			if(checkpoint!=null && numreturned % checkpoint == 0) {
				Log.INFO("Number of edges returned: "+numreturned);
			}

			GraphItemID id = GraphItemID.generateGraphItemID(this.graph, edgeschemaid);
			List<Node> currsource = new LinkedList<Node>();
			currsource.add(lastpair.get(0));
			List<Node> currtarget = new LinkedList<Node>();
			currtarget.add(lastpair.get(1));
			
			return this.graph.addDirectedEdge(id, currsource.iterator(), currtarget.iterator());
		}

		/**
		 * Return the next pair.  It returns null if there is none.
		 * 
		 * @return
		 */
		private List<Node> nextPair() {
			List<Node> nextpair = null;

			while(nextpair == null) {
				// Get candidate pair
				if(randomsample) {
					// Randomly select an index pair not previously considered
					while(true) {
						sindex = rand.nextInt(ssize);
						tindex = rand.nextInt(tsize);

						String key = sindex+"-"+tindex;
						if(!this.considered.contains(key)) {
							this.considered.add(key);
							break;
						}
					}
				} else {
					// Stop when all potential have
					// been considered
					if(sindex >= ssize || tindex >= tsize) {
						return null;
					}
				}

				Node n1 = sources.get(sindex);
				Node n2 = targets.get(tindex);

				// Set indices for next query
				tindex++;
				if(tindex == tsize) {
					sindex++;
					tindex = 0;
				}

				// Check if the pair already shares a link of the given type
				if((!selflink && n1.equals(n2)) || n1.isAdjacentTarget(n2, edgeschemaid)) {
					continue;
				}

				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				if(binfeaturevalue != null) {
					if(binvaluedistance != null) {
						NumValue n1val = (NumValue) n1.getFeatureValue(binfeaturevalue);
						NumValue n2val = (NumValue) n2.getFeatureValue(binfeaturevalue);

						if(Math.abs(n1val.getNumber() - n2val.getNumber()) > binvaluedistance) {
							continue;
						}
					} else if(!n1.getFeatureValue(binfeaturevalue).equals(n2.getFeatureValue(binfeaturevalue))) {
						continue;
					}
				}
				
				// Handle node similarity
				if(nnsim!=null) {
					if(getNodeSimilarity(n1, n2) < nodesimthreshold) {
						continue;
					}
				}
				
				// Handle homophilic links
				if(homophilylinks!=null) {
					if(homophilylinks.equals("knownonly")
							&& (!n1.hasFeatureValue(homophilyfeatureid)
									|| !n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else if(homophilylinks.equals("unknownonly")
							&& n1.hasFeatureValue(homophilyfeatureid)
							&& n2.hasFeatureValue(homophilyfeatureid))
					{
						continue;
					} else {
						// This pair is a potential pair
					}
				}

				// If you get to here,
				// the pair doesn't already share a link
				// so predict it.
				nextpair = new LinkedList<Node>();
				nextpair.add(n1);
				nextpair.add(n2);
			}

			return nextpair;
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}
	}

	/**
	 * Iterator for handling undirected edge prediction.
	 * 
	 * @author namatag
	 *
	 */
	private class UndirectedIterator implements Iterator<Edge> {
		private int index1 = 0;
		private int index2 = 0;
		private int nsize = 0;
		private List<Node> nextpair = null;

		private Graph graph = null;
		private String edgeschemaid = null;
		private List<Node> nodes = null;
		private int numreturned = 0;

		private int maxreturn;

		private Random rand = null;
		private Set<String> considered = new HashSet<String>();

		public UndirectedIterator(Graph g, String edgeschemaid, List<Node> nodes, int maxreturn) {

			this.graph = g;
			this.edgeschemaid = edgeschemaid;
			this.nodes = nodes;

			this.maxreturn = maxreturn;
			this.rand = new Random(seed);

			this.nsize = nodes.size();

			if(this.nsize == 0 || maxreturn<1) {
				this.nextpair = null;
			} else {
				this.nextpair = this.nextPair();
			}
		}

		public boolean hasNext() {
			// If there is no available next item, return false.
			boolean hasnext = nextpair != null;

			if(!hasnext) {
				this.graph = null;
				this.nodes = null;
				this.considered = null;
			}

			return hasnext;
		}

		public Edge next() {
			// Return null if there are no edges left
			if(this.nextpair == null) {
				return null;
			}

			// Save last value
			List<Node> lastpair = this.nextpair;
			this.numreturned++;
			
			// Get next value
			// Note: Do "<" instead of "<=" since at least one edge is added
			// by the initialization.
			if(this.numreturned<maxreturn) {
				this.nextpair = this.nextPair();
			} else {
				this.nextpair = null;
			}
			
			if(checkpoint!=null && numreturned % checkpoint == 0) {
				Log.INFO("Number of edges returned: "+numreturned);
			}

			GraphItemID id = GraphItemID.generateGraphItemID(graph, edgeschemaid);
			return graph.addUndirectedEdge(id, lastpair.iterator());
		}

		/**
		 * Return the next pair.  It returns null if there is none.
		 * 
		 * @return
		 */
		private List<Node> nextPair() {
			List<Node> nextpair = null;

			while(nextpair == null) {
				// Get candidate pair
				if(randomsample) {
					// Randomly select an index pair not previously considered
					while(true) {
						index1 = rand.nextInt(nsize);
						index2 = rand.nextInt(nsize);

						// Order key such that the
						// lower index of the two is in the front
						String key = null;
						if(index1>index2) {
							key = index2+"-"+index1;
						} else {
							key = index1+"-"+index2;
						}

						if(!this.considered.contains(key)) {
							this.considered.add(key);
							break;
						}
					}
				} else {
					// Stop when all potential have
					// been considered
					if(index1 >= nsize || index2 >= nsize) {
						return null;
					}
				}

				Node n1 = nodes.get(index1);
				Node n2 = nodes.get(index2);

				// Update index for next time
				index2++;
				if(index2 == nsize) {
					index1++;
					index2 = index1 + 1;
				}

				// Check if the pair already shares a link of the given type
				if(((!selflink && n1.equals(n2))) ||n1.isAdjacent(n2, edgeschemaid)) {
					continue;
				}

				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				if(binfeaturevalue != null) {
					if(binvaluedistance != null) {
						NumValue n1val = (NumValue) n1.getFeatureValue(binfeaturevalue);
						NumValue n2val = (NumValue) n2.getFeatureValue(binfeaturevalue);

						if(Math.abs(n1val.getNumber() - n2val.getNumber()) > binvaluedistance) {
							continue;
						}
					} else if(!n1.getFeatureValue(binfeaturevalue).equals(n2.getFeatureValue(binfeaturevalue))) {
						continue;
					}
				}
				
				// Handle node similarity
				if(nnsim!=null) {
					if(getNodeSimilarity(n1, n2) < nodesimthreshold) {
						continue;
					}
				}

				// Handle homophilic links
				if(homophilylinks!=null) {
					if(homophilylinks.equals("knownonly")
							&& (!n1.hasFeatureValue(homophilyfeatureid)
									|| !n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else if(homophilylinks.equals("unknownonly")
							&& (n1.hasFeatureValue(homophilyfeatureid)
									&& n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else {
						// This pair is a potential pair
					}
				}

				// If you get to here,
				// the pair doesn't already share a link
				// so predict it.
				nextpair = new LinkedList<Node>();
				nextpair.add(n1);
				nextpair.add(n2);
			}

			return nextpair;
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}
	}
	
	/**
	 * This method returns the possible edges existing which include a node.
	 * For undirected, this is all pairs which include this node.
	 * For directed, this is all pairs where this node is a source and
	 * all pairs where this node is a target.
	 */
	public Iterator<Edge> getLinksIteratively(Graph g, Node cn,
			String edgeschemaid) {
		if(!isInitialized) {
			initialize(g, edgeschemaid);
		}

		int maxreturn = Integer.MAX_VALUE;
		if(numberpotential != -1) {
			maxreturn = numberpotential;
		} else if(factorpotential != -1) {
			maxreturn = (int) (g.numGraphItems(edgeschemaid) * factorpotential);

			Log.DEBUG("Returning only "+maxreturn+" given "+g.numGraphItems(edgeschemaid)+
					" with a factor of "+factorpotential);
		}

		Schema schema = g.getSchema(edgeschemaid);

		// Create custom iterator
		if(schema.getType().equals(SchemaType.UNDIRECTED)) {
			String nsid = this.getStringParameter("nodeschemaid");

			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, nsid);

			List<Node> nodes = new LinkedList<Node>();
			// Check current node
			if(this.matchfv(cn, nodefv)) {
				// Check to see the bin value is defined for all items
				if(this.binfeaturevalue != null
						&& cn.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
					throw new InvalidStateException("Bin feature "+this.binfeaturevalue
							+" not defined for "+cn);
				}

				// Process possible neighbors of current
				Iterator<GraphItem> itr = g.getGraphItems(nsid);
				while(itr.hasNext()) {
					Node n = (Node) itr.next();

					if(this.matchfv(n, nodefv)) {
						// Check to see the bin value is defined for all items
						if(this.binfeaturevalue != null
								&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
							throw new InvalidStateException("Bin feature "+this.binfeaturevalue
									+" not defined for "+n);
						}

						nodes.add(n);
					}
				}
			}

			return new NCUndirectedIterator(g, edgeschemaid, cn, nodes, maxreturn);
		} else if(schema.getType().equals(SchemaType.DIRECTED)) {
			String snsid = this.getStringParameter("sourceschemaid");
			String tnsid = this.getStringParameter("targetschemaid");

			// Check to see if the feature for the bin value is specified
			checkForBinFeature(g, snsid);
			checkForBinFeature(g, tnsid);

			List<Node> sources = new LinkedList<Node>();
			List<Node> targets = new LinkedList<Node>();

			// Check central node
			if(this.matchfv(cn, targetfv)) {
				// Check to see the bin value is defined for all items
				if(this.binfeaturevalue != null
						&& cn.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
					throw new InvalidStateException("Bin feature "+this.binfeaturevalue
							+" not defined for "+cn);
				}
				
				// Fill sources if the cn is of the target schema id type
				if(cn.getSchemaID().equals(tnsid)) {
					Iterator<GraphItem> itr = g.getGraphItems(snsid);
					while(itr.hasNext()) {
						Node n = (Node) itr.next();
						if(this.matchfv(n, sourcefv)) {
							// Check to see the bin value is defined for all items
							if(this.binfeaturevalue != null
									&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
								throw new InvalidStateException("Bin feature "+this.binfeaturevalue
										+" not defined for "+n);
							}

							sources.add(n);
						}
					}
				}
			}
			
			if(this.matchfv(cn, sourcefv)) {
				// Check to see the bin value is defined for all items
				if(this.binfeaturevalue != null
						&& cn.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
					throw new InvalidStateException("Bin feature "+this.binfeaturevalue
							+" not defined for "+cn);
				}

				// Fill targets if the cn is of the source schema id type
				if(cn.getSchemaID().equals(snsid)) {
					Iterator<GraphItem> itr = g.getGraphItems(tnsid);
					while(itr.hasNext()) {
						Node n = (Node) itr.next();
						if(this.matchfv(n, targetfv)) {
							// Check to see the bin value is defined for all items
							if(this.binfeaturevalue != null
									&& n.getFeatureValue(this.binfeaturevalue).equals(FeatureValue.UNKNOWN_VALUE)) {
								throw new InvalidStateException("Bin feature "+this.binfeaturevalue
										+" not defined for "+n);
							}

							targets.add(n);
						}
					}
				}
			}

			return new NCDirectedIterator(g, edgeschemaid, cn, sources, targets, maxreturn);
		} else {
			throw new ConfigurationException("Unsupported edge schema type: "
					+edgeschemaid+" of type "+schema.getType());
		}
	}
	
	/**
	 * Iterator for handling directed edge prediction.
	 * 
	 * @author namatag
	 *
	 */
	public class NCDirectedIterator implements Iterator<Edge> {
		private int sindex = 0;
		private int tindex = 0;
		private int ssize = 0;
		private int tsize = 0;
		private List<Node> nextpair = null;
		private int numreturned = 0;

		private String edgeschemaid = null;
		private Graph graph = null;
		private List<Node> sources = null;
		private List<Node> targets = null;

		private int maxreturn;

		private Random rand = null;
		private Set<Integer> consideredsources = new HashSet<Integer>();
		private Set<Integer> consideredtargets = new HashSet<Integer>();
		
		private Node cn = null;
		private boolean isTarget = true;

		public NCDirectedIterator(Graph g, String edgeschemaid, Node cn,
				List<Node> sources, List<Node> targets, int maxreturn) {
			this.graph = g;
			this.edgeschemaid = edgeschemaid;
			this.sources = sources;
			this.targets = targets;
			this.cn = cn;

			this.maxreturn = maxreturn;
			this.rand = new Random(seed);

			this.ssize = sources.size();
			this.tsize = targets.size();

			if(this.ssize == 0 || this.tsize == 0 || maxreturn<1) {
				this.nextpair = null;
			} else {
				this.nextpair = this.nextPair();
			}
		}

		public boolean hasNext() {
			// If there is no available next item, return false.
			boolean hasnext = nextpair != null;

			if(!hasnext) {
				this.graph = null;
				this.sources = null;
				this.targets = null;
				this.consideredsources = null;
				this.consideredtargets = null;
			}

			return hasnext;
		}

		public Edge next() {
			// Return null if there are no edges left
			if(this.nextpair == null) {
				return null;
			}

			// Save last value
			List<Node> lastpair = this.nextpair;
			this.numreturned++;
			
			// Get next value
			// Note: Do "<" instead of "<=" since at least one edge is added
			// by the initialization.
			if(this.numreturned<maxreturn) {
				this.nextpair = this.nextPair();
			} else {
				this.nextpair = null;
			}
			
			if(checkpoint!=null && numreturned % checkpoint == 0) {
				Log.INFO("Number of edges returned: "+numreturned);
			}

			GraphItemID id = GraphItemID.generateGraphItemID(this.graph, edgeschemaid);
			List<Node> currsource = new LinkedList<Node>();
			currsource.add(lastpair.get(0));
			List<Node> currtarget = new LinkedList<Node>();
			currtarget.add(lastpair.get(1));
			
			return this.graph.addDirectedEdge(id, currsource.iterator(), currtarget.iterator());
		}

		/**
		 * Return the next pair.  It returns null if there is none.
		 * 
		 * @return
		 */
		private List<Node> nextPair() {
			List<Node> nextpair = null;
			
			while(nextpair == null) {
				// Get candidate pair
				if(randomsample) {
					// Randomly select an index pair not previously considered
					while(true) {
						isTarget = rand.nextBoolean();
						
						if(isTarget) {
							if(sources.isEmpty()) {
								continue;
							}
							
							sindex = rand.nextInt(ssize);
							if(!this.consideredsources.contains(sindex)) {
								this.consideredsources.add(sindex);
								break;
							}
						} else {
							if(targets.isEmpty()) {
								continue;
							}
							
							tindex = rand.nextInt(tsize);
							if(!this.consideredtargets.contains(tindex)) {
								this.consideredtargets.add(tindex);
								break;
							}
						}
					}
				} else {
					// Stop when all potential have
					// been considered
					if(sindex >= ssize && tindex >= tsize) {
						return null;
					}
				}
				
				Node n1 = null;
				Node n2 = null;
				
				// Propose a possible pair
				if(sources.isEmpty()) {
					isTarget = false;
				}
				
				if(isTarget) {
					// Do all variants where the cn is a target first
					n1 = sources.get(sindex);
					n2 = cn;
					sindex++;
					
					if(sindex >= ssize) {
						isTarget = false;
					}
				} else {
					// Do all variants where the cn is a source
					n1 = cn;
					n2 = targets.get(tindex);
					tindex++;
				}
				
				// Check if the pair already shares a link of the given type
				if((!selflink && n1.equals(n2)) || n1.isAdjacentTarget(n2, edgeschemaid)) {
					continue;
				}

				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				if(binfeaturevalue != null) {
					if(binvaluedistance != null) {
						NumValue n1val = (NumValue) n1.getFeatureValue(binfeaturevalue);
						NumValue n2val = (NumValue) n2.getFeatureValue(binfeaturevalue);

						if(Math.abs(n1val.getNumber() - n2val.getNumber()) > binvaluedistance) {
							continue;
						}
					} else if(!n1.getFeatureValue(binfeaturevalue).equals(n2.getFeatureValue(binfeaturevalue))) {
						continue;
					}
				}
				
				// Handle node similarity
				if(nnsim!=null) {
					if(getNodeSimilarity(n1, n2) < nodesimthreshold) {
						continue;
					}
				}

				// Handle homophilic links
				if(homophilylinks!=null) {
					if(homophilylinks.equals("knownonly")
							&& (!n1.hasFeatureValue(homophilyfeatureid)
									|| !n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else if(homophilylinks.equals("unknownonly")
							&& n1.hasFeatureValue(homophilyfeatureid)
							&& n2.hasFeatureValue(homophilyfeatureid))
					{
						continue;
					} else {
						// This pair is a potential pair
					}
				}

				// If you get to here,
				// the pair doesn't already share a link
				// so predict it.
				nextpair = new LinkedList<Node>();
				nextpair.add(n1);
				nextpair.add(n2);
			}

			return nextpair;
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}
	}

	/**
	 * Iterator for handling node centric undirected edge prediction.
	 * 
	 * @author namatag
	 *
	 */
	public class NCUndirectedIterator implements Iterator<Edge> {
		private int index2 = 0;
		private int nsize = 0;
		private List<Node> nextpair = null;

		private Graph graph = null;
		private String edgeschemaid = null;
		private List<Node> nodes = null;
		private int numreturned = 0;

		private int maxreturn;

		private Random rand = null;
		private Set<Integer> considered = new HashSet<Integer>();

		private Node cn = null;

		public NCUndirectedIterator(Graph g, String edgeschemaid, Node cn,
				List<Node> nodes, int maxreturn) {

			this.graph = g;
			this.edgeschemaid = edgeschemaid;
			this.nodes = nodes;
			this.cn = cn;

			this.maxreturn = maxreturn;
			this.rand = new Random(seed);

			this.nsize = nodes.size();
			
			if(this.nsize == 0 || maxreturn<1) {
				this.nextpair = null;
			} else {
				this.nextpair = this.nextPair();
			}
		}

		public boolean hasNext() {
			// If there is no available next item, return false.
			boolean hasnext = nextpair != null;

			if(!hasnext) {
				this.graph = null;
				this.nodes = null;
				this.considered = null;
			}

			return hasnext;
		}

		public Edge next() {
			// Return null if there are no edges left
			if(this.nextpair == null) {
				return null;
			}

			// Save last value
			List<Node> lastpair = this.nextpair;
			this.numreturned++;
			
			// Get next value
			// Note: Do "<" instead of "<=" since at least one edge is added
			// by the initialization.
			if(this.numreturned<maxreturn) {
				this.nextpair = this.nextPair();
			} else {
				this.nextpair = null;
			}
			
			if(checkpoint!=null && numreturned % checkpoint == 0) {
				Log.INFO("Number of edges returned: "+numreturned);
			}
			
			GraphItemID id = GraphItemID.generateGraphItemID(graph, edgeschemaid);
			return graph.addUndirectedEdge(id, lastpair.iterator());
		}

		/**
		 * Return the next pair.  It returns null if there is none.
		 * 
		 * @return
		 */
		private List<Node> nextPair() {
			List<Node> nextpair = null;

			while(nextpair == null) {
				// Get candidate pair
				if(randomsample) {
					// Randomly select an index pair not previously considered
					while(true) {
						index2 = rand.nextInt(nsize);

						if(!this.considered.contains(index2)) {
							this.considered.add(index2);
							break;
						}
					}
				} else {
					// Stop when all potential have
					// been considered
					if(index2 >= nsize) {
						return null;
					}
				}

				Node n1 = cn;
				Node n2 = nodes.get(index2);

				// Update index for next time
				index2++;

				// Check if the pair already shares a link of the given type
				if(((!selflink && n1.equals(n2))) || n1.isAdjacent(n2, edgeschemaid)) {
					continue;
				}

				// If a bin value is defined, only consider those with the
				// same value for the given bin feature
				if(binfeaturevalue != null) {
					if(binvaluedistance != null) {
						NumValue n1val = (NumValue) n1.getFeatureValue(binfeaturevalue);
						NumValue n2val = (NumValue) n2.getFeatureValue(binfeaturevalue);

						if(Math.abs(n1val.getNumber() - n2val.getNumber()) > binvaluedistance) {
							continue;
						}
					} else if(!n1.getFeatureValue(binfeaturevalue).equals(n2.getFeatureValue(binfeaturevalue))) {
						continue;
					}
				}
				
				// Handle node similarity
				if(nnsim!=null) {
					if(getNodeSimilarity(n1, n2) < nodesimthreshold) {
						continue;
					}
				}

				// Handle homophilic links
				if(homophilylinks!=null) {
					if(homophilylinks.equals("knownonly")
							&& (!n1.hasFeatureValue(homophilyfeatureid)
									|| !n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else if(homophilylinks.equals("unknownonly")
							&& (n1.hasFeatureValue(homophilyfeatureid)
									&& n2.hasFeatureValue(homophilyfeatureid)))
					{
						continue;
					} else {
						// This pair is a potential pair
					}
				}

				// If you get to here,
				// the pair doesn't already share a link
				// so predict it.
				nextpair = new LinkedList<Node>();
				nextpair.add(n1);
				nextpair.add(n2);
			}

			return nextpair;
		}

		public void remove() {
			throw new InvalidStateException("Remove feature unsupported");
		}
	}
	
	private double getNodeSimilarity(Node n1, Node n2) {
		if(usenormalizednsim) {
			return ((NormalizedNodeSimilarity) nnsim).getNormalizedSimilarity(n1, n2);
		} else {
			return nnsim.getSimilarity(n1, n2);
		}
	}
	
	public void addAllLinks(Graph g, String edgeschemaid) {
		PLGUtils.addAllLinks(g, edgeschemaid, this.getLinksIteratively(g, edgeschemaid));
	}

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature, boolean setnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature,
				this.getLinksIteratively(g, edgeschemaid), setnotexist);
	}
}
