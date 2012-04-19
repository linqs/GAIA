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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.model.BootstrapModel;
import linqs.gaia.model.oc.Classifier;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.model.oc.ncc.VBClassifier;
import linqs.gaia.model.oc.ncc.LibSVMClassifier;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.SimpleTimer;

/**
 * <p>
 * Implementation of the Iterative Classification Algorithm.<br>
 * 
 * Reference: Lu, Q., and Getoor, L. (2003). Link-based classification.
 * Proceedings of the Twentieth International Conference on Machine Learning
 * (pp. 496-503). Washington, DC: AAAI.
 * </p>
 * The relational features are implemented
 * through defining and specifying {@link linqs.gaia.feature.DerivedFeature}
 * for the items being predicted.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI>nonrelclassifier-Vector based classifier ({@link VBClassifier}) to use on non-relational features,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.model.oc.ncc.LibSVMClassifier}.
 * <LI>relclassifier-Vector based classifier ({@link VBClassifier}) to use on relational features,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is {@link linqs.gaia.model.oc.ncc.LibSVMClassifier}.
 * <LI>numiterations-Number of iterations to do.  Set to 0 to only use the
 * non-relational classifier.  Default is 10.
 * <LI>bootstraplearning-If "yes", apply local classifier to unlabeled instances
 * prior to training.  This bootstraps those instances to allow for relational
 * features in a semi-supervised classification setting.  Training is still only
 * performed using the annotated instances as the bootstrap values are only
 * used in the relational features.  Default is "no".
 * <LI>includenonrelfeatures-Non-relational/local features, in the schema of the target feature,
 * used during bootstrapping.
 * If not defined, all features NOT of type Derived are used.
 * The parameters are treated as a
 * comma delimited list of feature ids and/or regex pattern for feature IDs.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI>excludenonrelfeatures-Non-relational/local features excluded during bootstrapping.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI>includerelfeatures-Relational features used after bootstrapping.
 * If not defined, all features of type Derived are used.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * Note: Relational classifiers are run using the union of the defined
 * non-relational and relational features.
 * <LI>excluderelfeatures-Relational features to exclude after bootstrapping.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * Note: Relational classifiers are run using the union of the defined
 * non-relational and relational features.
 * 
 * <LI>checkprobs-If yes, when computing whether or not the predicted labels have converged
 * (not changed from the previous iteration),
 * verify that the probability distribution has not change.  The default is no where we only
 * check if the label value, regardless of the probability, has changed.
 * 
 * <LI>isbatch-If set to yes, only update the labels at the end of each iteration
 * such that the value of relational features are based on the values of the previous iteration.
 * Default is no which updates the labels within the iteration such that
 * the value of relational features are based on the most recent value, even with the same iteration,
 * of the labels.
 * 
 * <LI>modadjchangeonly-If yes, store the set of items who are
 * adjacent to the nodes whose label changed.  After the first iteration,
 * we only apply the relational classifier only on nodes where the label of at least one node is changed.
 * This provides an optimization which speeds up performance while getting the same results
 * as long as the relational features for a given item depend
 * only on the label of adjacent nodes (i.e., percentage of adjacent neighbors with a given label).
 * Default is no.
 * <LI>modadjsids-Comma delimited list of schema ids used when modadjchangeonly=yes.
 * If specified, only nodes adjacet given an incident item of the specified schema ids.
 * Default is to use all adjacent items, regardless of the incident item schema ids.
 * 
 * <LI>printbootstrap-If set to yes, print the id and predicted bootstrap
 * value of all items. Format "Boostrap Value:\tIDENTIFIER\tVALUE".  Default is no.
 * <LI>printchanges-If set to yes, print the id, old value, and new value of
 * items whose values have changed in a given iteration.
 * Format "Value Change at Iteration:\tITERATION\tITEM\tOLDVALUE\tNEWVALUE". Default is no.
 * 
 * <LI>savebootstrapfid-If specified, save the bootstrap label for each test item
 * to a feature with this feature ID.  This allows the efficient storage of this
 * predicted value for use in further analysis.  If a feature with the specified
 * feature id is not defined, it will be automatically added.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class ICA extends BaseConfigurable implements Classifier, BootstrapModel {
	private static final long serialVersionUID = 1L;
	private boolean bootstrap = true;
	
	// Print predictions, if requested
	private boolean printbootstrap = false;
	private boolean printchanges = false;
	
	// Bootstrap the learning process
	// e.g., for semisupervised data
	protected boolean bootstraplearning = false;
	
	// Target feature and schema information
	private String targetschemaid;
	private String targetfeatureid;
	
	// Feature sets to use
	private List<String> relfeatures;
	private List<String> nonrelfeatures;
	
	// Classifiers to use
	private VBClassifier nonrelvbc;
	private VBClassifier relvbc;
	
	// Number of iterations
	private int numiterations = 10;
	
	private boolean isbatch = false;
	private boolean modadjchangeonly = false;
	private String[] modadjsids = null;
	
	private boolean checkprobs = false;
	
	private String savebootstrapfid = null;
	
	private static final String DEFAULT_VBCLASSIFIER = LibSVMClassifier.class.getCanonicalName();
	
	/**
	 * Set the relational features to use.
	 * If specified, these relational features will be loaded
	 * rather than computing the relational features via configuration files.
	 * 
	 * @param relfeatures List of feature ids for relational features to use
	 */
	public void setRelFeatures(List<String> relfeatures) {
		this.relfeatures = relfeatures;
	}
	
	/**
	 * Set the non-relational features to use.
	 * If specified, these non-relational features will be loaded
	 * rather than computing the relational features via configuration files.
	 * 
	 * @param nonrelfeatures List of feature ids for relational features to use
	 */
	public void setNonRelFeatures(List<String> nonrelfeatures) {
		this.nonrelfeatures = nonrelfeatures;
	}
	
	/**
	 * Get the list of the feature ids of the relational features used
	 * 
	 * @return List of feature ids for relational features to use
	 */
	public List<String> getRelFeatures() {
		return this.relfeatures;
	}
	
	/**
	 * Get the list of the feature ids of the non-relational features used
	 * 
	 * @return List of feature ids for non-relational features to use
	 */
	public List<String> getNonRelFeatures() {
		return this.nonrelfeatures;
	}
	
	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid,
			String targetfeatureid) {
		
		// Initialize model using the configuration
		this.initialize(targetschemaid, targetfeatureid);
		
		boolean allowunsupervised = this.hasYesNoParameter("allowunsupervised", "yes");
		if(!allowunsupervised) {
			if(!trainitems.iterator().hasNext()) {
				throw new InvalidStateException("At least one training instance must be provided for ICA");
			}
			
			// If features not set manually
			if(nonrelfeatures==null && relfeatures==null) {
				// Initialize the features to use
				Decorable firstitem = trainitems.iterator().next();
				Schema schema = firstitem.getSchema();
				
				// Train non-relational classifier using features independent
				// of the label being predicted (i.e., non-relational features).
				// Train relational classifier using all the features from the non-relational
				// classifier, plus any relational only features.
				
				// Get non-relational features
				nonrelfeatures = FeatureUtils.parseFeatureList(this,
						schema, FeatureUtils.getFeatureIDs(schema, 0),
						"includenonrelfeatures", "excludenonrelfeatures");
				nonrelfeatures.remove(this.targetfeatureid);
				
				// Get relational features
				relfeatures = FeatureUtils.parseFeatureList(this,
						schema, FeatureUtils.getFeatureIDs(schema, 1),
						"includerelfeatures", "excluderelfeatures");
				relfeatures.remove(this.targetfeatureid);
				
				// Include all non-relational features but avoid duplicates
				for(String fid:nonrelfeatures) {
					if(!relfeatures.contains(fid)) {
						relfeatures.add(fid);
					}
				}
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Non-Relational Features Used: "+ListUtils.list2string(this.nonrelfeatures, ","));
			Log.DEBUG("Relational Features Used: "+ListUtils.list2string(this.relfeatures, ","));
		}
		
		// Train non-relational classifiers
		if(bootstrap || savebootstrapfid!=null) {
			// If bootstrapping, learn a non relational model.
			this.nonrelvbc.learn(trainitems, targetschemaid, targetfeatureid, this.nonrelfeatures);
		}
		
		// If semisupervised, bootstrap the unknown values prior
		// to training the relational classifier.
		// Note: We can probably extend this to bootstrap all
		// values that are unknown, not just the ones we're explicitly doing.
		List<Decorable> bootstraptrainitems = null;
		if(bootstraplearning && trainitems!=null && trainitems.iterator().hasNext()) {
			bootstraptrainitems = new ArrayList<Decorable>();
			
			// Assume decorable items are graph items
			// and get the corresponding graph for each.
			Set<Graph> allgraphs = new HashSet<Graph>();
			for(Decorable d:trainitems) {
				allgraphs.add(((GraphItem) d).getGraph());
			}
			
			// Get items whose target value is unknown in each graph
			for(Graph traingraph: allgraphs) {
				List<GraphItem> unknown = OCUtils.getItemsByFeature(traingraph, targetschemaid,
						targetfeatureid, true);
				bootstraptrainitems.addAll(unknown);
			}
			
			if(!bootstraptrainitems.isEmpty()) {
				this.nonrelvbc.predict(bootstraptrainitems);
			}
		}
		
		// Train relational classifiers
		this.relvbc.learn(trainitems, targetschemaid, targetfeatureid, this.relfeatures);
		
		// If semisupervised, remove bootstrap values
		if(bootstraptrainitems!=null) {
			for(Decorable d:bootstraptrainitems) {
				d.removeFeatureValue(targetfeatureid);
			}
		}
	}
	
	/**
	 * Initialize the model from the configurations.
	 */
	private void initialize(String targetschemaid, String targetfeatureid) {
		// Get parameters
		if(this.hasParameter("numiterations")) {
			this.numiterations = (int) this.getDoubleParameter("numiterations");
		}
		
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		
		// Create non-relational classifier
		String vbc = null;
		if(this.hasParameter("nonrelclassifier")) {
			vbc = this.getStringParameter("nonrelclassifier");
		} else {
			vbc = DEFAULT_VBCLASSIFIER;
		}
		this.nonrelvbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class, vbc);
		this.nonrelvbc.copyParameters(this);
		
		// Create relational classifier
		if(this.hasParameter("relclassifier")) {
			vbc = this.getStringParameter("relclassifier");
		} else {
			vbc = DEFAULT_VBCLASSIFIER;
		}
		this.relvbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class, vbc);
		this.relvbc.copyParameters(this);
		
		// Print values if requested
		this.printbootstrap = this.hasYesNoParameter("printbootstrap", "yes");
		this.printchanges = this.hasYesNoParameter("printchanges", "yes");
		
		this.bootstraplearning = this.hasYesNoParameter("bootstraplearning", "yes");
		
		this.isbatch = this.getYesNoParameter("isbatch", "no");
		
		this.modadjchangeonly = this.hasYesNoParameter("modadjchangeonly", "yes");
		if(this.hasParameter("modadjsids")) {
			this.modadjsids = this.getStringParameter("modadjsids").split(",");
		}
		
		this.checkprobs = this.getYesNoParameter("checkprobs","no");
		
		this.savebootstrapfid = this.getStringParameter("savebootstrapfid", null);
	}

	public void learn(Graph traingraph, String targetschemaid,
			String targetfeatureid) {
		this.learn(OCUtils.getItemsByFeature(traingraph,
				targetschemaid, targetfeatureid, false),
			targetschemaid,
			targetfeatureid);
	}

	public void predict(Iterable<? extends Decorable> testitems) {
		int numtestitems = IteratorUtils.numIterable(testitems);
		if(numtestitems == 0) {
			throw new InvalidStateException("No iterable items to predict over");
		}
		
		if(bootstrap || savebootstrapfid!=null) {
			Log.DEBUG("Applying ICA bootstrap");
			
			// Bootstrap by predicting labels using
			// only local/non-relational features
			Iterator<? extends Decorable> itr = testitems.iterator();
			while(itr.hasNext()) {
				Decorable d = itr.next();
				FeatureValue fv = this.nonrelvbc.predict(d);
				
				// Update value if bootstrapping
				if(bootstrap) {
					d.setFeatureValue(this.targetfeatureid, fv);
				}
				
				// Save bootstrap value, if requested
				if(savebootstrapfid!=null) {
					if(!d.getSchema().hasFeature(savebootstrapfid)) {
						Schema schema = d.getSchema();
						schema.addFeature(savebootstrapfid, schema.getFeature(targetfeatureid));
						
						if(d instanceof Graph) {
							((Graph) d).updateSchema(targetschemaid, schema);
						} else if(d instanceof GraphItem) {
							((GraphItem) d).getGraph().updateSchema(targetschemaid, schema);
						} else {
							throw new UnsupportedTypeException("Unsupported decorable item type: "
									+d.getClass().getCanonicalName());
						}
					}
					
					d.setFeatureValue(savebootstrapfid, fv);
				}
				
				// Print bootstrap values
				if(printbootstrap) {
					Log.INFO("Boostrap Value:\t"+d+"\t"+fv);
				}
			}
		}
		
		// Predict labels using all features
		// (non-relational and relational)
		// until some stopping criterion is met.
		// In this case, run for a set number of iterations.
		Set<Decorable> modadjchangeonlyset = null;
		if(modadjchangeonly) {
			modadjchangeonlyset = new HashSet<Decorable>(numtestitems);
			for(Decorable ti:testitems) {
				modadjchangeonlyset.add(ti);
			}
		}
		
		for(int i=0; i<this.numiterations; i++) {
			SimpleTimer itertimer = new SimpleTimer();
			Map<Decorable,FeatureValue> di2value = isbatch ? new HashMap<Decorable,FeatureValue>(numtestitems) : null;
			Set<Decorable> modadjchangeonlysetbatch = (isbatch && modadjchangeonly) ? new HashSet<Decorable>(numtestitems) : null;
			int numchanged = 0;
			int numconsidered = 0;
			
			Iterator<? extends Decorable> itr = testitems.iterator();
			while(itr.hasNext()) {
				Decorable d = itr.next();
				
				if(modadjchangeonly && !modadjchangeonlyset.contains(d)) {
					continue;
				}
				
				FeatureValue oldfv = d.getFeatureValue(this.targetfeatureid);
				FeatureValue fv = this.relvbc.predict(d);
				
				// Set the new feature value
				if(this.isbatch) {
					di2value.put(d, fv);
				} else {
					d.setFeatureValue(this.targetfeatureid, fv);
				}
				
				if(modadjchangeonly) {
					// Remove current item as change
					modadjchangeonlyset.remove(d);
				}
				
				// Count the number of values changed
				if((oldfv.equals(FeatureValue.UNKNOWN_VALUE) && !fv.equals(FeatureValue.UNKNOWN_VALUE))
					|| (!oldfv.equals(FeatureValue.UNKNOWN_VALUE) && fv.equals(FeatureValue.UNKNOWN_VALUE))
					|| (oldfv instanceof CategValue
						&& fv instanceof CategValue
						&& !(checkprobs ?
								((CategValue) oldfv).equals(fv) :
								((CategValue) oldfv).equalsIgnoreProbs(fv)
							)
						)
				)
				{
					numchanged++;
					
					if(modadjchangeonly) {
						// Add neighborhood of changed item
						if(modadjsids==null) {
							Iterator<GraphItem> gitr = ((GraphItem) d).getAdjacentGraphItems();
							while(gitr.hasNext()) {
								if(isbatch) {
									modadjchangeonlysetbatch.add(gitr.next());
								} else {
									modadjchangeonlyset.add(gitr.next());
								}
							}
						} else {
							for(String sid:modadjsids) {
								Iterator<GraphItem> gitr = ((GraphItem) d).getAdjacentGraphItems(sid);
								while(gitr.hasNext()) {
									if(isbatch) {
										modadjchangeonlysetbatch.add(gitr.next());
									} else {
										modadjchangeonlyset.add(gitr.next());
									}
								}
							}
						}
					}
					
					// Print changes
					if(printchanges) {
						Log.INFO("Value Change at Iteration:\t"+i+"\t"+d+"\t"+oldfv+"\t"+fv);
					}
				}
				
				numconsidered++;
			}
			
			// Batch set feature values, if specified.
			if(this.isbatch) {
				Set<Entry<Decorable,FeatureValue>> entries = di2value.entrySet();
				for(Entry<Decorable,FeatureValue> e:entries) {
					e.getKey().setFeatureValue(this.targetfeatureid, e.getValue());
				}
				
				for(Decorable d: modadjchangeonlysetbatch) {
					modadjchangeonlyset.add(d);
				}
			}
			
			// If no value has changed, convergence has occurred so stop
			if(Log.SHOWDEBUG) {
				Log.DEBUG("Number changed in iteration "+(i+1)+": "
						+numchanged+"/"+numconsidered
						+" "+itertimer.timeLapse());
			}
			
			if(numchanged==0) {
				break;
			}
		}
	}

	public void predict(Graph testgraph) {
		Iterable<? extends Decorable> testitems = OCUtils.getItemsByFeature(testgraph,
				targetschemaid, targetfeatureid, true);
		this.predict(testitems);
	}

	public void loadModel(String directory) {
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		String targetschemaid = this.getStringParameter("saved-targetschemaid");
		String targetfeatureid = this.getStringParameter("saved-targetfeatureid");
		
		this.nonrelfeatures = Arrays.asList(this.getStringParameter("saved-nonrelfeatures").split(","));
		this.relfeatures = Arrays.asList(this.getStringParameter("saved-relfeatures").split(","));
		
		this.bootstrap = this.getYesNoParameter("saved-shouldbootstrap");
		
		this.initialize(targetschemaid, targetfeatureid);
		
		this.nonrelvbc.loadModel(directory+File.separator+"nonrelvbcmodel");
		this.relvbc.loadModel(directory+File.separator+"relvbcmodel");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
		
		this.setParameter("saved-nonrelfeatures",ListUtils.list2string(this.nonrelfeatures, ","));
		this.setParameter("saved-relfeatures",ListUtils.list2string(this.relfeatures, ","));
		
		this.setParameter("saved-shouldbootstrap", this.bootstrap?"yes":"no");
		
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
		this.nonrelvbc.saveModel(directory+File.separator+"nonrelvbcmodel");
		this.relvbc.saveModel(directory+File.separator+"relvbcmodel");
	}

	public void shouldBootstrap(boolean bootstrap) {
		this.bootstrap = bootstrap;
	}
}
