package linqs.gaia.graph.dbgraph;

import java.util.Iterator;

import linqs.gaia.graph.Node;

public class DBNodeIterable implements Iterable<Node> {
	public String sql = null;
	public DBGraph g = null;
	
	public DBNodeIterable(DBGraph g, String sql) {
		this.g = g;
		this.sql = sql;
	}

	public Iterator<Node> iterator() {
		return new DBNodeIterator(this.g, this.sql);
	}

}
