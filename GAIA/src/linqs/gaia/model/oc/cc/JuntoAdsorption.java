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
package linqs.gaia.model.oc.cc;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.converter.jung.JungConverter;
import linqs.gaia.graph.converter.junto.JuntoConverter;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;

/**
 * Wrapper to the Junto Adsorption label propagation algorithm.
 * <p>
 * Note: This exporter requires the Junto 1.2.2 Library to be in the classpath
 * (i.e., junto.jar).
 * </p>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> jcclass-Junto Converter class to use.  If not specified
 * a Junto Converted with default parameters is used.
 * <LI> Parameters corresponding to the arguments in the
 * Junto method upenn.junto.algorithm.Adsorption.Run
 * (See Junto 1.2.2 documentation for details).
 * <UL>
 * <LI> maxIter-Default is 10.
 * <LI> mode-Runs Adsorption when mode = "original" and run
 * Modified Adsorption (MAD) when mode = "modified".  Default is "original".
 * <LI> mu1-Default is 10.
 * <LI> mu2-Default is 5.
 * <LI> mu3-Default is 10.
 * <LI> normalize-Default is "yes".
 * <LI> keepTopKLabels-Default is 100.
 * <LI> useBipartiteOptimization-Default is "no".
 * <LI> verbose-Default is "no".
 * </UL>
 * 
 * </UL>
 * 
 * @author jvalentine
 * @author namatag
 *
 */
public class JuntoAdsorption extends BaseConfigurable implements Classifier {
	private static final long serialVersionUID = 1L;
	
	String targetschemaid;
	String targetfeatureid;

	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
	}

	public void learn(Graph traingraph, String targetschemaid,
			String targetfeatureid) {
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
	}

	public void predict(Iterable<? extends Decorable> testitems) {
		Set<Graph> graphs = GraphItemUtils.getGraphs(testitems.iterator());
		if(graphs.size()!=1) {
			throw new UnsupportedTypeException("Junto can only handle inference from nodes in the same graph:"
					+" Number of graphs encountered="+graphs.size());
		}
		
		Graph g = graphs.iterator().next();
		predict(g);
	}

	public void predict(Graph testgraph) {
		String jcclass = this.getStringParameter("jcclass", JungConverter.class.getCanonicalName());
		JuntoConverter convert = (JuntoConverter)
			Dynamic.forConfigurableName(JuntoConverter.class, jcclass, this);
		
		upenn.junto.graph.Graph junto_g = this.hasParameter("jcclass") ?
				convert.exportGraph(testgraph)
				: convert.exportGraph(testgraph, targetschemaid, targetfeatureid);
		
		int maxIter = this.getIntegerParameter("maxIter",10);
		String mode = this.getStringParameter("mode","original");
        double mu1 = this.getDoubleParameter("mu1",10);
        double mu2 = this.getDoubleParameter("mu1",5);
        double mu3 = this.getDoubleParameter("mu1",10);
        int keepTopKLabels = this.getIntegerParameter("keepTopKLabels", 100);
        boolean useBipartiteOptimization = this.getYesNoParameter("useBipartiteOptimization", "no");
        boolean verbose = this.getYesNoParameter("verbose", "no");
        ArrayList<?> resultList = new ArrayList<Object>(1);
        
		upenn.junto.algorithm.Adsorption.Run(junto_g,
				maxIter, mode, mu1, mu2, mu3, keepTopKLabels,
				useBipartiteOptimization, verbose, resultList);
		
		JuntoConverter.overwriteGraph(testgraph, junto_g, targetschemaid, targetfeatureid);
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		targetschemaid = this.getStringParameter("saved-targetschemaid");
		targetfeatureid = this.getStringParameter("saved-targetfeatureid");
	}
}
