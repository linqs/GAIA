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
package linqs.gaia.model.lp;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.log.Log;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.similarity.NodeSimilarity;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.TopK;

/**
 * This link prediction model predicts the existence
 * of a link based on the similarity of the nodes
 * in it.  This implementation is only defined for binary graphs,
 * directed or undirected, where the two nodes must have the same schema id.
 * This link predictor calculates similarity based
 * a specified node similarity and predicts that a link exist if it is past some
 * threshold.  The implementation is based on the link prediction
 * algorithm described in:
 * <p>
 * Liben-Nowell, D. & Kleinberg, J.<br>
 * The link prediction problem for social networks<br>
 * International Conference on Information and Knowledge Management, 2003
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodesimclass-Node similarity measure to use to calculate node similarity.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> threshold-Value between 0 and 1, inclusive.
 * Anything equal to or above the threshold is predicted to exist.
 * Default is .5.
 * <LI> topk-If specified, predict the topk number of values as existing regardless of threshold value.
 * Default is to rely solely on threshold.
 * <LI> checkpoint-If set, this the current number of links predicted over is printed
 * as an INFO message whenever (the current number)%(checkpoint) == 0.
 * </UL>
 * 
 * @author namatag
 *
 */
public class ScoreThreshold extends BaseConfigurable implements LinkPredictor {
	private static final long serialVersionUID = 1L;
	
	private NodeSimilarity nodesim = null;
	private String edgeschemaid = null;
	private Double threshold = .5;
	private Integer topk = null;
	private Double checkpoint = null;
	
	/**
	 * Initialize the model
	 * 
	 * @param graph
	 * @param edgeschemaid
	 */
	private void initialize(Graph graph, String edgeschemaid) {
		this.edgeschemaid = edgeschemaid;
		
		// Get checkpoint value
		if(this.hasParameter("checkpoint")) {
			this.checkpoint = this.getDoubleParameter("checkpoint");
		}
		
		// Get threshold
		if(this.hasParameter("threshold")) {
			this.threshold = this.getDoubleParameter("threshold");
		}
		
		if(this.hasParameter("topk")) {
			this.topk = this.getIntegerParameter("topk");
		}
		
		// Get node similarity measure
		this.nodesim = (NodeSimilarity) Dynamic.forConfigurableName(NodeSimilarity.class,
				this.getStringParameter("nodesimclass"), this);
	}
	
	public void learn(Graph graph, PotentialLinkGenerator generator, String edgeschemaid) {
		this.initialize(graph, edgeschemaid);
	}

	public void learn(Graph graph, Iterable<Edge> knownedges, String edgeschemaid, String existfeature) {
		this.initialize(graph, edgeschemaid);
	}
	
	public void predict(Graph graph, Iterable<Edge> unknownedges) {
		this.predict(graph, unknownedges.iterator(), true, null);
	}

	public void predict(Graph graph, Iterable<Edge> unknownedges, boolean removenotexist, String existfeature) {
		this.predict(graph,
				unknownedges.iterator(),
				removenotexist,
				existfeature);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator) {
		Iterator<Edge> eitr = generator.getLinksIteratively(graph, edgeschemaid);
		this.predict(graph, eitr, true, null);
	}

	public void predict(Graph graph, PotentialLinkGenerator generator,
			boolean removenotexist, String existfeature) {
		this.predict(graph,
				generator.getLinksIteratively(graph, edgeschemaid),
				removenotexist,
				existfeature);
	}
	
	private void predict(Graph graph, Iterator<Edge> eitr,
			boolean removenotexist, String existfeature) {
		// Add exist feature, if not already defined
		Schema schema = graph.getSchema(edgeschemaid);
		if(existfeature!=null && !schema.hasFeature(existfeature)) {
			schema.addFeature(existfeature, LinkPredictor.EXISTENCEFEATURE);
			graph.updateSchema(edgeschemaid, schema);
		}
		
		int numpredover = 0;
		int numexist = 0;
		
		MinMax minmax = new MinMax();
		SimpleTimer timer = new SimpleTimer();
		TopK<Edge> topkedges = null;
		if(topk!=null){ 
			topkedges = new TopK<Edge>(topk);
		}
		
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(!e.getSchemaID().equals(this.edgeschemaid)) {
				String sid = e.getSchemaID();
				throw new InvalidStateException("Attempting to predict link with schema id"+sid+
						" expected edge with schema id "+this.edgeschemaid);
			}
			
			// Get nodes of the edges
			if(e.numNodes() < 1 || e.numNodes() > 2) {
				throw new InvalidStateException("Only pairwise links can be predicted: "
						+e+" has "+e.numNodes());
			}
			
			Iterator<Node> nitr = e.getAllNodes();
			Node n1 = nitr.next();
			Node n2 = n1;
			if(nitr.hasNext()) {
				n2 = nitr.next();
			}
			
			// Get similarity of nodes
			double sim = this.nodesim.getSimilarity(n1, n2);
			minmax.addValue(sim);
			
			// Keep everything above a threshold, inclusive
			if(topk == null) {
				if(sim >= this.threshold) {
					this.addEdgeAsExist(e, existfeature);
					numexist++;
				} else {
					this.removeEdgeAsNotExist(e, removenotexist, existfeature);
				}
			} else {
				List<Edge> removededges = topkedges.add(sim, e);
				// Handle case of having to remove
				// nodes to add e to topk, or if e was not added
				boolean addede = true;
				for(Edge re:removededges) {
					// Handle case where e was not added
					if(re.equals(e)) {
						addede = false;
					}
					
					this.removeEdgeAsNotExist(re, removenotexist, existfeature);
					numexist--;
				}
				
				// Handle case where e was added
				if(addede) {
					this.addEdgeAsExist(e, existfeature);
					numexist++;
				}
			}
			
			// Print checkpoint, if requested
			if(this.checkpoint != null && (numpredover % this.checkpoint) == 0) {
				Log.INFO("Similarity Threshold model predicted: "
						+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
				timer.start();
			}
			
			numpredover++;
		}
		
		if(this.checkpoint != null) {
			Log.INFO("Similarity Threshold model predicted: "
					+numexist+"/"+numpredover+" ("+timer.timeLapse(true)+")");
			timer.start();
		}
		
		Log.DEBUG("Similarity Distribution:"
				+" Min="+minmax.getMin()
				+" Max="+minmax.getMax()
				+" Mean="+minmax.getMean());
	}
	
	private void addEdgeAsExist(Edge e, String existfeature) {
		if(existfeature != null) {
			e.setFeatureValue(existfeature, LinkPredictor.EXISTVALUE);
		}
	}
	
	private void removeEdgeAsNotExist(Edge e, boolean removenotexist, String existfeature) {
		// Remove if not exist
		Graph g = e.getGraph();
		if(removenotexist) {
			g.removeEdge(e);
		} else {
			// Note: You either have to remove an edge or keep it
			// and set existence feature value.
			e.setFeatureValue(existfeature, LinkPredictor.NOTEXISTVALUE);
		}
	}
	
	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		this.edgeschemaid = this.getStringParameter("saved-edgeschemaid");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-edgeschemaid", this.edgeschemaid);
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
}
