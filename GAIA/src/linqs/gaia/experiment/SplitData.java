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

import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.DirectoryBasedIO;
import linqs.gaia.log.Log;
import linqs.gaia.sampler.decorable.DecorableSampler;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimpleTimer;

//TODO documentation
public class SplitData extends Experiment {
	private String targetschemaID;
	
	@Override
	public void runExperiment() {

		// Set logging level to show all
		Log.showAllLogging();

		DirectoryBasedIO io = (DirectoryBasedIO) Dynamic.forConfigurableName(DirectoryBasedIO.class, this.getStringParameter("inputioclass"));
		io.copyParameters(this);
		
		Graph graph = io.loadGraphFromDir(this.getStringParameter("inputdirectory"));
		Log.INFO("Loaded graph: "+GraphUtils.getSimpleGraphOverview(graph));
				
		// split data

		targetschemaID = this.getStringParameter("targetschemaID");

		
		DecorableSampler sampler = (DecorableSampler)
				Dynamic.forConfigurableName(DecorableSampler.class,this.getStringParameter("samplerclass"));
		sampler.copyParameters(this);
		sampler.generateSampling(graph.getGraphItems(targetschemaID));

		Log.DEBUG("Sampling complete: Number of subsets="+sampler.getNumSubsets());
		
		
		// output splits
		
		if(this.hasParameter("samplingioclass")) {
			String sid = this.getStringParameter("targetschemaID");
			String splitfid = this.getStringParameter("splitfid");
			int numsplits = sampler.getNumSubsets();
			
			Schema schema = graph.getSchema(sid);
			schema.addFeature(splitfid, new ExplicitString());
			graph.updateSchema(sid, schema);
			
			for(int i=0; i<numsplits; i++) {
				Iterable<? extends Decorable> itrbl = sampler.getSubset(i);
				for(Decorable d:itrbl) {
					d.setFeatureValue(splitfid, new StringValue(""+i));
				}
			}
		
			for (int i = 0; i < numsplits; i++) {				
				// Remove test instances from train graph
				Graph traingraph = graph.copy(graph.getID().getObjID()+"-traincopy"+i);
				for(Decorable d:sampler.getNotInSubset(i)) {
					GraphItem gi = (GraphItem) d;
					if(gi instanceof Node) {
						traingraph.removeNodeWithEdges(((Node) gi).getID().copyWithoutGraphID());
					} else {
						traingraph.removeGraphItem(gi.getID().copyWithoutGraphID());
					}
				}
				Log.DEBUG("Training Graph: "+GraphUtils.getSimpleGraphOverview(traingraph));
				
				
				IO samplingio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("samplingioclass"));
				samplingio.copyParameters(this);
				
				
				samplingio.setParameter("filedirectory", this.getStringParameter("outputdirectory")+"/trainSplit"+i);
				samplingio.saveGraph(traingraph);
				
				traingraph.destroy();
				
				Graph testgraph = graph.copy(graph.getID().getObjID()+"-testcopy"+i);
				for(Decorable d:sampler.getSubset(i)) {
					GraphItem gi = (GraphItem) d;
					if(gi instanceof Node) {
						testgraph.removeNodeWithEdges(((Node) gi).getID().copyWithoutGraphID());
					} else {
						testgraph.removeGraphItem(gi.getID().copyWithoutGraphID());
					}
				}
				Log.DEBUG("Testing Graph: "+GraphUtils.getSimpleGraphOverview(testgraph));
				

				samplingio.setParameter("filedirectory", this.getStringParameter("outputdirectory")+"/testSplit"+i);
				
				samplingio.saveGraph(testgraph);
				
				testgraph.destroy();
			}
		
			
		}
		
	}


	public static void main(String[] args) throws Exception {
		
		SplitData exp = new SplitData();
	
//		exp.setParameter("inputioclass", "io:linqs.gaia.graph.io.TabDelimIO");
//		exp.setParameter("samplingioclass", "io:linqs.gaia.graph.io.TabDelimIO");
//		exp.setParameter("splitfid", "split");
//		exp.setParameter("inputdirectory", "resource/citeseer");
//		exp.setParameter("outputdirectory", "SampleSave");
//		exp.setParameter("filedirectory", "SampleSaveSplits");
//		exp.setParameter("numsubsets", 4);
//		exp.setParameter("samplerclass", "sampler:linqs.gaia.sampler.decorable.EntityRandomSampler");
//		exp.setParameter("corefid", "author_cluster_id");
//		exp.setParameter("targetschemaID", "author");
//		

		SimpleTimer timer = new SimpleTimer();
		exp.loadParametersFile(args[0]);
		
		exp.runExperiment();

		Log.INFO("Splitting Runtime: "+timer.timeLapse(true)+"\t"+timer.timeLapse(false));
	}

}
