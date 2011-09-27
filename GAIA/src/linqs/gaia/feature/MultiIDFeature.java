package linqs.gaia.feature;

/**
 * Multiple ID feature interface for features which return
 * a list of ID objects.  The ID objects can be used
 * to refer to another identifiable object (i.e., graphs, nodes, edges,
 * graph items in the same or across graphs).
 * For example, a node created from "merging" nodes
 * in entity resolution can have a feature which lists
 * all the IDs of the nodes it was merged from.
 * <p>
 * MultiIDFeature features all return MultiIDValue objects.
 * 
 * @see linqs.gaia.feature.values.MultiIDValue
 * 
 * @author namatag
 *
 */
public interface MultiIDFeature extends Feature {
	
}
