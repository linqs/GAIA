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
package linqs.gaia.similarity.node;

import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.derived.neighbor.Adjacent;
import linqs.gaia.feature.derived.neighbor.Neighbor;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.similarity.NodeDistance;
import linqs.gaia.similarity.NormalizedListSimilarity;
import linqs.gaia.similarity.NormalizedNodeSimilarity;
import linqs.gaia.similarity.NormalizedSetSimilarity;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.IteratorUtils;

/**
 * Naive relational similarity as defined in:
 * <p>
 * Bhattacharya, I. & Getoor, L.,
 * Collective Entity Resolution in Relational Data,
 * ACM Transactions on Knowledge Discovery from Data, 2007, 1, 1-36 
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> nodeschemaid-Schema ID of the edge nodes
 * <LI> featuresimclass-{@link NormalizedListSimilarity} to use to calculate feature similarity.
 * Either featuresimclass or relsimclass or both must be defined.
 * <LI> relsimclass-{@link NormalizedSetSimilarity} to use to calculate relational similarity.
 * Either featuresimclass or relsimclass or both must be defined.
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> neighborclass-The {@link Neighbor} object,
 * instantiated using in {@link Dynamic#forConfigurableName}, used to compute the neighbors
 * of the nodes.  Default is to use {@link linqs.gaia.feature.derived.neighbor.Adjacent}.
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
 * <LI> alpha-Value between 0 and 1, inclusive.  Parameter to weigh the different similarity measures.
 * Default is .5.
 * </UL>
 * 
 * @see linqs.gaia.util.Dynamic#forConfigurableName(Class, String)
 * @author namatag
 *
 */
public class NaiveRelationalSim extends BaseConfigurable
		implements NormalizedNodeSimilarity, NodeDistance {
	private static final long serialVersionUID = 1L;
	
	private Double alpha = .5;
	private NormalizedListSimilarity featuresim = null;
	private NormalizedSetSimilarity relsim = null;
	private Neighbor neighbor = null;
	private String nodeschemaid = null;
	private List<String> featureids = null;
	
	private boolean initialize = true;
	
	private void initialize(Graph graph) {
		if(!initialize) {
			return;
		}
		
		synchronized(this) {
			if(!initialize) {
				return;
			}
			
			// Initialize feature similarity
			if(this.hasParameter("featuresimclass")) {
				String featuresimclass = this.getStringParameter("featuresimclass");
				featuresim = (NormalizedListSimilarity) 
					Dynamic.forConfigurableName(NormalizedListSimilarity.class, featuresimclass);
				featuresim.copyParameters(this);
				
				// Get feature ids to perform feature similarity over
				this.nodeschemaid = this.getStringParameter("nodeschemaid");
				Schema schema = graph.getSchema(this.nodeschemaid);
				featureids = FeatureUtils.parseFeatureList(this,
						schema, FeatureUtils.getFeatureIDs(schema, 2));
			}
			
			// Initialize relational similarity
			if(this.hasParameter("relsimclass")) {
				String relsimclass = this.getStringParameter("relsimclass");
				relsim = (NormalizedSetSimilarity) 
					Dynamic.forConfigurableName(NormalizedSetSimilarity.class, relsimclass);
				relsim.copyParameters(this);
			}
			
			// Get neighbor class
			if(this.hasParameter("neighborclass")) {
				String neighborclass = this.getStringParameter("neighborclass");
				this.neighbor = (Neighbor)
					Dynamic.forConfigurableName(Neighbor.class, neighborclass);
				this.neighbor.copyParameters(this);
			} else {
				this.neighbor = new Adjacent();
			}
			
			// Get alpha
			alpha = .5;
			if(this.hasParameter("alpha")) {
				this.alpha = this.getDoubleParameter("alpha");
			}
			
			initialize = false;
		}
	}

	public double getNormalizedSimilarity(Node item1, Node item2) {
		if(initialize) {
			this.initialize(item1.getGraph());
		}
		
		double sim = 0;
		double fsim = 0;
		double rsim = 0;
		
		if(this.featuresim != null) {
			fsim = this.featuresim.getNormalizedSimilarity(
					item1.getFeatureValues(this.featureids),
					item2.getFeatureValues(this.featureids));
		}
		
		if(this.relsim != null) {
			rsim = this.relsim.getNormalizedSimilarity(
					IteratorUtils.iterator2nodeset(this.neighbor.getNeighbors(item1).iterator()),
					IteratorUtils.iterator2nodeset(this.neighbor.getNeighbors(item2).iterator()));
		}
		
		if(this.featuresim!=null && this.relsim!=null) {
			sim = (fsim*(1.0-this.alpha) + (rsim*(alpha)));
		} else if(this.featuresim!=null) {
			sim = fsim;
		} else if(this.relsim!=null) {
			sim = rsim;
		} else {
			throw new InvalidStateException("No similarity measures defined");
		}
		
		return sim;
	}

	public double getSimilarity(Node item1, Node item2) {
		if(initialize) {
			this.initialize(item1.getGraph());
		}
		
		double sim = 0;
		double fsim = 0;
		double rsim = 0;
		
		if(this.featuresim != null) {
			fsim = this.featuresim.getSimilarity(
					item1.getFeatureValues(this.featureids),
					item2.getFeatureValues(this.featureids));
		}
		
		if(this.relsim != null) {
			rsim = this.relsim.getSimilarity(
					IteratorUtils.iterator2nodeset(this.neighbor.getNeighbors(item1).iterator()),
					IteratorUtils.iterator2nodeset(this.neighbor.getNeighbors(item2).iterator()));
		}
		
		if(this.featuresim!=null && this.relsim!=null) {
			sim = (fsim*(1.0-this.alpha) + (rsim*(alpha)));
		} else if(this.featuresim!=null) {
			sim = fsim;
		} else if(this.relsim!=null) {
			sim = rsim;
		} else {
			throw new InvalidStateException("No similarity measures defined");
		}
		
		return sim;
	}

	public double getDistance(Node item1, Node item2) {
		return 1.0 - this.getNormalizedSimilarity(item1, item2);
	}
	
	public void setAlpha(Graph g, double alpha) {
		if(initialize) {
			this.initialize(g);
		}
		
		this.alpha = alpha;
	}
	
	public double getAlpha(Graph g) {
		if(initialize) {
			this.initialize(g);
		}
		
		return this.alpha;
	}
}
