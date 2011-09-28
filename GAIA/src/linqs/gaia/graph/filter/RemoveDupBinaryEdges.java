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
package linqs.gaia.graph.filter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;

/**
 * Remove all duplicate edges.  An edge is considered duplicate
 * if at least one other edge exists with the same schema ID
 * and adjacent to the same nodes.
 * <p>
 * Note:  Currently only supports binary edges.
 * 
 * @author namatag
 *
 */
public class RemoveDupBinaryEdges extends Filter {
	@Override
	public void filter(Graph graph) {
		Set<Edge> toremove = new HashSet<Edge>();
		Set<String> processed = new HashSet<String>();
		Iterator<Edge> eitr = graph.getEdges();
		while(eitr.hasNext()) {
			Edge e = eitr.next();
			if(e.numNodes()!=2) {
				throw new UnsupportedTypeException("Only supports binary edges: "+e.numNodes());
			}
			
			if(e instanceof DirectedEdge) {
				Node s = ((DirectedEdge) e).getSourceNodes().next();
				Node t = ((DirectedEdge) e).getTargetNodes().next();
				String key = e.getSchemaID()+"+"+s.getID()+"+"+t.getID();
				
				if(processed.contains(key)) {
					toremove.add(e);
				} else {
					processed.add(key);
				}
			} else if(e instanceof UndirectedEdge) {
				Iterator<Node> nitr = e.getAllNodes();
				Node n1 = nitr.next();
				Node n2 = nitr.next();
				
				String key = e.getSchemaID()+"+"+n1.getID()+"+"+n2.getID();
				
				if(processed.contains(key)) {
					toremove.add(e);
				} else {
					String key1 = e.getSchemaID()+"+"+n1.getID()+"+"+n2.getID();
					String key2 = e.getSchemaID()+"+"+n2.getID()+"+"+n1.getID();
					processed.add(key1);
					processed.add(key2);
				}
			} else {
				throw new UnsupportedTypeException("Unsupported edge type: "+e.getClass().getCanonicalName());
			}
		}
		
		// Remove duplicate edges
		for(Edge e:toremove) {
			graph.removeEdge(e);
		}
	}
}
