package linqs.gaia.graph.dbgraph;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import linqs.gaia.configurable.BaseConfigurable;

/**
 * Abstract class created to simplify calls to the database
 * 
 * @author namatag
 *
 */
public abstract class DBHelper extends BaseConfigurable {
	
	/**
	 * Check to see if the port is available
	 * 
	 * @param port Port to check
	 * @return True if a port is available and False otherwise
	 */
	public static boolean isPortAvailable(int port) {
		try {
			ServerSocket srv = new ServerSocket(port);

			srv.close();
			srv = null;
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Get the connection for the database.
	 * 
	 * Note: This will allow you to access and directly
	 * manipulate the underlying database.
	 * While this will allow for custom operations,
	 * it may also result in advertantly corrupting the database.
	 * 
	 * @return Database Connection
	 */
	public abstract Connection getConnection();

	/**
	 * Execute the specified sql statement.
	 * 
	 * @param sqlstatement SQL statement to query
	 * @return Result set
	 */
	protected boolean executeSQL(String sqlstatement){
		boolean output = false;
		Statement stmt;

		try {
			Connection connection = this.getConnection();

			// Create the statement
			stmt = connection.createStatement();
			output = stmt.execute(sqlstatement);			
			stmt.close();
		} catch (Exception e) {
			throw new RuntimeException("Problem executing: "+sqlstatement, e);
		}

		return output;
	}

	/**
	 * Perform the sql query and return the resulting result set.
	 * 
	 * Warning:  Always close a result set once its no longer needed.
	 * 
	 * @param sqlstatement SQL statement to query
	 * @return Result set
	 */
	protected ResultSet queryDatabase(String sqlstatement){
		ResultSet rs=null;
		Statement stmt;

		try {
			Connection connection = this.getConnection();

			// Create the statement          
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sqlstatement);
		} catch (Exception e) {
			throw new RuntimeException("Exception running the query: "+sqlstatement, e);
		}

		return rs;
	}

	/**
	 * Query the database and return the rows of a specific column
	 * as a list of strings
	 * 
	 * @param sqlstatement SQL Query
	 * @param columnname Column to return the values of
	 * @return List of strings
	 */
	protected List<String> queryDatabaseString(String sqlstatement, String columnname) {
		List<String> results = new ArrayList<String>();

		ResultSet rs = this.queryDatabase(sqlstatement);
		try {
			while(rs.next()) {
				results.add(rs.getString(columnname));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sqlstatement, e);
		} finally {
			try {
				rs.close();
				rs.getStatement().close();
			} catch (SQLException e) {
				// Do nothing
			}
		}

		return results;
	}

	/**
	 * Query the database and return the rows of a specific column
	 * as a list of integers
	 * 
	 * @param sqlstatement SQL Query
	 * @param columnname Column to return the values of
	 * @return List of integers
	 */
	protected List<Integer> queryDatabaseInteger(String sqlstatement, String columnname) {
		List<Integer> results = new ArrayList<Integer>();

		ResultSet rs = this.queryDatabase(sqlstatement);
		try {
			while(rs.next()) {
				results.add(rs.getInt(columnname));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sqlstatement, e);
		} finally {
			try {
				rs.close();
				rs.getStatement().close();
			} catch (SQLException e) {
				// Do nothing
			}
		}

		return results;
	}

	/**
	 * Return true if the statement has at least one result
	 * and false otherwise
	 * 
	 * @param sqlstatement SQL statement
	 * @return True if at least one row and false otherwise
	 */
	protected boolean queryHasResult(String sqlstatement) {
		ResultSet rs = this.queryDatabase(sqlstatement);
		try {
			if(rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Exception running the query: "+sqlstatement, e);
		} finally {
			try {
				rs.close();
				rs.getStatement().close();
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Drop specified table, if it exists
	 * Note:  Some database systems (i.e., MySQL) may support
	 * DROP TABLE IF EXISTS TABLENAME
	 * 
	 * @param table Table name
	 */
	protected void dropTableIfExists(String table){
		try {
			this.executeSQL("DROP TABLE "+table);
		} catch(RuntimeException e) {
			// Catch exception thrown by dropping a table not there
			Throwable cause = e.getCause();
			if(cause instanceof SQLException && ((SQLException) cause).getSQLState().equals("42Y55")) {
				return;
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * Truncate contents of table
	 * Note:  Some databases (i.e., MySQL) may support
	 * TRUNCATE TABLENAME
	 * 
	 * @param table Table name
	 */
	protected void truncateTable(String table){
		this.executeSQL("DELETE FROM "+table);
	}
}
