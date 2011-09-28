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

import java.util.LinkedHashMap;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.derived.structural.EdgeNodeShortestPath;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.converter.adjmatrix.AdjacencyMatrix;
import linqs.gaia.util.Dynamic;
import linqs.gaia.util.MapUtils;

/**
 * Compute the diameter (longest shortest path) of the graph.
 * If the graph consists of multiple components,
 * we return the diameter, using the key "diameter", of the largest component.
 * If the graph is disconnected, we also return a value of "1"
 * with the key of "isdisconnected".
 * <p>
 * Note: Computing this statistic maybe slow for large graphs since
 * it requires computing the shortest path between all pairs of nodes.
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> adjclass-{@link AdjacencyMatrix} class to use,
 * instantiated using in {@link Dynamic#forConfigurableName}.
 * Default is to create an {@link AdjacencyMatrix} class with default parameters.
 * </UL>
 * 
 * @author namatag
 *
 */
public class Diameter extends BaseConfigurable implements GraphStatistic {
	private String NAME = "diameter";
	private String ISDISCONNECTED = "isdisconnected";
	
	public Map<String, Double> getStatisticDoubles(Graph g) {
		AdjacencyMatrix conv = new AdjacencyMatrix();
		double[][] shortest = EdgeNodeShortestPath.computeShortestPathLengths(conv.exportGraph(g));
		
		// Compute all the shortest paths
		double max = Double.NEGATIVE_INFINITY;
		boolean isdisconnected = false;
		for(int i=0; i<shortest.length; i++) {
			for(int j=0; j<shortest.length; j++) {
				if(shortest[i][j]==Double.POSITIVE_INFINITY) {
					isdisconnected = true;
					continue;
				}
				
				if(max<shortest[i][j]) {
					max = shortest[i][j];
				}
			}
		}
		Map<String, Double> stats = new LinkedHashMap<String, Double>(1);
		stats.put(NAME, max);
		
		if(isdisconnected) {
			stats.put(ISDISCONNECTED, 1.0);
		}
		
		return stats;
	}
	
	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(this.getStatisticDoubles(g),
				new LinkedHashMap<String,String>());
	}
}
