package linqs.gaia.graph.generator;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.graph.Graph;

/**
 * Base interface for graph generation models i.e., Preferential Attachment, Forest Fire
 * 
 * @author namatag
 *
 */
public interface Generator extends Configurable {
	/**
	 * Generate a graph using this model
	 * 
	 * @return Graph
	 */
	Graph generateGraph();
	
	/**
	 * Generate a graph using this model
	 * 
	 * @param objid Object ID to use for graph
	 * @return Graph
	 */
	Graph generateGraph(String objid);
}
