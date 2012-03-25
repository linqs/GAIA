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

package linqs.gaia.model.util.plg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import linqs.gaia.configurable.BaseConfigurable;
import linqs.gaia.exception.ConfigurationException;
import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.BaseIterator;


/**
 * 
 * Create binary potential links by sorting nodes by a key and sliding a window over
 * the sorted list, instantiating all links inside the window. Does not currently support undirected links.
 * <p>
 * Required Parameters:
 * <UL>
 * <LI> sortfid-Feature to sort nodes by.
 * <LI> nodeschemaid-Schema ID of the nodes to consider potential edges between
 * </UL>
 * <p>
 * Optional Parameters:
 * <UL>
 * <LI> allowselflinks-If "yes", allow self links (i.e., allow node to link to self).  Default is "no".
 * <LI> allowduplinks-If "yes", allow duplicate links (i.e., create edges between two nodes
 * even if they are already adjacent given the specified edge schema id).  Default is "no".
 * <LI> windowLength - size of window. Default is 10.
 * </UL>
 * 
 * @author bert
 *
 */
public class SortedNeighborhood extends BaseConfigurable implements PotentialLinkGenerator {

	private String nodeschemaid;
	private boolean allowselflinks;
	private boolean allowduplinks;
	private boolean initialize = true;
	private String sortfid;
	private int windowLength;
	
	public Iterator<Edge> getLinksIteratively(Graph g, String edgeschemaid) {
		if(initialize) {
			this.initialize();
		}
		SchemaType type = g.getSchemaType(edgeschemaid);

		// Return existing edges matching some criterion
		if(!type.equals(SchemaType.UNDIRECTED)) {
			throw new UnsupportedTypeException("Unsupported edge type: "+edgeschemaid+" is "+type);
		}
		
		// Check to see if the feature for the bin value is specified
		checkForBinFeature(g, nodeschemaid);
		return new SortIterator(g, edgeschemaid, windowLength);
	}
	
	public void addAllLinks(Graph g, String edgeschemaid) {
		PLGUtils.addAllLinks(g, edgeschemaid, this.getLinksIteratively(g, edgeschemaid));
	}

	public void addAllLinks(Graph g, String edgeschemaid, String existfeature,
			boolean setasnotexist) {
		PLGUtils.addAllLinks(g, edgeschemaid, existfeature,
				this.getLinksIteratively(g, edgeschemaid), setasnotexist);
	}

	private void checkForBinFeature(Graph g, String schemaid) {
		if(this.sortfid == null){
			return;
		}

		Schema schema = g.getSchema(schemaid);
		if(!schema.hasFeature(this.sortfid)) {
			throw new ConfigurationException("Bin value not defined for schema: "
					+this.sortfid+" from "+schemaid);
		}
	}
	
	
	private void initialize() {
		initialize = false;
		
		// Get sorting feature
		this.sortfid = this.getStringParameter("sortfid");
		
		// Allow selflinks
		this.allowselflinks = this.getYesNoParameter("allowselflinks", "no");
		
		// Allow allowduplinks
		this.allowduplinks = this.getYesNoParameter("allowduplinks", "no");
		
		// Get node schema id
		nodeschemaid = this.getStringParameter("nodeschemaid");
		
		// Set window
		windowLength = this.getIntegerParameter("windowlength", 10);
	}
	
	private class SortIterator extends BaseIterator<Edge> {
		private Iterator<Node> nitr;
		private Queue<Node> queue = new LinkedList<Node>();
		private int window;
		private Node currentNode;
		private Node neighbor;
		private Iterator<Node> queueIterator;
		private Graph graph;
		private String edgeschemaid;
		
		public SortIterator(Graph g, String edgeschemaid, int windowLength) {
			window = windowLength;
			this.edgeschemaid = edgeschemaid;
			graph = g;
			SortedMap<String,Node> nodes = new TreeMap<String,Node>();
			
			int uniqueTag = 0;
			Iterator<Node> itr = g.getNodes();
			while (itr.hasNext()) {
				Node n = itr.next();
				nodes.put(n.getFeatureValue(sortfid).getStringValue() + uniqueTag, n);
				uniqueTag++;
			}
						
			currentNode = nodes.get(nodes.firstKey());
			
			nitr = nodes.values().iterator();
			
			while (queue.size() < window) 
				queue.add(nitr.next());
			
			queueIterator = queue.iterator();
		}

		@Override
		public Edge getNext() {
			while (true) {
				if (!queueIterator.hasNext()) {
					queue.remove();
					if (nitr.hasNext()) 
						queue.add(nitr.next());

					if (queue.isEmpty())
						return null;
					queueIterator = queue.iterator();
					currentNode = queue.peek();
				}
				
				neighbor = queueIterator.next();
				
				boolean valid = true;
				if(!allowselflinks && neighbor.equals(currentNode)) {
					valid = false;
				} else if(!allowduplinks && neighbor.isAdjacent(currentNode, edgeschemaid)) {
					valid = false;
				}
				if (valid)
					return graph.addUndirectedEdge(GraphItemID.generateGraphItemID(graph, edgeschemaid),
							currentNode, neighbor);
			}
		}
	}
}
