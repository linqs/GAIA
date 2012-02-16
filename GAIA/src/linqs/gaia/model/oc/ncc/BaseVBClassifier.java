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
package linqs.gaia.model.oc.ncc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.explicit.ExplicitCateg;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.log.Log;
import linqs.gaia.model.oc.OCUtils;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.FileIO;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.ListUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * <p>Vector Base Classification Model (VBC) uses any classification model which
 * takes as input a flat, static array of features.  i.e. logistic regression, naive bayes.
 * This handles two of the three variances of learning functions, as well as computes
 * the features to use in the model.
 * </p>
 * 
 * Optional Parameters:
 * <UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex "patterns"
 * used to identify the set of features to use in the model.
 * All feature ids, from the specified featureschemaid, which match
 * at least one of the patterns is included.  Default is to use
 * all the features defined for the specified schema id.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI>excludefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex "patterns"
 * used to identify the set of features to use in the model.
 * Given the set of feature ids which match at least
 * one pattern of includefeatures (or the default set of features when
 * includefeatures is not specified), remove all feature ids
 * which match at least one of these patterns.
 * Format defined in {@link FeatureUtils#parseFeatureList(String, List)}.
 * <LI> incrementalupdate-If yes, the predicted value is assigned to the target attribute
 * as soon as it's predicted. Otherwise, the label is only updated at the end (i.e., derived features
 * won't be affected until after all items are predicted.).  Default is no.
 * </UL>
 * 
 * @see VBClassifier
 * 
 * @author namatag
 *
 */
public abstract class BaseVBClassifier extends BaseConfigurable implements VBClassifier {
	private static final long serialVersionUID = 1L;
	
	protected String targetschemaid;
	protected String targetfeatureid;
	protected List<String> featureids;
	private boolean isincremental = false;

	public void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid,
			String targetfeatureid) {
		
		// Load target schema id and feature
		this.targetschemaid = targetschemaid;
		this.targetfeatureid = targetfeatureid;
		
		if(this.hasParameter("incrementalupdate","yes")) {
			this.isincremental = true;
		}
		
		// Initialize features to use
		// Assume all graph items have the same schema.
		GraphItem firstitem = (GraphItem) trainitems.iterator().next();
		this.initializeFeatureIDs(firstitem.getSchema());
		
		// Learn vector based classifier
		this.learn(trainitems, targetschemaid, targetfeatureid, featureids);
	}
	
	public void learn(Graph traingraph, String targetschemaid,
			String targetfeatureid) {
		this.learn(OCUtils.getItemsByFeature(traingraph, targetschemaid, targetfeatureid, false),
				targetschemaid,
				targetfeatureid);
	}

	public void predict(Iterable<? extends Decorable> testitems) {
		Graph g = null;
		String updatefid = this.targetfeatureid;
		if(!this.isincremental) {
			// Initialize for batch update
			// Batch update is done by creating a temporary explicit
			// feature for the affected schema.  The temporary feature
			// will store all the values for the item until the very end
			// where the target feature will be updated with the
			// values of the temporary feature for all testitems.
			Decorable firstitem = testitems.iterator().next();
			if(firstitem instanceof GraphItem) {
				GraphItem gi = (GraphItem) firstitem;
				g = gi.getGraph();
			} else if(firstitem instanceof Graph) {
				g = (Graph) firstitem;
			} else {
				throw new UnsupportedTypeException("Unsupported decorable type: "
						+firstitem.getClass().getCanonicalName());
			}
			
			updatefid = this.getTempFID();
			
			Schema schema = firstitem.getSchema();
			CategFeature f = (CategFeature) schema.getFeature(this.targetfeatureid);
			schema.addFeature(updatefid, new ExplicitCateg(f.getAllCategories()));
			g.updateSchema(this.targetschemaid, schema);
		}
		
		// Perform prediction per test item
		for(Decorable d: testitems) {
			FeatureValue fv = this.predict(d);
			d.setFeatureValue(updatefid, fv);
		}
		
		// Perform batch update
		if(!this.isincremental) {
			// Copy, for each test item, the value from
			// the temporary feature to the target feature
			for(Decorable d: testitems) {
				FeatureValue fv = d.getFeatureValue(updatefid);
				d.setFeatureValue(this.targetfeatureid, fv);
			}
			
			// Remove temporary feature
			Schema schema = g.getSchema(this.targetschemaid);
			schema.removeFeature(updatefid);
			g.updateSchema(this.targetschemaid, schema);
		}
	}

	public void predict(Graph testgraph) {
		this.predict(OCUtils.getItemsByFeature(testgraph,
				targetschemaid, targetfeatureid, true));
	}
	
	private String getTempFID() {
		return this.targetfeatureid +"-VBCTemp";
	}

	public void loadModel(String directory) {
		// Load target schema and feature
		this.loadParametersFile(directory+File.separator+"savedparameters.cfg");
		
		if(this.hasParameter("saved-cid")) {
			this.setCID(this.getStringParameter("saved-cid"));
		}
		
		this.targetschemaid = this.getStringParameter("saved-targetschemaid");
		this.targetfeatureid = this.getStringParameter("saved-targetfeatureid");
		
		// Load features to use
		String allfeatureids = this.getStringParameter("saved-featureids");
		if(allfeatureids.equals("NO_FEATURES")) {
			this.featureids = new LinkedList<String>();
		} else {
			// Use only specified features
			List<String> fidlist = new ArrayList<String>(Arrays.asList(allfeatureids.split(",")));
			for(String fid:fidlist) {
				if(fid.equals(this.targetfeatureid)) {
					Log.WARN("Target feature cannot be in the feature list: "+fid);
				}
			}
			fidlist.remove(this.targetfeatureid);
			
			this.featureids = fidlist;
		}
		
		// Load saved classifier
		this.loadVBOC(directory+File.separator+"vbclassifier");
	}

	public void saveModel(String directory) {
		FileIO.createDirectories(directory);
		
		if(this.getCID()!=null) {
			this.setParameter("saved-cid", this.getCID());
		}
		
		// Save targetschemaid
		this.setParameter("saved-targetschemaid", this.targetschemaid);
		
		// Save targetfeatureid
		this.setParameter("saved-targetfeatureid", this.targetfeatureid);
		
		// Save feature ids
		this.setParameter("saved-featureids", featureids==null ? "NO_FEATURES" : ListUtils.list2string(this.featureids, ","));
		
		// Save classifier
		this.saveVBOC(directory+File.separator+"vbclassifier");
		
		// Save parameters
		this.saveParametersFile(directory+File.separator+"savedparameters.cfg");
	}
	
	protected void initializeFeatureIDs(Schema schema) {
		List<String> currfids = FeatureUtils.parseFeatureList(this,
			schema, IteratorUtils.iterator2stringlist(schema.getFeatureIDs()));
		
		// Never include the target feature
		currfids.remove(this.targetfeatureid);
		
		this.featureids = currfids;
	}
	
	public void learn(Graph graph,
			String targetschemaid, String targetfeatureid, List<String> featureids) {
		this.learn(graph.getIterableGraphItems(targetschemaid),
				targetschemaid, targetfeatureid, featureids);
	}
	
	/**
	 * The base, naive, implementation of this method
	 * is to use the {@link #saveModel(String)} and {@link #loadModel(String)}
	 * methods of the model to save the model to a file
	 * and then create the copy by reloading the file.
	 * Overwrite this method in models where this is too
	 * costly to perform.
	 */
	public VBClassifier copyModel() {
		String tmpdir = FileIO.getTemporaryDirectory();
		this.saveModel(tmpdir);
		VBClassifier vbc = (VBClassifier) Dynamic.forConfigurableName(VBClassifier.class,
				this.getClass().getCanonicalName());
		vbc.loadModel(tmpdir);
		
		return vbc;
	}
	
	public UnmodifiableList<String> getFeatureIDs() {
		return new UnmodifiableList<String>(this.featureids);
	}
	
	/**
	 * Specify the following abstract functions
	 */
	public abstract void learn(Iterable<? extends Decorable> trainitems,
			String targetschemaid, String targetfeatureid, List<String> featureids);

	public abstract FeatureValue predict(Decorable testitem);
	
	public abstract void loadVBOC(String directory);
	
	public abstract void saveVBOC(String directory);
}
