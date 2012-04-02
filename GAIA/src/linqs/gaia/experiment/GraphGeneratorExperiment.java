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
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.generator.Generator;
import linqs.gaia.graph.generator.decorator.Decorator;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.noise.Noise;
import linqs.gaia.graph.statistic.GraphStatistic;
import linqs.gaia.graph.transformer.Transformer;
import linqs.gaia.log.Log;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.SimpleTimer;

/**
 * This experiment will load a graph generator, generate the graph,
 * and output the graph given the specified I/O instance.
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>ggclass-Graph generator class ({@link Generator}) to instantiate using in {@link Dynamic#forConfigurableName}
 * in order to create a graph
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>ioclass-{@link IO} class, instantiate using in {@link Dynamic#forConfigurableName}, used to output/save the graph
 * <LI>nonnoisyioclass-{@link IO} class used to output the graph prior to adding noise
 * to instantiate using in {@link Dynamic#forConfigurableName}
 * <LI>decoratorclasses-Comma delimited list of configurable {@link Decorator} classes
 * (to instantiate using {@link Dynamic#forConfigurableName}) to use add attributes to the graph
 * <LI>noiseclasses-Comma delimited list of configurable {@link Noise} classes
 * (to instantiate using {@link Dynamic#forConfigurableName}) to use add noise to the graph
 * <LI>transformerclasses=Comma delimited list of configurable {@link Transformer} classes
 * (to instantiate using {@link Dynamic#forConfigurableName}) to use transform the graph
 * <LI>graphstats-Comma delimited list of {@link GraphStatistic} classes,
 * instantiated using in {@link Dynamic#forConfigurableName},
 * to calculate over the loaded graph
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class GraphGeneratorExperiment extends Experiment {
	@Override
	public void runExperiment() {
		Log.INFO("Parameters Used: "+this.allParameters2String());
		SimpleTimer fulltimer = new SimpleTimer();
		SimpleTimer indivtimer = new SimpleTimer();
		
		// Get generator
		String ggclass = this.getStringParameter("ggclass");
		Generator gg = (Generator) Dynamic.forConfigurableName(Generator.class, ggclass);
		gg.copyParameters(this);
		Log.INFO("Generating Graph With: "+ggclass);
		indivtimer.start();
		Graph g = gg.generateGraph();
		Log.INFO("Generated Graph With: "+ggclass+" "+indivtimer.timeLapse());
		
		// Get decorator, if specified
		if(this.hasParameter("decoratorclasses")) {
			String[] decoratorclasses = this.getStringParameter("decoratorclasses").split(",");
			for(String decoratorclass:decoratorclasses) {
				Decorator d = (Decorator) Dynamic.forConfigurableName(Decorator.class, decoratorclass);
				d.copyParameters(this);
				Log.INFO("Decorating Graph With: "+decoratorclass);
				indivtimer.start();
				d.decorate(g);
				Log.INFO("Decorated Graph With: "+decoratorclass+" "+indivtimer.timeLapse());
			}
		}
		
		// Get non-noisy io
		if(this.hasParameter("nonnoisyioclass")) {
			String ioclass = this.getStringParameter("nonnoisyioclass");
			IO io = (IO) Dynamic.forConfigurableName(IO.class, ioclass);
			io.copyParameters(this);
			Log.INFO("Saving Graph With: "+ioclass);
			io.saveGraph(g);
		}
		
		// Get noise generators, if specified
		if(this.hasParameter("noiseclasses")) {
			// Print statistics of the generated graph before adding noise
			Log.INFO("Graph before noise: \n"+GraphUtils.getGraphOverview(g));
			
			String[] decoratorclasses = this.getStringParameter("noiseclasses").split(",");
			for(String decoratorclass:decoratorclasses) {
				Noise n = (Noise) Dynamic.forConfigurableName(Noise.class, decoratorclass);
				n.copyParameters(this);
				Log.INFO("Adding Noisy to Graph With: "+decoratorclass);
				indivtimer.start();
				n.addNoise(g);
				Log.INFO("Added Noisy to Graph With: "+decoratorclass+" "+indivtimer.timeLapse());
			}
		}
		
		// Get noise generators, if specified
		if(this.hasParameter("transformerclasses")) {
			// Print statistics of the generated graph before transforming
			Log.INFO("Graph before transformer: \n"+GraphUtils.getGraphOverview(g));
			
			String[] transformerclasses = this.getStringParameter("transformerclasses").split(",");
			for(String transformerclass:transformerclasses) {
				Transformer t = (Transformer) Dynamic.forConfigurableName(Transformer.class, transformerclass);
				t.copyParameters(this);
				Log.INFO("Transforming Graph With: "+transformerclass);
				indivtimer.start();
				t.transform(g);
				Log.INFO("Transforming Graph With: "+transformerclass+" "+indivtimer.timeLapse());
			}
		}
		
		// Print statistics of the generated graph
		Log.INFO("Final Graph: \n"+GraphUtils.getGraphOverview(g));
		
		if(this.hasParameter("graphstats")) {
			// Load requested Graph Statistics
			List<String> graphstats = Arrays.asList(this.getStringParameter("graphstats").split(","));
			boolean isfirst = true;
			for(String gsclass:graphstats) {
				GraphStatistic gs = (GraphStatistic)
					Dynamic.forConfigurableName(GraphStatistic.class, gsclass);
				gs.copyParameters(this);
				
				// Calculate statistics
				Map<String, String> stats = gs.getStatisticStrings(g);
				
				// Print statistics
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
		}
		
		// Get io
		if(this.hasParameter("ioclass")) {
			String ioclass = this.getStringParameter("ioclass");
			IO io = (IO) Dynamic.forConfigurableName(IO.class, ioclass);
			io.copyParameters(this);
			Log.INFO("Saving Graph With: "+ioclass);
			io.saveGraph(g);
		}
		
		// Show time to completion
		Log.INFO("Completed generating graph: "+fulltimer.timeLapse());
		
		g.destroy();
	}
	
	/**
	 * Main class to run experiment
	 * 
	 * To execute, the command is:
	 * <p>
	 * <code>
	 * java {@link linqs.gaia.experiment.GraphGeneratorExperiment} &lt;configfile&gt
	 * </code>
	 * <p>
	 * where configfile is the configuration file to load for the experiment.
	 * 
	 * @param args Arguments for experiment
	 */
	public static void main(String[] args) throws Exception {
		if(args.length != 1) {
			throw new ConfigurationException("Arguments: <configfile>");
		}
		
		GraphGeneratorExperiment e = new GraphGeneratorExperiment();
		e.setCID("exp");
		e.loadParametersFile(args[0]);
		e.runExperiment();
	}
}
