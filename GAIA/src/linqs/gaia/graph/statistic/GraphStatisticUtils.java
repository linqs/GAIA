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

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.util.ArrayUtils;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedCount;

/**
 * Utility functions for anyone calculating graph statistics
 * 
 * @author namatag
 *
 */
public class GraphStatisticUtils {
	/**
	 * Return degree statistics of graph items.
	 * Degree is defined for nodes as the number
	 * of unique edges a node is in, and for an edge,
	 * the number of nodes in that edge.
	 * 
	 * @param gitems List of items to check degree of
	 * @param schemaid If specified, only count incident items of given schema.
	 * If null, count all.
	 * @param degreetype Specify the type of node degree to count, if applicable.
	 * Set to null otherwise.  For nodes, the possible values are all (all edges),
	 * undirected (all undirected edges), inandout (all directed edges),
	 * in (all incoming directed edges), and out (all outgoing directed edges).
	 * 
	 * @return Double array with [min,max,average,stddev,numwithdegree0]
	 */
	public static double[] degreeStats(Iterable<? extends GraphItem> gitems,
			String schemaid, String degreetype) {
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		double avg = 0;
		double stddev = 0;
		double num0 = 0;
		int numitems = IteratorUtils.numIterable(gitems);
		double degrees[] = new double[numitems];
		int counter = 0;
		
		for(GraphItem gi:gitems){
			double degree = 0;
			Iterator<? extends GraphItem> conn = null;
			if(gi instanceof Node) {
				if(degreetype==null || degreetype.equals("all")) {
					conn = gi.getIncidentGraphItems();
				} else if(degreetype.equals("undirected")) {
					conn = ((Node) gi).getUndirEdges();
				} else if (degreetype.equals("inandout")){ 
					conn = ((Node) gi).getDirEdges();
				} else if(degreetype.equals("in")){
					conn = ((Node) gi).getEdgesWhereTarget();
				} else if(degreetype.equals("out")){
					conn = ((Node) gi).getEdgesWhereSource();
				} else {
					throw new UnsupportedTypeException("Unsupported degree type: "+degreetype);
				}
			} else if(gi instanceof Edge) {
				conn = ((Edge) gi).getAllNodes();
			} else {
				throw new UnsupportedTypeException("Unsupported Graph Item: "+gi.getClass().getCanonicalName());
			}
			
			while(conn.hasNext()){
				// Only count connected items of the specified type
				String connsid = conn.next().getSchemaID();
				if(schemaid == null || connsid.equals(schemaid)){
					degree++;
				}
			}
			
			degrees[counter] = degree;
			avg+=degree;
			if(max<degree) {
				max = degree;
			}
			
			if(min>degree){
				min = degree;
			}
			
			if(degree==0) {
				num0++;
			}
			
			counter++;
		}
		
		avg = avg/(double) numitems;
		stddev = ArrayUtils.stddev(degrees);
		
		double results[] = new double[]{min,max,avg,stddev,num0};
		return results;
	}
	
	/**
	 * Return degree statistics of graph items.
	 * Degree is defined for nodes as the number
	 * of unique edges a node is in, and for an edge,
	 * the number of nodes in that edge.
	 * 
	 * @param gitems List of items to check degree of
	 * 
	 * @return Double array with [min,max,average,stddev,numwithdegree0]
	 */
	public static double[] degreeStats(Iterable<? extends GraphItem> gitems) {
		return degreeStats(gitems, null, null);
	}
	
	/**
	 * Return degree statistics of graph items.
	 * Degree is defined for nodes as the number
	 * of unique edges a node is in, and for an edge,
	 * the number of nodes in that edge.
	 * 
	 * @param gitems List of items to check degree of
	 * @param incidentsid If specified, only count incident items of given schema.
	 * If null, count all.
	 * @param degreetype Specify the type of node degree to count, if applicable.
	 * Set to null otherwise.  For nodes, the possible values are all (all edges),
	 * undirected (all undirected edges), inandout (all directed edges),
	 * in (all incoming directed edges), and out (all outgoing directed edges).
	 * 
	 * @return Keyed count of the degree distributions where the
	 * key is the degree and the value is the number of graph items with that degree.
	 */
	public static KeyedCount<Integer> degreeDistribution(Iterator<? extends GraphItem> gitems,
			String incidentsid, String degreetype) {
		KeyedCount<Integer> kc = new KeyedCount<Integer>();
		while(gitems.hasNext()){
			GraphItem gi = gitems.next();
			
			double degree = 0;
			Iterator<? extends GraphItem> conn = null;
			if(gi instanceof Node) {
				if(degreetype==null || degreetype.equals("all")) {
					conn = gi.getIncidentGraphItems();
				} else if(degreetype.equals("undirected")) {
					conn = ((Node) gi).getUndirEdges();
				} else if (degreetype.equals("inandout")){ 
					conn = ((Node) gi).getDirEdges();
				} else if(degreetype.equals("in")){
					conn = ((Node) gi).getEdgesWhereTarget();
				} else if(degreetype.equals("out")){
					conn = ((Node) gi).getEdgesWhereTarget();
				} else {
					throw new UnsupportedTypeException("Unsupported degree type: "+degreetype);
				}
			} else if(gi instanceof Edge) {
				conn = ((Edge) gi).getAllNodes();
			} else {
				throw new UnsupportedTypeException("Unsupported Graph Item: "+gi.getClass().getCanonicalName());
			}
			
			while(conn.hasNext()){
				// Only count connected items of the specified type
				String connsid = conn.next().getSchemaID();
				if(incidentsid == null || connsid.equals(incidentsid)){
					degree++;
				}
			}
			
			kc.increment((int) degree);
		}
		
		return kc;
	}
}
