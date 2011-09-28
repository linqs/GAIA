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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.MapUtils;

/**
 * Count the number of singletons (nodes with no edges)
 * overall and given all node and edge type.
 * The following keys are returned:
 * 
 * <UL>
 * <LI> NumberOfSingletons-Number of singletons in the graph
 * <LI> NumberOfSingletons:&lt;edgeschemaid&gt;-Number of singletons in the graph
 * where the node is a singleton given the specified edge schema id
 * <LI> NumberOfSingletons:&lt;nodeschemaid&gt;:&lt;edgeschemaid&gt;-Number of singletons in the graph
 * where the node is of the given schema and the singleton given the specified edge schema id
 * </UL>
 * 
 * @author namatag
 *
 */
public class SingletonCount extends BaseConfigurable implements GraphStatistic {
	public Map<String, Double> getStatisticDoubles(Graph g) {
		Map<String, Double> stats = new LinkedHashMap<String, Double>();
		
		// Count the number of singletons
		double count = 0;
		Iterator<Node> nitr = g.getNodes();
		while(nitr.hasNext()) {
			Node n = nitr.next();
			if(n.numEdges()==0) {
				count++;
			}
		}
		
		stats.put("NumOfSingletons", count);
		
		// Count the number of singletons for specific directed edge types
		count = 0;
		List<String> esids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.DIRECTED));
		esids.addAll(IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.UNDIRECTED)));
		for(String sid:esids) {
			nitr = g.getNodes();
			while(nitr.hasNext()) {
				Node n = nitr.next();
				if(n.numIncidentGraphItems(sid)==0) {
					count++;
				}
			}
			
			stats.put("NumSingletons:"+sid, count);
		}
		
		List<String> nsids = IteratorUtils.iterator2stringlist(g.getAllSchemaIDs(SchemaType.NODE));
		for(String nsid:nsids) {
			for(String esid:esids) {
				nitr = g.getNodes(nsid);
				while(nitr.hasNext()) {
					Node n = nitr.next();
					if(n.numIncidentGraphItems(esid)==0) {
						count++;
					}
				}
				
				stats.put("NumSingletons:"+nsid+":"+esid, count);
			}
		}
		
		return stats;
	}
	
	public String getStatisticString(Graph g) {
		return MapUtils.map2string(this.getStatisticDoubles(g), "=", ",");
	}

	public Map<String, String> getStatisticStrings(Graph g) {
		return MapUtils.map2stringmap(
				this.getStatisticDoubles(g),
				new LinkedHashMap<String, String>());
	}
}
