package linqs.gaia.graph.test;

import linqs.gaia.graph.Graph;
import linqs.gaia.graph.dbgraph.DBGraph;
import linqs.gaia.identifiable.GraphID;

public class DBGraphTestCase extends BaseGraphTestCase {
	@Override
	protected Graph getGraph(GraphID gid) {
		DBGraph g = new DBGraph(gid);
		
		return g;
	}
}
