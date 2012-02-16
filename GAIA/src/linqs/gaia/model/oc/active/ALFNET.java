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
package linqs.gaia.model.oc.active;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.BootstrapModel;
import linqs.gaia.model.gd.GroupDetection;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.Entropy;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.TopK;
import linqs.gaia.util.WeightedSampler;

public class ALFNET extends BaseModel implements ActiveLearning, BootstrapModel {
	private static final long serialVersionUID = 1L;
	private String targetschemaid;
	private String targetfeatureid;
	private Random rand;
	private boolean bootstrap;
	private boolean cacheclusters = false;
	private Map<GraphID,List<Set<Node>>> gid2clusters = null;
	private String localfid = null;
	private List<String> categories = null;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		int seed = this.getIntegerParameter("seed",0);
		rand = new Random(seed);
		
		localfid = this.getStringParameter("localfid");
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}

	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		Set<Node> nodes2query = new HashSet<Node>(numqueries);
		
		Set<Graph> graphs = new HashSet<Graph>();
		Set<Node> testnodes = new HashSet<Node>();
		for(Decorable d:testitems) {
			if(!(d instanceof Node)) {
				throw new UnsupportedTypeException("Active learning method only applicable to nodes: "
						+d.getClass().getCanonicalName());
			}
			
			testnodes.add((Node) d);
			graphs.add(((Node) d).getGraph());
		}
		
		if(graphs.size()!=1) {
			throw new UnsupportedTypeException("Active learning method only applicable to nodes in the same graph: "
					+graphs);
		}
		Graph g = graphs.iterator().next();
		
		if(categories==null) {
			categories = ((CategFeature) g.getSchema(targetschemaid).getFeature(targetfeatureid)).getAllCategories().copyAsList();
		}
		
		List<Set<Node>> clusters = null;
		if(cacheclusters && this.gid2clusters.containsKey(g.getID())) {
			clusters = new ArrayList<Set<Node>>(this.gid2clusters.get(g));
		} else {
			// Load and apply clustering algorithm
			String gdclass = this.getStringParameter("gdclass");
			GroupDetection gd = (GroupDetection) Dynamic.forConfigurableName(GroupDetection.class, gdclass, this);
			String tmpgdsid = Schema.generateRandomSchemaID(g);
			gd.predictAsEdge(g, tmpgdsid);
			
			clusters = new ArrayList<Set<Node>>(g.numGraphItems(tmpgdsid)); 
			Iterator<Edge> eitr = g.getEdges(tmpgdsid);
			while(eitr.hasNext()) {
				Edge e = eitr.next();
				Set<Node> nodes = IteratorUtils.iterator2nodeset(e.getAllNodes());
				clusters.add(nodes);
			}
			
			if(cacheclusters) {
				this.gid2clusters.put(g.getID(), clusters);
			}
		}
		
		if(bootstrap) {
			// Bootstrap the active learning by returning
			// items from among a set of clusters sampled by size
			List<Double> weights = new ArrayList<Double>();
			List<List<Node>> objects = new ArrayList<List<Node>>();
			for(Set<Node> set:clusters) {
				List<Node> copyset = new ArrayList<Node>(set);
				copyset.retainAll(testnodes);
				if(copyset.isEmpty()) {
					continue;
				}
				
				weights.add(0.0+set.size());
				objects.add(copyset);
			}
			
			List<Object> samples = WeightedSampler.performWeightedSampling(objects, weights,
					numqueries, false, rand);
			for(Object o:samples) {
				@SuppressWarnings("unchecked")
				List<Node> nodes = (List<Node>) o;
				nodes2query.add(nodes.get(rand.nextInt(nodes.size())));
			}
		}
		
		// Identify clusters with test nodes
		List<Map<Node,Double>> objects = new ArrayList<Map<Node,Double>>();
		List<Double> weights = new ArrayList<Double>();
		for(Set<Node> set:clusters) {
			Set<Node> copyset = new HashSet<Node>(set);
			copyset.retainAll(testnodes);
			if(copyset.isEmpty()) {
				continue;
			}
			
			// Compute disagreement per cluster
			SimplePair<Double, Map<Node,Double>> pair = this.computeClusterDisagreement(set, copyset);
			objects.add(pair.getSecond());
			Double denominator = (double) set.size()-copyset.size();
			weights.add(pair.getFirst()/denominator);
		}
		
		// Sample the queries by disagreement
		List<Query> queries = new ArrayList<Query>();
		List<Object> sampledc = WeightedSampler.performWeightedSampling(objects, weights, numqueries, false, rand);
		for(Object o:sampledc) {
			// Select the nodes in the sampled clusters
			// with highest disagreement
			TopK<Node> top1 = new TopK<Node>(1);
			@SuppressWarnings("unchecked")
			Map<Node,Double> omap = (Map<Node,Double>) o;
			Set<Entry<Node,Double>>  entries = omap.entrySet();
			for(Entry<Node,Double> e:entries) {
				top1.add(e.getValue(), e.getKey());
			}
			
			Set<Node> topnode = top1.getTopK();
			queries.add(new Query(topnode.iterator().next()));
		}
		
		return queries;
	}
	
	private SimplePair<Double, Map<Node,Double>> computeClusterDisagreement(Set<Node> cluster, Set<Node> testcluster) {
		// Compute the majority label among the labeled cluster instances
		KeyedCount<String> clusterlabels = new KeyedCount<String>();
		for(Node n:cluster) {
			if(testcluster.contains(n)) {
				continue;
			}
			
			clusterlabels.increment(((CategValue) n.getFeatureValue(targetfeatureid)).getCategory());
		}
		String majorityclustlabel = clusterlabels.highestCountKey();
		
		Map<Node,Double> node2dis = new HashMap<Node,Double>();
		double totaldisagreement = 0;
		for(Node n:cluster) {
			KeyedCount<String> labelcount = new KeyedCount<String>();
			// Load and apply local classifier, if requested
			// Get local classifier label
			labelcount.increment(((CategValue) n.getFeatureValue(localfid)).getCategory());
			
			// Load and apply relational classifier, if requested
			// Get relational classifier label
			labelcount.increment(((CategValue) n.getFeatureValue(targetfeatureid)).getCategory());
			
			// Get cluster label
			if(majorityclustlabel!=null) {
				labelcount.increment(majorityclustlabel);
			} else {
				// Randomly select label in cases where a cluster has no labeled nodes
				labelcount.increment(categories.get(rand.nextInt(categories.size())));
			}
			
			// Create probability distribution over predicted labels
			double[] probs = new double[categories.size()];
			for(int i=0; i<probs.length; i++) {
				probs[i] = labelcount.getPercent(categories.get(i));
			}
			
			// Compute entropy of probability distribution
			double disagreement = Entropy.computeEntropy(probs);
			node2dis.put(n, disagreement);
			totaldisagreement += disagreement;
		}
		
		return new SimplePair<Double, Map<Node,Double>>(totaldisagreement,node2dis);
	}
	
	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String targetschemaid = this.getStringParameter("saved-targetschemaid");
		String targetfeatureid = this.getStringParameter("saved-targetfeatureid");
		this.initialize(targetschemaid, targetfeatureid);
	}

	public void shouldBootstrap(boolean bootstrap) {
		this.bootstrap = bootstrap;
	}
}
