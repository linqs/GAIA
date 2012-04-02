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
package linqs.gaia.model.er;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.MultiIDFeature;
import linqs.gaia.feature.explicit.ExplicitMultiID;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.feature.values.CategValue;
import linqs.gaia.feature.values.FeatureValue;
import linqs.gaia.feature.values.MultiIDValue;
import linqs.gaia.graph.Edge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.GraphUtils;
import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;
import linqs.gaia.model.lp.LinkPredictor;
import linqs.gaia.util.IteratorUtils;
import linqs.gaia.util.KeyedList;
import linqs.gaia.util.SimplePair;
import linqs.gaia.util.UnmodifiableSet;

/**
 * General utilities for use with many entity resolution models
 * 
 * @author namatag
 *
 */
public class ERUtils {
	/**
	 * Create entity nodes for all references.
	 * A "refers-to" directed edge is added from each reference node
	 * to the corresponding entity node.
	 * 
	 * @param refgraph Graph where reference nodes are defined
	 * @param entitygraph Graph where entity nodes are defined
	 * @param corefsid Schema ID of "co-reference" edges
	 * @param coreffid Feature ID of "co-reference" edge existence feature
	 * @param entitysid Schema ID of entity nodes
	 * @param refsid Schema ID of reference nodes
	 * @param referstosid Schema ID of directed "refers-to" edges.  If null, it is not added.
	 * @param reffeatureid Feature ID of reference {@link MultiIDFeature} which, for an entity,
	 * will contain the IDs of that entities references.  If null, it is not added.
	 * @param removecoref If true, remove all "co-reference" edges
	 * @param removeschema If true, remove the schema for "co-reference" edges
	 */
	public static void addEntityNodesFromCoRef(Graph refgraph, Graph entitygraph,
			String corefsid, String coreffid, String entitysid, String refsid, String referstosid,
			String reffeatureid, boolean removecoref, boolean removeschema) {
		// Assume transitivity to get sets of entities
		List<Set<Node>> entitysets = null;
		if(coreffid==null){
			entitysets = getTransitiveEntity(refgraph, refsid, corefsid);
		} else {
			entitysets = getTransitiveEntity(refgraph, refsid, corefsid, coreffid);
		}
		
		// Remove the co-reference edges
		if(removecoref) {
			refgraph.removeAllGraphItems(corefsid);
		}
		
		// Remove the co-reference edge schema
		if(removeschema) {
			refgraph.removeSchema(corefsid);
		}
		
		// Create a schema for the specified entity type
		if(!entitygraph.hasSchema(entitysid)) {
			entitygraph.addSchema(entitysid, new Schema(SchemaType.NODE));
		}
		
		if(reffeatureid!=null) {
			Schema eschema = entitygraph.getSchema(entitysid);
			if(!eschema.hasFeature(reffeatureid)){
				eschema.addFeature(reffeatureid, new ExplicitMultiID());
				entitygraph.updateSchema(entitysid, eschema);
			}
		}
		
		// Create a schema for the same entity type
		if(referstosid!= null && !entitygraph.hasSchema(referstosid)) {
			entitygraph.addSchema(referstosid, new Schema(SchemaType.DIRECTED));
		}
		
		for(Set<Node> eset:entitysets) {
			// Create a node for each set
			Node entityn = entitygraph.addNode(GraphItemID.generateGraphItemID(entitygraph, entitysid));
			
			// Add a "refers-to" edge between an entity and its reference
			if(referstosid!= null) {
				for(Node refn:eset) {
					entitygraph.addDirectedEdge(GraphItemID.generateGraphItemID(entitygraph, referstosid),
							refn, entityn);
				}
			}
			
			// Add references to the references multi id feature
			if(reffeatureid!=null) {
				Set<ID> refids = new HashSet<ID>();
				for(Node refn:eset) {
					refids.add(refn.getID());
				}
				
				entityn.setFeatureValue(reffeatureid, new MultiIDValue(refids));
			}
		}
		
		if(Log.SHOWDEBUG) {
			Log.DEBUG("Graph after adding entity nodes: "+GraphUtils.getSimpleGraphOverview(entitygraph));
		}
	}
	
	/**
	 * Create entity nodes for all references and remove the co-referent edges.
	 * A "refers-to" directed edge is added from each reference node
	 * to the corresponding entity node.
	 * 
	 * @param refgraph Graph where reference nodes are defined
	 * @param entitygraph Graph where entity nodes are defined
	 * @param corefsid Schema ID of "co-reference" edges
	 * @param entitysid Schema ID of entity nodes
	 * @param refsid Schema ID of reference nodes
	 * @param referstosid Schema ID of directed "refers-to" edges.  If null, it is not added.
	 * @param reffeatureid Feature ID of reference {@link MultiIDFeature} which, for an entity,
	 * will contain the IDs of that entities references.  If null, it is not added.
	 * @param removeschema If true, remove the schema for "co-reference" edges
	 */
	public static void addEntityNodesFromCoRef(Graph refgraph, Graph entitygraph,
			String corefsid, String entitysid, String refsid, String referstosid,
			String reffeatureid, boolean removeschema) {
		ERUtils.addEntityNodesFromCoRef(refgraph, entitygraph,
				corefsid, null, entitysid,
				refsid, referstosid,
				reffeatureid, true, removeschema);
	}
	
	public static void addCoRefFromEntityFeature(Graph g, String corefsid,
			String refsid, String entityfid) {
		if(!g.hasSchema(corefsid)) {
			g.addSchema(corefsid, new Schema(SchemaType.UNDIRECTED));
		}
		
		// Go over all references
		Iterator<Node> nitr = g.getNodes(refsid);
		KeyedList<ID,Node> eid2references = new KeyedList<ID,Node>();
		while(nitr.hasNext()) {
			Node refn = nitr.next();
			if(!refn.hasFeatureValue(entityfid)) {
				continue;
			}
			
			MultiIDValue eids = (MultiIDValue) refn.getFeatureValue(entityfid);
			UnmodifiableSet<ID> eset = eids.getIDs();
			for(ID eid:eset) {
				eid2references.addItem(eid, refn);
			}
		}
		
		// Add co-ref edges between reference with at least
		// one entity id in common
		Set<ID> keys = eid2references.getKeys();
		for(ID key:keys) {
			List<Node> refs = eid2references.getList(key);
			int numrefs = refs.size();
			for(int i=0; i<numrefs; i++) {
				for(int j=i+1; j<numrefs; j++) {
					// Ensure that a co-referent edge is not added twice
					if(refs.get(i).isAdjacent(refs.get(j),corefsid)) {
						continue;
					}
					
					g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, corefsid, ""),
							refs.get(i), refs.get(j));
				}
			}
		}
	}
	
	/**
	 * Add a directed refers-to edge from a reference to its entity,
	 * as defined by a {@link MultiIDFeature} containing the ID of the entity
	 * of each reference.  If a reference node has no value specified
	 * for the given entity node, a refers-to edge is not added for that reference.
	 * 
	 * @param g Graph
	 * @param referstosid Schema ID of refers-to edges
	 * @param refsid SchemaID of reference nodes
	 * @param entityfid Feature ID of the {@link MultiIDFeature} whose value defines a references entity
	 */
	public static void addRefersToFromEntityFeature(Graph g, String referstosid,
			String refsid, String entityfid) {
		g.addSchema(referstosid, new Schema(SchemaType.DIRECTED));
		Iterator<Node> nitr = g.getNodes(refsid);
		while(nitr.hasNext()) {
			Node refn = nitr.next();
			if(!refn.hasFeatureValue(entityfid)) {
				continue;
			}
			
			MultiIDValue eids = (MultiIDValue) refn.getFeatureValue(entityfid);
			UnmodifiableSet<ID> eset = eids.getIDs();
			for(ID id:eset) {
				Node entityn = (Node) g.getEquivalentGraphItem((GraphItemID) id);
				g.addDirectedEdge(
						GraphItemID.generateGraphItemID(g, referstosid, ""),
						refn, entityn);
			}
		}
	}
	
	/**
	 * Assuming transitivity of the ER edge,
	 * this returns a list of sets where each set contains
	 * nodes which are transitively the same entity.
	 * Nodes without "co-reference" edges are returned as sets
	 * of size one.
	 * <p>
	 * @param g Graph to get entities for
	 * @param refschemaid Schema ID of the reference nodes
	 * @param corefsid Schema ID of the "co-reference" edges
	 * @return List of sets of nodes where nodes are in the same
	 * set if they are the same entity
	 */
	public static List<Set<Node>> getTransitiveEntity(Graph g, String refschemaid, String corefsid) {
		List<Set<Node>> entities = new ArrayList<Set<Node>>();
		Iterator<Edge> eitr = g.getEdges(corefsid);
		
		Set<Node> entityprocessed = new HashSet<Node>();
		while(eitr.hasNext()) {
			Set<Node> sameentity = new HashSet<Node>();
			Set<GraphItem> unprocessed = new HashSet<GraphItem>();
			
			// Iterate over the same entity edges
			Edge e = eitr.next();
			@SuppressWarnings("unchecked")
			List<GraphItem> iterator2list = IteratorUtils.iterator2list(e.getAllNodes());
			
			// Don't reprocess if node is already listed as part of another entity
			if(entityprocessed.contains(iterator2list.get(0))) {
				continue;
			}
			
			// Do a depth first search from the nodes
			// add all nodes connected by a "co-reference" edge
			unprocessed.addAll(iterator2list);
			while(!unprocessed.isEmpty()) {
				Node n = (Node) unprocessed.iterator().next();
				if(!n.getSchemaID().equals(refschemaid)) {
					throw new InvalidStateException("Merging node with wrong reference schema id: "
							+n.getSchemaID()+" expecting "+refschemaid);
					
				}
				
				sameentity.add(n);
				unprocessed.remove(n);
				
				// Go through neighbors
				Iterator<GraphItem> nitr = n.getAdjacentGraphItems(corefsid);
				while(nitr.hasNext()) {
					// Add neighbors who aren't already in the list or it was processed
					Node currn = (Node) nitr.next();
					if(!currn.getSchemaID().equals(refschemaid)) {
						throw new InvalidStateException("Merging node with wrong reference schema id: "
								+currn.getSchemaID()+" expecting "+refschemaid);
						
					}
					
					if(!sameentity.contains(currn) && !unprocessed.contains(currn)) {
						unprocessed.add(currn);
					}
				}
			}
			
			// Store all the nodes who we already have entity information for
			entityprocessed.addAll(sameentity);
			
			entities.add(sameentity);
		}
		
		// Create entity sets for all nodes which do not have a "co-reference" link
		Iterator<Node> nitr = g.getNodes(refschemaid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			if(n.numIncidentGraphItems(corefsid)!=0) {
				continue;
			}
			
			Set<Node> sameentity = new HashSet<Node>();
			sameentity.add(n);
			entities.add(sameentity);
		}
		
		
		return entities;
	}
	
	/**
	 * Assuming transitivity of the ER edge,
	 * this returns a list of sets where each set contains
	 * nodes which are transitively the same entity.
	 * Nodes without "co-reference" edges are returned as sets
	 * of size one.
	 * <p>
	 * @param g Graph to get entities for
	 * @param refschemaid Schema ID of the reference nodes
	 * @param corefsid Schema ID of the "co-reference" edges
	 * @param coreffid Feature ID of "co-reference" edge existence feature
	 * @return List of sets of nodes where nodes are in the same
	 * set if they are the same entity
	 */
	public static List<Set<Node>> getTransitiveEntity(Graph g, String refschemaid,
			String corefsid, String coreffid) {
		List<Set<Node>> entities = new ArrayList<Set<Node>>();
		Iterator<Edge> eitr = g.getEdges(corefsid);
		
		Set<Node> entityprocessed = new HashSet<Node>();
		while(eitr.hasNext()) {
			Set<Node> sameentity = new HashSet<Node>();
			Set<GraphItem> unprocessed = new HashSet<GraphItem>();
			
			// Iterate over the same entity edges
			Edge e = eitr.next();
			FeatureValue fv = e.getFeatureValue(coreffid);
			if(!(fv instanceof CategValue)) {
				throw new InvalidStateException("Categorical value expected.  Received: "
						+fv+" of type "+fv.getClass().getCanonicalName());
			}
			
			if(!((CategValue) fv).getCategory().equalsIgnoreCase(LinkPredictor.EXIST)) {
				continue;
			}
			
			@SuppressWarnings("unchecked")
			List<GraphItem> iterator2list = IteratorUtils.iterator2list(e.getAllNodes());
			
			// Don't reprocess if node is already listed as part of another entity
			if(entityprocessed.contains(iterator2list.get(0))) {
				continue;
			}
			
			// Do a depth first search from the nodes
			// add all nodes connected by a "co-reference" edge
			unprocessed.addAll(iterator2list);
			while(!unprocessed.isEmpty()) {
				Node n = (Node) unprocessed.iterator().next();
				if(!n.getSchemaID().equals(refschemaid)) {
					throw new InvalidStateException("Merging node with wrong reference schema id: "
							+n.getSchemaID()+" expecting "+refschemaid);
					
				}
				
				sameentity.add(n);
				unprocessed.remove(n);
				
				Iterator<Edge> curreitr = n.getAllEdges(corefsid);
				while(curreitr.hasNext()) {
					Edge curre = curreitr.next();
					FeatureValue currfv = curre.getFeatureValue(coreffid);
					if(!(currfv instanceof CategValue)) {
						throw new InvalidStateException("Categorical value expected.  Received: "
								+currfv+" of type "+currfv.getClass().getCanonicalName());
					}
					
					if(!((CategValue) currfv).getCategory().equalsIgnoreCase(LinkPredictor.EXIST)) {
						continue;
					}
					
					// Go through neighbors
					Iterator<Node> nitr = curre.getAllNodes(refschemaid);
					while(nitr.hasNext()) {
						// Add neighbors who aren't already in the list or it was processed
						Node currn = nitr.next();
						
						if(!sameentity.contains(currn) && !unprocessed.contains(currn)) {
							unprocessed.add(currn);
						}
					}
				}
			}
			
			// Store all the nodes who we already have entity information for
			entityprocessed.addAll(sameentity);
			
			entities.add(sameentity);
		}
		
		// Create entity sets for all nodes which do not have a "co-reference" link
		Iterator<Node> nitr = g.getNodes(refschemaid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			Iterator<Edge> curreitr = n.getAllEdges(corefsid);
			boolean hascoref = false;
			while(curreitr.hasNext()) {
				Edge curre = curreitr.next();
				FeatureValue currfv = curre.getFeatureValue(coreffid);
				if(!(currfv instanceof CategValue)) {
					throw new InvalidStateException("Categorical value expected.  Received: "
							+currfv+" of type "+currfv.getClass().getCanonicalName());
				}
				
				if(((CategValue) currfv).getCategory().equalsIgnoreCase(LinkPredictor.EXIST)) {
					hascoref = true;
					break;
				}
			}
			
			if(hascoref) {
				continue;
			}
			
			Set<Node> sameentity = new HashSet<Node>();
			sameentity.add(n);
			entities.add(sameentity);
		}
		
		return entities;
	}
	
	/**
	 * Create a co-reference edge between all references who have a refers to edge
	 * to the same entity.
	 * 
	 * @param g Graph
	 * @param entitysid Schema ID of entity nodes
	 * @param referstosid Schema ID of refers-to edges
	 * @param corefsid Schema ID of co-reference edges
	 */
	public static void addCoRefFromRefersToEdges(Graph g, String entitysid,
			String referstosid, String corefsid) {
		ERUtils.addCoRefFromRefersToEdges(g, entitysid, referstosid, corefsid, null);
	}
	
	/**
	 * Create a co-reference edge between all references who have a refers to edge
	 * to the same entity.
	 * 
	 * @param g Graph
	 * @param entitysid Schema ID of entity nodes
	 * @param referstosid Schema ID of refers-to edges
	 * @param corefsid Schema ID of co-reference edges
	 * @param coreffid Feature ID of existence feature for coref.
	 * All added edges have the value of existing.
	 */
	public static void addCoRefFromRefersToEdges(Graph g, String entitysid,
			String referstosid, String corefsid, String coreffid) {
		// Add reference schema, if not available
		if(!g.hasSchema(corefsid)) {
			Schema schema = new Schema(SchemaType.UNDIRECTED);
			g.addSchema(corefsid, schema);
		}
		
		// Add existence feature, if not available
		if(coreffid!=null) {
			Schema schema = g.getSchema(corefsid);
			if(!schema.hasFeature(coreffid)) {
				schema.addFeature(coreffid, LinkPredictor.EXISTENCEFEATURE);
				g.updateSchema(corefsid, schema);
			}
		}
		
		// Iterate over all entities
		Iterator<Node> nitr = g.getNodes(entitysid);
		while(nitr.hasNext()) {
			Node n = nitr.next();
			
			// Get references for all entities
			Iterator<GraphItem> ritr = n.getAdjacentGraphItems(referstosid);
			List<Node> refs = IteratorUtils.iterator2nodelist(ritr);
			
			// Add a corefsid edge between all references
			for(int i=0; i<refs.size(); i++) {
				for(int j=i+1; j<refs.size(); j++) {
					Edge e = g.addUndirectedEdge(GraphItemID.generateGraphItemID(g, corefsid),
							refs.get(i), refs.get(j));
					
					if(coreffid!=null) {
						e.setFeatureValue(coreffid, LinkPredictor.EXISTVALUE);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the references of an entity.
	 * Uses the existence of incoming "refer-to" edges
	 * from the references to this entity.
	 * If the entity has no "refers-to" edges,
	 * it was not an ambiguous entity.
	 * 
	 * @param n Entity node
	 * @param referstosid Schema ID of refers-to edge.  If null, just return n.
	 * @return Set of reference nodes
	 */
	public static Set<Node> getRefersToReferences(Node n, String referstosid) {
		Set<Node> refs = new HashSet<Node>();
		
		if(referstosid!=null) {
			Iterator<Node> itr = n.getAdjacentSources(referstosid);
			while(itr.hasNext()) {
				refs.add(itr.next());
			}
		}
		
		return refs;
	}
	
	/**
	 * Gets the references which are co-referent to the given node (excluding itself).
	 * Uses the existence of incoming "refer-to" edges
	 * from the references to some entity.
	 * If the reference has no "refers-to" edges,
	 * it was not an ambiguous entity and empty set is returned.
	 * 
	 * @param n Entity node
	 * @param referstosid Schema ID of refers-to edge.  If null, just return n.
	 * @return Set of reference nodes
	 */
	public static Set<Node> getRefersToCoReferences(Node n, String referstosid) {
		Node entity = getRefersToEntity(n, referstosid);
		Set<Node> refs = getRefersToReferences(entity, referstosid);
		refs.remove(n);
		
		return refs;
	}
	
	/**
	 * Get the node that this reference refers-to.
	 * Uses the existence of an outgoing "refers-to" edge
	 * from this reference to its entity.  If the reference
	 * has no "refers-to" edges, it is not a reference.
	 * An exception is thrown if there is more than one outgoing
	 * refers-to edge from this reference.
	 * 
	 * @param n Reference node
	 * @param referstosid Schema ID of refers-to edge.  If null, just return n.
	 * @return Entity
	 */
	public static Node getRefersToEntity(Node n, String referstosid) {
		if(n==null) {
			throw new InvalidStateException("Reference node is null");
		}
		
		Node entity = null;
		if(referstosid==null) {
			// Node is not a reference so just return the node itself
			return n;
		}
		
		int numentities = n.numAdjacentTargets(referstosid);
		
		if(numentities==0) {
			// Node is not a reference so just return the node itself
			entity = n;
		} else if(numentities==1) {
			// Node is a reference, return the single entity
			entity = n.getAdjacentTargets(referstosid).next();
		} else {
			// Only support one entity per reference
			throw new InvalidStateException("Only one entity can be defined per reference: "
					+numentities);
		}
		
		return entity;
	}
	
	/**
	 * Create sets of items where an item is in the same
	 * set as all of the items where it is paired
	 * with in the specified list of pairs.
	 * This essentially performs transitive closure
	 * over the pairs of nodes in the list of pairs.
	 * 
	 * @param pairs List of SimplePair objects
	 * @return Set of sets where the contained sets correspond to
	 * nodes in the same set due to transitive closure over the pairs
	 */
	public static Set<Set<Object>> getPairwiseCoreferentSets(List<SimplePair<Object,Object>> pairs) {
		Map<Object,Set<Object>> obj2set = new ConcurrentHashMap<Object,Set<Object>>();
		
		for(SimplePair<Object,Object> pair:pairs) {
			Object o1 = pair.getFirst();
			Object o2 = pair.getSecond();
			
			if(obj2set.containsKey(o1) && obj2set.containsKey(o2)) {
				// If both items are already in sets
				Set<Object> o1set = obj2set.get(o1);
				Set<Object> o2set = obj2set.get(o2);
				
				// Note: If they're already in the same set, don't need to do anything.
				if(!o1set.equals(o2set)) {
					Set<Object> unionset = new HashSet<Object>(o1set.size()+o2set.size());
					unionset.addAll(o1set);
					unionset.addAll(o2set);
					
					// Update the set to the union set,
					// including for o1 and o2
					for(Object o:unionset) {
						obj2set.put(o, unionset);
					}
				}
			} else if(obj2set.containsKey(o1)) {
				// If only the first is in a set
				Set<Object> o1set = obj2set.get(o1);
				
				// Add o2 to o1's set and then add o2 to map
				o1set.add(o2);
				obj2set.put(o2, o1set);
			} else if(obj2set.containsKey(o2)) {
				// If only the second is in a set
				Set<Object> o2set = obj2set.get(o2);
				
				// Add o1 to o2's set and then add o1 to map
				o2set.add(o1);
				obj2set.put(o1, o2set);
			} else {
				// If neither is in a set
				
				// Add both to new set
				Set<Object> unionset = new HashSet<Object>(2);
				unionset.add(o1);
				unionset.add(o2);
				
				// Add o1 and o2 to map
				obj2set.put(o1, unionset);
				obj2set.put(o2, unionset);
			}
		}
		
		Set<Set<Object>> finalsets = new HashSet<Set<Object>>(obj2set.values());

		return finalsets;
	}
}
