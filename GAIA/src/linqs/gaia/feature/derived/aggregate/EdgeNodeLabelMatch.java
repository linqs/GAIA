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
package linqs.gaia.feature.derived.aggregate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.BaseDerived;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.derived.composite.CVNum;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * This is a composite numeric feature to capture correlations in the labels
 * of incident nodes to binary edges.
 * It check all pairwise combinations of a given categorical feature
 * and returns whether or not (1.0 or 0.0) that combination is observed in the
 * pair of incident edges.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureschemaid-Schema id of incident nodes
 * <LI> featureid-Feature id of the categorical feature to aggregate over
 * </UL>
 * 
 * Optional Parameters:
 * <UL>
 * <LI> unorderedpairs-If "yes", instead of defining a numeric feature for all
 * ordered pairs of labels (e.g., for labels A and B, compute for ordered pairs AA,AB,BA,BB)
 * only define a numeric feature for a single numeric feature for each unordered pair of labels (e.g., AA,AB,BB).
 * <LI> delimiter-Delimiter to use in between the two categorical features.
 * Default is "-".
 * </UL>
 * 
 * @author namatag
 *
 */
public class EdgeNodeLabelMatch extends BaseDerived implements 
	DerivedFeature, GraphDependent, CompositeFeature {
	private String featureid;
	private String featureschemaid;
	private boolean ordercats = false;
	private String delimiter = "-";
	private Graph g = null;
	private UnmodifiableList<SimplePair<String, CVFeature>> features = null;
	
	private static NumValue notmatch = new NumValue(0);
	private static NumValue match = new NumValue(1);
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {		
		// Feature applicable only to edges
		if(!(di instanceof Edge)) {
			throw new UnsupportedTypeException("Feature only defined for edges: "+
					di.getClass().getCanonicalName());
		}
		
		// Feature applicable only to binary edges
		Edge e = (Edge) di;
		if(e.numNodes()>2) {
			throw new UnsupportedTypeException("Feature only defined for binary edges: "+
					e.numNodes());
		}
		
		Node n1, n2;
		if(e instanceof DirectedEdge) {
			DirectedEdge de = (DirectedEdge) e;
			n1 = de.getSourceNodes().next();
			n2 = de.getTargetNodes().next();
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			Iterator<Node> itr = ue.getAllNodes();
			n1 = itr.next();
			
			// To support self loops
			if(itr.hasNext()) {
				n2 = itr.next();
			} else {
				n2 = n1;
			}
		} else {
			throw new UnsupportedTypeException("Unsupported Edge Type: "+
					di.getClass().getCanonicalName());
		}
		
		String fid = this.getFeatureName(getFeatureValue(n1), getFeatureValue(n2));
		
		String fid2 = null;
		// Handle undirected edges
		if(!ordercats && (e instanceof UndirectedEdge)) {
			fid2 = this.getFeatureName(getFeatureValue(n2), getFeatureValue(n1));
		}
		
		// Note:  This will return a match only for one pair of labels.
		// All else set to value of not match.
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(int i=0; i<features.size(); i++) {
			if(fid != null && fid.equals(features.get(i).getFirst())) {
				fvalues.add(match);
			} else if(fid2 != null && fid2.equals(features.get(i).getFirst()))  {
				fvalues.add(match);
			} else {
				fvalues.add(notmatch);
			}
		}
		
		return new CompositeValue(fvalues);
	}

	public void setGraph(Graph g) {
		// Need the graph to compute the set of features
		this.g = g;
	}

	public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures() {
		this.initializeFeature();
		
		return features;
	}
	
	protected void initialize() {		
		featureschemaid = this.getStringParameter("featureschemaid");
		featureid = this.getStringParameter("featureid");
		if(this.hasParameter("delimiter")) {
			this.delimiter = this.getStringParameter("delimiter");
		}
		
		// Use specified features or, if not specified, use all features
		// for the schema of the specified feature schema id.
		Schema schema = g.getSchema(featureschemaid);
		Feature f = schema.getFeature(featureid);
		if(!(f instanceof CategFeature)) {
			throw new ConfigurationException("Only categorical features supported: "+
					featureid+" of type "+ schema.getFeature(featureid).getClass().getCanonicalName());
		}
		
		// Enforce ordering over categories (using natural ordering of category values)
		ordercats = this.getYesNoParameter("ordercats", "no");
		
		CategFeature cf = (CategFeature) f;
		UnmodifiableList<String> cats = cf.getAllCategories();
		
		// Create a category for all pairs
		Set<String> consideredpairs = new HashSet<String>();
		List<SimplePair<String, CVFeature>> fpairs = new LinkedList<SimplePair<String, CVFeature>>();
		for(String cat1:cats) {
			for(String cat2:cats) {
				String newfid = this.getFeatureName(cat1, cat2);
				// Ensure there are no duplicates,
				// specially in cases where we order the categories
				if(!consideredpairs.contains(newfid)) {
					consideredpairs.add(newfid);
					fpairs.add(new SimplePair<String,CVFeature>(newfid, new CVNum()));
				}
			}
		}
		
		this.features = new UnmodifiableList<SimplePair<String, CVFeature>>(fpairs);
	}
	
	private String getFeatureName(String cat1, String cat2) {
		if(cat1==null || cat2==null) {
			return null;
		}
		
		// Reorder nodes, if requested
		if(ordercats && cat1.compareTo(cat2) > 0) {
			String tmp = cat1;
			cat1 = cat2;
			cat2 = tmp;
		}
		
		return featureid+delimiter+cat1+delimiter+cat2;
	}
	
	private String getFeatureValue(GraphItem gi) {
		FeatureValue fv = gi.getFeatureValue(this.featureid);
		if(fv.equals(FeatureValue.UNKNOWN_VALUE)) {
			return null;
		}
		
		return ((CategValue) fv).getCategory();
	}
	
	public int numFeatures() {
		return this.features.size();
	}
}
