package linqs.gaia.graph.dbgraph;

import java.sql.ResultSet;
import java.sql.SQLException;

import linqs.gaia.graph.Node;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.BaseIterator;

public class DBNodeIterator extends BaseIterator<Node> {
	protected ResultSet rs = null;
	private DBGraph g = null;
	private String sql = null;
	
	/**
	 * The sql statement must return
	 * the following columns: dbid, sid, oid
	 * 
	 * @param g Graph
	 * @param sql SQL statement to use
	 */
	public DBNodeIterator(DBGraph g, String sql) {
		this.rs = g.queryDatabase(sql);
		this.g = g;
		this.sql = sql;
	}
	
	public void disconnect() {
		try {
			this.rs.close();
			this.rs.getStatement().close();
		} catch (SQLException e) {
			// Do nothing
		}
	}

	@Override
	public Node getNext() {
		Node n = null;
		
		try {
			if(rs.next()) {
				GraphItemID gid = new GraphItemID(g.getID(), rs.getString("sid"), rs.getString("oid"));
				n = new DBNode(g, gid, rs.getInt("dbid"));
			} else {
				rs.close();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sql, e);
		}
		
		return n;
	}
}
