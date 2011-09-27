package linqs.gaia.graph.test;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.datagraph.DataGraph;
import linqs.gaia.identifiable.GraphID;

public class DataGraphTestCase extends BaseGraphTestCase {
	@Override
	protected Graph getGraph(GraphID gid) {
		return new DataGraph(gid);
	}
}
