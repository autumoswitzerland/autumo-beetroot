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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Database manager.
 * Supported databases: H2, MySQL, MariaDB, Oracle, PostgreSQL.
 */
public class BeetRootDatabaseManager {

	public static final String CFG_KEY_DB_POOL_INIT_SIZE = "db_pool_init_size";
	public static final String CFG_KEY_DB_POOL_MIN_SIZE = "db_pool_min_size";
	public static final String CFG_KEY_DB_POOL_MAX_SIZE = "db_pool_max_size";
	public static final String CFG_KEY_DB_POOL_INCREMENT = "db_pool_increment";
	public static final String CFG_KEY_DB_POOL_MAX_STMTS = "db_pool_max_stmts";
	public static final String CFG_KEY_DB_POOL_MAX_STMTS_PER_CONN = "db_pool_max_stmts_per_conn";
	
	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootDatabaseManager.class.getName());
	
	private static BeetRootDatabaseManager instance = null;	
	
	private static boolean isInitialized = false;
	
	private String url = null;
	private String user = null;
	private String pass = null;
	private String driver = null;
	
	private boolean isH2Db = false;
	private boolean isMysqlDb = false;
	private boolean isMariaDb = false;
	private boolean isOracleDb = false;
	private boolean isPostgreDb = false;

	private ComboPooledDataSource cpds = null;
	
	private boolean isUnsupported = false;

	
	private BeetRootDatabaseManager() {
	}
	
	/**
	 * Access DB manager.
	 * 
	 * @return DB manager
	 */
	public static BeetRootDatabaseManager getInstance() {
        if (instance == null)
        	instance = new BeetRootDatabaseManager();
 
        return instance;
    }
	
	/**
	 * Has this database manager been initialized?
	 *  
	 * @return true if so, otherwise false
	 */
	public boolean isInitialized() {
		return isInitialized;
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
		
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of database manager is called more than once!");
    		return;
		}
		
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
			driver = "com.mysql.cj.jdbc.Driver";
		if (isMariaDb)
			driver = "org.mariadb.jdbc.Driver";
		if (isOracleDb)
			driver = "oracle.jdbc.OracleDriver";
		if (isPostgreDb)
			driver = "org.postgresql.Driver";
		if (isH2Db)
			driver = "org.h2.Driver";	

		//Class.forName(driver);	
		
		this.url = url;
		this.user = user;
		this.pass = pass;
		
		this.initializePool();
		
		isInitialized = true;
	}
	
	/**
	 * Initialize the DB manager with an officially unsupported database.
	 * This can work, but only the supported databases have been tested.
	 * 
	 * @param driverClass driver class
	 * @param url JDBC URL
	 * @param user user
	 * @param pass password
	 * @throws Exception
	 */
	public void initializeUnsupported(String driverClass, String url, String user, String pass) throws Exception {
	
		if (isInitialized) {
    		LOG.warn("WARNING: Initialisation of database manager is called more than once!");
    		return;
		}
		
		// unsupported
		isUnsupported = true;
		
		driver = driverClass;
		
		//Class.forName(driver);	
		
		this.url = url;
		this.user = user;
		this.pass = pass;
		
		this.initializePool();
		
		isInitialized = true;
	}
	
	private void initializePool() throws Exception {
		
		cpds = new ComboPooledDataSource();
		
		cpds.setDriverClass(driver);            
		cpds.setJdbcUrl(url);
		cpds.setUser(user);                                  
		cpds.setPassword(pass);
		
		final BeetRootConfigurationManager cm = BeetRootConfigurationManager.getInstance();
		
		// the settings below are optional -- c3p0 can work with defaults
		final String poolConfig[] = cm.getKeys("db_pool_");
		for (int i = 0; i < poolConfig.length; i++) {
			switch (poolConfig[0]) {
				case CFG_KEY_DB_POOL_INIT_SIZE: 
					cpds.setMinPoolSize(cm.getInt(CFG_KEY_DB_POOL_INIT_SIZE, 5));
					break;
				case CFG_KEY_DB_POOL_MIN_SIZE: 
					cpds.setMinPoolSize(cm.getInt(CFG_KEY_DB_POOL_MIN_SIZE, 5));
					break;
				case CFG_KEY_DB_POOL_MAX_SIZE: 
					cpds.setMaxPoolSize(cm.getInt(CFG_KEY_DB_POOL_MAX_SIZE, 50));
					break;
				case CFG_KEY_DB_POOL_INCREMENT: 
					cpds.setMaxPoolSize(cm.getInt(CFG_KEY_DB_POOL_INCREMENT, 5));
					break;
				case CFG_KEY_DB_POOL_MAX_STMTS: 
					cpds.setMaxStatements(cm.getInt(CFG_KEY_DB_POOL_MAX_STMTS, 200));
					break;
				case CFG_KEY_DB_POOL_MAX_STMTS_PER_CONN: 
					cpds.setMaxStatements(cm.getInt(CFG_KEY_DB_POOL_MAX_STMTS_PER_CONN, 20));
					break;
			}
		}
	}
	
	/**
	 * Get an new DB connection
	 * @return DB connection
	 * 
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return cpds.getConnection();
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

	public boolean isUnsupported() {
		return isUnsupported;
	}
	
	/**
	 * Rest users token.
	 * @param dbId user id
	 * @throws Exception
	 */
	public void resetToken(int dbId) throws Exception {

		Connection conn = null;
		Statement stmt = null;
		
		try {
			conn = instance.getConnection();
			// Now save edited data !
			stmt = conn.createStatement();
			
			String stmtStr = "UPDATE users SET lasttoken='NONE' WHERE id=" + dbId;
			//int a = 
			stmt.executeUpdate(stmtStr);
			
			//LOG.debug("Token resetted for "+a+" users.");
		
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
	}

	/**
	 * Count amount of records of an entity / table in database.
	 * 
	 * @param entity entity
	 * @return amount of records
	 * @throws SQLException
	 */
	public int countRecords(String entity) throws SQLException {

		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		int amount = -1;
				
		try {

			String stmtStr = "SELECT count(1) AS amount FROM " + entity;
			
			conn = instance.getConnection();
			stmt = conn.createStatement();
			set = stmt.executeQuery(stmtStr);
			boolean found = set.next();
			
			if (found)
				amount = set.getInt("amount");
			else
				amount = 0;
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return amount;		
	}
	
	/**
	 * Get property value from database (table 'properties').
	 * If the value isn't found, null is returned.
	 * 
	 * @param name name/key
	 * @return value for name/key
	 * @throws SQLException
	 */
	public String getProperty(String name) throws SQLException {
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		String value = null;
				
		try {

			conn = instance.getConnection();
			
			stmt = conn.createStatement();
			String stmtStr = "SELECT value FROM properties WHERE name='" + name +"'";
			set = stmt.executeQuery(stmtStr);
			boolean found = set.next();
			
			if (found)
				value = set.getString("value");
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return value;
	}
	
	/**
	 * Get language for user.
	 * @param dbId user id
	 * @return language
	 * @throws Exception
	 */
	public String getLanguage(int dbId) throws Exception {

		String lang = null;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		
		try {
		
			conn = instance.getConnection();
			
			// Now save edited data !
			stmt = conn.createStatement();
			
			String stmtStr = "SELECT lang FROM users WHERE id=" + dbId;
			set = stmt.executeQuery(stmtStr);
			
			if (set.next()) {
				lang = set.getString("lang");
			}
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return lang;
	}

	/**
	 * Update language.
	 * @param lang language code
	 * @param dbId user id
	 * @throws Exception
	 */
	public void updateLanguage(String lang, int dbId) throws Exception {
		
		Connection conn = null;
		Statement stmt = null;
		
		try {
			
			conn = instance.getConnection();
			stmt = conn.createStatement();
			
			String stmtStr = "UPDATE users SET lang='"+lang+"' WHERE id=" + dbId;
			stmt.executeUpdate(stmtStr);
		
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPass() {
		return pass;
	}

	public String getDriver() {
		return driver;
	}
	
}
