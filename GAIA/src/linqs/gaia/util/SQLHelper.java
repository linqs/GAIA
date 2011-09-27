package linqs.gaia.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import linqs.gaia.log.Log;

/**
 * Helper utility class for querying and executing database statements.
 * 
 * @author namatag
 */
public class SQLHelper {
	private String driver;
	private String url;
	private String user;
	private String pass;
	private Connection connection = null;
	
	/**
	 * Constructor
	 * 
	 * @param driver JDBC driver (i.e., com.mysql.jdbc.Driver)
	 * @param url JDBC Url (i.e., jdbc:mysql://cebu.cs.umd.edu/testdb)
	 * @param user Username
	 * @param pass Password
	 */
	public SQLHelper(String driver, String url, String user, String pass) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.pass = pass;
		
		// Load the JDBC driver
		try {
			Class.forName(this.driver);
			this.connection = DriverManager.getConnection(this.url, this.user, this.pass);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Perform the sql query and return the resulting result set.
	 * 
	 * Warning:  Always close a result set once its no longer needed.
	 * 
	 * @param sqlstatement SQL statement to query
	 * @return Result set
	 */
	public ResultSet queryDatabase(String sqlstatement){
		ResultSet rs=null;
		Statement stmt;

		try {
			// Create a connection to the database
			if(connection==null) {
				connection = DriverManager.getConnection(url, user, pass);
			}
			
			// Create the statement          
			stmt = connection.createStatement();
			rs = stmt.executeQuery(sqlstatement);
		} catch (Exception e) {
			throw new RuntimeException("Exception running the query: "+sqlstatement, e);
		}

		return rs;
	}
	
	/**
	 * Execute the specified sql statement.
	 * 
	 * @param sqlstatement SQL statement to query
	 * @return Result set
	 */
	public boolean executeSQL(String sqlstatement){
		boolean output = false;
		Statement stmt;

		try {
			// Create a connection to the database
			if(connection==null) {
				connection = DriverManager.getConnection(url, user, pass);
			}

			// Create the statement
			stmt = connection.createStatement();
			output = stmt.execute(sqlstatement);			
			stmt.close();
		} catch (Exception e) {
			throw new RuntimeException("Problem executing: "+sqlstatement, e);
		}

		return output;
	}
	
	public void disconnect() {
		// Disconnect immediately
		try {
			connection.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Return a string representation with the back slash, quotation mark,
	 * and apostrophe mark escaped.  For use when entering the value as part
	 * of a SQL statement.
	 * 
	 * @param value Value to escape the SQL charaters of
	 * @return Escaped string
	 */
	public static String escapeSQLCharacters(String value) {
		String escaped = value.replaceAll("\\\\","\\\\\\\\");
		escaped = escaped.replaceAll("'", "\\\\'");
		escaped = escaped.replaceAll("\"", "\\\\\"");
		
		return escaped;
	}

	/**
	 * Example main class
	 * 
	 * @param args No arguments
	 */
	public static void main(String args[]) {
		// Use to test out your connection
		String sqlstmt = "select * from enron_fmt_internal_msgs limit 0,10";
		SQLHelper helper = new SQLHelper("com.mysql.jdbc.Driver",
				"jdbc:mysql://cebu.cs.umd.edu/testdb", "testuser", "testpass");
		ResultSet rs = helper.queryDatabase(sqlstmt);

		// Iterate over the result set
		int count = 0;
		try {
			while(rs.next ()) {
				int mid = rs.getInt ("id");
				String subject = rs.getString ("subject");
				Log.INFO("id = " + mid
						+ ", name = " + subject);
				++count;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			// Close the result set
			try {
				if(rs!=null) {
					rs.close();
				}
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Return the integer value provided at the column with the specified name
	 * 
	 * @param sql SQL statement to execute
	 * @param colname Column name of value to return
	 * @return Value to return
	 */
	public int getIntegerValue(String sql, String colname) {
		try{
			ResultSet rs = queryDatabase(sql);
			rs.next();
			if(!rs.isLast()) {
				throw new RuntimeException("Only one value is expected");
			}
			
			int value = rs.getInt(colname);
			
			return value;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
