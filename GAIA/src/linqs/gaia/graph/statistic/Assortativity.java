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
package linqs.gaia.graph.statistic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.feature.CategFeature;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.log.Log;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.MapUtils;
import linqs.gaia.util.UnmodifiableList;

/**
 * Calculate the assortativity statistics as a measure
 * of the degree of autocorrelation for the value of a
 * given categorical feature over the edges of a given
 * schema ID.  A single value is returned with the key
 * of "assortativity" if a featureid and edgeschemaid is provided.
 * Otherwise, multiple assortativity values are returned
 * for all possible categorical values (if featureid is not defined)
 * over all possible edges (if edgeschemaid is not defined)
 * with the key "assortativity=[featureid].[edgeschemaid]"
 * <p>
 * Reference: Mixing patterns in networks, M. E. J. Newman, Phys. Rev. E 67, 026126 (2003).
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> featureschemaid-Schema ID of the feature over which to measure assortativity
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> edgefvpair-Constraint on which edges to use.  Given a comma delimited
 * set of feature=value pairs, only calculate assortativity on edges
 * which match the features and string value specified (i.e., label=red,length=5.0)
 * <LI> featureid-Feature ID of the feature over which to measure assortativity.
 * If not defined, assortativity is computed for all categorical valued
 * feature for the given schema.
 * <LI> edgeschemaid-Schema ID of edges over which to measure assortativity.
 * If not defined, assortativity is computed for all edge schema ids
 * defined in the graph.
 * <LI> omitzero-If set to yes, only return the assortativity of featureids
 * and edgeschemaid pairs which have an assortativity that is non-zero.
 * Default is no.
 * </UL>
 * 
 * @author mbilgic
 * @author namatag
 *
 */
public class Assortativity extends BaseConfigurable implements GraphStatistic {
	private String NAME = "assortativity";
	
	/**
	 * Calculate assortativity
	 * 
	 * @param g Graph
	 * @param eitr Iterator over edges to calculate assortativity over.  If null, return 0.
	 * @param fschemaid Feature schema id of feature to calculate assortativity over
	 * @param featureid Feature id to calculate assortativity over
	 * @param edgeschemaid Schema ID of edges to calculate assortativity over
	 * @param fvpairs Map of key-value pairs edges must pass to consider
	 * where the key is the feature id and the value is the string value.
	 * Set as null if no constraints.
	 * @return Assortativity
	 */
	public static double calcAssortativity(Graph g, Iterator<Edge> eitr, String fschemaid,
			String featureid, String edgeschemaid, Map<String,String> fvpairs) {
		Feature f = g.getSchema(fschemaid).getFeature(featureid);
		
		if(!(f instanceof CategFeature)) {
			throw new ConfigurationException("Unsupported feature type: "+f.getClass().getCanonicalName());
		}
		
		CategFeature cf = (CategFeature) f;
		UnmodifiableList<String> categories = cf.getAllCategories();
		int nc = categories.size();

		double[][] e = new double[nc][nc];
		int count = 0;
		
		Schema edgeschema = g.getSchema(edgeschemaid);
		boolean isdirected = true;
		if(edgeschema.getType().equals(SchemaType.DIRECTED)) {
			isdirected = true;
		} else if(edgeschema.getType().equals(SchemaType.UNDIRECTED)) {
			isdirected = false;
		} else {
			throw new ConfigurationException("Unsupported edge type: "+edgeschema.getType());
		}
		
		while(eitr.hasNext()) {
			Edge curredge = eitr.next();
			
			// Only support binary edges
			if(curredge.numNodes() != 2) {
				Log.DEBUG("Skipping edge.  " +
						"Unsupported number of nodes in edge: "
						+curredge.numNodes()+" in "+curredge+".");
				
				// Continue instead of breaking to handle
				// the case of undirected edges which sometimes
				// has self loops.
				continue;
			}
			
			// Only evaluate over edges with the specified match
			if(fvpairs!=null) {
				boolean skip = true;
				for(Entry<String,String> fvp:fvpairs.entrySet()) {
					if(!curredge.getFeatureValue(fvp.getKey()).getStringValue().equals(fvp.getValue())) {
						skip = false;
						break;
					}
				}
				
				// Skip if the conditions are not met
				if(skip) {
					continue;
				}
			}
			
			Node n1 = null;
			Node n2 = null;
			if(isdirected) {
				DirectedEdge de = (DirectedEdge) curredge;
				
				// Set n1 to the source node and n2 for the target node
				n1 = de.getSourceNodes().next();
				n2 = de.getTargetNodes().next();
			} else {
				UndirectedEdge ue = (UndirectedEdge) curredge;
				
				// Arbitrarily set n1 and n2 of the nodes for now.
				// Later, we count both directions n1->n2 and n2->n1
				Iterator<Node> nodes = ue.getAllNodes();
				n1 = nodes.next();
				n2 = nodes.next();
			}
			
			// Check to see if nodes match the schema that we're working with
			if(!n1.getSchemaID().equals(fschemaid) || !n2.getSchemaID().equals(fschemaid)) {
				if(Log.SHOWDEBUG) {
					Log.DEBUG("Encountered unexpected node schema: "
						+n1.getSchemaID() +" or "+n2.getSchemaID()+" on "+curredge+"."
						+" Stopping computation for "+featureid+".");
				}
				
				continue;
			}
			
			FeatureValue fv1 = n1.getFeatureValue(featureid);
			FeatureValue fv2 = n2.getFeatureValue(featureid);
			
			// Continue if the value for at least one feature value is unknown
			if(fv1.equals(FeatureValue.UNKNOWN_VALUE) || fv2.equals(FeatureValue.UNKNOWN_VALUE)) {
				continue;
			}
			
			String sourceClass = ((CategValue) fv1).getCategory();
			String destClass = ((CategValue) fv2).getCategory();

			int sourceIndex = categories.indexOf(sourceClass);
			int destIndex = categories.indexOf(destClass);

			e[sourceIndex][destIndex]++;
			count++;
			
			// Count in the opposite direction
			if(!isdirected) {
				e[destIndex][sourceIndex]++;
				count++;
			}
		}
		
		// Normalize e
		for(int i=0;i<nc;i++){
			for(int j=0;j<nc;j++){
				e[i][j] /= count;
			}
		}

		// Calculate a and b
		double[] a = new double[nc];
		double[] b = new double[nc];
		for(int i=0; i<nc;i++){
			double sum=0;
			for(int j=0;j<nc;j++){
				sum += e[i][j];
			}

			a[i] = sum;
		}

		for(int j=0;j<nc;j++){
			double sum=0;
			for(int i=0;i<nc;i++){
				sum += e[i][j];
			}

			b[j] = sum;
		}

		// Numerator
		double trace=0;
		double ab=0;
		for(int i=0;i<nc;i++){
			trace += e[i][i];
			ab += a[i]*b[i];
		}

		// Calculate assortativity
		double assortativity = (trace - ab) / (1 - ab);
		
		return assortativity;
	}
	
	/**
	 * Calculate assortativity
	 * 
	 * @param g Graph
	 * @param fschemaid Feature schema id of feature to calculate assortativity over
	 * @param featureid Feature id to calculate assortativity over
	 * @param edgeschemaid Schema ID of edges to calculate assortativity over
	 * @param fvpairs Map of key-value pairs edges must pass to consider
	 * where the key is the feature id and the value is the string value.
	 * Set as null if no constraints.
	 * @return Assortativity
	 */
	public static double calcAssortativity(Graph g, String fschemaid,
			String featureid, String edgeschemaid, Map<String,String> fvpairs) {
		return Assortativity.calcAssortativity(g, g.getEdges(edgeschemaid),
				fschemaid, featureid, edgeschemaid, fvpairs);
	}

	public Map<String, Double> getStatisticDoubles(Graph g) {
		String fschemaid = this.getStringParameter("featureschemaid");
		String featureid = null;
		if(this.hasParameter("featureid")) {
			featureid = this.getStringParameter("featureid");
		}
		
		String edgeschemaid = null;
		if(this.hasParameter("edgeschemaid")) {
			edgeschemaid = this.getStringParameter("edgeschemaid");
		}
		
		Map<String,String> fvpairs = null;
		// Get edge feature constraints
		if(this.hasParameter("edgefvpair")) {
			String efvp = this.getStringParameter("edgefvpair");
			String[] pairs = efvp.split(",");
			fvpairs = new HashMap<String,String>(pairs.length);
			for(String pair:pairs) {
				String[] pairparts = pair.split("=");
				if(pairparts.length!=2) {
					throw new ConfigurationException("Invalid configuration: "+efvp);
				}
				
				fvpairs.put(pairparts[0], pairparts[1]);
			}
		}
		
		Map<String, Double> stat = new HashMap<String,Double>(1);
		if(edgeschemaid!=null && featureid!=null) {
			Iterator<Edge> eitr = g.getEdges(edgeschemaid);
			double assortativity = Assortativity.calcAssortativity(g, eitr, fschemaid, featureid,
					edgeschemaid, fvpairs);
			stat.put(NAME, assortativity);
		} else {
			// Consider all types of edges, if not defined
			Set<String> esids = new HashSet<String>();
			if(edgeschemaid == null) {
				esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED)));
				esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED)));
			} else {
				esids.add(edgeschemaid);
			}
			
			// Consider all types of categorical features, if not defined
			Set<String> fids = new HashSet<String>();
			if(featureid==null) {
				Schema schema = g.getSchema(fschemaid);
				Iterator<String> fitr = schema.getFeatureIDs();
				while(fitr.hasNext()) {
					String fid = fitr.next();
					Feature f = schema.getFeature(fid);
					if(f instanceof CategFeature) {
						fids.add(fid);
					}
				}
			} else {
				fids.add(featureid);
			}
			
			// Iterate over all possible pairs
			for(String esid:esids) {
				for(String fid:fids) {
					double assortativity = Assortativity.calcAssortativity(g, g.getEdges(esid),
							fschemaid, fid,
							esid, fvpairs);
					
					if(this.hasYesNoParameter("omitzero", "yes") && assortativity==0.0) {
						// Don't include the assortativity
					} else {
						stat.put(NAME+"="+esid+"+"+fid, assortativity);
					}
				}
			}
		}

		return stat;
	}

	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(this.getStatisticDoubles(g),
				new LinkedHashMap<String,String>());
	}
}
