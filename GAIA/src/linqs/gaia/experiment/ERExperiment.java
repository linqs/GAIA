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

import java.util.Map;
import java.util.Iterator;

import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.io.DirectoryBasedIO;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.SparseTabDelimIO;
import linqs.gaia.log.Log;
import linqs.gaia.model.er.EntityResolution;
import linqs.gaia.model.util.plg.PotentialLinkGenerator;
import linqs.gaia.prediction.PredictionGroup;
import linqs.gaia.prediction.existence.ExistencePred;
import linqs.gaia.prediction.existence.ExistencePredGroup;
import linqs.gaia.prediction.statistic.Statistic;
import linqs.gaia.prediction.statistic.StatisticUtils;
import linqs.gaia.util.Dynamic;


public class ERExperiment extends Experiment {
	private String refschemaid = null;
	private String edgeschemaid = null;
	private String savemodelfile = null;
	private String loadmodelfile = null;
	
	@Override
	public void runExperiment() {

		// Set logging level to show all
		Log.showAllLogging();

		// load config options
		refschemaid = this.getStringParameter("refschemaid");
		edgeschemaid = this.getStringParameter("edgeschemaid");
		
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
			Log.DEBUG("Training classifier");
			er.learn(graph, refschemaid, edgeschemaid, generator);
		} else {
			Log.DEBUG("Loading saved classifier");
			er.loadModel(this.loadmodelfile);
		}
		
		if(this.savemodelfile!=null) {
			er.saveModel(this.savemodelfile);
		}
	
		er.predictAsLink(graph, generator);
		
		// Saved the predicted graph
		if(this.hasParameter("predioclass")) {
			IO predio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("predioclass"));
			predio.copyParameters(this);

			if(predio instanceof SparseTabDelimIO) {
				String prefix = predio.getCID()==null ? "" : (predio.getCID()+".");
				predio.setParameter(prefix+"filedirectory",
						this.getStringParameter(prefix+"filedirectory"));
			}
			
			Log.DEBUG("Tried saving prediction graph");

			predio.saveGraph(graph);
		}

		
		// evaluate prediction
	
		// count number of potential edges
		Iterator<Edge> edgeCounter = generator.getLinksIteratively(graph, edgeschemaid);
		int potentialEdges = 0;
		while (edgeCounter.hasNext()) {
			edgeCounter.next();
			potentialEdges++;
		}
		
		Log.DEBUG("Counted " + potentialEdges + " possible coreference edges using " + this.getStringParameter("linkgenerator"));
		
		ExistencePredGroup epg = new ExistencePredGroup(potentialEdges, graph.numEdges());
		
		Iterator<Edge> corefEdges = graph.getEdges(edgeschemaid);
		
		while (corefEdges.hasNext()) {
			epg.addPrediction(new ExistencePred("coreferent"));
		}
		
		
	}


	public static void main(String[] args) throws Exception {
		
		ERExperiment exp = new ERExperiment();

		exp.setParameter("inputioclass", "io:linqs.gaia.graph.io.TabDelimIO");
		exp.setParameter("inputdirectory", "resource/citeseer");
		exp.setParameter("outputdirectory", "SampleSave");
		
		exp.setParameter("linkgenerator", "lg:linqs.gaia.model.util.plg.AllPairwise");
		exp.setParameter("nodeschemaid", "author");
	
		exp.setParameter("entityresolver", "er:linqs.gaia.model.er.ERNullModel");
		exp.setParameter("refschemaid", "author");
		exp.setParameter("edgeschemaid", "coauthor");
	
		exp.setParameter("predioclass", "predio:linqs.gaia.graph.io.TabDelimIO");
		exp.setParameter("filedirectory", "SampleSave");
		
		exp.runExperiment();
		
	}

}
