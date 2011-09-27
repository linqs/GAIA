package linqs.gaia.experiment.groovy.feature;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import linqs.gaia.exception.UnsupportedTypeException;
import linqs.gaia.feature.Feature;
import linqs.gaia.feature.schema.Schema;
import linqs.gaia.graph.DirectedEdge;
import linqs.gaia.graph.Graph;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.graph.Node;

/**
 * Collection of static functions to simplify feature construction with Groovy
 * 
 * @author namatag
 *
 */
public class FeatureConstruction {
	/**
	 * Create a numeric derived feature where the value of this
	 * numeric feature is of type double and is returned by the groovyscript.
	 * <p>
	 * Note: The static functions of {@link linqs.gaia.experiment.groovy.feature.FeatureConstruction}
	 * are automatically imported in the groovy script
	 * (i.e., "import static linqs.gaia.experiment.groovy.feature.FeatureConstruction.*"
	 * is inserted at the top of the groovy script).
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID of schema to add feature to
	 * @param featureID Feature ID of feature
	 * @param groovyscript Groovy script to return the derived value
	 * @return Feature that was added
	 */
	public static Feature ADDNUMFEATURE(Graph g, String schemaID, String featureID, String groovyscript) {
		Schema schema = g.getSchema(schemaID);
		Feature f = new GroovyNum(groovyscript);
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Create a string derived feature where the value of this
	 * numeric feature is of type double and is returned by the groovyscript.
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID of schema to add feature to
	 * @param featureID Feature ID of feature
	 * @param groovyscript Groovy script to return the derived value
	 * @return Feature that was added
	 */
	public static Feature ADDSTRING(Graph g, String schemaID, String featureID, String groovyscript) {
		Schema schema = g.getSchema(schemaID);
		Feature f = new GroovyString(groovyscript);
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Create a categorical derived feature where the value of this
	 * numeric feature is of type double and is returned by the groovyscript.
	 * 
	 * @param g Graph to add feature to
	 * @param schemaID Schema ID of schema to add feature to
	 * @param featureID Feature ID of feature
	 * @param categories Comma delimited strings of the categories of this feature
	 * @param groovyscript Groovy script to return the derived value
	 * @return Feature that was added
	 */
	public static Feature ADDCATEG(Graph g, String schemaID, String featureID,
			String categories, String groovyscript) {
		Schema schema = g.getSchema(schemaID);
		Feature f = new GroovyCateg(categories, groovyscript);
		schema.addFeature(featureID, f);
		g.updateSchema(schemaID, schema);
		
		return f;
	}
	
	/**
	 * Return the graph items incident to the given graph item
	 * (i.e., return the set of incident edges to a node
	 * or the set of incident nodes of an edge)
	 * 
	 * @param gi Graph Item to get incident graph items for
	 * @return Set of incident graph items
	 */
	public static Set<GraphItem> INCIDENT(GraphItem gi) {
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<GraphItem> itr = gi.getIncidentGraphItems();
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * If the specified item is a node, get the set of edges
	 * where this node is a source node.
	 * If the specified item is a directed edge, get the set of
	 * nodes which are the source nodes of this edge.
	 * 
	 * @param gi {@link Node} or {@link DirectedEdge}
	 * @return Set of nodes or edges incident as source
	 */
	public static Set<GraphItem> INCIDENTASSOURCE(GraphItem gi) {
		Set<GraphItem> set = new HashSet<GraphItem>();
		if(gi instanceof Node) {
			Iterator<DirectedEdge> itr = ((Node) gi).getEdgesWhereSource();
			while(itr.hasNext()) {
				set.add(itr.next());
			}
		} else if(gi instanceof DirectedEdge) {
			Iterator<Node> itr = ((DirectedEdge) gi).getSourceNodes();
			while(itr.hasNext()) {
				set.add(itr.next());
			}
		} else {
			throw new UnsupportedTypeException("Only nodes and directed edge supported.  Encountered: "
					+gi.getClass().getCanonicalName());
		}
		
		return set;
	}
	
	/**
	 * If the specified item is a node, get the set of edges
	 * where this node is a target node.
	 * If the specified item is a directed edge, get the set of
	 * nodes which are the target nodes of this edge.
	 * 
	 * @param gi {@link Node} or {@link DirectedEdge}
	 * @return Set of nodes or edges incident as target
	 */
	public static Set<GraphItem> INCIDENTASTARGET(GraphItem gi) {
		Set<GraphItem> set = new HashSet<GraphItem>();
		if(gi instanceof Node) {
			Iterator<DirectedEdge> itr = ((Node) gi).getEdgesWhereTarget();
			while(itr.hasNext()) {
				set.add(itr.next());
			}
		} else if(gi instanceof DirectedEdge) {
			Iterator<Node> itr = ((DirectedEdge) gi).getTargetNodes();
			while(itr.hasNext()) {
				set.add(itr.next());
			}
		} else {
			throw new UnsupportedTypeException("Only nodes and directed edge supported.  Encountered: "
					+gi.getClass().getCanonicalName());
		}
		
		return set;
	}
	
	/**
	 * Return the graph items incident to at least one of the given graph items
	 * (i.e., return the set of incident edges to at least one of the nodes
	 * or the set of incident nodes of at least one of the edge)
	 * 
	 * @param set Set of graph items to get incident graph items for
	 * @return Set of incident graph items
	 */
	public static Set<GraphItem> INCIDENT(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(INCIDENT(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the set of nodes or directed edges incident as source,
	 * defined in {@link #INCIDENTASSOURCE(GraphItem)},
	 * of at least one of the items in the set
	 * 
	 * @param set Set of {@link Node} or {@link DirectedEdge} objects
	 * @return Set of nodes or edges incident as source
	 */
	public static Set<GraphItem> INCIDENTASSOURCE(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(INCIDENTASSOURCE(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the set of nodes or directed edges incident as target,
	 * defined in {@link #INCIDENTASTARGET(GraphItem)},
	 * of at least one of the items in the set
	 * 
	 * @param set Set of {@link Node} or {@link DirectedEdge} objects
	 * @return Set of nodes or edges incident as target
	 */
	public static Set<GraphItem> INCIDENTASTARGET(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(INCIDENTASTARGET(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph items adjacent to the given graph item
	 * (i.e., return the set of nodes which share at least one common
	 * edge to the specified node
	 * or the set of nodes of an edge which share at least one common
	 * node to the specified edge).
	 * 
	 * @param gi Graph Item to get incident graph items for
	 * @return Set of incident graph items
	 */
	public static Set<GraphItem> ADJACENT(GraphItem gi) {
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<GraphItem> itr = gi.getAdjacentGraphItems();
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the graph items adjacent to the given graph item
	 * (i.e., return the set of nodes which share at least one common
	 * edge, with the given incident schema ID, to the specified node
	 * or the set of nodes of an edge which share at least one common
	 * node, with the given incident schema ID, to the specified edge).
	 * 
	 * @param gi Graph Item to get incident graph items for
	 * @param incidentsid Schema ID of item incident to the adjacent graph items
	 * @return Set of incident graph items
	 */
	public static Set<GraphItem> ADJACENT(GraphItem gi, String incidentsid) {
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<GraphItem> itr = gi.getAdjacentGraphItems(incidentsid);
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the nodes adjacent to the specified node
	 * where the specified node is a target for
	 * a directed edge which at least one of the return adjacent
	 * nodes is a source.
	 * 
	 * @param gi Node
	 * @return Nodes adjacent to this node as a source
	 */
	public static Set<GraphItem> ADJACENTSOURCES(GraphItem gi) {
		if(!(gi instanceof Node)) {
			throw new UnsupportedTypeException("ADJACENTASSOURCE supported only for nodes: "+gi);
		}
		
		Node n = (Node) gi;
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<Node> itr = n.getAdjacentSources();
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the nodes adjacent to the specified node
	 * where the specified node is a target for
	 * a directed edge, with the given incident schema ID,
	 * which at least one of the return adjacent
	 * nodes is a source.
	 * 
	 * @param gi Node
	 * @param incidentsid Schema ID of directed edges
	 * @return Nodes adjacent to this node as a source
	 */
	public static Set<GraphItem> ADJACENTSOURCES(GraphItem gi, String incidentsid) {
		if(!(gi instanceof Node)) {
			throw new UnsupportedTypeException("ADJACENTASSOURCE supported only for nodes: "+gi);
		}
		
		Node n = (Node) gi;
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<Node> itr = n.getAdjacentSources(incidentsid);
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the nodes adjacent to the specified node
	 * where the specified node is a source for
	 * a directed edge
	 * which at least one of the return adjacent
	 * nodes is a target.
	 * 
	 * @param gi Node
	 * @return Nodes adjacent to this node as a target
	 */
	public static Set<GraphItem> ADJACENTTARGETS(GraphItem gi) {
		if(!(gi instanceof Node)) {
			throw new UnsupportedTypeException("ADJACENTASSOURCE supported only for nodes: "+gi);
		}
		
		Node n = (Node) gi;
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<Node> itr = n.getAdjacentTargets();
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the nodes adjacent to the specified node
	 * where the specified node is a source for
	 * a directed edge, with the given incident schema ID,
	 * which at least one of the return adjacent
	 * nodes is a target.
	 * 
	 * @param gi Node
	 * @param incidentsid Schema ID of directed edges
	 * @return Nodes adjacent to this node as a target
	 */
	public static Set<GraphItem> ADJACENTTARGETS(GraphItem gi, String incidentsid) {
		if(!(gi instanceof Node)) {
			throw new UnsupportedTypeException("ADJACENTASSOURCE supported only for nodes: "+gi);
		}
		
		Node n = (Node) gi;
		Set<GraphItem> set = new HashSet<GraphItem>();
		Iterator<Node> itr = n.getAdjacentTargets(incidentsid);
		while(itr.hasNext()) {
			set.add(itr.next());
		}
		
		return set;
	}
	
	/**
	 * Return the graph {@link #ADJACENT} to at least
	 * one node in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENT} graph items for
	 * @return Set of {@link #ADJACENT} graph items for
	 */
	public static Set<GraphItem> ADJACENT(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENT(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph {@link #ADJACENT} to at least
	 * one graph item in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENT} graph items for
	 * @param incidentsid Schema ID of item incident to the adjacent graph items
	 * @return Set of {@link #ADJACENT} graph items for
	 */
	public static Set<GraphItem> ADJACENT(Set<? extends GraphItem> set, String incidentsid) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENT(gi, incidentsid));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph {@link #ADJACENTSOURCES} to at least
	 * one node in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENTSOURCES} graph items for
	 * @return Set of {@link #ADJACENTSOURCES} graph items for
	 */
	public static Set<GraphItem> ADJACENTSOURCES(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENTSOURCES(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph {@link #ADJACENTSOURCES} to at least
	 * one node in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENTSOURCES} graph items for
	 * @param incidentsid Schema ID of item incident to the adjacent graph items
	 * @return Set of {@link #ADJACENTSOURCES} graph items for
	 */
	public static Set<GraphItem> ADJACENTSOURCES(Set<? extends GraphItem> set, String incidentsid) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENTSOURCES(gi, incidentsid));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph {@link #ADJACENTTARGETS} to at least
	 * one node in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENTTARGETS} graph items for
	 * @return Set of {@link #ADJACENTTARGETS} graph items for
	 */
	public static Set<GraphItem> ADJACENTTARGETS(Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENTTARGETS(gi));
		}
		
		return newset;
	}
	
	/**
	 * Return the graph {@link #ADJACENTTARGETS} to at least
	 * one node in the specified set
	 * 
	 * @param set Set of items to get {@link #ADJACENTTARGETS} graph items for
	 * @param incidentsid Schema ID of item incident to the adjacent graph items
	 * @return Set of {@link #ADJACENTTARGETS} graph items for
	 */
	public static Set<GraphItem> ADJACENTTARGETS(Set<? extends GraphItem> set, String incidentsid) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			newset.addAll(ADJACENTTARGETS(gi, incidentsid));
		}
		
		return newset;
	}
	
	/**
	 * Return the subset of the graph items which have the given value
	 * for the specified feature
	 * 
	 * @param set Set of graph items
	 * @param featureID Feature ID of feature whose value to match
	 * @param value String representation of the feature value.
	 * Set to "?" to denote an unknown value.
	 * @return Subset of graph items which have the correct feature value
	 */
	public static Set<GraphItem> HASVALUE(Set<? extends GraphItem> set, String featureID, String value) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		boolean unknown = value.trim().equals("?");
		for(GraphItem gi:set) {
			if(unknown) {
				if(!gi.hasFeatureValue(featureID)) {
					newset.add(gi);
				}
			} else if(gi.getFeatureValue(featureID).getStringValue().equals(value)) {
				newset.add(gi);
			}
		}
		
		return newset;
	}
	
	/**
	 * Return the subset of the graph items which have the given schema ID
	 * 
	 * @param set Set of graph items
	 * @param schemaID Schema ID
	 * @return Subset of graph items which have the specified schema ID
	 */
	public static Set<GraphItem> HASSCHEMA(Set<? extends GraphItem> set, String schemaID) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		for(GraphItem gi:set) {
			if(gi.getSchemaID().equals(schemaID)) {
				newset.add(gi);
			}
		}
		
		return newset;
	}
	
	/**
	 * Return the set union of the two sets
	 * 
	 * @param set1 Set of graph items
	 * @param set2 Set of graph items
	 * @return Union of the two graph item sets
	 */
	public static Set<GraphItem> UNION(Set<? extends GraphItem> set1, Set<? extends GraphItem> set2) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		newset.addAll(set1);
		newset.addAll(set2);
		
		return newset;
	}
	
	/**
	 * Add the graph item to set
	 * 
	 * @param set Set of graph items
	 * @param gi Graph item
	 * @return Set with graph item
	 */
	public static Set<GraphItem> ADD(GraphItem gi, Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		newset.addAll(set);
		newset.add(gi);
		
		return newset;
	}
	
	/**
	 * Remove the graph item from set
	 * 
	 * @param set Set of graph items
	 * @param gi Graph item
	 * @return Set with graph item removed
	 */
	public static Set<GraphItem> REMOVE(GraphItem gi, Set<? extends GraphItem> set) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		newset.addAll(set);
		newset.remove(gi);
		
		return newset;
	}
	
	/**
	 * Return the set intersection of the two sets
	 * 
	 * @param set1 Set of graph items
	 * @param set2 Set of graph items
	 * @return Intersection of the two graph item sets
	 */
	public static Set<GraphItem> INTERSECTION(Set<? extends GraphItem> set1, Set<? extends GraphItem> set2) {
		Set<GraphItem> newset = new HashSet<GraphItem>();
		newset.addAll(set1);
		newset.retainAll(set2);
		
		return newset;
	}
	
	/**
	 * Return the set difference of the two sets
	 * 
	 * @param set1 Set of graph items
	 * @param set2 Set of graph items
	 * @return Difference of the two graph item sets
	 */
	public static Set<?> DIFFERENCE(Set<? extends GraphItem> set1, Set<? extends GraphItem> set2) {
		Set<GraphItem> newset = UNION(set1,set2);
		newset.removeAll(INTERSECTION(set1,set2));
		return newset;
	}
	
	/**
	 * Return the number of graph items in set
	 * 
	 * @param set Set of graph items
	 * @return Size of set
	 */
	public static double COUNT(Set<? extends GraphItem> set) {
		return set.size();
	}
}
