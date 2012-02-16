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
package linqs.gaia.graph.generator.decorator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Creates a categorical feature, as well as numeric features,
 * for some graph following the methods described in:
 * <p>
 * M. Rattigan, M. Maier and D. Jensen. 2007.
 * Exploiting network structure for active inference in collective classification.
 * University of Massachusetts Technical Report 07-22. 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodeschemaid-Schema ID of nodes to set attributes for
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> targetfeatureid-Feature id of the label feature to add.  Default is "label".
 * <LI> numlabels-Number of labels to use.  Default is 2.
 * <LI> numrandomperlabel-Number of nodes to randomly assign to each label.  Default is 1.
 * <LI> pctrandomperlabel-Percentage of nodes to randomly assign to each label.
 * This overrides "numrandomperlabel" if specified.  Default is to use the default "numrandomperlabel".
 * <LI> usepercent-If yes, use percent to rerank neighbors.  Default is no.
 * <LI> seed-Random number generator seed.  Default is 0.
 * </UL>
 * 
 * @author mbilgic
 * @author namatag
 *
 */
public class RattiganTR07Labeler extends BaseConfigurable implements Decorator {
	protected String nodeschemaid;
	protected String targetfeatureid = "label";
	protected int numlabels = 2;
	private int numrandomperlabel = 1;
	private int seed = 0;
	
	private List<CategValue> values = null;
	
	private boolean usepercent = false;
	
	/**
	 * Support for function calls which call this method.
	 * Initializes all required and optional features.
	 * 
	 * @param g Graph
	 * @param targetfeatureid Feature ID to use
	 * @param numlabels Number of labels
	 * @param numrandomperlabel Number of random labels to create
	 * @param usepercent If yes, use percent to rerank neighbors.  Default is no.
	 * @param seed Random number generator seed to use
	 */
	public void decorate(Graph g, String nodeschemaid, String targetfeatureid,
			int numlabels, int numrandomperlabel,
			boolean usepercent, int seed) {
		
		this.setParameter("nodeschemaid", nodeschemaid);
		this.setParameter("targetfeatureid", targetfeatureid);
		this.setParameter("numlabels", ""+numlabels);
		this.setParameter("numrandomperlabel", ""+numrandomperlabel);
		this.setParameter("usepercent", usepercent ? "yes" : "no");
		this.setParameter("seed", ""+seed);
		
		this.decorate(g);
	}
	
	public void decorate(Graph g) {
		// Set parameters
		this.nodeschemaid = this.getStringParameter("nodeschemaid");
		
		if(this.hasParameter("targetfeatureid")) {
			this.targetfeatureid = this.getStringParameter("targetfeatureid");
		}
		
		if(this.hasParameter("numlabels")) {
			this.numlabels = (int) this.getDoubleParameter("numlabels");
		}
		
		if(this.hasParameter("numrandomperlabel")) {
			this.numrandomperlabel = (int) this.getDoubleParameter("numrandomperlabel");
		}
		
		if(this.hasParameter("pctrandomperlabel")) {
			double pct = this.getDoubleParameter("pctrandomperlabel");
			this.numrandomperlabel = (int) (g.numGraphItems(this.nodeschemaid) * pct);
			Log.DEV(this.numrandomperlabel);
		}
		
		if(this.hasParameter("usepercent", "yes")) {
			this.usepercent = true;
		} else if(this.hasParameter("usepercent", "no")) {
			this.usepercent = false;
		} else if(this.hasParameter("usepercent")) {
			throw new ConfigurationException("Invalid usepercent option: "
					+this.getStringParameter("usepercent"));
		}
		
		if(this.hasParameter("seed")) {
			this.seed = (int) this.getDoubleParameter("seed");
		}
		
		values = new ArrayList<CategValue>(numlabels);
		for(int i=0; i<numlabels; i++) {
			// Set target attribute
			double[] probs = new double[this.numlabels];
			probs[i] = 1;
			values.add(new CategValue(""+i, probs));
		}
		
		// Label graph
		this.labelAGraph(g);
		
		values = null;
	}
	
	/**
	 * Labels a given graph in spirit of Rattigan et al TR'07
	 * 
	 * @param g Graph
	 */
	public void labelAGraph(Graph g){
		// Domain mappings
		String[] classDomain = new String[numlabels];

		for(int i=0;i<numlabels;i++){
			classDomain[i] = i+"";
		}
		
		// Update schema to include the new features
		Schema schema = g.getSchema(nodeschemaid);
		
		// Add feature for label
		schema.addFeature(targetfeatureid, new ExplicitCateg(Arrays.asList(classDomain)));
		
		// Update schema
		g.updateSchema(nodeschemaid, schema);
		
		// This map will map the unique identifier for a node to
		// all comparable node wrappers for the specified node.
		// A node is assigned to the label which has the highest
		// rank for a given label.
		Map<String, ArrayList<ComparableNode>> mappings =
					new HashMap<String,ArrayList<ComparableNode>>();
		
		// Place each node in a tree set, for use in assigning labels
		@SuppressWarnings("unchecked")
		TreeSet<ComparableNode>[] labelings = new TreeSet[numlabels];
		for(int i=0;i<numlabels;i++){
			labelings[i] = new TreeSet<ComparableNode>();
			Iterator<Node> nitr = g.getNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				
				String key = n.toString();
				ComparableNode cn = new ComparableNode(n, 0);
				labelings[i].add(cn);

				ArrayList<ComparableNode> cns = mappings.get(key);
				if(cns == null){
					cns = new ArrayList<ComparableNode>();					
				}

				cns.add(cn);
				
				mappings.put(key, cns);
			}
		}

		Random rand = new Random(seed);
		@SuppressWarnings("unchecked")
		List<Node> randomSel = (List<Node>) IteratorUtils.iterator2list(g.getNodes());
		
		// Randomly assign label to a specified number of nodes for each label
		for(int i=0;i<numlabels;i++){
			for(int j=0;j<numrandomperlabel;j++){
				if(randomSel.isEmpty()) {
					break;
				}
				
				int objId = rand.nextInt(randomSel.size());

				Node n = randomSel.get(objId);
				String key = n.toString();

				// Generate features and set label i for the given node
				this.setLabel(n, i, rand);

				// Update its neighbors
				updateNeighbors(n, labelings, mappings);

				// Remove it from all storages
				randomSel.remove(objId);
				ArrayList<ComparableNode> cbdns = mappings.get(key);				
				for(int k=0;k<labelings.length;k++){
					labelings[k].remove(cbdns.get(k));
				}
				
				mappings.remove(key);
			}
		}
		
		randomSel.clear();
		
		// Fill in for those not randomly set
		while(!mappings.isEmpty()){
			for(int i=0;i<numlabels;i++){
				ComparableNode cbdn = labelings[i].first();
				Node n = cbdn.bdn;

				String key = n.toString();
				
				// Generate features and set label i for the given node
				this.setLabel(n, i, rand);
				
				// Update its neighbors
				updateNeighbors(n, labelings, mappings);
				
				// Remove it from all storages
				ArrayList<ComparableNode> cbdns = mappings.get(key);				
				for(int k=0;k<labelings.length;k++){
					labelings[k].remove(cbdns.get(k));
				}
				
				// Update mapping
				mappings.remove(key);
				if(mappings.isEmpty()) {
					break;
				}
			}
		}
	}
	
	/**
	 * Generate attributes for the given node
	 * 
	 * @param n Node to generate attribute for
	 * @param c Label index
	 * @param rand Random number generator
	 */
	private void setLabel(Node n, int c, Random rand) {
		// Set target attribute
		n.setFeatureValue(this.targetfeatureid, values.get(c));
	}
	
	/**
	 * Update the rank of the neighboring nodes
	 * 
	 * @param n Neighbors to re-rank
	 * @param labelings Labeling sets
	 * @param mappings Mapped lists
	 */
	private void updateNeighbors(Node n, TreeSet<ComparableNode>[] labelings,
			Map<String, ArrayList<ComparableNode>> mappings) {

		Set<Node> neighbors = getNeighbors(n); 
		for(Node n2: neighbors){
			// If still unlabeled
			if(!n2.hasFeatureValue(targetfeatureid)) {
				String key2 = n2.toString();
				
				// Compute a new percentage OR a count
				double[] rank;
				if(usepercent) {
					rank = computePercentages(n2, labelings.length);
				} else {
					rank = computeCounts(n2, labelings.length);
				}

				// Remove from treesets and add it back in with the new rank
				ArrayList<ComparableNode> cns = new ArrayList<ComparableNode>();
				for(int k=0;k<labelings.length;k++){
					// Remove from tree set
					ComparableNode cn2 = mappings.get(key2).get(k);
					labelings[k].remove(cn2);
					
					// Add back in with new rank
					cn2 = new ComparableNode(n2, rank[k]);
					cns.add(cn2);
					labelings[k].add(cn2);
				}
				
				mappings.put(key2, cns);
			}
		}
	}
	
	/**
	 * Return the number of neighbors which have the different
	 * target feature values
	 * 
	 * @param n Node
	 * @param numLabels Number of labels
	 * 
	 * @return Double array of form [#withCat1,#withCat2,...,#withCatN].
	 */
	private double[] computeCounts(Node n, int numLabels) {
		double[] counts = new double[numLabels];
		Schema schema = n.getSchema();
		CategFeature cf = (CategFeature) schema.getFeature(targetfeatureid);
		UnmodifiableList<String> categs = cf.getAllCategories();
		
		Set<Node> neighbors = getNeighbors(n);
		for(int i=0;i<counts.length;i++){
			double count=0;
			String currcat = categs.get(i);
			
			for(Node n2: neighbors){
				if(n2.hasFeatureValue(targetfeatureid)) {
					CategValue fv = (CategValue) n2.getFeatureValue(targetfeatureid);
					if(fv.getCategory().equals(currcat)) {
						count++;
					}
				}
			}

			counts[i] = count;
		}

		return counts;
	}

	/**
	 * Computer percent of neighbors with a given value.
	 * 
	 * @param n Node
	 * @param numLabels Number of labels
	 * @return Double array of form [%withCat1,%withCat2,...,%withCatN].
	 */
	private double[] computePercentages(Node n, int numLabels) {
		double[] pers = this.computeCounts(n, numLabels);

		Set<Node> neighbors = getNeighbors(n);
		double size = neighbors.size();
		
		for(int i=0;i<pers.length;i++){
			pers[i] = pers[i]/size;
		}

		return pers;
	}
	
	/**
	 * For the given node, get all the other nodes
	 * which share an edge with this node.
	 * 
	 * @param bdn Node
	 * @return Node neighbors
	 */
	public Set<Node> getNeighbors(Node bdn) {
		Iterator<GraphItem> ngiset = bdn.getAdjacentGraphItems();
		Set<Node> neighbors = new HashSet<Node>();
		while(ngiset.hasNext()) {
			neighbors.add((Node) ngiset.next());
		}
		
		return neighbors;
	}
	
	/**
	 * Make nodes comparable using some value.
	 * 
	 * @author namatag
	 *
	 */
	protected static class ComparableNode implements Comparable<Object> {
		double value;
		String key;
		Node bdn;

		public ComparableNode(Node bdn, double value){
			this.bdn=bdn;
			this.value=value;
			this.key = bdn.toString();
		}

		public int compareTo(Object o) {
			if(this.equals(o)) {
				return 0;
			}

			ComparableNode other = (ComparableNode) o;
			
			// Sort by value, then by the object id
			// Note that the return values are reversed
			// since we want to maximize, not minimize,
			// the first() call to TreeSet.
			if(this.value<other.value){
				return 1;
			} else if(this.value>other.value){
				return -1;
			} else if(this.key.compareTo(other.key)<0) {
				return -1;
			} else {
				return 1;
			}
		}
		
		public String toString() {
			return key+" : "+value;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			
			long temp;
			temp = Double.doubleToLongBits(value);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			
			if (obj == null) {
				return false;
			}
			
			if (getClass() != obj.getClass()) {
				return false;
			}
			
			final ComparableNode other = (ComparableNode) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key)) {
				return false;
			}
			
			if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
				return false;
			}
			
			return true;
		}
	}
}
