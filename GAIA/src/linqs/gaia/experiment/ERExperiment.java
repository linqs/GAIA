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
package linqs.gaia.experiment;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.io.DirectoryBasedIO;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.SparseTabDelimIO;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.ERUtils;
import linqs.gaia.model.er.EntityResolution;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.prediction.existence.ExistencePred;
import linqs.gaia.prediction.existence.ExistencePredGroup;
import linqs.gaia.prediction.statistic.Statistic;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimpleTimer;


/**
 * Experiment for running within-network entity resolution
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> refschemaid - schema id for the references to be resolved
 * <LI> corefschemaid - schema id for the coreference relation
 * <LI> inputioclass - IO class from linqs.gaia.graph.io name for input
 * <LI> inputdirectory - directory to read input graph files
 * <LI> entityresolver - class from linqs.gaia.model.er used to resolve entities
 * <LI> linkgenerator - class from linqs.gaia.model.util.plg to determine which coreference pairs are possible (e.g., blocking)
 * </UL>
 * Optional parameters:
 * <UL>
 * <LI> savemodelfile - filename to save learned ER model
 * <LI> loadmodelfile - filename of previously learned ER model
 * <LI> predioclass - IO class from linqs.gaia.graph.io to save predicted graph
 * <LI> statistics - comma delimited list of statistic classes from linqs.gaia.prediction.statistic to compute on predicted graph
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author bert
 *
 */

public class ERExperiment extends Experiment {
	private String refschemaid = null;
	private String corefschemaid = null;
	private String savemodelfile = null;
	private String loadmodelfile = null;
	private List<Statistic> statistics;
	
	@Override
	public void runExperiment() {

		// Set logging level to show all
		Log.showAllLogging();

		// load config options
		refschemaid = this.getStringParameter("refschemaid");
		corefschemaid = this.getStringParameter("corefschemaid");
		
		if (this.hasParameter("savemodelfile"))
			savemodelfile = this.getStringParameter("savemodelfile");
		if (this.hasParameter("loadmodelfile"))
			loadmodelfile = this.getStringParameter("loadmodelfile");

		// load graph from data
		DirectoryBasedIO io = (DirectoryBasedIO) Dynamic.forConfigurableName(DirectoryBasedIO.class, this.getStringParameter("inputioclass"));
		io.copyParameters(this);
		
		Graph graph = io.loadGraphFromDir(this.getStringParameter("inputdirectory"));
		Log.INFO("Loaded graph: "+GraphUtils.getSimpleGraphOverview(graph));

		// Set up entity resolver
		EntityResolution er = (EntityResolution) Dynamic.forConfigurableName(EntityResolution.class, this.getStringParameter("entityresolver"));
		er.copyParameters(this);
		
		PotentialLinkGenerator generator = (PotentialLinkGenerator) Dynamic.forConfigurableName(PotentialLinkGenerator.class, 
				this.getStringParameter("linkgenerator"));
		generator.copyParameters(this);

		// Train classifier
		if(this.loadmodelfile==null) {
			er.learn(graph, refschemaid, corefschemaid, generator);
		} else {
			er.loadModel(this.loadmodelfile);
		}
		
		if(this.savemodelfile!=null) {
			er.saveModel(this.savemodelfile);
		}
		
		Schema coref = graph.getSchema(corefschemaid);
		Graph predGraph = graph.copy("predictedGraph");
		
		predGraph.removeSchema(corefschemaid);
		predGraph.addSchema(corefschemaid, coref);
		
		er.predictAsLink(predGraph, generator);
		
		// compute transitive closure if desired
		if (this.getYesNoParameter("transitiveclosure", "no")) {
			Log.INFO("Computing transitive closure");
			List<Set<Node>> entities = ERUtils.getTransitiveEntity(predGraph, refschemaid, corefschemaid); 

			for (Set<Node> entity : entities)
				for (Node n1 : entity) 
					for (Node n2 : entity) {
						if (!n1.equals(n2) && !n1.isAdjacent(n2, corefschemaid)) {
							predGraph.addUndirectedEdge(GraphItemID.generateGraphItemID(predGraph, corefschemaid),
									n1, n2);
						}
					}

		}

		Log.INFO("Predicted graph:");
		Log.INFO(GraphUtils.getSimpleGraphOverview(predGraph));
		
		// Saved the predicted graph
		if(this.hasParameter("predioclass")) {
			IO predio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("predioclass"));
			predio.copyParameters(this);

			if(predio instanceof SparseTabDelimIO) {
				String prefix = predio.getCID()==null ? "" : (predio.getCID()+".");
				predio.setParameter(prefix+"filedirectory",
						this.getStringParameter(prefix+"filedirectory"));
			}
			
			predio.saveGraph(graph);
		}

		// evaluate prediction
				
		ExistencePredGroup epg = new ExistencePredGroup(graph.numNodes()*(graph.numNodes()-1)/2, graph.numGraphItems(corefschemaid));
		
		Iterator<Edge> itr = predGraph.getEdges(corefschemaid);
		
		while (itr.hasNext()) {
			Edge e = itr.next();
			
			if(e.numNodes()!=2) {
				throw new UnsupportedTypeException("Only binary edges supported");
			}
			
			Iterator<Node> nodeIter = e.getAllNodes();
			
			Node n1 = (Node) graph.getEquivalentGraphItem(nodeIter.next());
			Node n2 = (Node) graph.getEquivalentGraphItem(nodeIter.next());
			
			ExistencePred pred = new ExistencePred(e.getID().toString(), 
					n1.isAdjacent(n2, corefschemaid) ? ExistencePredGroup.EXIST : ExistencePredGroup.NOTEXIST);
			epg.addPrediction(pred);
		}
				
		
		// compute statistics

		// Load set of requested statistics
		String statparam = null;
		if(this.hasParameter("statistics")) {
			statparam = this.getStringParameter("statistics");
		}
		String[] statclasses = statparam.split(",");

		statistics = new LinkedList<Statistic>();
		
		for(String statclass:statclasses) {
			Statistic stat = (Statistic) Dynamic.forConfigurableName(Statistic.class, statclass);
			stat.copyParameters(this);
			statistics.add(stat);
		}
		
		// Get split statistics
		Log.INFO("Statistics: ");
		for(Statistic stat: statistics) {
			Map<String,Double> statVals = stat.getStatisticDoubles(epg);
			for (String key : statVals.keySet()) {
				Log.INFO(key + ": "+statVals.get(key));
			}
		}
	}


	public static void main(String[] args) throws Exception {
		
		if(args.length != 1) {
			throw new ConfigurationException("Arguments: <configfile>");
		}
		
		ERExperiment exp = new ERExperiment();

		SimpleTimer timer = new SimpleTimer();
		exp.loadParametersFile(args[0]);
		
		exp.runExperiment();

		Log.INFO("ER Experiment running time: "+timer.timeLapse(true)+"\t"+timer.timeLapse(false));
		
		
	}

}
