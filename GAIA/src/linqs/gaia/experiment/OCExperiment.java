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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitString;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.StringValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.event.CustomEvent;
import linqs.gaia.graph.event.GraphEventListener;
import linqs.gaia.graph.io.IO;
import linqs.gaia.graph.io.SparseTabDelimIO;
import linqs.gaia.log.Log;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.ncc.SimpleBaselineClassifier;
import linqs.gaia.prediction.feature.CategValuePred;
import linqs.gaia.prediction.feature.CategValuePredGroup;
import linqs.gaia.prediction.statistic.Accuracy;
import linqs.gaia.prediction.statistic.ConfusionMatrix;
import linqs.gaia.prediction.statistic.Statistic;
import linqs.gaia.prediction.statistic.StatisticUtils;
import linqs.gaia.sampler.decorable.DecorableSampler;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;

/**
 * Experiment for running object feature classification where
 * we are classifying a single, categorical feature value.
 * <p>
 * Note: This experiment has the default configuration id of "exp".
 * <p>
 * Required Parameters:
 * <UL>
 * <LI>evaltype-Type of experimental evaluation.  The options are:
 *     <UL>
 *     <LI>innetwork-Run in network evaluation using a sampler
 *          <UL>
 *          <LI>ioclass-Required for innetwork. {@link IO} class, instantiated using {@link Dynamic#forConfigurableName},
 *          to use in loading the graph
 *          <LI>samplerclass-Sampler class (see {@link DecorableSampler}), instantiated using {@link Dynamic#forConfigurableName},
 *          to use for the sampler when doing in network evaluation.
 *          </UL>
 *     <LI>insplitnetwork-Run in network evaluation using a sampler where, based on the sampling,
 *        the network is split into two disjoint networks, one for training and the other for testing.
 *          <UL>
 *          <LI>ioclass-Required for innetwork. {@link IO} class, instantiated using {@link Dynamic#forConfigurableName},
 *          to use in loading the graph
 *          <LI>samplerclass-Sampler class (see {@link DecorableSampler}), instantiated using {@link Dynamic#forConfigurableName},
 *          to use for the sampler when doing in network evaluation.
 *          </UL>
 *     <LI>acrossnetwork-Run across network evaluation
 *          <UL>
 *          <LI>trainioclass-Required for across network. {@link IO} class, instantiated using {@link Dynamic#forConfigurableName},
 *          to use in loading the training graph
 *          <LI>testioclass-Required for across network. {@link IO} class, instantiated using {@link Dynamic#forConfigurableName},
 *          to use in loading the testing graph
 *          </UL>
 *     </UL>
 * <LI>targetfeatureID-Feature ID of feature you want to predict
 * <LI>targetschemaID-Schema where the target feature is in
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>classifierclass-{@link Classifier} class, instantiated using {@link Dynamic#forConfigurableName}, to use in classifying objects.
 * Default is {@link linqs.gaia.model.oc.ncc.SimpleBaselineClassifier}.
 * <LI>statistics-Comma delimited list of statistics (see {@link Statistic}), instantiated using {@link Dynamic#forConfigurableName},
 * to compute per split.  Statistics used must be able to handle {@link CategValuePredGroup} predictions.
 * Default is {@link linqs.gaia.prediction.statistic.ConfusionMatrix} and {@link linqs.gaia.prediction.statistic.Accuracy}.
 * <LI>savemodelfile-Directory to save the learned model to
 * <LI>loadmodelfile-Do not learn a model.  Load from the given train file.
 * <LI>featureconstructorfile-File with the feature construction configurations
 * <LI>grapheventlisteners-Comma delimited list of graph event listeners (see {@link GraphEventListener}),
 * instantiated using {@link Dynamic#forConfigurableName}, to use
 * <LI>trainonsubset-Yes/No parameter.  If yes, when doing in network training,
 * train on subset i and test on all other splits.
 * Default is to train on all other splits and test on split i.
 * <LI>debugmode-If on, show debug messages.  Otherwise, do not show debug messages.
 * <LI>predioclass-{@link IO} class, instantiated using {@link Dynamic#forConfigurableName},
 * in order to save the predicted graph to.  This feature
 * is only valid for {@link SparseTabDelimIO} when doing in network evalution.
 * </UL>
 * <p>
 * Note: An example configuration can be found in resource/SampleFiles/OCExperimentSample.
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class OCExperiment extends Experiment {
	private List<Statistic> statistics = new LinkedList<Statistic>();
	private String targetschemaID;
	private String targetfeatureID;
	private boolean trainonsubset;
	
	private String classifierclass = SimpleBaselineClassifier.class.getCanonicalName();
	private String statparam = ConfusionMatrix.class.getCanonicalName()+","+Accuracy.class.getCanonicalName();
	
	private List<String> header;
	private String delimiter = "\t";
	private String savemodelfile;
	private String loadmodelfile;
	
	public void runExperiment() {
		Log.INFO("Parameters Used: "+this.allParameters2String());
		
		// Set logging level using the configuration
		Log.setLoggingLevel(this);
		
		// Load set of requested statistics
		if(this.hasParameter("statistics")) {
			statparam = this.getStringParameter("statistics");
		}
		String[] statclasses = statparam.split(",");
		
		for(String statclass:statclasses) {
			Statistic stat = (Statistic) Dynamic.forConfigurableName(Statistic.class, statclass);
			stat.copyParameters(this);
			statistics.add(stat);
		}
		
		// Load classifier class
		if(this.hasParameter("classifierclass")) {
			this.classifierclass = this.getStringParameter("classifierclass");
		}
		
		// Print headers for statistics
		header = StatisticUtils.getHeader(statistics);
		Log.INFO("Summary"
				+delimiter
				+"Header"
				+ delimiter
				+ ListUtils.list2string(header, delimiter));
		
		// Get features
		targetfeatureID = this.getStringParameter("targetfeatureID");
		targetschemaID = this.getStringParameter("targetschemaID");
		
		// Get additional parameters
		if(this.hasParameter("savemodelfile")) {
			savemodelfile = this.getStringParameter("savemodelfile");
		}
		
		if(this.hasParameter("loadmodelfile")) {
			this.loadmodelfile = this.getStringParameter("loadmodelfile");
		}
		
		if(this.hasParameter("debugmode", "on")) {
			Log.SHOWDEBUG = true;
		}
		
		// Choose type of evaluation
		if(this.hasParameter("evaltype", "innetwork")) {
			trainonsubset = this.hasYesNoParameter("trainonsubset", "yes");
			
			this.runInNetwork();
		} else if(this.hasParameter("evaltype", "insplitnetwork")) {
			trainonsubset = this.hasYesNoParameter("trainonsubset", "yes");
			
			this.runInSplitNetwork();
		} else if(this.hasParameter("evaltype", "acrossnetwork")) {
			this.runAcrossNetwork();
		} else {
			throw new ConfigurationException("Unrecognized evaltype: "
				+this.getStringParameter("evaltype"));
		}
	}
	
	private void loadGEL(Graph g) {
		// Load graph event listener
		if(this.hasParameter("grapheventlisteners")) {
			String gels =  this.getStringParameter("grapheventlisteners");
			String parts[] = gels.split(",");
			for(String p:parts) {
				GraphEventListener gel =
					(GraphEventListener) Dynamic.forConfigurableName(GraphEventListener.class, p);
				gel.copyParameters(this);
				
				g.addListener(gel);
			}
		}
	}
	
	/**
	 * Perform in network evaluation
	 */
	public void runInSplitNetwork() {
		// Load Graph
		Graph graph = null;
		
		// Load IO class
		IO io = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("ioclass"));
		io.copyParameters(this);
		
		graph = io.loadGraph();
		Log.DEBUG("Graph loaded: Nodes="+graph.numNodes()+" Edges="+graph.numEdges());
		
		// Load feature constructor and add features
		if(this.hasParameter("featureconstructorfile")) {
			String fcfile = this.getStringParameter("featureconstructorfile");
			ExperimentUtils.loadFeatureConstruction(graph, fcfile, this);
		}
		
		// Load graph event listeners
		this.loadGEL(graph);
		
		// Sample graph
		DecorableSampler sampler = (DecorableSampler)
			Dynamic.forConfigurableName(DecorableSampler.class,this.getStringParameter("samplerclass"));
		sampler.copyParameters(this);
		sampler.generateSampling(graph.getGraphItems(targetschemaID));
		
		Log.DEBUG("Sampling complete: Number of subsets="+sampler.getNumSubsets());
		
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
			
			IO samplingio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("samplingioclass"));
			samplingio.copyParameters(this);
			samplingio.saveGraph(graph);
			
			schema = graph.getSchema(sid);
			schema.removeFeature(splitfid);
			graph.updateSchema(sid, schema);
		}
		
		List<Map<String, Double>> statmaps = new LinkedList<Map<String, Double>>();
		int numsplits = sampler.getNumSubsets();
		
		for(int i=0; i<numsplits; i++) {
			Iterable<Decorable> trainitems;
			Iterable<Decorable> testitems;
			
			// Get test and training items
			if(trainonsubset) {
				trainitems = sampler.getSubset(i);
				testitems = sampler.getNotInSubset(i);
			} else {
				trainitems = sampler.getNotInSubset(i);
				testitems = sampler.getSubset(i);
			}
			
			// Remove test instances from train graph
			Graph traingraph = graph.copy(graph.getID().getObjID()+"-traincopy");
			for(Decorable d:testitems) {
				GraphItem gi = (GraphItem) d;
				if(gi instanceof Node) {
					traingraph.removeNodeWithEdges(((Node) gi).getID().copyWithoutGraphID());
				} else {
					traingraph.removeGraphItem(gi.getID().copyWithoutGraphID());
				}
			}
			Log.DEBUG("Training Graph: "+GraphUtils.getSimpleGraphOverview(traingraph));
			
			// Initialize classifier
			Classifier classifier = (Classifier)
				Dynamic.forConfigurableName(Classifier.class, this.classifierclass);
			classifier.copyParameters(this);
			
			// Train classifier
			if(this.loadmodelfile==null) {
				Log.DEBUG("Training classifier for split "+i);
				classifier.learn(traingraph, targetschemaID, targetfeatureID);
			} else {
				Log.DEBUG("Loading saved classifier");
				classifier.loadModel(this.loadmodelfile+"-split-"+i);
			}
			
			if(this.savemodelfile!=null) {
				classifier.saveModel(this.savemodelfile+"-split-"+i);
			}
			
			// Remove train graph since its no longer needed
			traingraph.destroy();
			
			// Remove train instances from test graph
			Graph testgraph = graph.copy(graph.getID().getObjID()+"-testcopy");
			for(Decorable d:trainitems) {
				GraphItem gi = (GraphItem) d;
				if(gi instanceof Node) {
					testgraph.removeNodeWithEdges(((Node) gi).getID().copyWithoutGraphID());
				} else {
					testgraph.removeGraphItem(gi.getID().copyWithoutGraphID());
				}
			}
			Log.DEBUG("Test Graph: "+GraphUtils.getSimpleGraphOverview(testgraph));
			
			// Save the test graph prior to removing labels
			if(this.hasParameter("splitioclass")) {
				IO splitio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("splitioclass"));
				splitio.copyParameters(this);
				
				if(splitio instanceof SparseTabDelimIO) {
					String prefix = splitio.getCID()==null ? "" : (splitio.getCID()+".");
					splitio.setParameter(prefix+"filedirectory",
							this.getStringParameter(prefix+"filedirectory")+"-split-"+i);
				}
				
				splitio.saveGraph(testgraph);
			}
			
			// Remove labels from test graph
			Iterator<GraphItem> gitr = testgraph.getGraphItems(targetschemaID);
			while(gitr.hasNext()) {
				GraphItem gi = gitr.next();
				gi.removeFeatureValue(targetfeatureID);
			}
			
			// Test classifier
			Log.DEBUG("Testing classifier for split "+i);
			classifier.predict(testgraph);
			
			// Get predictions
			Feature f = testgraph.getSchema(this.targetschemaID).getFeature(this.targetfeatureID);
			if(!(f instanceof CategFeature)) {
				throw new ConfigurationException("Cannot classify non-categorical featue: "
						+f.getClass().getCanonicalName());
			}
			
			CategValuePredGroup cvpg =
				new CategValuePredGroup(((CategFeature) f).getAllCategories());
			Iterable<GraphItem> alltest = testgraph.getIterableGraphItems(targetschemaID);
			for(GraphItem gi: alltest) {
				FeatureValue fv = gi.getFeatureValue(targetfeatureID);
				CategValuePred cvp = null;
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					cvp = new CategValuePred(
							((CategValue) graph.getEquivalentGraphItem(gi).
									getFeatureValue(targetfeatureID)).getCategory(),
							null);
				} else {
					CategValue predvalue = (CategValue) fv;
					cvp = new CategValuePred(
							((CategValue) graph.getEquivalentGraphItem(gi).
									getFeatureValue(targetfeatureID)).getCategory(),
							predvalue.getCategory(),
							predvalue.getProbs());
				}
				
				cvpg.addPrediction(cvp);
			}
			
			// Get split statistics
			Log.INFO("Statistics for split "+i+" ("+(i+1)+ "/"+numsplits+")");
			for(Statistic stat: statistics) {
				String statstring = stat.getStatisticString(cvpg);
				Log.INFO("Statistic string: "+statstring);
			}
			
			// Saved the predicted graph
			if(this.hasParameter("predioclass")) {
				IO predio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("predioclass"));
				predio.copyParameters(this);
				
				if(predio instanceof SparseTabDelimIO) {
					String prefix = predio.getCID()==null ? "" : (predio.getCID()+".");
					predio.setParameter(prefix+"filedirectory",
							this.getStringParameter(prefix+"filedirectory")+"-split-"+i);
				}
				
				predio.saveGraph(testgraph);
			}
			
			// Print statistic for split in a single line
			Map<String, Double> foldstats = StatisticUtils.getDoubleStatistics(
					statistics, cvpg, header, delimiter);
			Log.INFO("Summary"
					+ delimiter
					+ "Statistics"
					+ delimiter
					+ (i + 1)+ "/"+ numsplits
					+ delimiter
					+ StatisticUtils
							.getStatistics(foldstats, header, delimiter));
			
			// Store statistics for average
			statmaps.add(foldstats);
			
			// Destroy test graph
			testgraph.destroy();
		}
		
		// Get average statistics
		Map<String, Double> avgstats = StatisticUtils.getAverageStatistics(statmaps);
		
		Log.INFO("Summary"
				+ delimiter
				+ "Statistics"
				+ delimiter
				+ "Average"
				+ delimiter
				+ StatisticUtils
					.getStatistics(avgstats, header, delimiter));
		
		graph.destroy();
	}
	
	/**
	 * Perform in network evaluation
	 */
	public void runInNetwork() {
		// Load Graph
		Graph graph = null;
		
		// Load IO class
		IO io = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("ioclass"));
		io.copyParameters(this);
		
		graph = io.loadGraph();
		Log.DEBUG("Graph loaded: Nodes="+graph.numNodes()+" Edges="+graph.numEdges());
		
		// Load feature constructor and add features
		if(this.hasParameter("featureconstructorfile")) {
			String fcfile = this.getStringParameter("featureconstructorfile");
			ExperimentUtils.loadFeatureConstruction(graph, fcfile, this);
		}
		
		// Load graph event listeners
		this.loadGEL(graph);
		
		// Sample graph
		DecorableSampler sampler = (DecorableSampler)
			Dynamic.forConfigurableName(DecorableSampler.class,this.getStringParameter("samplerclass"));
		sampler.copyParameters(this);
		sampler.generateSampling(graph.getGraphItems(targetschemaID));
		
		Log.DEBUG("Sampling complete: Number of subsets="+sampler.getNumSubsets());
		
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
			
			IO samplingio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("samplingioclass"));
			samplingio.copyParameters(this);
			samplingio.saveGraph(graph);
			
			schema = graph.getSchema(sid);
			schema.removeFeature(splitfid);
			graph.updateSchema(sid, schema);
		}
		
		List<Map<String, Double>> statmaps = new LinkedList<Map<String, Double>>();
		int numsplits = sampler.getNumSubsets();
		CCGroundTruth lastccgt = null;
		Iterable<? extends Decorable> prevtestitems = null;
		for(int i=0; i<numsplits; i++) {
			// Get test and training items
			Iterable<? extends Decorable> trainitems = null;
			Iterable<? extends Decorable> testitems = null;
			if(trainonsubset) {
				trainitems = sampler.getSubset(i);
				testitems = sampler.getNotInSubset(i);
			} else {
				trainitems = sampler.getNotInSubset(i);
				testitems = sampler.getSubset(i);
			}
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Number of instances for split "+i+" ("+(i+1)+ "/"+numsplits+")"
						+" # Train instances="+IteratorUtils.numIterable(trainitems)
						+" # Test instances="+IteratorUtils.numIterable(testitems));
			}
			
			// Initialize classifier
			Classifier classifier = (Classifier)
				Dynamic.forConfigurableName(Classifier.class, this.classifierclass);
			classifier.copyParameters(this);
			
			graph.processListeners(new CustomEvent("Preparing Data Splits"));
			
			// Restore labels removed from the last split
			if(lastccgt != null) {
				this.restoreTestValues(prevtestitems.iterator(), lastccgt, targetfeatureID);
			}

			// Remove labels and retain ground truth
			CCGroundTruth ccgt = this.removeTestValues(testitems.iterator(), targetfeatureID);
			lastccgt = ccgt;
			prevtestitems = testitems;
			
			graph.processListeners(new CustomEvent("Data Split Preparation Complete"));
			
			// Train classifier
			if(this.loadmodelfile==null) {
				Log.DEBUG("Training classifier for split "+i);
				classifier.learn(trainitems, targetschemaID, targetfeatureID);
			} else {
				Log.DEBUG("Loading saved classifier");
				classifier.loadModel(this.loadmodelfile+"-split-"+i);
			}
			
			if(this.savemodelfile!=null) {
				classifier.saveModel(this.savemodelfile+"-split-"+i);
			}
			
			graph.processListeners(new CustomEvent("Predicting Labels"));
			// Test classifier
			Log.DEBUG("Testing classifier for split "+i);
			classifier.predict(testitems);
			graph.processListeners(new CustomEvent("Label Prediction Complete"));
			
			// Get predictions
			Feature f = graph.getSchema(this.targetschemaID).getFeature(this.targetfeatureID);
			if(!(f instanceof CategFeature)) {
				throw new ConfigurationException("Cannot classify non-categorical featue: "
						+f.getClass().getCanonicalName());
			}
			
			CategValuePredGroup cvpg =
				new CategValuePredGroup(((CategFeature) f).getAllCategories());
			for(Decorable d: testitems) {
				FeatureValue fv = d.getFeatureValue(targetfeatureID);
				CategValuePred cvp = null;
				if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
					cvp = new CategValuePred(
							ccgt.getTrueClass(d).getCategory(),
							null);
				} else {
					CategValue predvalue = (CategValue) fv;
					cvp = new CategValuePred(
							ccgt.getTrueClass(d).getCategory(),
							predvalue.getCategory(),
							predvalue.getProbs());
				}
				
				cvpg.addPrediction(cvp);
			}
			
			// Get split statistics
			Log.INFO("Statistics for split "+(i+1)+" ("+(i+1)+ "/"+numsplits+")");
			for(Statistic stat: statistics) {
				String statstring = stat.getStatisticString(cvpg);
				Log.INFO("Statistic string: "+statstring);
			}
			
			// Saved the predicted graph
			if(this.hasParameter("predioclass")) {
				IO predio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("predioclass"));
				predio.copyParameters(this);
				
				if(predio instanceof SparseTabDelimIO) {
					String prefix = predio.getCID()==null ? "" : (predio.getCID()+".");
					predio.setParameter(prefix+"filedirectory",
							this.getStringParameter(prefix+"filedirectory")+"-split-"+i);
				}
				
				predio.saveGraph(graph);
			}
			
			// Print statistic for split in a single line
			Map<String, Double> foldstats = StatisticUtils.getDoubleStatistics(
					statistics, cvpg, header, delimiter);
			Log.INFO("Summary"
					+ delimiter
					+ "Statistics"
					+ delimiter
					+ (i + 1)+ "/"+ numsplits
					+ delimiter
					+ StatisticUtils
							.getStatistics(foldstats, header, delimiter));
			
			// Store statistics for average
			statmaps.add(foldstats);
		}
		
		// Get average statistics
		Map<String, Double> avgstats = StatisticUtils.getAverageStatistics(statmaps);
		
		Log.INFO("Summary"
				+ delimiter
				+ "Statistics"
				+ delimiter
				+ "Average"
				+ delimiter
				+ StatisticUtils
					.getStatistics(avgstats, header, delimiter));
		
		graph.destroy();
	}
	
	/**
	 * Run across network experiment
	 */
	public void runAcrossNetwork() {
		// Load IO class
		IO testio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("testioclass"));
		IO trainio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("trainioclass"));
		testio.copyParameters(this);
		trainio.copyParameters(this);
		
		Graph traingraph = trainio.loadGraph();
		Graph testgraph = testio.loadGraph();
		
		// Load graph event listeners to the train graph
		this.loadGEL(testgraph);
		
		// Load feature constructor and add features
		if(this.hasParameter("featureconstructorfile")) {
			String fcfile = this.getStringParameter("featureconstructorfile");
			ExperimentUtils.loadFeatureConstruction(traingraph, fcfile, this);
			ExperimentUtils.loadFeatureConstruction(testgraph, fcfile, this);
		}
		
		// Initialize classifier
		Classifier classifier = (Classifier)
			Dynamic.forConfigurableName(Classifier.class, this.classifierclass);
		classifier.copyParameters(this);
		
		// Train from full training graph
		if(this.loadmodelfile==null) {
			Log.DEBUG("Training classifier");
			classifier.learn(traingraph, targetschemaID, targetfeatureID);
		} else {
			Log.DEBUG("Loading saved classifier");
			classifier.loadModel(this.loadmodelfile);
		}
		
		if(this.savemodelfile!=null) {
			classifier.saveModel(this.savemodelfile);
		}
		
		// Remove all values from testing graph and
		// get CCGroundTruth from testing graph
		CCGroundTruth ccgt = 
			this.removeTestValues(testgraph.getGraphItems(this.targetschemaID), targetfeatureID);
		
		// Predict over the testing graph
		testgraph.processListeners(new CustomEvent("Predicting Labels"));
		Log.DEBUG("Testing classifier");
		classifier.predict(testgraph);
		testgraph.processListeners(new CustomEvent("Label Prediction Complete"));
		
		// Gather statistics
		Feature f = testgraph.getSchema(this.targetschemaID).getFeature(this.targetfeatureID);
		CategValuePredGroup cvpg = new CategValuePredGroup(((CategFeature) f).getAllCategories());
		Iterator<? extends Decorable> testitr = testgraph.getGraphItems(this.targetschemaID);
		while(testitr.hasNext()) {
			Decorable d = testitr.next();
			CategValue predvalue = (CategValue) d.getFeatureValue(targetfeatureID);
			CategValuePred cvp = new CategValuePred(
					ccgt.getTrueClass(d).getCategory(),
					predvalue.getCategory(),
					predvalue.getProbs());
			cvpg.addPrediction(cvp);
		}
		
		// Get split statistics
		Log.INFO("Statistics:");
		for(Statistic stat: statistics) {
			String statstring = stat.getStatisticString(cvpg);
			Log.INFO("Statistic string: "+statstring);
		}
		
		// Saved the predicted graph
		if(this.hasParameter("predioclass")) {
			IO predio = (IO) Dynamic.forConfigurableName(IO.class, this.getStringParameter("predioclass"));
			predio.copyParameters(this);
			predio.saveGraph(testgraph);
		}
		
		// Print statistic for split in a single line
		Map<String, Double> foldstats = StatisticUtils.getDoubleStatistics(
				statistics, cvpg, header, delimiter);
		Log.INFO("Summary"
				+ delimiter
				+ "Statistics"
				+ delimiter
				+ testgraph
				+ delimiter
				+ StatisticUtils
						.getStatistics(foldstats, header, delimiter));
		
		traingraph.destroy();
		testgraph.destroy();
	}
	
	private static class CCGroundTruth {
		private Map<Decorable, CategValue> groundtruth = new HashMap<Decorable, CategValue>();
		
		public void setTrueClass(Decorable d, CategValue cvalue) {
			this.groundtruth.put(d, cvalue);
		}
		
		public CategValue getTrueClass(Decorable d) {
			return this.groundtruth.get(d);
		}
	}
	
	private CCGroundTruth removeTestValues(Iterator<? extends Decorable> testitems, String targetfeatureid) {
		CCGroundTruth ccgt = new CCGroundTruth();
		
		while(testitems.hasNext()) {
			Decorable d = testitems.next();
			
			// Save feature value
			ccgt.setTrueClass(d, (CategValue) d.getFeatureValue(targetfeatureid));
			
			// Remove feature value
			d.setFeatureValue(targetfeatureid, FeatureValue.UNKNOWN_VALUE);
		}
		
		return ccgt;
	}
	
	private CCGroundTruth restoreTestValues(Iterator<? extends Decorable> testitems,
			CCGroundTruth ccgt, String targetfeatureid) {
		
		while(testitems.hasNext()) {
			Decorable d = testitems.next();
			
			// Remove feature value
			d.setFeatureValue(targetfeatureid, ccgt.getTrueClass(d));
		}
		
		return ccgt;
	}
	
	/**
	 * Main class to run experiment
	 * 
	 * To execute, the command is:
	 * <p>
	 * <code>
	 * java {@link linqs.gaia.experiment.OCExperiment} &lt;configfile&gt
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
		
		OCExperiment e = new OCExperiment();
		e.setCID("exp");
		e.loadParametersFile(args[0]);
		e.runExperiment();
	}
}
