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
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.CompositeFeature;
import linqs.gaia.feature.DerivedFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.decorable.Decorable;
import linqs.gaia.feature.derived.BaseDerived;
import linqs.gaia.feature.derived.composite.CVFeature;
import linqs.gaia.feature.derived.composite.CVNum;
import linqs.gaia.feature.derived.neighbor.Incident;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.CompositeValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.NumValue;
import linqs.gaia.global.Constants;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphDependent;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableList;

/**
 * This is a multi-valued feature defined over the specified set of
 * categorical (with categories Constants.TRUE and Constants.FALSE)
 * or numeric features.  The feature returns multiple numeric
 * features, for each defined feature, whose value is 0, for a given feature,
 * if the neighbors (a set of size 2) are both true
 * (i.e., Constants.TRUE for categorical features, value greater than 0 for numeric features)
 * and false otherwise.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureschemaid-Feature schema ID of the connected graph items to consider.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neigborclass-Class of neighbor implementation to use when calculating
 * neighborhood.  The number of neighbors must be two.
 * Default is {@link Incident}.
 * <LI> type-Type of aggregation to perform over the feature values
 * 	<UL>
 * 	<LI> AND-Take the AND of the values
 * 	<LI> OR-Take the OR of the values
 * 	<LI> XOR-Take the XOR of the values
 * 	</UL>
 * <LI>includefeatures-The parameters is treated as a
 * comma delimited list of feature ids and/or regex pattern
 * for feature IDs in the form REGEX:&lt;pattern&gt;
 * (e.g., color,size,REGEX:\\d,length).  Default is to use
 * all the features defined for the specified schema id.
 * <LI>excludefeatures-Same format as include features
 * but any matching feature id and/or regex pattern
 * is removed.
 * </UL>
 * 
 * @author namatag
 *
 */
public class BinaryNeighborXOR extends BaseDerived implements 
	DerivedFeature, GraphDependent, CompositeFeature {
	private UnmodifiableList<SimplePair<String, CVFeature>> features = null;
	private List<String> featureids;
	private Set<String> cffeatureids = new HashSet<String>();
	private String featureschemaid;
	private String type = null; 
	private Graph g;
	
	private String neighborclass = Incident.class.getCanonicalName();
	private Neighbor neighbor = null;
	private static NumValue numval0 = new NumValue(0.0);
	private static NumValue numval1 = new NumValue(1.0);
	
	@Override
	protected FeatureValue calcFeatureValue(Decorable di) {
		
		if(!(di instanceof GraphItem)) {
			throw new UnsupportedTypeException("Feature only defined for graph items: "
					+di.getClass().getCanonicalName());
		}
		
		GraphItem gi = (GraphItem) di;
		
		// Get both neighbors
		Iterable<GraphItem> currneighbors = this.neighbor.getNeighbors(gi);
		int numneighbors = IteratorUtils.numIterable(currneighbors);
		if(numneighbors != 2) {
			throw new ConfigurationException("This feature only supports exactly two neighbors: "
					+numneighbors);
		}
		
		Iterator<GraphItem> nitr = currneighbors.iterator();
		GraphItem g1 = nitr.next();
		GraphItem g2 = nitr.next();
		
		// Get if features of connected match
		List<FeatureValue> fvalues = new LinkedList<FeatureValue>();
		for(String fid:featureids) {
			if(this.cffeatureids.contains(fid)) {
				FeatureValue fv1 = g1.getFeatureValue(fid);
				FeatureValue fv2 = g2.getFeatureValue(fid);
				
				UnmodifiableList<FeatureValue> cffv1 = null;
				UnmodifiableList<FeatureValue> cffv2 = null;
				if(fv1 instanceof CompositeValue && fv2 instanceof CompositeValue) {
					cffv1 = ((CompositeValue) fv1).getFeatureValues();
					cffv2 = ((CompositeValue) fv2).getFeatureValues();
				}
				
				// Go through all values of composite feature
				CompositeFeature cf = (CompositeFeature) g1.getSchema().getFeature(fid);
				int length = cf.getFeatures().size();
				for(int i=0; i<length; i++) {
					boolean isg1true = false;
					boolean isg2true = false;
					if(cffv1==null || cffv2==null) {
						// If either composite value is unknown, set as false for all values
						isg1true = false;
						isg2true = false;
					} else {
						isg1true = this.isTrue(fid, cffv1.get(i));
						isg2true = this.isTrue(fid, cffv2.get(i));
					}
					
					NumValue value = this.getValue(isg1true, isg2true);
					fvalues.add(value);
				}
			} else {
				boolean isg1true = this.isTrue(fid, g1.getFeatureValue(fid));
				boolean isg2true = this.isTrue(fid, g2.getFeatureValue(fid));
				
				NumValue value = this.getValue(isg1true, isg2true);
				fvalues.add(value);
			}
		}
		
		return (FeatureValue) new CompositeValue(fvalues);
	}
	
	private NumValue getValue(boolean val1, boolean val2) {
		if(this.type.equals("XOR")) {
			if(val1!=val2) {
				return numval1;
			}
		} else if(this.type.equals("AND")) {
			if(val1 && val2) {
				return numval1;
			}
		} else if(this.type.equals("OR")) {
			if(val1 || val2) {
				return numval1;
			}
		} else {
			throw new InvalidStateException("Unsupported parameter found: "+this.type);
		}
		
		return numval0;
	}
	
	private boolean isTrue(String fid, FeatureValue fv) {
		if(!fv.equals(FeatureValue.UNKNOWN_VALUE)) {
			if(fv instanceof CategValue) {
				CategValue cfv = (CategValue) fv;
				if(!cfv.getCategory().equals(Constants.FALSE) 
					&& !cfv.getCategory().equals(Constants.TRUE)) {
					throw new UnsupportedTypeException("Only binary categorical values supported: "
							+fid+"="
							+cfv.getCategory());
				}
				
				return cfv.getCategory().equals(Constants.TRUE);
			} else if(fv instanceof NumValue) {
				NumValue nfv = (NumValue) fv;
				return nfv.getNumber() > 0;
			} else {
				throw new UnsupportedTypeException("Unsupported Type: "
						+fv.getClass().getCanonicalName());
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Initialize information required by the feature
	 */
	protected void initialize() {		
		// Initialize neighbor information
		if(this.hasParameter("neighborclass")) {
			this.neighborclass = this.getStringParameter("neighborclass");
		}
		
		this.type = "XOR";
		if(this.hasParameter("type")) {
			this.type = this.getCaseParameter("type", new String[]{"AND","OR","XOR"});
		}
		
		this.neighbor = (Neighbor) Dynamic.forConfigurableName(Neighbor.class, this.neighborclass);
		this.neighbor.copyParameters(this);
		
		this.featureschemaid = this.getStringParameter("featureschemaid");
		// Use specified features or, if not specified, use all features
		// for the schema of the specified feature schema id.
		Schema schema = g.getSchema(this.featureschemaid);
		this.featureids = FeatureUtils.parseFeatureList(this,
				schema, FeatureUtils.getFeatureIDs(schema, 2));
		
		// Return categorically valued items
		List<SimplePair<String, CVFeature>> fpairs = new LinkedList<SimplePair<String, CVFeature>>();
		for(String fid:featureids) {
			Feature f = schema.getFeature(fid);
			
			if(f instanceof CompositeFeature) {
				cffeatureids.add(fid);
				CompositeFeature cf = (CompositeFeature) f;
				UnmodifiableList<SimplePair<String, CVFeature>> cffeatures = cf.getFeatures();
				for(SimplePair<String, CVFeature> cff:cffeatures) {
					// Return a separate value for each entry in the composite value
					fpairs.add(new SimplePair<String,CVFeature>(fid+"-bnxorcf-"+cff.getFirst(), new CVNum()));
				}
			} else {
				fpairs.add(new SimplePair<String,CVFeature>(fid, new CVNum()));
			}
		}
		
		this.features = new UnmodifiableList<SimplePair<String, CVFeature>>(fpairs);
	}
	
	public UnmodifiableList<SimplePair<String, CVFeature>> getFeatures() {
		this.initializeFeature();
		
		return features;
	}

	public void setGraph(Graph g) {
		this.g = g;
	}
	
	public int numFeatures() {
		return this.features.size();
	}
}
