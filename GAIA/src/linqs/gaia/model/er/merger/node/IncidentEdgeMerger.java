package linqs.gaia.model.er.merger.node;

import java.io.Serializable;

import linqs.gaia.graph.Node;
import linqs.gaia.model.er.merger.Merger;

/**
 * Abstract class for defining how to merge the specified nodes.
 * 
 * @author namatag
 *
 */
public abstract class IncidentEdgeMerger extends Merger<Node> implements Serializable {
	private static final long serialVersionUID = 1L;
}
