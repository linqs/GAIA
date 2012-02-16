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
package linqs.gaia.graph.datagraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.InvalidOperationException;
import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphItemUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.graph.UndirectedEdge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.IteratorUtils;

public class DGUndirected extends DGEdge implements UndirectedEdge {
	private static final long serialVersionUID = 1L;
	
	/** Stores the references for this undirected edge  */
	private Set<Node> undirected;
	
	public DGUndirected(DataGraph graph, GraphItemID id) {
		super(graph, id);
		this.undirected = Collections.synchronizedSet(new HashSet<Node>(2));
	}

	@Override
	protected synchronized void nodeRemovedNotification(Node n) {
		// Note: Assumes edge validity checking is being done
		// in the node removal function.
		this.undirected.remove(n);
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets() {
		Set<GraphItem> connected = new HashSet<GraphItem>(this.undirected);
		return connected;
	}

	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID) {
		if(!this.getGraph().hasSchema(schemaID)) {
			throw new InvalidOperationException("No graph items defined with schema: "+schemaID);
		}
		
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.getSchemaID().equals(schemaID)){
				connected.add(n);
			}
		}
		
		return connected;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(GraphItem adjacent) {
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.isIncident(adjacent)){
				connected.add(n);
			}
		}
		
		return connected;
	}
	
	@Override
	protected Set<GraphItem> getIncidentGraphItemSets(String schemaID, GraphItem adjacent) {
		Set<GraphItem> connected = new HashSet<GraphItem>();
		for(Node n:this.undirected){
			if(n.getSchemaID().equals(schemaID) && n.isIncident(adjacent)){
				connected.add(n);
			}
		}
		
		return connected;
	}

	public synchronized void addNode(Node n) {
		if(!this.getGraph().hasNode(n.getID())) {
			throw new InvalidStateException("Node not a part of this graph: "+n
					+" not in graph "+this.getGraph());
		}
		
		if(this.isIncident(n)) {
			throw new InvalidOperationException("Node already part of the undirected edge: "
						+n+" in edge "+this);
		}
		
		this.undirected.add(n);
		((DGNode) n).edgeAddedNotification(this, null);
	}

	public Iterator<Node> getAllNodes() {
		return this.undirected.iterator();
	}

	public Iterator<Node> getAllNodes(String schemaID) {
		Collection<Node> allrefs = new HashSet<Node>();
		
		for(Node n: this.undirected){
			if(n.getSchemaID().equals(schemaID)){
				allrefs.add(n);
			}
			
		}
		
		return allrefs.iterator();
	}

	public boolean isIncident(Node n) {
		return this.undirected.contains(n);
	}

	public synchronized void removeNode(Node n) {
		((DGNode) n).edgeRemovedNotification(this);
		this.undirected.remove(n);
		
		checkValidity("Removing node resulted in invalid edge: Removing "
				+GraphItemUtils.getNodeIDString(n)+" invalidated "+this);
	}

	public int numNodes() {
		return this.undirected.size();
	}
	
	/**
	 * Check validity of the edge
	 */
	@Override
	protected void checkValidity(String message) {
		if(this.numNodes()==0){
			throw new InvalidStateException(message);
		}
	}

	@Override
	protected boolean isValid() {
		if(this.numNodes()<=0) {
			return false;
		}
		
		return true;
	}

	public boolean hasSameNodes(Edge e) {
		if(!(e instanceof UndirectedEdge)) {
			throw new InvalidStateException("Edge is not undirected: "+e
					+" of type "+e.getClass().getCanonicalName());
		}
		
		Set<Node> nodes = new HashSet<Node>(IteratorUtils.iterator2nodelist(e.getAllNodes()));
		return this.undirected.equals(nodes);
	}
}
