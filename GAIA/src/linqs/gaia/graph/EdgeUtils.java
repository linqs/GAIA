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
package linqs.gaia.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.FeatureUtils;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

/**
 * Utility functions for accessing, manipulating, and creating edges
 * 
 * @author namatag
 *
 */
public class EdgeUtils {
	/**
	 * Add equivalent undirected edges from directed edges.
	 * More specifically, if two nodes have at least one binary
	 * directed edge (in either direction), we create
	 * a binary undirected edge between them.
	 * <p>
	 * Note: The schema of the directed edge is not copied to the undirected edge.
	 * 
	 * @param g Graph
	 * @param dirsid Directed edge schema ID
	 * @param undirsid Undirected edge schema ID
	 * @param omitdups Do not add an undirected between two nodes which
	 * already have an instance of that undirected edge between them.
	 */
	public static void copyDir2Undir(Graph g, String dirsid, String undirsid, boolean omitdups) {
		EdgeUtils.copyDir2Undir(g, dirsid, undirsid, omitdups, false);
	}
	
	/**
	 * Add equivalent undirected edges from directed edges.
	 * More specifically, if two nodes have at least one binary
	 * directed edge (in either direction), we create
	 * a binary undirected edge between them.
	 * <p>
	 * Note: The schema of the directed edge is not copied to the undirected edge.
	 * 
	 * @param g Graph
	 * @param dirsid Directed edge schema ID
	 * @param undirsid Undirected edge schema ID
	 * @param omitdups Do not add an undirected between two nodes which
	 * already have an instance of that undirected edge between them.
	 * @param omitselflink Omit self link
	 */
	public static void copyDir2Undir(Graph g, String dirsid, String undirsid, boolean omitdups, boolean omitselflink) {
		if(g.hasSchema(undirsid)) {
			throw new InvalidStateException("Undirected SID already defined: "+undirsid);
		}
		
		Schema schema = new Schema(SchemaType.UNDIRECTED);
		g.addSchema(undirsid, schema);
		
		Iterator<Edge> eitr = g.getEdges(dirsid);
		while(eitr.hasNext()) {
			DirectedEdge de = (DirectedEdge) eitr.next();
			
			List<Node> sources = IteratorUtils.iterator2nodelist(de.getSourceNodes());
			List<Node> targets = IteratorUtils.iterator2nodelist(de.getTargetNodes());
			
			// Check to see if the addition of an edge will result in a duplicate
			boolean hasdup = false;
			if(omitdups) {
				for(Node s:sources) {
					for(Node t:targets) {
						if(s.isAdjacent(t, undirsid)) {
							hasdup = true;
							break;
						}
					}
					
					if(hasdup) {
						break;
					}
				}
			}
			
			// Add undirected edge between source and target, if one doesn't already exist.
			if(omitdups && hasdup) {
				// If removing dups and this edge will be a dup, don't add it
			} else {
				// Add new edge
				GraphItemID giid = new GraphItemID((GraphID) g.getID(), undirsid, de.getID().getObjID());
				Set<Node> nodes = new HashSet<Node>();
				nodes.addAll(sources);
				nodes.addAll(targets);
				
				if(omitselflink) {
					if(nodes.size()!=1) {
						g.addUndirectedEdge(giid, nodes);
					}
				} else {
					g.addUndirectedEdge(giid, nodes);
				}
			}
		}
	}
	
	/**
	 * Create an edge with the same nodes as the specified edge
	 * but with the specified schema ID.
	 * <p>
	 * Note: An exception is thrown if the a schema of the specified
	 * copy schema id is not specified.
	 * 
	 * @param e Edge to copy
	 * @param copysid Schema ID of edge to copy
	 * @param copyfeatures If true, copy the features of the original nodes and false otherwise
	 * @return Copy edge
	 */
	public static Edge copyEdge(Edge e, String copysid, boolean copyfeatures) {
		Edge copye = null;
		Graph g = e.getGraph();
		if(e instanceof DirectedEdge) {
			DirectedEdge de = (DirectedEdge) e;
			Iterator<Node> sources = de.getSourceNodes();
			Iterator<Node> targets = de.getTargetNodes();
			copye = g.addDirectedEdge(new GraphItemID(copysid,e.getID().getObjID()), sources, targets);
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			Iterator<Node> nodes = ue.getAllNodes();
			copye = g.addUndirectedEdge(new GraphItemID(copysid,e.getID().getObjID()), nodes);
		} else {
			throw new UnsupportedTypeException("Unsupported edge type: "+e.getClass().getCanonicalName());
		}
		
		if(copyfeatures) {
			FeatureUtils.copyFeatureValues(e, copye);
		}
		
		return copye;
	}
	
	/**
	 * Add an edge to the specified graph with the "equivalent"
	 * set of nodes and the same values.
	 * This assumes that the schema of the edge is already defined
	 * in the graph and that the graph contains an equivalent
	 * node for each node incident the specified edge.
	 * 
	 * @param g Graph to add equivalent edge into
	 * @param e Edge to create an equivalent copy of
	 * @param copyfeatures If true, copy the feature values of
	 * the original edge to the equivalent edge.  An exception
	 * is thrown if the two edges do not have the same schema
	 * @return Equivalent edge added
	 */
	public static Edge addEquivalentEdge(Graph g, Edge e, boolean copyfeatures) {
		Edge newe;
		// Add an equivalent edge
		if(e instanceof DirectedEdge) {
			DirectedEdge de = (DirectedEdge) e;
			List<Node> sources = new ArrayList<Node>();
			Iterator<Node> nitr = de.getSourceNodes();
			while(nitr.hasNext()) {
				Node orign = nitr.next();
				GraphItem gi = g.getEquivalentGraphItem(orign);
				if(gi==null) {
					throw new InvalidStateException("Cannot add equivalent edge since" +
							" a node in the edge was encountered without an equivalent node" +
							" in the graph: "+orign);
				}
				
				sources.add((Node) gi);
			}
			
			List<Node> targets = new ArrayList<Node>();
			nitr = de.getTargetNodes();
			while(nitr.hasNext()) {
				Node orign = nitr.next();
				GraphItem gi = g.getEquivalentGraphItem(orign);
				if(gi==null) {
					throw new InvalidStateException("Cannot add equivalent edge since" +
							" a node in the edge was encountered without an equivalent node" +
							" in the graph: "+orign);
				}
				
				targets.add((Node) gi);
			}
			
			newe = g.addDirectedEdge(de.getID().copyWithoutGraphID(),
					sources,
					targets);
			
			
		} else if(e instanceof UndirectedEdge) {
			UndirectedEdge ue = (UndirectedEdge) e;
			
			List<Node> uenodes = new ArrayList<Node>();
			Iterator<Node> nitr = ue.getAllNodes();
			while(nitr.hasNext()) {
				Node orign = nitr.next();
				GraphItem gi = g.getEquivalentGraphItem(orign);
				if(gi==null) {
					throw new InvalidStateException("Cannot add equivalent edge since" +
							" a node in the edge was encountered without an equivalent node" +
							" in the graph: "+orign);
				}
				
				uenodes.add((Node) gi);
			}
			
			newe = g.addUndirectedEdge(e.getID().copyWithoutGraphID(), uenodes);
		} else {
			throw new UnsupportedTypeException("Unsupported edge type: "+e);
		}
		
		if(copyfeatures) {
			FeatureUtils.copyFeatureValues(e, newe);
		}
		
		return newe;
	}
}
