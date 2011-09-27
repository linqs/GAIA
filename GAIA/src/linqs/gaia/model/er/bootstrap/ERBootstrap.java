package linqs.gaia.model.er.bootstrap;

import java.io.Serializable;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.GraphItem;

/**
 * Bootstrap interface for use with bootstrapping
 * entity resolution algorithms
 * 
 * @author namatag
 *
 */
public interface ERBootstrap extends Configurable, Serializable {
	/**
	 * Return true if the two graph items are the same
	 * entity and false otherwise.
	 * 
	 * @param gi1 First graph item
	 * @param gi2 Second graph item
	 * @return True if the graph items refer to the same entity, false otherwise
	 */
	boolean isSameEntity(GraphItem gi1, GraphItem gi2);
}
