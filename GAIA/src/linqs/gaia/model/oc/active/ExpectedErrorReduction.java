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
import java.util.List;
import java.util.Map;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.model.BaseModel;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.model.oc.active.query.Query;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.Entropy;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.TopK;
import linqs.gaia.util.UnmodifiableList;

public class ExpectedErrorReduction extends BaseModel implements ActiveLearning {
	private static final long serialVersionUID = 1L;
	private String targetschemaid;
	private String targetfeatureid;
	
	public void initialize(String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
	}

	public List<Query> getQueries(Graph g, int numqueries) {
		return this.getQueries(g.getIterableGraphItems(targetschemaid), numqueries);
	}

	public List<Query> getQueries(Iterable<? extends Decorable> testitems,
			int numqueries) {
		// Initialize predictions, if needed
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
		
		Graph currg = graphs.iterator().next();
		UnmodifiableList<String> categories =
			((CategFeature) currg.getSchema(targetschemaid).getFeature(targetfeatureid)).getAllCategories();
		
		// Assume all labeled instances from all graphs,
		// excluding the test instances, can be used for training.
		Set<Decorable> trainsets = new HashSet<Decorable>();
		for(Graph g:graphs) {
			List<GraphItem> curritems = OCUtils.getItemsByFeature(g,
					targetschemaid, targetfeatureid, false);
			trainsets.addAll(curritems);
		}
		trainsets.removeAll(testnodes);
		
		Iterable<? extends Decorable> candidates = null;
		if(this.hasParameter("alclasses")) {
			candidates = this.getCandidates(testitems);
		} else {
			candidates = testitems;
		}
		
		TopK<Decorable> topk = new TopK<Decorable>(numqueries);
		for(Decorable d:candidates) {
			double score = this.computeEMC(d, trainsets, testnodes, categories);
			topk.add(score, d);
		}
		
		List<Query> queries = new ArrayList<Query>();
		List<SimplePair<Decorable,Double>> pairs = topk.getTopKSortedWithScores();
		for(SimplePair<Decorable,Double> p:pairs) {
			queries.add(new Query(p.getFirst(), targetfeatureid, p.getSecond()));
		}
		
		return queries;
	}
	
	private Iterable<? extends Decorable> getCandidates(Iterable<? extends Decorable> testitems) {
		int numcands = this.getIntegerParameter("numcandidates");
		
		Set<Decorable> fullcandidates = new HashSet<Decorable>();
		for(Decorable d:testitems) {
			fullcandidates.add(d);
		}
		
		Set<Decorable> candidates = new HashSet<Decorable>();
		String alclasses = this.getStringParameter("alclasses");
		String[] alcs = alclasses.split(",");
		for(String alc:alcs) {
			ActiveLearning al = (ActiveLearning) Dynamic.forConfigurableName(ActiveLearning.class,
					alc, this);
			List<Query> queries = al.getQueries(fullcandidates, numcands);
			for(Query q:queries) {
				candidates.add(q.getDecorable());
				fullcandidates.remove(q.getDecorable());
			}
		}
		
		return candidates;
	}
	
	private double computeEMC(Decorable candidate, 
			Set<? extends Decorable> currentsurveyed,
			Set<? extends Decorable> currentunsurveyed,
			UnmodifiableList<String> categories)
	{
		int numcats = categories.size();
		
		// Save original values to restore later
		Map<Decorable,FeatureValue> node2savedfv = new HashMap<Decorable,FeatureValue>();
		for(Decorable n:currentunsurveyed) {
			node2savedfv.put(n, n.getFeatureValue(targetfeatureid));
		}
		
		// Get probability estimate for each query
		double[] probs = null;
		if(candidate.hasFeatureValue(targetfeatureid)) {
			probs = ((CategValue) candidate.getFeatureValue(targetfeatureid)).getProbs();
		} else {
			// Handle special case where the probability distribution is null
			probs = new double[numcats];
			for(int i=0; i<numcats; i++) {
				probs[i] = 1.0/numcats;
			}
		}
		
		// Suppose candidate was already surveyed
		Set<Decorable> modcurrentsurveyed = new HashSet<Decorable>(currentsurveyed);
		modcurrentsurveyed.add(candidate);
		Set<Decorable> toinfer = new HashSet<Decorable>(currentunsurveyed);
		toinfer.remove(candidate);
		
		double emc = 0;
		for(int i=0; i<numcats; i++) {
			// Suppose the survey returns this value
			String c = categories.get(i);
			
			candidate.setFeatureValue(targetfeatureid, new CategValue(c));
			
			// Learn CC model from available annotations, including possible survey result
			String occlass = this.getStringParameter("emcocclass");
			Classifier classifier = (Classifier) Dynamic.forConfigurableName(Classifier.class, occlass, this);
			
			classifier.learn(modcurrentsurveyed, targetschemaid, targetfeatureid);
			classifier.predict(toinfer);
			
			// Compute expected error reduction
			double[] qprobs = null;
			// Compute over all instances
			for(Decorable q:currentunsurveyed) {
				qprobs = ((CategValue) q.getFeatureValue(targetfeatureid)).getProbs();
				emc += probs[i]*Entropy.computeEntropy(qprobs);
			}
			
			// Restore original labels
			for(Decorable n:currentunsurveyed) {
				n.setFeatureValue(targetfeatureid, node2savedfv.get(n));
			}
		}
		
		return emc;
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
}
