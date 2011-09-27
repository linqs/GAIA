package linqs.gaia.graph.dbgraph;

import java.util.Iterator;

import linqs.gaia.graph.Edge;

public class DBEdgeIterable implements Iterable<Edge> {
	public String sql = null;
	public DBGraph g = null;
	
	public DBEdgeIterable(DBGraph g, String sql) {
		this.g = g;
		this.sql = sql;
	}

	public Iterator<Edge> iterator() {
		return new DBEdgeIterator(this.g, this.sql);
	}

}
