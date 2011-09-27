package linqs.gaia.model.er.merger.node;

import linqs.gaia.graph.Node;

/**
 * Do not merge edges for the specified merged item.
 * When run, no new edges will be added.
 * 
 * @author namatag
 *
 */
public class NoEdges extends IncidentEdgeMerger {
	private static final long serialVersionUID = 1L;

	@Override
	public void merge(Iterable<Node> items, Node mergeditem) {
		// Do nothing
	}

}
