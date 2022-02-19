/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database manager.
 * Supported databases: H2, MySQL, MariaDB, Oracle, PostgreSQL.
 */
public class DatabaseManager {

	protected final static Logger LOG = LoggerFactory.getLogger(DatabaseManager.class.getName());
	
	private static DatabaseManager instance = null;	
	
	private String url = null;
	private String user = null;
	private String pass = null;
	
	private boolean isH2Db = false;
	private boolean isMysqlDb = false;
	private boolean isMariaDb = false;
	private boolean isOracleDb = false;
	private boolean isPostgreDb = false;
	
	/**
	 * Access DB manager.
	 * 
	 * @return DB manager
	 */
	public static DatabaseManager getInstance() {
        if (instance == null)
        	instance = new DatabaseManager();
 
        return instance;
    }

	private DatabaseManager() {
	}
	
	/**
	 * Initialize DB manager.
	 * 
	 * @param url jdbc url
	 * @param user user
	 * @param pass password
	 * @throws Exception
	 */
	public void initialize(String url, String user, String pass) throws Exception {
		
		// Is H2 db?
		isH2Db = url.startsWith(Constants.JDBC_H2_DB);
		// Is mysql db?
		isMysqlDb = url.startsWith(Constants.JDBC_MYSQL_DB);
		// Is maria db?
		isMariaDb = url.startsWith(Constants.JDBC_MARIA_DB);
		// Is Oracle db?
		isOracleDb = url.startsWith(Constants.JDBC_ORACLE_DB);
		// Is Postgre db?
		isPostgreDb = url.startsWith(Constants.JDBC_POSTGRE_DB);
		
		if (isMysqlDb)
			Class.forName("com.mysql.cj.jdbc.Driver");
		if (isMariaDb)
			Class.forName("org.mariadb.jdbc.Driver");
		if (isOracleDb)
			Class.forName("oracle.jdbc.driver.OracleDriver");
		if (isPostgreDb)
			Class.forName("org.postgresql.Driver");
		if (isH2Db)
			Class.forName("org.h2.Driver");	
		
		this.url = url;
		this.user = user;
		this.pass = pass;
	}
	
	/**
	 * Get an new DB connection
	 * @return DB connection
	 * 
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		
		return DriverManager.getConnection(this.url, this.user, this.pass);
	}

	public boolean isH2Db() {
		return isH2Db;
	}

	public boolean isMysqlDb() {
		return isMysqlDb;
	}

	public boolean isMariaDb() {
		return isMariaDb;
	}

	public boolean isOracleDb() {
		return isOracleDb;
	}

	public boolean isPostgreDb() {
		return isPostgreDb;
	}

	/**
	 * Rest users token.
	 * @param dbId user id
	 * @throws Exception
	 */
	public static void resetToken(int dbId) throws Exception {

		final Connection conn = instance.getConnection();
		Statement stmt = null;
		
		// Now save edited data !
		stmt = conn.createStatement();
		
		String stmtStr = "UPDATE users SET lasttoken='NONE' WHERE id=" + dbId;
		//int a = 
		stmt.executeUpdate(stmtStr);
		
		//LOG.debug("Token resetted for "+a+" users.");
		
		stmt.close();
		conn.close();
	}

	/**
	 * Get property value from database (table 'properties').
	 * If the value isn't found, null is returned.
	 * 
	 * @param name name/key
	 * @return value for name/key
	 * @throws Exception
	 */
	public static String getProperty(String name) throws Exception {
		
		final Connection conn = instance.getConnection();
		Statement stmt = null;
		stmt = conn.createStatement();
		String stmtStr = "SELECT value FROM properties WHERE name='" + name +"'";
		ResultSet set = stmt.executeQuery(stmtStr);
		boolean found = set.next();
		
		String value = null;
		if (found)
			value = set.getString("value");
		
		set.close();
		stmt.close();
		conn.close();
		return value;
	}
	
	/**
	 * Get language for user.
	 * @param dbId user id
	 * @return language
	 * @throws Exception
	 */
	public static String getLanguage(int dbId) throws Exception {

		String lang = null;
		
		final Connection conn = instance.getConnection();
		Statement stmt = null;
		
		// Now save edited data !
		stmt = conn.createStatement();
		
		String stmtStr = "SELECT lang FROM users WHERE id=" + dbId;
		ResultSet set = stmt.executeQuery(stmtStr);
		
		if (set.next()) {
			lang = set.getString("lang");
		}
		
		set.close();
		stmt.close();
		conn.close();
		
		return lang;
	}

	/**
	 * Update language.
	 * @param lang language code
	 * @param dbId user id
	 * @throws Exception
	 */
	public void updateLanguage(String lang, int dbId) throws Exception {
		
		final Connection conn = instance.getConnection();
		Statement stmt = null;
		
		stmt = conn.createStatement();
		
		String stmtStr = "UPDATE users SET lang='"+lang+"' WHERE id=" + dbId;
		stmt.executeUpdate(stmtStr);
		
		stmt.close();
		conn.close();
	}
	
}
