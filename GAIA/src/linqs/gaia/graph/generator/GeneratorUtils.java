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
package linqs.gaia.graph.generator;

import java.util.Arrays;
import java.util.Iterator;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphID;
import linqs.gaia.identifiable.GraphItemID;

public class GeneratorUtils {
	/**
	 * Add equivalent binary undirected edges from binary directed edges.
	 * More specifically, if two nodes have at least one binary
	 * directed edge (in either direction), we create
	 * a binary undirected edge between them.
	 * <p>
	 * Note: The schema of the directed edge is not copied to the undirected edge.
	 * 
	 * @param g Graph
	 * @param dirsid Directed edge schema ID
	 * @param undirsid Undirected edge schema ID
	 */
	public static void copyDir2Undir(Graph g, String dirsid, String undirsid) {
		if(g.hasSchema(undirsid)) {
			throw new InvalidStateException("Undirected SID already defined: "+undirsid);
		}
		
		Schema schema = new Schema(SchemaType.UNDIRECTED);
		g.addSchema(undirsid, schema);
		
		Iterator<Edge> eitr = g.getEdges(dirsid);
		while(eitr.hasNext()) {
			DirectedEdge de = (DirectedEdge) eitr.next();
			
			// Only support binary edges
			if(de.numSourceNodes() != 1 && de.numTargetNodes() != 1) {
				throw new InvalidStateException("Only binary edges supported: "+de
						+" has #sources="+de.numSourceNodes()
						+" has #targets="+de.numTargetNodes());
			}
			
			Node source = de.getSourceNodes().next(); // Assume only 1
			Node target = de.getTargetNodes().next(); // Assume only 1
			
			// Add undirected edge between source and target, if one doesn't already exist.
			if(!source.isAdjacent(target, undirsid)) {
				GraphItemID giid = new GraphItemID((GraphID) g.getID(), undirsid, de.getID().getObjID());
				g.addUndirectedEdge(giid, Arrays.asList(new Node[]{source, target}).iterator());
			}
		}
	}
}
