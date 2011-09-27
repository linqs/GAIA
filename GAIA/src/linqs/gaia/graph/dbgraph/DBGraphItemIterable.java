package linqs.gaia.graph.dbgraph;

import java.util.Iterator;

import linqs.gaia.graph.GraphItem;

public class DBGraphItemIterable implements Iterable<GraphItem> {
	public String sql = null;
	public DBGraph g = null;
	
	public DBGraphItemIterable(DBGraph g, String sql) {
		this.g = g;
		this.sql = sql;
	}

	public Iterator<GraphItem> iterator() {
		return new DBGraphItemIterator(this.g, this.sql);
	}

}
