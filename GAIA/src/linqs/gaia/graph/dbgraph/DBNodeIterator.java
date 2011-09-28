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
