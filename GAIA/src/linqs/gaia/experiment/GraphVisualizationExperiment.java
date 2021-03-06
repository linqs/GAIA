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

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.io.IO;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import linqs.gaia.visualization.Visualization;
import linqs.gaia.visualization.lightviz.LightVizVisualization;

/**
 * This experiment class loads a graph, as specified by the io and its configurations,
 * and runs the visualization specified.
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>ioclass-Class for the {@link IO} to instantiate using {@link Dynamic#forConfigurableName}
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>visclass-Class for the {@link Visualization} to instantiate using {@link Dynamic#forConfigurableName}
 * Default is {@link linqs.gaia.visualization.lightviz.LightVizVisualization}.
 * </UL>
 * <p>
 * Note: An example configuration can be found in resource/SampleFiles/GraphVisualizationExperimentSample.
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class GraphVisualizationExperiment extends Experiment {

	@Override
	public void runExperiment() {
		// Load Graph
		Graph graph = null;
		
		// Load IO class
		IO io = (IO)
			Dynamic.forConfigurableName(IO.class, this.getStringParameter("ioclass"));
		io.copyParameters(this);
		
		graph = io.loadGraph();
		Log.INFO(GraphUtils.getSimpleGraphOverview(graph));
		
		// Load requested visualization
		String visclass = LightVizVisualization.class.getCanonicalName();
		if(this.hasParameter("visclass")) {
			visclass = this.getStringParameter("visclass");
		}
		
		Visualization v = (Visualization)
			Dynamic.forConfigurableName(Visualization.class, visclass);
		v.copyParameters(this);
		
		// Visualize graph using the visualization
		v.visualize(graph);
		
		graph.destroy();
	}
	
	public static void main(String[] args) {
		if(args.length != 1) {
			throw new ConfigurationException("Arguments: <configfile>");
		}
		
		GraphVisualizationExperiment e = new GraphVisualizationExperiment();
		e.setCID("exp");
		e.loadParametersFile(args[0]);
		e.runExperiment();
	}
}
