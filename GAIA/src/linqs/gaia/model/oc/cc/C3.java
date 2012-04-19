package linqs.gaia.model.oc.cc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.log.Log;
import linqs.gaia.model.BootstrapModel;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.MultiFeatureClassifier;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.model.oc.ncc.VBClassifier;
import linqs.gaia.model.util.itemorder.ItemOrder;
import linqs.gaia.model.util.itemorder.PipelineOrder;
import linqs.gaia.model.util.itemorder.RandomOrder;
import linqs.gaia.sampler.decorable.DecorableSampler;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.MinMax;
import linqs.gaia.util.ProbDist;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.SimpleTimer;
import linqs.gaia.util.TopK;
import linqs.gaia.util.UnmodifiableList;

/**
 * Implementation of the Coupled Collective Classifiers (C3) algorithm.
 * <p>
 * Reference: Namata, G., Kok, S., and Getoor, L. (2011). Collective Graph Identification.
 * Proceedings of the ACM SIGKDD International Conference On Knowledge Discovery And Data Mining.
 * <p>
 * As with {@link linqs.gaia.model.oc.cc.ICA}, the relational features are implemented
 * through defining and specifying {@link linqs.gaia.feature.DerivedFeature}
 * for the items being predicted.
 * <p>
 * To improve runtime performance and to aid in analysis,
 * a number of functions called at various points in the
 * learning and inference can be overridden
 * (i.e., , {@link #preLearning(List)}, {@link #preRelationalLearning(List)},
 * {@link #postLearning(List)}, {@link #prePredicting(List)}, {@link #preIteration(List, int)}, 
 * {@link #postIteration(List, int)},  {@link #postPredicting(List)}).
 * 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> tids-Comma delimited list of task ids with a 1-1 correspondence to
 * the list of schema and feature IDs specified during the learn methods.
 * The task IDs are used to specify the classifiers and features used for each task.
 * For example, the first item in this delimited list is used to specify
 * the classifiers and features used to predict the value specified by the
 * first schema and feature ID specified.
 * <LI> &lt;tid&gt;-nonrelvbc-Vector based classifier ({@link VBClassifier}) to use on non-relational features,
 * for the task corresponding to the task id &lt;tid&gt;,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * <LI> &lt;tid&gt;-relvbc-Vector based classifier ({@link VBClassifier}) to use on relational features,
 * for the task corresponding to the task id &lt;tid&gt;,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> &lt;tid&gt;-includenonrelfeatures-Non-relational/local features, in the schema of the target feature,
 * used during bootstrapping.
 * If not defined, all features NOT of type Derived are used.
 * The parameters are treated as a
 * comma delimited list of feature ids and/or regex pattern for feature IDs.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI> &lt;tid&gt;-excludenonrelfeatures-Non-relational/local features excluded during bootstrapping.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI> &lt;tid&gt;-includerelfeatures-Relational features used after bootstrapping.
 * If not defined, all features of type Derived are used.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * Note: Relational classifiers are run using the union of the defined
 * non-relational and relational features.
 * <LI> &lt;tid&gt;-excluderelfeatures-Relational features to exclude after bootstrapping.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * Note: Relational classifiers are run using the union of the defined
 * non-relational and relational features.
 * 
 * <LI> numiterations-Number of iterations to do.  Set to 0 to only use the
 * non-relational classifier.  Default is 10.
 * <LI> bootstraplearning-If "yes", apply local classifier to unlabeled instances
 * prior to training.  This bootstraps those instances to allow for relational
 * features in a semi-supervised classification setting.  Training is still only
 * performed using the annotated instances as the bootstrap values are only
 * used in the relational features.  Default is "no".
 * <LI> ordertype-Class of {@link ItemOrder} used to order instances within each iteration.
 * Default is {@link linqs.gaia.model.util.itemorder.RandomOrder}.
 * <LI> batchupdate-If set to "yes", only update the labels at the end of each iteration
 * such that the value of relational features are based on the values of the previous iteration.
 * If set to "no", set labels within the iteration such that
 * the value of relational features are based on the most recent value, even with the same iteration,
 * of the labels.  Default is "yes".
 * <LI> permanentcommitkpct-If "yes", at each iteration, only updates the most confident
 * (in terms of probability of the most likely value) items to their predicted values
 * at the end of each iteration.
 * Once set in an iteration, the values of these items is not updated in later iterations.
 * <LI> kpercent-A value between 0 (exclusive) and 1 (inclusive) which specifies the proportion
 * of labels to commit, for each task, at the end of each iteration.  The items labeled are chosen as
 * the top K most confident (in terms of probability of the most likely value) items.
 * If permanentcommitkpct is "yes", the K is computed as K=(kpercent * number of items for each task).
 * If permanentcommitkpct is "no", the K is computed relative to the current iteration
 * (kpercent*currentiteration*number of items).
 * If kpercent<1, the batchupdate parameter is automatically set to "yes".
 * 
 * <LI> checkoscillation-If "yes", check for an oscillation by storing the predicted
 * values at every iteration and comparing (including the probability distribution)
 * the predicted values of the current iteration with the predicted values of
 * a previous iteration.  If the predicted values of the current iteration exactly
 * match the predicted values of a previous iteration, an oscillation or convergence
 * has been reached and we can stop at this iteration.  Default is "no".
 * <LI> ignoreprobs-If "yes", ignore the probabilities when checking for oscillation.
 * Default is "no".
 * <LI> checkconvergence-If "yes", check for convergence by looking at the number
 * of values changed per iteration and stopping when there are no changes made,
 * with the predicted labels (excluding the probability distribution)
 * in a particular iteration.  Default is "no".
 * 
 * <LI> performstochastic-If "yes", instead of deterministically choosing the most
 * likely label at every iteration, do a stochastic sampling instead and select
 * the label from the distribution returned by the classifier.  Default is "no".
 * <LI> performstochastictemp-If "yes", instead of deterministically choosing the most
 * likely label at every iteration, do a stochastic sampling instead and select
 * the label from the distribution returned by the classifier.  Default is "no".
 * 
 * <LI> performsampling-If "yes", instead of performing the prediction by selecting
 * the most likely label at every iteration, a sampling procedure is performed
 * where the current label is drawn from the current probability distribution.
 * The sampled label is stored for each item and the final distribution is computed
 * by looking at the number of times a given label was sampled for each item.
 * Default is no.
 * <LI> seed-The seed to use for the random number generator used when performing sampling.
 * Default is 0.
 * <LI> burnin-The number of iterations to ignore, from the beginning, as burn-in
 * for the sampling.  Note: The number of samples is (numiterations-burnin). Default is 0.
 * 
 * <LI> performstacking-If "yes", instead of learning a single relational classifier to
 * apply at every iteration, perform stacked learning to learn a different classifier
 * at each iteration.  Default is "no".
 * <LI> predlabelfids-When performing stacked learning of the relational features,
 * you need to learn from features computed over the predicted value, instead of the true value.
 * To accomplish that, you need to specify a temporary feature to store that predicted value
 * for use in the features.  This parameter is a comma delimited string where the entries
 * correspond to each of the target feature ids specified during learning and where
 * where the temporary feature, correspoding to each target feature id, is used during the
 * stacked learning.  Only applicable if performstacking=yes.
 * <LI>&lt;tid&gt;-samplerclass-For each specified task id (given by tids parameter),
 * specify the {@link DecorableSampler} class to use in sampling items of that task for the cross-validation
 * in stacked learning.  Required if performstacking=yes.
 * <LI>unlabeledstackingonly-If "yes", run the variant of stacking where you
 * learn a different set of classifiers per iteration but only over the labeled instances.
 * No cross validation is done but prior to learning the classifier for the current iteration,
 * apply the classifier from the previous iteration.
 * Only applicable if performstacking=yes.
 * Default is "no".
 * <LI>emstacking-If "yes", run the variant of stacking where you
 * learn a different set of classifiers per iteration using both the labeled and unlabeled instances.
 * Only applicable if performstacking=yes.
 * Default is "no".
 * <LI>saveperiterdir-Directory to use to save the predicted values of each item
 * for each iteration.  A file named using the iteration number (i.e., 1.txt)
 * is created for each iteration with two tab delimited colums, the first containing
 * a string representation of the object and the second a string representation of the feature value.
 * Only applicable if performstacking=yes.
 * 
 * <LI> printtrainderivetime-If "yes", print the time it takes to compute the features of the graphs
 * of the train items (computed using {@link GraphUtils#printDerivedTime(Graph)}.
 * This is useful for analyzing performance.  Default is "no".
 * <LI> printtrainvalues-If "yes", print a summary of the features of the graphs
 * of the train items (computed using {@link GraphUtils#printFeatureValueOverview(Graph)}).
 * This is useful for analyzing performance.  Default is "no".
 * <LI> printbootstrap-If "yes", print the predicted value of each item after bootstrapping.
 * This is useful for analyzing performance.  Default is "no".
 * <LI> printchanges-If "yes", print the changes found in each iteration.
 * This is useful for analyzing performance.  Default is "no".
 * <LI> printtestderivetime-If "yes", print the time it takes to compute the features of the graphs
 * of the test items (computed using {@link GraphUtils#printDerivedTime(Graph)}.
 * This is useful for analyzing performance.  Default is "no".
 * <LI> printoscillation-If "yes", print the iteration that the oscillation was encountered,
 * as well as what previous iteration was the start of the oscillation.  Default is "no".
 * <LI> printnumiterations-If "yes", print the number of iterations run during inference.
 * Default is "no".
 * <LI> printprogress-If "yes", print the current iteration, and the time to needed to complete it,
 * at the end of an iteration.  Default is "no".
 * </UL>
 * 
 * @author namatag
 *
 */
public class C3 extends BaseConfigurable
	implements Classifier, MultiFeatureClassifier, BootstrapModel {
	
	private static final long serialVersionUID = 1L;

	protected boolean shouldBootstrap = true;
	
	protected List<VBClassifier> relvbclist;
	protected List<VBClassifier> nonrelvbclist;
	protected int listsize;
	
	protected int numiterations = 10;
	protected ItemOrder itemorder = null;
	
	protected List<String> targetschemaids;
	protected List<String> targetfeatureids;
	
	protected boolean bootstraplearning = false;
	protected boolean batchupdate = true;
	
	protected double kpercent = 1;
	protected boolean permanentcommitkpct = false;
	
	protected boolean printbootstrap = false;
	protected boolean printchanges = false;
	protected boolean printtestderivetime = false;
	protected boolean printtrainderivetime = false;
	protected boolean printtrainvalues = false;
	protected boolean printoscillation = false;
	protected boolean printnumiterations = false;
	protected boolean printprogress = false;
	
	protected boolean checkoscillation = false;
	protected boolean ignoreprobs = false;
	protected boolean checkconvergence = true;
	
	protected boolean performstochastic = false;
	protected boolean performstochastictemp = false;
	
	protected boolean performsampling = false;
	protected int burnin = 0;
	protected Random rand = null;
	
	protected boolean performstacking = false;
	protected String[] predlabelfids;
	protected List<List<VBClassifier>> relvbclevellist;
	protected boolean unlabeledstackingonly = false;
	protected boolean emstacking = false;
	
	protected int numthreads = -1;
	protected boolean learninparallel = false;
	
	protected String saveperiterdir = null;
	
	public void learn(List<VBClassifier> nrvbc, List<VBClassifier> rvbc,
			List<String> targetschemaids, List<String> targetfeatureids) {
		this.initialize(targetschemaids, targetfeatureids);
		
		if(nrvbc.size() != targetschemaids.size()
			&& rvbc.size() != targetschemaids.size() 
			&& targetfeatureids.size() != targetschemaids.size()) {
			throw new InvalidStateException("All lists should have the same size: "
					+ "nrvbc: "+nrvbc.size()
					+ " rvbc: "+rvbc.size()
					+ " targetschemaids: "+targetschemaids.size()
					+ " targetfeatureids: "+targetfeatureids.size()
					);
		}
		
		this.nonrelvbclist = new ArrayList<VBClassifier>(nrvbc);
		this.relvbclist = new ArrayList<VBClassifier>(rvbc);
	}
	
	public void learn(List<Iterable<? extends Decorable>> trainitems,
			List<String> targetschemaids, List<String> targetfeatureids) {
		this.initialize(targetschemaids, targetfeatureids);
		
		String[] tids = this.getStringParameter("tids").split(",");
		if(tids.length != trainitems.size() 
			&& trainitems.size() != targetschemaids.size() 
			&& targetschemaids.size() != targetfeatureids.size()) {
			throw new InvalidStateException("All lists should have the same size: "
					+ "TIDs: "+tids.length
					+ " trainitems: "+trainitems.size()
					+ " targetschemaids: "+targetschemaids.size()
					+ " targetfeatureids: "+targetfeatureids.size()
					);
		}
		
		// Preprocess learning
		preLearning(trainitems);
		
		// Support parallelism
		ExecutorService pool = null;
		List<Future<Integer>> futurelist = null;
		if(learninparallel) {
			Log.DEBUG("Initializing thread pool of size "+numthreads);
			pool = Executors.newFixedThreadPool(numthreads);
			futurelist = new LinkedList<Future<Integer>>();
		}
		
		// Add stacking temporary features, if needed
		if(this.performstacking) {
			for(int i=0; i<listsize; i++) {
				Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
				
				// Create predicted label, if not already defined
				for(Decorable d:currtrainitems) {
					Schema dschema = d.getSchema();
					if(!dschema.hasFeature(predlabelfids[i])) {
						dschema.addFeature(predlabelfids[i],
							dschema.getFeature(this.targetfeatureids.get(i)).copy());
						((GraphItem) d).getGraph().updateSchema(d.getSchemaID(), dschema);
					}
				}
			}
		}
		
		// Learn local classifiers
		if(shouldBootstrap || bootstraplearning) {
			// Note: Only learn local classifiers if they are going to be used
			for(int i=0; i<listsize; i++) {
				String tid = tids[i];
				String sid = targetschemaids.get(i);
				String fid = targetfeatureids.get(i);
				Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
				Decorable firstitem = currtrainitems.iterator().next();
				
				Schema schema = firstitem.getSchema();
				List<String> nonrelfeatures = FeatureUtils.parseFeatureList(this, schema,
						FeatureUtils.getFeatureIDs(schema, 0),
						tid+"-includenonrelfeatures",
						tid+"-excludenonrelfeatures");
				
				Log.DEBUG("Non-Relational Features Used: "+ListUtils.list2string(nonrelfeatures, ","));
	
				// Train classifiers
				VBClassifier nonrelvbc = this.nonrelvbclist.get(i);
				if(learninparallel) {
					Future<Integer> future =
						pool.submit(new LearningThread(currtrainitems, sid, fid, nonrelfeatures, nonrelvbc));
					futurelist.add(future);
				} else {
					nonrelvbc.learn(currtrainitems, sid, fid, nonrelfeatures);
				} 
			}
			
			if(learninparallel) {
				// Wait for thread to complete
				int timewaiting = 0;
				try {
					pool.shutdown();
					while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
						timewaiting++;
						Log.DEBUG("Awaiting the bootstrap learning threads: "+timewaiting+" minutes");
					}
					
					// Check for exceptions
					for(Future<Integer> future:futurelist) {
						future.get();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		// Only learn relational classifiers if they are going to be used
		if(this.numiterations==0) {
			Log.DEBUG("Iteration number is 0.  No need to train relational classifier.");
			return;
		}
		
		// If semisupervised, bootstrap the unknown values prior
		// to training the relational classifier.
		// Note: We can probably extend this to bootstrap all
		// values that are unknown, not just the ones we're explicitly doing.
		List<List<Decorable>> bootstraptrainitems = null;
		List<List<Decorable>> traintestitems = null;
		if(bootstraplearning) {
			bootstraptrainitems = new ArrayList<List<Decorable>>();
			for(int i=0; i<listsize; i++){
				bootstraptrainitems.add(new LinkedList<Decorable>());
			}
			
			if(emstacking) {
				traintestitems = new ArrayList<List<Decorable>>();
				
				for(int i=0; i<listsize; i++){
					// Add all train items first
					List<Decorable> newlist = new LinkedList<Decorable>();
					for(Decorable d:trainitems.get(i)) {
						newlist.add(d);
					}
					
					traintestitems.add(newlist);
				}
			}
			
			Set<Graph> allgraphs = new HashSet<Graph>();
			for(Iterable<? extends Decorable> tiset: trainitems) {
				for(Decorable d:tiset) {
					allgraphs.add(((GraphItem) d).getGraph());
				}
			}
			
			for(Graph traingraph: allgraphs) {
				List<Iterable<? extends Decorable>> items = this.getItems(traingraph, true);
				if(items.size()!=listsize) {
					throw new InvalidStateException("List sizes should be the same: "
							+items.size()+"!="+listsize);
				}
				
				for(int i=0; i<listsize; i++) {
					Iterable<? extends Decorable> curritems = items.get(i);
					List<Decorable> currbootstraptrainitems = bootstraptrainitems.get(i);
					for(Decorable d:curritems) {
						currbootstraptrainitems.add(d);
					}
					
					// Add test items
					if(emstacking) {
						List<Decorable> currtraintestitems = traintestitems.get(i);
						for(Decorable d:curritems) {
							currtraintestitems.add(d);
						}
					}
				}
				
				this.bootstrap(items, true, trainitems);
			}
		}
		
		// Preprocess learning
		preRelationalLearning(trainitems);
		
		// Print values or derived time
		// Used for debugging features
		if( printtrainvalues || printtrainderivetime) {
			Set<Graph> graphs = new HashSet<Graph>();
			for(Iterable<? extends Decorable> ti:trainitems) {
				for(Decorable d:ti) {
					if(!(d instanceof GraphItem)) {
						throw new UnsupportedTypeException("Decorable items must be Graph Items: "
								+d+" of type "+d.getClass().getCanonicalName());
					}
					
					graphs.add(((GraphItem) d).getGraph());
				}
			}
			
			if(printtrainvalues) {
				for(Graph currg:graphs) {
					GraphUtils.printFeatureValueOverview(currg);
				}
			}
			
			if(printtrainderivetime) {
				// Used for debugging features
				// Note:  Will throw an exception for stacking since at this point,
				// the predicted labels used in the relational features are not defined
				for(Graph currg:graphs) {
					Log.INFO("Derived time for learning");
					GraphUtils.printDerivedTime(currg);
				}
			}
		}
		
		// Train relational classifiers
		
		// Stacking way
		if(this.performstacking) {
			// Learn a classifier for each level/iteration
			for(int l=0; l<numiterations; l++) {		
				Map<SimplePair<Decorable,Integer>,FeatureValue> batchvalues
					= new HashMap<SimplePair<Decorable,Integer>,FeatureValue>();
				
				List<VBClassifier> levelrelvbclist = this.relvbclevellist.get(l);
				
				// Bootstrap unlabeled instances using the
				// previously learned relational classifier.
				// If the first iteration, assume bootstrapping
				// occurred already using the non-relational classifier.
				if(bootstraplearning && l!=0) {
					List<VBClassifier> prevrelvbclist = this.relvbclevellist.get(l-1);
					
					if(learninparallel) {
						Log.DEBUG("Initializing thread pool of size "+numthreads);
						pool = Executors.newFixedThreadPool(numthreads);
						futurelist = new LinkedList<Future<Integer>>();
						
						Log.DEBUG("Applying bootstrap in parallel");
						List<SimplePair<Integer,Decorable>> allitems =
							new LinkedList<SimplePair<Integer,Decorable>>();
						for(int i=0; i<listsize; i++) {
							Iterable<? extends Decorable> ditrbl = bootstraptrainitems.get(i);
							for(Decorable d:ditrbl) {
								allitems.add(new SimplePair<Integer,Decorable>(i, d));
							}
						}
						
						// Use PredictionThread in a batch setting
						PredictionThread.setItems(allitems, batchvalues, null,
								checkconvergence, checkoscillation,
								printchanges, 0, null);
						
						List<String> fids = Arrays.asList(predlabelfids);
						// Execute the inference in parallel
						for(int p=0; p<this.numthreads; p++) {
							Future<Integer> future =
								pool.submit(new PredictionThread(targetschemaids, fids, prevrelvbclist));
							futurelist.add(future);
						}
						
						int timewaiting = 0;
						try {
							pool.shutdown();
							while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
								timewaiting++;
								Log.DEBUG("Awaiting the learning threads: "+timewaiting+" minutes");
							}
							
							// Check for exceptions
							for(Future<Integer> future:futurelist) {
								future.get();
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {
						// Bootstrap learning using the relational classifier
						// from previous iteration (if available) on
						// the unlabeled instances
						for(int i=0; i<listsize; i++) {
							Iterable<? extends Decorable> testitems = bootstraptrainitems.get(i);
							VBClassifier currrelvbc = prevrelvbclist.get(i);
							for(Decorable d:testitems) {
								//d.setFeatureValue(predlabelfids[i], currrelvbc.predict(d));
								batchvalues.put(new SimplePair<Decorable,Integer>(d, i),
										currrelvbc.predict(d));
							}
						}
					}
				}
				
				if(unlabeledstackingonly) {
					for(int i=0; i<listsize; i++) {
						Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
						
						for(Decorable d:currtrainitems) {
							batchvalues.put(new SimplePair<Decorable,Integer>(d, i),
									d.getFeatureValue(targetfeatureids.get(i)));
						}
					}
				} else {
					// Set the predicted value for level l-1 using cross validation for
					// use in training the classifiers in level l
					for(int i=0; i<listsize; i++) {
						String tid = tids[i];
						String sid = targetschemaids.get(i);
						String fid = targetfeatureids.get(i);
						Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
						
						// Get features to use
						Decorable firstitem = currtrainitems.iterator().next();
						Schema schema = firstitem.getSchema();
						List<String> nonrelfeatures = FeatureUtils.parseFeatureList(this, schema,
								FeatureUtils.getFeatureIDs(schema, 0),
								tid+"-includenonrelfeatures",
								tid+"-excludenonrelfeatures");
						
						List<String> relfeatures = FeatureUtils.parseFeatureList(this, schema,
								FeatureUtils.getFeatureIDs(schema, 1),
								tid+"-includerelfeatures",
								tid+"-excluderelfeatures");
						
						Log.DEBUG("Non-Relational Features Used: "+ListUtils.list2string(nonrelfeatures, ","));
						Log.DEBUG("Relational Features Used: "+ListUtils.list2string(relfeatures, ","));
						
						// Subsample the training items for cross validation
						// Initialized values of predicted labels
						String samplerclass = this.getStringParameter(tid+"-samplerclass");
						DecorableSampler sampler = (DecorableSampler)
							Dynamic.forConfigurableName(DecorableSampler.class, samplerclass, this);
						sampler.generateSampling(currtrainitems.iterator());
						
						Log.DEBUG("Sampling complete: Number of subsets="+sampler.getNumSubsets());
						
						if(learninparallel) {
							Log.DEBUG("Initializing thread pool of size "+numthreads);
							pool = Executors.newFixedThreadPool(numthreads);
							futurelist = new LinkedList<Future<Integer>>();
						}
						
						String classifiersuffix = l==0 ? "-nonrelvbc" : "-relvbc";
						List<String> features = l==0 ? nonrelfeatures : relfeatures;
						int numsplits = sampler.getNumSubsets();
						List<VBClassifier> splitvbcs = new ArrayList<VBClassifier>(numsplits);
						for(int j=0; j<numsplits; j++) {
							// Train classifiers on training split
							VBClassifier vbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class,
									this.getStringParameter(tid+classifiersuffix), this);
							splitvbcs.add(vbc);
							
							if(learninparallel) {
								Future<Integer> future =
									pool.submit(new LearningThread(sampler.getNotInSubset(j), sid, fid, features, vbc));
								futurelist.add(future);
							} else {
								vbc.learn(sampler.getNotInSubset(j), sid, fid, features);
							}
						}
						
						if(learninparallel) {
							// Wait for thread to complete
							int timewaiting = 0;
							try {
								pool.shutdown();
								while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
									timewaiting++;
									Log.DEBUG("Awaiting the learning threads: "+timewaiting+" minutes");
								}
								
								// Check for exceptions
								for(Future<Integer> future:futurelist) {
									future.get();
								}
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
						
						// Apply the predictions on each split
						List<SimplePair<Integer,Decorable>> allitems =
							new LinkedList<SimplePair<Integer,Decorable>>();;
						for(int j=0; j<numsplits; j++) {
							if(learninparallel) {
								Log.DEBUG("Initializing thread pool of size "+numthreads);
								pool = Executors.newFixedThreadPool(numthreads);
								futurelist = new LinkedList<Future<Integer>>();
								
								Log.DEBUG("Applying split vbc in parallel");
								Iterable<? extends Decorable> ditrbl = sampler.getSubset(j);
								for(Decorable d:ditrbl) {
									allitems.add(new SimplePair<Integer,Decorable>(j, d));
								}
							} else {
								VBClassifier vbc = splitvbcs.get(j);
								Iterable<Decorable> ditrbl = sampler.getSubset(j);
								for(Decorable d:ditrbl) {
									// Set the predicted label value for those in test set
									FeatureValue fv = vbc.predict(d);
									//d.setFeatureValue(predlabelfids[i], fv);
									batchvalues.put(new SimplePair<Decorable,Integer>(d, i),fv);
								}
							}
						}
						
						if(this.learninparallel) {
							// Use PredictionThread in a batch setting
							PredictionThread.setItems(allitems, batchvalues, null,
									checkconvergence, checkoscillation,
									printchanges, 0, i);
							
							// Assign the sid and fid to use for each split
							List<String> splitsids = new ArrayList<String>();
							List<String> splitfids = new ArrayList<String>();
							for(int j=0; j<numsplits; j++) {
								splitsids.add(targetschemaids.get(i));
								splitfids.add(predlabelfids[i]);
							}
							
							// Execute the inference in parallel
							for(int p=0; p<this.numthreads; p++) {
								// Do the prediction over the multiple splits
								Future<Integer> future =
									pool.submit(new PredictionThread(splitsids, splitfids, splitvbcs));
								futurelist.add(future);
							}
							
							int timewaiting = 0;
							try {
								pool.shutdown();
								while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
									timewaiting++;
									Log.DEBUG("Awaiting the learning threads: "+timewaiting+" minutes");
								}
								
								// Check for exceptions
								for(Future<Integer> future:futurelist) {
									future.get();
								}
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					}
				}			
				
				// Apply the batch updates
				Set<Entry<SimplePair<Decorable,Integer>,FeatureValue>> set = batchvalues.entrySet();
				for(Entry<SimplePair<Decorable,Integer>,FeatureValue> entry:set) {
					SimplePair<Decorable,Integer> pair = entry.getKey();
					Decorable di = pair.getFirst();
					int index = pair.getSecond();
					FeatureValue fv = entry.getValue();
					di.setFeatureValue(predlabelfids[index], fv);
				}
				
				// Preprocess learning, prior to bootstrapping
				preRelationalLearning(trainitems);
				
				// Now learn the relational classifiers using the predictions made
				// by the classifiers learned from the samplings
				
				// Learn in parallel
				if(learninparallel) {
					Log.DEBUG("Initializing thread pool of size "+numthreads);
					pool = Executors.newFixedThreadPool(numthreads);
					futurelist = new LinkedList<Future<Integer>>();
				}
				
				for(int i=0; i<listsize; i++) {
					String tid = tids[i];
					String sid = targetschemaids.get(i);
					String fid = targetfeatureids.get(i);
					Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
					
					Decorable firstitem = currtrainitems.iterator().next();
					
					// Get features to use
					Schema schema = firstitem.getSchema();
					
					// If EM, train over all items (not just training set)
					if(emstacking) {
						Log.DEV("Learning from all: "+IteratorUtils.numIterable(traintestitems.get(i))
								+ " instead of "+IteratorUtils.numIterable(trainitems.get(i)));
						currtrainitems = traintestitems.get(i);
					}
					
					List<String> relfeatures = FeatureUtils.parseFeatureList(this, schema,
							FeatureUtils.getFeatureIDs(schema, 1),
							tid+"-includerelfeatures",
							tid+"-excluderelfeatures");
					
					Log.DEBUG("Relational Features Used: "+ListUtils.list2string(relfeatures, ","));
					
					// Train classifiers
					VBClassifier relvbc = levelrelvbclist.get(i);
					
					// Assumes references to the labels, in the relational features,
					// are set to use the predicted labels in the training data.
					if(this.learninparallel) {
						Future<Integer> future =
							pool.submit(new LearningThread(currtrainitems, sid, fid, relfeatures, relvbc));
						futurelist.add(future);
					} else {
						relvbc.learn(currtrainitems, sid, fid, relfeatures);
					}
				}
				
				if(learninparallel) {
					// Wait for thread to complete
					int timewaiting = 0;
					try {
						pool.shutdown();
						while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
							timewaiting++;
							Log.DEBUG("Awaiting the learning threads: "+timewaiting+" minutes");
						}
						
						// Check for exceptions
						for(Future<Integer> future:futurelist) {
							future.get();
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		} else {
			// Initialize pool for parallelization
			if(learninparallel) {
				Log.DEBUG("Initializing thread pool of size "+numthreads);
				pool = Executors.newFixedThreadPool(numthreads);
				futurelist = new LinkedList<Future<Integer>>();
			}
			
			// Standard way
			for(int i=0; i<listsize; i++) {
				String tid = tids[i];
				String sid = targetschemaids.get(i);
				String fid = targetfeatureids.get(i);
				Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
				Decorable firstitem = currtrainitems.iterator().next();
				
				Schema schema = firstitem.getSchema();
				List<String> relfeatures = FeatureUtils.parseFeatureList(this, schema,
						FeatureUtils.getFeatureIDs(schema, 1),
						tid+"-includerelfeatures",
						tid+"-excluderelfeatures");
				
				Log.DEBUG("Relational Features Used: "+ListUtils.list2string(relfeatures, ","));
				
				// Train classifiers
				VBClassifier relvbc = this.relvbclist.get(i);
				if(this.learninparallel) {
					Future<Integer> future =
						pool.submit(new LearningThread(currtrainitems, sid, fid, relfeatures, relvbc));
					futurelist.add(future);
				} else {
					relvbc.learn(currtrainitems, sid, fid, relfeatures);
				}
			}
			
			if(learninparallel) {
				// Wait for thread to complete
				int timewaiting = 0;
				try {
					pool.shutdown();
					while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
						timewaiting++;
						Log.DEBUG("Awaiting the relational learning threads: "+timewaiting+" minutes");
					}
					
					// Check for exceptions
					for(Future<Integer> future:futurelist) {
						future.get();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		/********************************************************/
		// Perform post processing
		
		// If semisupervised, remove bootstrap values
		if(bootstraptrainitems!=null) {
			for(int i=0; i<bootstraptrainitems.size(); i++) {
				String fid = this.targetfeatureids.get(i);
				Iterable<? extends Decorable> ditrbl = bootstraptrainitems.get(i);
				for(Decorable d:ditrbl) {
					if(d.hasFeatureValue(fid)) {
						d.removeFeatureValue(fid);
					}
				}
			}
		}
		
		// Remove predicted label once done
		if(this.performstacking) {
			for(int i=0; i<listsize; i++) {
				String fid = predlabelfids[i];
				Iterable<? extends Decorable> currtrainitems = trainitems.get(i);
				
				// Create predicted label for node, if not already defined
				for(Decorable d:currtrainitems) {
					Schema dschema = d.getSchema();
					if(dschema.hasFeature(fid)) {
						dschema.removeFeature(fid);
						((GraphItem) d).getGraph().updateSchema(this.targetschemaids.get(i), dschema);
					}
				}
			}
		}
		
		// Post process learning
		postLearning(trainitems);
	}
	
	private class LearningThread implements Callable<Integer> {
		private Iterable<? extends Decorable> currtrainitems = null;
		private String sid = null;
		private String fid = null;
		private List<String> fids = null;
		private VBClassifier vbc;
		
		public LearningThread(Iterable<? extends Decorable> currtrainitems,
				String sid, String fid, List<String> fids, VBClassifier vbc) {
			this.currtrainitems = currtrainitems;
			this.sid = sid;
			this.fid  = fid;
			this.fids = fids;
			this.vbc = vbc;
		}

		public Integer call() throws Exception {
			vbc.learn(currtrainitems, sid, fid, fids);

			return 1;
		}
	}
	
	private static class PredictionThread implements Callable<Integer> {
		private List<String> targetschemaids;
		private List<String> targetfeatureids;
		private List<VBClassifier> vbclist;
		
		private static Object nextItemLock = new Object();
		
		private static List<SimplePair<Integer,Decorable>> itemlist;
		private static Map<SimplePair<Decorable,Integer>,FeatureValue> batchvalues = null;
		private static KeyedCount<String> numupdates = null;
		private static int curriteration = 0;
		private static boolean printchanges;
		private static boolean isbatchvalues = false;
		private static boolean checkcount;
		private static Integer defaultindex = 0;
		
		public PredictionThread(
				List<String> targetschemaids,
				List<String> targetfeatureids,
				List<VBClassifier> vbclist) {
			this.targetfeatureids = targetfeatureids;
			this.targetschemaids = targetschemaids;
			
			this.vbclist = new ArrayList<VBClassifier>(vbclist.size());
			// Make copies of the classifiers
			for(VBClassifier vbc:vbclist) {
				this.vbclist.add(vbc.copyModel());
			}
		}
		
		public Integer call() {
			SimplePair<Integer,Decorable> pair = this.getNextItem();
			while(pair!=null) {
				int index = pair.getFirst();
				Decorable d = pair.getSecond();
				
				VBClassifier vbc = vbclist.get(index);
				FeatureValue oldfv = d.getFeatureValue(targetfeatureids.get(index));
				FeatureValue fv = vbc.predict(d);
				
				if(!fv.equals(oldfv)) {
					if(isbatchvalues) {
						// Handle all types of batch updates, including sampling
						int batchupdateindex = PredictionThread.defaultindex==null ?
								index : PredictionThread.defaultindex;
						batchvalues.put(new SimplePair<Decorable,Integer>(d,batchupdateindex), fv);
					} else {
						d.setFeatureValue(targetfeatureids.get(index), fv);
					}
					
					// Update the modification count
					if(checkcount) {
						// Count the number of values changed, if needed
						if( ((oldfv.equals(FeatureValue.UNKNOWN_VALUE) && !fv.equals(FeatureValue.UNKNOWN_VALUE))
							|| (!oldfv.equals(FeatureValue.UNKNOWN_VALUE) && fv.equals(FeatureValue.UNKNOWN_VALUE))
							|| (oldfv instanceof CategValue
								&& fv instanceof CategValue
								&& !((CategValue) oldfv).equalsIgnoreProbs(fv))
							)
						)
						{
							String key = this.targetschemaids.get(index)
											+"."+this.targetfeatureids.get(index)
											+"."+oldfv.getStringValue()+"."+fv.getStringValue();
							numupdates.increment(key);
							
							// Print changes
							if(printchanges) {
								Log.INFO("Value Change at Iteration:\t"+curriteration+"\t"+d+"\t"+oldfv+"\t"+fv);
							}
						}
					}
				}
				
				pair = this.getNextItem();
			}
			
			return 1;
		}
		
		public static void setItems(List<SimplePair<Integer,Decorable>> itemlist,
				Map<SimplePair<Decorable,Integer>,FeatureValue> batchvalues,
				KeyedCount<String> numupdates,
				boolean checkconvergence, boolean checkoscillation,
				boolean printchanges, int curriteration,
				Integer defaultindex) {
			if(PredictionThread.itemlist!=null && !PredictionThread.itemlist.isEmpty()) {
				throw new InvalidStateException("Attempting to update item list while it still contains items: "
						+PredictionThread.itemlist.size());
			}
			
			PredictionThread.itemlist = itemlist;
			
			// Support batch updates
			PredictionThread.batchvalues = batchvalues;
			isbatchvalues = batchvalues==null ? false : true;
			
			// Support detecting changes
			PredictionThread.numupdates = numupdates;
			PredictionThread.printchanges = printchanges;
			PredictionThread.curriteration = curriteration;
			
			checkcount = PredictionThread.numupdates != null &&
				(checkconvergence || checkoscillation || printchanges || Log.SHOWDEBUG);
			
			// Support using an alternate index
			PredictionThread.defaultindex = defaultindex;
		}
		
		private SimplePair<Integer,Decorable> getNextItem() {
			synchronized(nextItemLock) {
				SimplePair<Integer,Decorable> nextpair = itemlist.isEmpty() ? null : itemlist.remove(0);
		        return nextpair;
			}
	    }
	}
	
	public void learn(Graph traingraph, List<String> targetschemaids,
			List<String> targetfeatureids) {
		this.initialize(targetschemaids, targetfeatureids);
		this.learn(this.getItems(traingraph, false), targetschemaids, targetfeatureids);
	}
	
	private List<Iterable<? extends Decorable>> getItems(Graph graph, boolean istest) {
		List<Iterable<? extends Decorable>> trainitems =
			new ArrayList<Iterable<? extends Decorable>>(this.listsize);
		
		for(int i=0; i<this.listsize; i++) {
			String sid = this.targetschemaids.get(i);
			String fid = this.targetfeatureids.get(i);
			Iterable<? extends Decorable> iterable =
				OCUtils.getItemsByFeature(graph, sid, fid, istest);
			
			trainitems.add(iterable);
		}
		
		return trainitems;
	}
	
	public void predict(List<Iterable<? extends Decorable>> testitems) {
		prePredicting(testitems);
		
		this.itemorder.initializeItemLists(testitems);
		
		// Apply bootstrap
		if(this.shouldBootstrap) {
			SimpleTimer itertimer = new SimpleTimer();
			this.bootstrap(testitems, false, null);
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Time to complete bootstrap: "+itertimer.timeLapse());
			}
		}
		
		Map<SimplePair<Decorable,Integer>,FeatureValue> batchvalues = null;
		if(this.batchupdate) {
			batchvalues = new ConcurrentHashMap<SimplePair<Decorable,Integer>,FeatureValue>();
		}
		
		Map<Integer,Integer> index2num = new ConcurrentHashMap<Integer,Integer>();
		for(int j=0; j<this.listsize; j++) {
			index2num.put(j, IteratorUtils.numIterable(testitems.get(j)));
		}
		
		// Handle permament commit
		List<Set<Decorable>> permcommited = new ArrayList<Set<Decorable>>();
		if(permanentcommitkpct) {
			for(int j=0; j<listsize; j++) {
				permcommited.add(new HashSet<Decorable>());
			}
		}
		
		// Create sampling
		Map<Integer,Map<Decorable,double[]>> samples = null;
		Map<Integer,Map<String,Integer>> val2indexlist = null;
		Map<Integer,Map<Integer,String>> index2vallist = null;
		if(this.performsampling || this.performstochastic || this.performstochastictemp) {
			samples = new HashMap<Integer,Map<Decorable,double[]>>();
			val2indexlist = new HashMap<Integer,Map<String,Integer>>(listsize);
			index2vallist = new HashMap<Integer,Map<Integer,String>>(listsize);
		}
		
		// Create list of previous iterations to store
		List<List<Map<Decorable,Object>>> prevvalues = null;
		if(this.checkoscillation) {
			prevvalues = new ArrayList<List<Map<Decorable,Object>>>(numiterations);
		}
		
		// Iterate over items
		List<PredictionThread> savedpts = this.performstacking 
				? null : new ArrayList<PredictionThread>(numthreads);
		int i=0;
		for(i=0; i<this.numiterations; i++) {
			SimpleTimer itertimer = new SimpleTimer();
			preIteration(testitems, i);
			itemorder.reset();
			
			// Used for debugging derived feature computation time.
			if(printtestderivetime) {
				// Get graphs
				Set<Graph> graphs = new HashSet<Graph>();
				for(Iterable<? extends Decorable> ti:testitems) {
					for(Decorable d:ti) {
						if(!(d instanceof GraphItem)) {
							throw new UnsupportedTypeException("Decorable items must be Graph Items: "
									+d+" of type "+d.getClass().getCanonicalName());
						}
						
						graphs.add(((GraphItem) d).getGraph());
					}
				}
				
				for(Graph g:graphs) {
					Log.INFO("Derived time at iteration: "+(i+1));
					GraphUtils.printDerivedTime(g);
				}
			}
			
			// Support permanently committing the top K-Percent values
			Map<Integer,TopK<SimplePair<Decorable,FeatureValue>>> index2topk = null;
			if(this.kpercent<1) {
				index2topk = new ConcurrentHashMap<Integer,TopK<SimplePair<Decorable,FeatureValue>>>();
				// If permanently committing k percent, only commit k percent
				// (computed from the initial set of nodes) at every iteration.
				// Otherwise, the percentage to commit at iteration i (starting at i=0) is (i+1)*(k percent).
				double currkpct = permanentcommitkpct ? this.kpercent : (((double) i+1) * this.kpercent);
				
				for(int j=0; j<this.listsize; j++) {
					int currk = (int) (currkpct * index2num.get(j));
					if(i==(this.numiterations-1)) {
						currk = index2num.get(j);
					}
					
					TopK<SimplePair<Decorable,FeatureValue>> topk =
						new TopK<SimplePair<Decorable,FeatureValue>>(currk, true);
					index2topk.put(j, topk);
				}
			}
			
			// Used to time the prediction
			Map<Integer,MinMax> predtime = new HashMap<Integer,MinMax>();
			if(Log.SHOWDEBUG) {
				for(int mmi=0; mmi<listsize; mmi++) {
					predtime.put(mmi, new MinMax());
				}
			}
			
			// Can only parallelize batch updating
			KeyedCount<String> numupdates = new KeyedCount<String>();
			if(learninparallel && batchupdate) {
				ExecutorService pool = Executors.newFixedThreadPool(numthreads);
				List<Future<Integer>> futurelist = new LinkedList<Future<Integer>>();;
				
				// Get items to infer in parallel
				List<SimplePair<Integer,Decorable>> allitems =
					new LinkedList<SimplePair<Integer,Decorable>>();
				while(true) {
					// Get next classifier, test item pair
					SimplePair<Integer, Object> pair = this.itemorder.getNextPair();
					
					// Stop if no more pairs available (finishing 1 iteration)
					if(pair==null) {
						break;
					}
					
					int index = pair.getFirst();
					Decorable di = (Decorable) pair.getSecond();
					
					// Skip items already permanently committed
					if(this.permanentcommitkpct) {
						Set<Decorable> prevpermcommited = permcommited.get(index);
						if(prevpermcommited.contains(di)) {
							continue;
						}
					}
					
					allitems.add(new SimplePair<Integer,Decorable>(index,di));
				}
				
				// Use PredictionThread in a batch setting, for checking update
				PredictionThread.setItems(allitems, batchvalues, numupdates,
						checkconvergence, checkoscillation,
						printchanges, i, null);
				
				// Apply prediction in parallel
				List<VBClassifier> currrelvbclist = getRelClassifiers(i);
				
				if(this.performstacking || (savedpts!=null && savedpts.isEmpty())) {
					// Execute the inference in parallel
					for(int p=0; p<this.numthreads; p++) {
						PredictionThread pt = new PredictionThread(this.targetschemaids, 
								this.targetfeatureids, currrelvbclist);
						if(!this.performstacking) {
							savedpts.add(pt);
						}
						
						Future<Integer> future = pool.submit(pt);
						futurelist.add(future);
					}
				} else {
					for(PredictionThread pt:savedpts) {
						Future<Integer> future = pool.submit(pt);
						futurelist.add(future);
					}
				}
				
				int timewaiting = 0;
				try {
					pool.shutdown();
					while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
						timewaiting++;
						Log.DEBUG("Awaiting the prediction threads: "+timewaiting+" minutes");
					}
					
					// Check for exceptions
					for(Future<Integer> future:futurelist) {
						future.get();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				while(true) {
					// Get next classifier, test item pair
					SimplePair<Integer, Object> pair = this.itemorder.getNextPair();
					
					// Stop if no more pairs available (finishing 1 iteration)
					if(pair==null) {
						break;
					}
					
					int index = pair.getFirst();
					Decorable di = (Decorable) pair.getSecond();
					
					// Skip items already permanently committed
					if(this.permanentcommitkpct) {
						Set<Decorable> prevpermcommited = permcommited.get(index);
						if(prevpermcommited.contains(di)) {
							continue;
						}
					}
					
					// Predict for given pair
					VBClassifier vbc = this.getRelClassifier(index, i);
	
					SimpleTimer predtimer = new SimpleTimer();
					FeatureValue fv = vbc.predict(di);
					if(Log.SHOWDEBUG) {
						predtime.get(index).addValue(predtimer.msecLapse());
					}
					
					FeatureValue oldfv = di.getFeatureValue(targetfeatureids.get(index));
					if(index2topk!=null) {
						TopK<SimplePair<Decorable,FeatureValue>> topk = index2topk.get(index);
						double[] probs = ((CategValue) fv).getProbs();
						double maxprob = ArrayUtils.maxValue(probs);
						
						topk.add(maxprob, new SimplePair<Decorable,FeatureValue>(di,fv));
					} else if(this.batchupdate) {
						// Handle all types of batch updates, including sampling
						batchvalues.put(new SimplePair<Decorable,Integer>(di,index), fv);
					} else if(this.performsampling) {
						// Handle sampling, when not doing batch updates
						// Update sampling
						FeatureValue sampledfv = 
							this.updateSampling(i, index, di, fv, samples, val2indexlist, index2vallist);
						di.setFeatureValue(targetfeatureids.get(index), sampledfv);
					} else if(this.performstochastic || performstochastictemp) {
						if(!index2vallist.containsKey(index)) {
							// Initialize index list
							Schema schema = di.getSchema();
							Feature f = schema.getFeature(this.targetfeatureids.get(index));
							if(!(f instanceof CategFeature)) {
								throw new InvalidStateException("Only categorical features supported: "
										+f+" of type "+f.getClass().getCanonicalName());
							}

							Map<Integer,String> index2val = new HashMap<Integer,String>();
							UnmodifiableList<String> vallist = ((CategFeature) f).getAllCategories();
							for(int j=0; j<vallist.size(); j++) {
								index2val.put(j, vallist.get(j));
							}
							
							index2vallist.put(index, index2val);
						}
						
						// Handle stochastic assignment instead of deterministically
						// choosing the most likely one
						CategValue cv = (CategValue) fv;
						int valindex = ProbDist.sampleProbDist(cv.getProbs(), true, rand);
						String sampledcat = index2vallist.get(index).get(valindex);
						FeatureValue sampledfv = new CategValue(sampledcat);
						
						if(performstochastic 
							|| (performstochastictemp && rand.nextDouble() < (1.0-((double) i/(this.numiterations-1.0))))) {
							di.setFeatureValue(targetfeatureids.get(index), sampledfv);
						} else {
							di.setFeatureValue(targetfeatureids.get(index), fv);
						}
					} else {
						di.setFeatureValue(targetfeatureids.get(index), fv);
					}
					
					// Count the number of values changed, if needed
					if( (this.checkconvergence || this.checkoscillation || printchanges || Log.SHOWDEBUG)
						&&
						((oldfv.equals(FeatureValue.UNKNOWN_VALUE) && !fv.equals(FeatureValue.UNKNOWN_VALUE))
						|| (!oldfv.equals(FeatureValue.UNKNOWN_VALUE) && fv.equals(FeatureValue.UNKNOWN_VALUE))
						|| (oldfv instanceof CategValue
							&& fv instanceof CategValue
							&& !((CategValue) oldfv).equalsIgnoreProbs(fv))
						)
					)
					{
						numupdates.increment(this.targetschemaids.get(index)
								+"."+this.targetfeatureids.get(index)
								+"."+oldfv.getStringValue()+"."+fv.getStringValue());
						
						// Print changes
						if(printchanges) {
							Log.INFO("Value Change at Iteration:\t"+i+"\t"+di+"\t"+oldfv+"\t"+fv);
						}
					}
				}
			}
			
			// Print the prediction time for each item type
			if(Log.SHOWDEBUG) {
				for(Entry<Integer,MinMax> entry:predtime.entrySet()) {
					int index = entry.getKey();
					MinMax mm = entry.getValue();
					Log.DEBUG("Time to predict "
							+targetschemaids.get(index)+"."+targetfeatureids.get(index)+": "
							+"Total="+SimpleTimer.msec2string(mm.getSumTotal(), true)
							+" "+mm.toString());
				}
			}
			
			if(index2topk!=null) {
				// Process those to permanently commit
				for(int j=0; j<this.listsize; j++) {
					TopK<SimplePair<Decorable,FeatureValue>> topk = index2topk.get(j);
					Set<SimplePair<Decorable,FeatureValue>> topkset = topk.getTopK();
					String fid = this.targetfeatureids.get(j);
					for(SimplePair<Decorable,FeatureValue> pair:topkset) {
						Decorable d = pair.getFirst();
						FeatureValue fv = pair.getSecond();
						d.setFeatureValue(fid, fv);
						
						// Remember decorable was already committed
						if(this.permanentcommitkpct) {
							Set<Decorable> prevpermcommited = permcommited.get(j);
							prevpermcommited.add(d);
						}
					}
					
					if(Log.SHOWDEBUG) {
						Log.DEBUG("Committed top K="+topkset.size()+" of "+index2num.get(j)+" for "
							+this.targetschemaids.get(j)+"."+this.targetfeatureids.get(j));
					}
				}
			} else if(this.batchupdate) {
				// Process batch updating
				Set<Entry<SimplePair<Decorable,Integer>,FeatureValue>> set = batchvalues.entrySet();
				for(Entry<SimplePair<Decorable,Integer>,FeatureValue> entry:set) {
					SimplePair<Decorable,Integer> pair = entry.getKey();
					Decorable di = pair.getFirst();
					int index = pair.getSecond();
					FeatureValue fv = entry.getValue();
					
					if(performsampling) {
						// Update sampling
						FeatureValue sampledfv = 
							this.updateSampling(i, index, di, fv, samples, val2indexlist, index2vallist);
						di.setFeatureValue(targetfeatureids.get(index), sampledfv);
					} else if(this.performstochastic || performstochastictemp) {
						if(!index2vallist.containsKey(index)) {
							// Initialize index list
							Schema schema = di.getSchema();
							Feature f = schema.getFeature(this.targetfeatureids.get(index));
							if(!(f instanceof CategFeature)) {
								throw new InvalidStateException("Only categorical features supported: "
										+f+" of type "+f.getClass().getCanonicalName());
							}

							Map<Integer,String> index2val = new HashMap<Integer,String>();
							UnmodifiableList<String> vallist = ((CategFeature) f).getAllCategories();
							for(int j=0; j<vallist.size(); j++) {
								index2val.put(j, vallist.get(j));
							}
							
							index2vallist.put(index, index2val);
						}
						
						// Handle stochastic assignment instead of deterministically
						// choosing the most likely one
						CategValue cv = (CategValue) fv;
						int valindex = ProbDist.sampleProbDist(cv.getProbs(), true, rand);
						String sampledcat = index2vallist.get(index).get(valindex);
						FeatureValue sampledfv = new CategValue(sampledcat);
						
						if(performstochastic 
							|| (performstochastictemp && rand.nextDouble() < (1.0-((double) i/(this.numiterations-1.0))))) {
							di.setFeatureValue(targetfeatureids.get(index), sampledfv);
						} else {
							di.setFeatureValue(targetfeatureids.get(index), fv);
						}
					} else {
						di.setFeatureValue(targetfeatureids.get(index), fv);
					}
				}
				
				batchvalues.clear();
			}
			
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Number changed in iteration "
						+(i+1)+": "+numupdates.toString("=",",")
						+" "+itertimer.timeLapse());
			}
			
			// Perform post iteration
			postIteration(testitems, i);
			
			// Save per iteration results, if requested
			if(this.saveperiterdir!=null) {
				FileIO.createDirectories(this.saveperiterdir);
				
				try {
					FileWriter fstream = new FileWriter(saveperiterdir+File.separator+(i+1));
					BufferedWriter out = new BufferedWriter(fstream);
					
					// Save values
					for(int j=0; j<this.listsize; j++) {
						Iterable<? extends Decorable> curritems = testitems.get(j);
						for(Decorable d:curritems) {
							out.write(d+"\t"+d.getFeatureValue(this.targetfeatureids.get(j))+"\n");
						}
					}
					
					out.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			if(printprogress) {
				Log.INFO("Completed iteration "+(i+1)+" in "+itertimer.timeLapse());
			}
			
			// Test below to see whether or not to stop iterating
			
			// If no value has changed, convergence has occurred so stop
			if(index2topk==null && checkconvergence && !checkoscillation && numupdates.numKeys()==0) {
				// Handle the special case of a pipeline ordering
				if(itemorder instanceof PipelineOrder) {
					PipelineOrder porder = (PipelineOrder) itemorder;
					porder.stopCurrent();
					
					// Break if there are no updates after
					// running the last set at least once
					if(porder.isLastInList()) {
						break;
					}
				} else {
					// If not doing topK and there are no more updates, break.
					break;
				}
			}
			
			// Check oscillation by comparing to the predicted
			// values of previous iterations and see if the current
			// set of values are exactly the same as the values in a previous iteration
			if(checkoscillation) {
				List<Map<Decorable,Object>> maplist =
					new ArrayList<Map<Decorable,Object>>(this.listsize);
				for(int j=0; j<this.listsize; j++) {
					String currfid = targetfeatureids.get(j);
					Map<Decorable,Object> dec2fv = new HashMap<Decorable,Object>();
					Iterable<? extends Decorable> currtestitems = testitems.get(j);
					for(Decorable d:currtestitems) {
						dec2fv.put(d, 
								ignoreprobs ? d.getFeatureValue(currfid).getStringValue()
										: d.getFeatureValue(currfid));
					}
					
					maplist.add(dec2fv);
				}
				
				boolean oscillationreached = false;
				int numprevs = prevvalues.size();
				int p=0;
				for(p=0; p<numprevs; p++) {
					// Skip comparing to the previous one if
					// the labels have been known to change
					if(p==0 && numupdates.numKeys()!=0) {
						continue;
					}
					
					boolean matchcurrent = true;
					List<Map<Decorable,Object>> prevlist = prevvalues.get(p);
					for(int j=0; j<this.listsize; j++) {
						// Compare map objects
						// If not doing topk the values are the same,
						// oscillation detected and break out
						if(index2topk==null && !prevlist.get(j).equals(maplist.get(j))) {
							// At least one set of values doesn't match exactly so break
							matchcurrent = false;
							break;
						}
					}
					
					if(matchcurrent) {
						// Current iteration matches the predicted
						// values of a previous one
						oscillationreached = true;
						break;
					}
				}
				
				// If all the items match, oscillation is reached
				if(oscillationreached) {
					// Print oscillation detected
					if(printoscillation) {
						Log.INFO("Reached an oscillation at "
								+(i+1)+" with previous iteration "+((i+1)-(p+1)));
					}
					
					// Handle the special case of a pipeline ordering
					if(itemorder instanceof PipelineOrder) {
						PipelineOrder porder = (PipelineOrder) itemorder;
						porder.stopCurrent();
						
						// Break if there are no updates after
						// running the last set at least once
						if(porder.isLastInList()) {
							break;
						}
					} else {
						// If not doing topK and there are no more updates, break.
						break;
					}
				}
				
				// Insert to the top of the list
				// since the most likely oscillation probably
				// occurred in a recent iteration
				prevvalues.add(0, maplist);
			}
		}
		
		if(performsampling) {
			// Set the values and probabilities of attributes based on sampling
			int numsamples = this.numiterations-this.burnin;
			Set<Integer> indices = samples.keySet();
			if(indices.isEmpty()) {
				throw new InvalidStateException("No samples were taken");
			}
			
			Log.DEBUG("Number of samples collected for each item: "+numsamples);
			
			for(Integer index:indices) {
				Map<Integer,String> index2val = index2vallist.get(index);
				
				Map<Decorable,double[]> isamples = samples.get(index);
				Set<Decorable> dset = isamples.keySet();
				for(Decorable d:dset) {
					double[] probs = isamples.get(d);
					
					// Sum array entries to make sure they add up
					int sum = 0;
					for(double p:probs) {
						sum+=p;
					}
					
					if(sum!=numsamples) {
						throw new InvalidStateException(
								"Number of samples does not match number of iterations: "+
								"Expected "+numsamples+".  Got "+sum+" samples");
					}
					
					// Convert counts to probs
					for(int pi=0; pi<probs.length; pi++) {
						probs[pi] = probs[pi]/numsamples;
					}
					
					// Set the value to the most sampled value
					int maxindex = ArrayUtils.maxValueIndex(probs);
					d.setFeatureValue(this.targetfeatureids.get(index),
							new CategValue(index2val.get(maxindex), probs));
				}
			}
		}
		
		if(printnumiterations) {
			Log.INFO("Actual number of iterations run: "+(i<this.numiterations ? i+1 : i));
		} else {
			Log.DEBUG("Actual number of iterations run: "+(i<this.numiterations ? i+1 : i));
		}
		
		// Print last iteration
		if(this.saveperiterdir!=null) {
			FileIO.createDirectories(this.saveperiterdir);
			
			try {
				FileWriter fstream = new FileWriter(saveperiterdir+File.separator+(i+1));
				BufferedWriter out = new BufferedWriter(fstream);
				
				// Save values
				for(int j=0; j<this.listsize; j++) {
					Iterable<? extends Decorable> curritems = testitems.get(j);
					for(Decorable d:curritems) {
						out.write(d+"\t"+d.getFeatureValue(this.targetfeatureids.get(j))+"\n");
					}
				}
				
				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		postPredicting(testitems);
	}
	
	public void predict(Graph testgraph) {
		this.predict(this.getItems(testgraph, true));
	}
	
	public void bootstrap(List<Iterable<? extends Decorable>> testitems,
			boolean bootstraptrain, List<Iterable<? extends Decorable>> trainitems) {
		if(learninparallel) {
			Log.DEBUG("Initializing thread pool of size "+numthreads);
			ExecutorService pool = Executors.newFixedThreadPool(numthreads);
			List<Future<Integer>> futurelist = new LinkedList<Future<Integer>>();
			
			Log.DEBUG("Applying bootstrap in parallel");
			List<SimplePair<Integer,Decorable>> allitems =
				new LinkedList<SimplePair<Integer,Decorable>>();
			for(int i=0; i<testitems.size(); i++) {
				Iterable<? extends Decorable> ditrbl = testitems.get(i);
				for(Decorable d:ditrbl) {
					allitems.add(new SimplePair<Integer,Decorable>(i, d));
				}
			}
			
			// Use PredictionThread in an incremental setting
			PredictionThread.setItems(allitems, null, null,
					checkconvergence, checkoscillation,
					printchanges, 0, null);
			
			List<String> fids = performstacking && bootstraptrain ? 
					Arrays.asList(predlabelfids) : this.targetfeatureids;
			// Execute the inference in parallel
			for(int p=0; p<this.numthreads; p++) {
				Future<Integer> future =
					pool.submit(new PredictionThread(this.targetschemaids,
							fids, this.nonrelvbclist));
				futurelist.add(future);
			}
			
			int timewaiting = 0;
			try {
				pool.shutdown();
				while(!pool.awaitTermination(1, TimeUnit.MINUTES)) {
					timewaiting++;
					Log.DEBUG("Awaiting the prediction threads: "+timewaiting+" minutes");
				}
				
				// Check for exceptions
				for(Future<Integer> future:futurelist) {
					future.get();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			Log.DEBUG("Applying bootstrap");
			for(int i=0; i<testitems.size(); i++) {
				Iterable<? extends Decorable> ditrbl = testitems.get(i);
				for(Decorable d:ditrbl) {
					// Predict for given pair
					VBClassifier vbc = this.nonrelvbclist.get(i);
					FeatureValue fv = vbc.predict(d);
					if(performstacking && bootstraptrain) {
						d.setFeatureValue(this.predlabelfids[i], fv);
					} else {
						d.setFeatureValue(this.targetfeatureids.get(i), fv);
					}
					
					// Print bootstrap values
					if(printbootstrap) {
						Log.INFO("Boostrap Value:\t"+d+"\t"+fv);
					}
				}
			}
		}
	}
	
	/**
	 * Return relational classifier for items in the current index
	 * for the specified iteration.
	 * 
	 * @param index Index item belongs to
	 * @param iteration Number of current iteration (starting from 0)
	 * @return Relational classifier to use during prediction
	 */
	protected VBClassifier getRelClassifier(int index, int iteration) {
		VBClassifier vbc = null;
		if(this.performstacking) {
			vbc = this.relvbclevellist.get(iteration).get(index);
		} else {
			vbc = this.relvbclist.get(index);
		}
		
		return vbc;
	}
	
	/**
	 * Return relational classifier for items in the current index
	 * for the specified iteration.
	 * 
	 * @param index Index item belongs to
	 * @param iteration Number of current iteration (starting from 0)
	 * @return Relational classifier to use during prediction
	 */
	protected List<VBClassifier> getRelClassifiers(int iteration) {
		if(this.performstacking) {
			return this.relvbclevellist.get(iteration);
		} else {
			return this.relvbclist;
		}
	}
	
	protected void initialize(List<String> targetschemaids, List<String> targetfeatureids) {
		this.targetschemaids = targetschemaids;
		this.targetfeatureids = targetfeatureids;
		this.listsize = targetschemaids.size();
		
		// Get parameters
		this.numiterations = 10;
		if(this.hasParameter("numiterations")) {
			this.numiterations = (int) this.getDoubleParameter("numiterations");
		}
		
		bootstraplearning = this.hasYesNoParameter("bootstraplearning", "yes");
		batchupdate = this.hasYesNoParameter("batchupdate","yes");
		
		// Set order type
		String ordertype = this.getStringParameter("ordertype", RandomOrder.class.getCanonicalName());
		itemorder = (ItemOrder) Dynamic.forConfigurableName(ItemOrder.class, ordertype, this);
		
		// Print values if requested
		printbootstrap = this.hasYesNoParameter("printbootstrap", "yes");
		printchanges = this.hasYesNoParameter("printchanges", "yes");
		printtestderivetime = this.hasYesNoParameter("printtestderivetime", "yes");
		printtrainderivetime = this.hasYesNoParameter("printtrainderivetime", "yes");
		printtrainvalues = this.hasYesNoParameter("printtrainvalues", "yes");
		printnumiterations = this.hasYesNoParameter("printnumiterations", "yes");
		printprogress = this.hasYesNoParameter("printprogress", "yes");
		
		checkoscillation = this.getYesNoParameter("checkoscillation","no");
		ignoreprobs = this.getYesNoParameter("ignoreprobs", "no");
		printoscillation = this.getYesNoParameter("printoscillation","no");
		checkconvergence = this.getYesNoParameter("checkconvergence","no");
		
		saveperiterdir = this.getStringParameter("saveperiterdir", null);
		
		performstochastic = this.getYesNoParameter("performstochastic","no");
		performstochastictemp = this.getYesNoParameter("performstochastictemp","no");
		
		performsampling = this.getYesNoParameter("performsampling","no");
		burnin = this.getIntegerParameter("burnin",0);
		int seed = this.getIntegerParameter("seed",0);
		rand = new Random(seed);
		
		performstacking = this.getYesNoParameter("performstacking","no");
		if(performstacking) {
			predlabelfids = this.getStringParameter("predlabelfids").split(",");
			if(predlabelfids.length!=targetfeatureids.size()) {
				throw new ConfigurationException("The number of predicted label feature ids"
						+" must match the number of target feature ids: Expected "+targetfeatureids.size()
						+" got "+predlabelfids.length);
			}
		}
		unlabeledstackingonly = this.getYesNoParameter("unlabeledstackingonly","no");
		emstacking = this.getYesNoParameter("emstacking","no");
		
		permanentcommitkpct = this.hasYesNoParameter("permanentcommitkpct", "yes");
		if(this.hasParameter("kpercent")) {
			this.kpercent = this.getDoubleParameter("kpercent");
			if(this.kpercent<=0 || this.kpercent>1) {
				throw new ConfigurationException("K-percent parameter must be between" +
						" 0 (exclusive) and 1 (inclusive): "+this.kpercent);
			}
		}
		
		// Acquire configuration IDs for use in initializing and configuring models
		String[] tids = this.getStringParameter("tids").split(",");
		if(tids.length != targetschemaids.size() 
			&& targetschemaids.size() != targetfeatureids.size()) {
			throw new InvalidStateException("All lists should have the same size: "
					+ "TIDs: "+tids.length
					+ " targetschemaids: "+targetschemaids.size()
					+ " targetfeatureids: "+targetfeatureids.size()
					);
		}
		
		// Initialize the classifiers from the configuration
		if(this.performstacking) {
			this.relvbclevellist = new ArrayList<List<VBClassifier>>(tids.length);
		} else {
			this.relvbclist = new ArrayList<VBClassifier>(tids.length);
		}
		
		this.nonrelvbclist = new ArrayList<VBClassifier>(tids.length);
		for(int i=0; i<listsize; i++) {
			String tid = tids[i];
			
			VBClassifier nonrelvbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class,
					this.getStringParameter(tid+"-nonrelvbc"), this);
			
			this.nonrelvbclist.add(nonrelvbc);
			
			if(!this.performstacking) {
				VBClassifier relvbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class,
						this.getStringParameter(tid+"-relvbc"), this);
				this.relvbclist.add(relvbc);
			}
		}
		
		if(this.performstacking) {
			for(int l=0; l<numiterations; l++) {
				List<VBClassifier> relvbclist = new ArrayList<VBClassifier>(tids.length);
				
				for(int i=0; i<listsize; i++) {
					String tid = tids[i];
					
					// Train classifiers
					VBClassifier relvbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class,
							this.getStringParameter(tid+"-relvbc"), this);
					relvbclist.add(relvbc);
				}
				
				relvbclevellist.add(relvbclist);
			}
		}
		
		// Support limited parallelization
		learninparallel = this.getYesNoParameter("learninparallel","no");
		numthreads = this.getIntegerParameter("numthreads", 1);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String targetschemaids = this.getStringParameter("saved-targetschemaids");
		String targetfeatureids = this.getStringParameter("saved-targetfeatureids");
		
		this.shouldBootstrap = this.getYesNoParameter("saved-shouldbootstrap");
		
		this.initialize(Arrays.asList(targetschemaids.split(",")),
				Arrays.asList(targetfeatureids.split(",")));
		
		// Load non-relational models
		int numfeatures = targetschemaids.split(",").length;
		for(int i=0; i<numfeatures; i++) {
			this.nonrelvbclist.get(i).loadModel(directory+File.separator+"nrvbc-"+i);
		}
		
		// As an optimization, if there are no iterations performed,
		// the relational features are not needed.
		if(this.numiterations!=0) {
			// Load relational models
			for(int i=0; i<numfeatures; i++) {
				this.relvbclist.get(i).loadModel(directory+File.separator+"rvbc-"+i);
			}
		}
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		// Save model configuration id
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		// Save SIDS and FIDS
		this.setParameter("saved-targetschemaids", ListUtils.list2string(this.targetschemaids,","));
		this.setParameter("saved-targetfeatureids", ListUtils.list2string(this.targetfeatureids,","));
		
		this.setParameter("saved-shouldbootstrap", this.shouldBootstrap?"yes":"no");
		
		// Save configuration
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
		
		// Save non-relational models
		for(int i=0; i<this.nonrelvbclist.size(); i++) {
			nonrelvbclist.get(i).saveModel(directory+File.separator+"nrvbc-"+i);
		}
		
		// As an optomization, if there are no iterations performed,
		// the relational features are not needed.
		if(this.numiterations!=0) {
			// Save relational models
			for(int i=0; i<this.relvbclist.size(); i++) {
				relvbclist.get(i).saveModel(directory+File.separator+"rvbc-"+i);
			}
		}
	}

	public void shouldBootstrap(boolean bootstrap) {
		this.shouldBootstrap = bootstrap;
	}

	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid) {
		List<Iterable<? extends Decorable>> list = new ArrayList<Iterable<? extends Decorable>>();
		list.add(trainitems);
		this.learn(list,
				Arrays.asList(new String[]{targetschemaid}),
				Arrays.asList(new String[]{targetfeatureid}));
	}

	public void learn(Graph traingraph, String targetschemaid,
			String targetfeatureid) {
		this.learn(traingraph,
				Arrays.asList(new String[]{targetschemaid}),
				Arrays.asList(new String[]{targetfeatureid}));
	}

	public void predict(Iterable<? extends Decorable> testitems) {
		List<Iterable<? extends Decorable>> list = new ArrayList<Iterable<? extends Decorable>>();
		list.add(testitems);
		this.predict(testitems);
	}
	
	private FeatureValue updateSampling(int iteration, int index, Decorable di, FeatureValue fv,
			Map<Integer,Map<Decorable,double[]>> samples,
			Map<Integer,Map<String,Integer>> val2indexlist,
			Map<Integer,Map<Integer,String>> index2vallist) {
		if(!val2indexlist.containsKey(index)) {
			// Initialize index list
			Schema schema = di.getSchema();
			Feature f = schema.getFeature(this.targetfeatureids.get(index));
			if(!(f instanceof CategFeature)) {
				throw new InvalidStateException("Only categorical features supported: "
						+f+" of type "+f.getClass().getCanonicalName());
			}
			
			Map<String,Integer> val2index = new HashMap<String,Integer>();
			Map<Integer,String> index2val = new HashMap<Integer,String>();
			UnmodifiableList<String> vallist = ((CategFeature) f).getAllCategories();
			for(int i=0; i<vallist.size(); i++) {
				val2index.put(vallist.get(i), i);
				index2val.put(i, vallist.get(i));
			}
			
			val2indexlist.put(index, val2index);
			index2vallist.put(index, index2val);
		}
		
		CategValue cv = (CategValue) fv;
		int valindex = ProbDist.sampleProbDist(cv.getProbs(), true, rand);
		String sampledcat = index2vallist.get(index).get(valindex);
		FeatureValue sampledfv = new CategValue(sampledcat, cv.getProbs());
		
		// Take samples after burn in
		if(iteration>=this.burnin) {
			// Handle case where this is the first sample for a given index
			Map<String,Integer> valmap = val2indexlist.get(index);
			if(!samples.containsKey(index)) {
				samples.put(index, new HashMap<Decorable,double[]>());
			}
			
			// Handle case where this is the first sample for a decorable item
			Map<Decorable,double[]> indexmap = samples.get(index);
			if(!indexmap.containsKey(di)) {
				indexmap.put(di, new double[valmap.size()]);
			}
			
			indexmap.get(di)[valindex]++;
		}
		
		return sampledfv;
	}
	
	/**
	 * Method called prior to learning any classifiers.
	 * Can be overloaded to do any preprocessing or analysis prior to
	 * learning the classifiers.
	 * 
	 * @param trainitems Train items passed to learn methods.
	 */
	protected void preLearning(List<Iterable<? extends Decorable>> trainitems) {
		// Do nothing
	}
	
	/**
	 * Method called prior to learning the relational classifiers.
	 * When doing stacked learning, this method is also called
	 * prior to learning the classifier for the current iteration.
	 * Can be overloaded to do any preprocessing or analysis prior to
	 * learning the relational classifiers.
	 * 
	 * @param trainitems Train items passed to learn methods.
	 */
	protected void preRelationalLearning(List<Iterable<? extends Decorable>> trainitems) {
		// Do nothing
	}
	
	/**
	 * Method called after to learning the classifiers.
	 * Can be overloaded to do any postprocessing or analysis after
	 * learning the classifiers.
	 * 
	 * @param trainitems Train items passed to learn methods.
	 */
	protected void postLearning(List<Iterable<? extends Decorable>> trainitems) {
		// Do nothing
	}
	
	/**
	 * Method called prior to applying any prediction.
	 * Can be overloaded to do any preprocessing or analysis before
	 * applying the classifiers.
	 * 
	 * @param trainitems Test items passed to predict methods.
	 */
	protected void prePredicting(List<Iterable<? extends Decorable>> testitems) {
		// Do nothing
	}
	
	/**
	 * Method called after applying any prediction.
	 * Can be overloaded to do any postprocessing or analysis after
	 * applying the classifiers.
	 * 
	 * @param trainitems Test items passed to predict methods.
	 */
	protected void postPredicting(List<Iterable<? extends Decorable>> testitem) {
		// Do nothing
	}
	
	/**
	 * Method called at the beginning of an iteration during inference.
	 * Can be overloaded to do any preprocessing or analysis before
	 * applying the classifiers in the current iteration.
	 * 
	 * @param trainitems Test items passed to predict methods.
	 * @param iteration Number of the current iteration (starting at 0).
	 */
	protected void preIteration(List<Iterable<? extends Decorable>> testitems, int iteration) {
		// Do nothing
	}
	
	/**
	 * Method called at the end of an iteration during inference.
	 * Can be overloaded to do any postprocessing or analysis after
	 * applying the classifiers in the current iteration.
	 * 
	 * @param trainitems Test items passed to predict methods.
	 * @param iteration Number of the current iteration (starting at 0).
	 */
	protected void postIteration(List<Iterable<? extends Decorable>> testitems, int iteration) {
		// Do nothing
	}
}
