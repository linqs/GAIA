/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.graph.dbgraph;

import java.sql.ResultSet;
import java.sql.SQLException;

import linqs.gaia.exception.InvalidStateException;
import linqs.gaia.feature.schema.SchemaType;
import linqs.gaia.graph.Edge;
import linqs.gaia.identifiable.GraphItemID;
import linqs.gaia.util.BaseIterator;

public class DBEdgeIterator extends BaseIterator<Edge> {
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
	public DBEdgeIterator(DBGraph g, String sql) {
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
	public Edge getNext() {
		Edge edge = null;
		
		try {
			if(rs.next()) {
				String sid = rs.getString("sid");
				SchemaType stype = g.getSchemaType(sid);
				GraphItemID gid = new GraphItemID(g.getID(), sid, rs.getString("oid"));
				
				if(stype.equals(SchemaType.DIRECTED)) {
					edge = new DBDirectedEdge(g, gid, rs.getInt("dbid"));
				} else if(stype.equals(SchemaType.UNDIRECTED)) {
					edge = new DBUndirectedEdge(g, gid, rs.getInt("dbid"));
				} else {
					throw new InvalidStateException("Unsupported edge schema type: "+stype);
				}
			} else {
				rs.close();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sql, e);
		}
		
		return edge;
	}
}
