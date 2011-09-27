package linqs.gaia.graph.dbgraph;

import java.sql.ResultSet;
import java.sql.SQLException;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.GraphItem;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.BaseIterator;

public class DBGraphItemIterator extends BaseIterator<GraphItem> {
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
	public DBGraphItemIterator(DBGraph g, String sql) {
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
	public GraphItem getNext() {
		GraphItem gi = null;
		
		try {
			if(rs.next()) {
				String sid = rs.getString("sid");
				SchemaType stype = g.getSchemaType(sid);
				GraphItemID gid = new GraphItemID(g.getID(), sid, rs.getString("oid"));
				
				if(stype.equals(SchemaType.NODE)) {
					gi = new DBNode(g, gid, rs.getInt("dbid"));
				} else if(stype.equals(SchemaType.DIRECTED)) {
					gi = new DBDirectedEdge(g, gid, rs.getInt("dbid"));
				} else if(stype.equals(SchemaType.UNDIRECTED)) {
					gi = new DBUndirectedEdge(g, gid, rs.getInt("dbid"));
				} else {
					throw new InvalidStateException("Unsupported edge schema type: "+stype);
				}
			} else {
				rs.close();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sql, e);
		}
		
		return gi;
	}
}
