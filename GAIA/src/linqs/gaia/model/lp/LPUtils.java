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
package linqs.gaia.model.lp;

import java.util.Iterator;

import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;

/**
 * General utilities for use with link prediction tasks
 * 
 * @author namatag
 *
 */
public class LPUtils {
	/**
	 * Return true if n1 and n2 share at least
	 * one edge of the given schema id.
	 * If the schema id is null, return true
	 * if n1 and n2 share at least one edge.
	 * Return false otherwise.
	 * 
	 * @param edgeschemaid Schema ID of edge.  Set to null if you want to consider all edge types.
	 * @param n1 Node
	 * @param n2 Node
	 * @return True if they share an edge.  False otherwise.
	 * @deprecated Use isAdjacent instead
	 */
	public static boolean edgeExists(String edgeschemaid, Node n1, Node n2) {
		Iterator<GraphItem> conn = null;
		if(edgeschemaid!=null) {
			conn = n1.getIncidentGraphItems(edgeschemaid);
		} else {
			conn = n1.getIncidentGraphItems();
		}
		
		while(conn.hasNext()) {
			UndirectedEdge ue = (UndirectedEdge) conn.next();
			if(ue.isIncident(n2)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return true if n1 and n2 share at least
	 * one edge of the given schema id.
	 * Return false otherwise.
	 * 
	 * @param n1 Node
	 * @param n2 Node
	 * @return True if they share an edge.  False otherwise.
	 * @deprecated Use isAdjacent instead
	 */
	public static boolean edgeExists(Node n1, Node n2) {
		return LPUtils.edgeExists(null, n1, n2);
	}
}
