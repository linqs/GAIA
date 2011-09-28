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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.statistic.GraphStatistic;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;

/**
 * This experiment class loads a graph, as specified by the io and its configurations,
 * and runs the specified statistics over it.  The statistics
 * are printed in order as {@link Log#INFO} messages.
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>ioclass-Class for the {@link IO} to instantiate using in {@link Dynamic#forConfigurableName}
 * <LI>graphstats-Comma delimited list of {@link GraphStatistic} classes,
 * instantiated using in {@link Dynamic#forConfigurableName},
 * to calculate over the loaded graph
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>ioconfigfile-Configuration file for the {@link IO}.  It uses the parameters set for the experiment otherwise.
 * </UL>
 * <p>
 * Note: An example configuration can be found in resource/SampleFiles/GraphAnalysisExperimentSample.
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class GraphAnalysisExperiment extends Experiment {

	@Override
	public void runExperiment() {
		// Load Graph
		Graph graph = null;
		
		// Load IO class
		IO io = (IO)
			Dynamic.forConfigurableName(IO.class, this.getStringParameter("ioclass"));
		if(this.hasParameter("ioconfigfile")) {
			io.loadParametersFile("ioconfigfile");
		} else {
			io.copyParameters(this);
		}
		
		graph = io.loadGraph();
		
		// Load requested Graph Statistics
		List<String> graphstats = Arrays.asList(this.getStringParameter("graphstats").split(","));
		boolean isfirst = true;
		for(String gsclass:graphstats) {
			GraphStatistic gs = (GraphStatistic)
				Dynamic.forConfigurableName(GraphStatistic.class, gsclass);
			gs.copyParameters(this);
			
			// Calculate statistics
			Map<String, String> stats = gs.getStatisticStrings(graph);
			
			// Print statistics
			// Space statistics
			if(isfirst) {
				isfirst = false;
			} else {
				Log.INFO("");
			}
			
			Log.INFO("Statistic "+gs.getClass().getCanonicalName()+":");
			Set<Entry<String,String>> entries = stats.entrySet();
			for(Entry<String,String> entry:entries) {
				Log.INFO(entry.getKey()+"="+entry.getValue());
			}
		}
		
		graph.destroy();
	}
	
	public static void main(String[] args) {
		if(args.length != 1) {
			throw new ConfigurationException("Arguments: <configfile>");
		}
		
		GraphAnalysisExperiment e = new GraphAnalysisExperiment();
		e.setCID("exp");
		e.loadParametersFile(args[0]);
		e.runExperiment();
	}
}
