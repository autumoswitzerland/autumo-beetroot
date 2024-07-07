/**
 * 
 * Copyright (c) 2023 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.autumo.beetroot;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.DBField;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.OS;

/**
 * Database manager.
 * 
 * Supported databases: H2, MySQL, MariaDB, Oracle, PostgreSQL and
 * unsupported databases.
 */
public class BeetRootDatabaseManager {

	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootDatabaseManager.class.getName());
	
	public static final String POOL_NAME_PEFIX = "-DB-Pool";
	
	public static final String CFG_KEY_DS_EXT_JNDI = "db_ds_ext_jndi";
	public static final String CFG_KEY_DS_INT_DSCN = "db_ds_int_dataSourceClassName";
	
	private static BeetRootDatabaseManager instance = null;	
	private static boolean isInitialized = false;
	
	private HikariDataSource dataSource = null;
	
	private String dsExternalJndi = null;
	private String dataSourceClassName = null;
	private String dataSourceDriverClassName = null;
	
	private String url = null;
	private String user = null;
	private String pass = null;
	
	private boolean isH2Db = false;
	private boolean isMysqlDb = false;
	private boolean isMariaDb = false;
	private boolean isOracleDb = false;
	private boolean isPostgreDb = false;
	private boolean isPostgreNGDb = false;
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
	 * @param webAppRootPath Web app root path
	 * @throws Exception exception
	 */
	public void initialize(String webAppRootPath) throws Exception {
		
		String webAppRootWithoutSlash = webAppRootPath;
		if (webAppRootPath.endsWith(Helper.FILE_SEPARATOR))
			webAppRootWithoutSlash = webAppRootPath.substring(0, webAppRootPath.length() - 1);

		final BeetRootConfigurationManager configMan = BeetRootConfigurationManager.getInstance();
		
		this.url = configMan.getString("db_url");

		// Might NULL be here, lets continue for a while... 
		if (this.url != null && 
			this.url.length() > 0 && 
			this.url.contains(Constants.KEY_DB_URL_WEB_CONTEXT_PATH)) {
				this.url = this.url.replace(Constants.KEY_DB_URL_WEB_CONTEXT_PATH, webAppRootWithoutSlash);
		}
		
		this.initialize();
	}
	
	/**
	 * Initialize DB manager.
	 * 
	 * @throws Exception exception
	 */
	public void initialize() throws Exception {
		
		if (isInitialized) {
    		LOG.warn("Initialisation of database manager is called more than once!");
    		return;
		}
		
		final BeetRootConfigurationManager configMan = BeetRootConfigurationManager.getInstance();
		
		/** this is an undocumented configuration key: it allows to use unsupported databases! */
		final String driverClass = configMan.getStringNoWarn("db_driver");
		if (driverClass != null && driverClass.length() != 0) {
			dataSourceDriverClassName = driverClass;
			isUnsupported = true;
		}

		// default parameters
		final boolean pwEncoded = configMan.getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
		
		if (this.url == null) {
			// Not yet initialized by a web-app context
			this.url = configMan.getString("db_url");
			
			// If the URL is still Null, this is not OK! 
			// We still need a JDBC-prefix 'jdbc:<db-name>';
			// it is used internally to determine what DB is 
			// used for specific vendor-operations.
			if (this.url == null || this.url.length() == 0) {
				throw new Exception("'db_url' is not specified; at least 'jdbc:<db-name>' is required if a sole external JNDI or an own internal data source is used for vendor specific operations!");
			}
		}
		
		this.user = configMan.getString("db_user");
		this.pass = pwEncoded ? configMan.getDecodedString("db_password", SecureApplicationHolder.getInstance().getSecApp()) : configMan.getString("db_password");
		
		
		// if external JNDI data-source provides a JDBC URL, we still must beetRoot
		// to know what data-base is used, at least 'jdbc:[DB-identifier]' must be provided!
		
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
		// Is Postgre NG db?
		isPostgreNGDb = url.startsWith(Constants.JDBC_POSTGRE_NG_DB);
		
		
		// only used if no external JNDI data-source is provided and the internal
		// data-source needs a pre-defined driver class
		if (isH2Db) {
			dataSourceClassName = "org.h2.jdbcx.JdbcDataSource";
			dataSourceDriverClassName = "org.h2.Driver";
		}
		if (isMysqlDb) {
			dataSourceClassName = null;
			dataSourceDriverClassName = "com.mysql.cj.jdbc.Driver";
			// The MySQL DataSource is known to be broken with respect to network timeout support. Use jdbc-Url instead.
		}
		if (isMariaDb) {
			dataSourceClassName = "org.mariadb.jdbc.MariaDbDataSource";
			dataSourceDriverClassName = "org.mariadb.jdbc.Driver";
		}
		if (isOracleDb) {
			dataSourceClassName = "oracle.jdbc.pool.OracleDataSource";
			dataSourceDriverClassName = "oracle.jdbc.OracleDriver";
		}
		if (isPostgreDb) {
			dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource";
			dataSourceDriverClassName = "org.postgresql.Driver";
		}
		if (isPostgreNGDb) {
			dataSourceClassName = "com.impossibl.postgres.jdbc.PGDataSource";
			dataSourceDriverClassName = "com.impossibl.postgres.jdbc.PGDriver";
		}
		
		this.initializePool();
		
		isInitialized = true;
	}
	
	private void initializePool() throws Exception {

		// hikari data-source
		dataSource = new HikariDataSource();

		// hikari proerties if any
		final Properties dsProps = new Properties();
		
		// read additional configuration parameters
		final BeetRootConfigurationManager cm = BeetRootConfigurationManager.getInstance();
		
		
		// 1. external JNDI and data-source?
		dsExternalJndi = cm.getStringNoWarn(CFG_KEY_DS_EXT_JNDI);
		if (dsExternalJndi != null && dsExternalJndi.length() > 0) {
			LOG.info("External JNDI data-source '"+dsExternalJndi+"' has been configured");
			// check if we still have a JDBC-URL prefix for determining the db type for beetRoot
			if (url == null || url.length() == 0)
				throw new Exception("External JNDI data-source '"+dsExternalJndi+"' has been configured, but no JDBC-URL-prefix within 'db_url' " 
									+ "configuration parameterhas been defined! "
									+ OS.LINE_SEPARATOR +
									"It is used at least for determining what database is used; scheme 'jdbc:<database-id>'.");
			
			dataSource.setDataSourceJNDI(dsExternalJndi);
			// This means jdbcUrl, driverClassName, dataSourceProperties, user-name, password have been set externally
			// --> All done!
			return;
		}
		
		
		// 2. set pool name for internal data-source
		dataSource.setPoolName(cm.getString(Constants.KEY_SERVER_NAME) + POOL_NAME_PEFIX);

		
		// 3 optional settings?
		final String poolConfigKeys[] = cm.getKeys("db_pool_");
		for (int i = 0; i < poolConfigKeys.length; i++) {
			final String key = poolConfigKeys[i];
			final String value = cm.getString(key);
			final String dsPropKey = key.substring(8, key.length());
			dsProps.put(dsPropKey, value);
		}
		if (dsProps.size() > 0)
			dataSource.setDataSourceProperties(dsProps);

		
		// 4. own defined data-source?
		final String dscn = cm.getStringNoWarn(CFG_KEY_DS_INT_DSCN);
		if (dscn != null && dscn.length() > 0) {
			dataSource.setDataSourceClassName(dscn);
			final String intDsConfigKeys[] = cm.getKeys("db_ds_int_");
			for (int i = 0; i < intDsConfigKeys.length; i++) {
				final String key = intDsConfigKeys[i];
				final String value = cm.getString(key);
				final String dsPropKey = key.substring(8, key.length());
				dsProps.put(dsPropKey, value);
			}
			dataSource.setDataSourceProperties(dsProps);
			// In his case, we are finished too!
			return;
		}
				
		
		// 5. Default initialization with JDBC URL and driver class
		dataSource.setJdbcUrl(url);
		dataSource.setUsername(user);
		dataSource.setPassword(pass);
		dataSource.setDriverClassName(dataSourceDriverClassName);
	}
	
	/**
	 * Resource database pool resources. Should be called when a container
	 * life-cycle or a server ends!
	 */
	public void release() {
		// close pool
		if (dataSource != null) {
			dataSource.close();
		}
		// de-register database drivers loaded by this class-loader only!
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		final Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			final Driver driver = drivers.nextElement();
			if (driver.getClass().getClassLoader() == cl) {
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException e) {
					LOG.warn("Couldn't de-register database driver '"+driver,getClass().getName()+"'.");
				}
			}
		}		
	}
	
	/**
	 * Get the data source.
	 * 
	 * @return data source
	 */
	public DataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * Get an new DB connection.
	 * 
	 * @return DB connection
	 * @throws SQLException SQL exception
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/**
	 * Get an new global DB connection.
	 * 
	 * You have to roll back or commit the transaction, before you retire
	 * it with {@link #retireGlobalConnection(Connection)}. If you use {@link DB}
	 * roll-backs are done automatically and you'll receive an {@link SQLException}.
	 * 
	 * Don't close it by yourself!
	 * 
	 * @return global DB connection
	 * @throws SQLException SQL exception
	 */
	public Connection getGlobalConnection() throws SQLException {
		final Connection conn = dataSource.getConnection();
		conn.setAutoCommit(false);
		return conn;
	}

	/**
	 * Retire a global DB connection.
	 * 
	 * @see #getGlobalConnection()
	 * 
	 * @throws SQLException SQL exception
	 */
	public void retireGlobalConnection(Connection conn) throws SQLException {
		if (!conn.isClosed()) {
			conn.setAutoCommit(true);
			try {
				conn.close();
			} catch (Exception e) {
			}
		}
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

	public boolean isPostgreDbWithNGDriver() {
		return isPostgreNGDb;
	}
	
	public boolean isUnsupported() {
		return isUnsupported;
	}
	
	/**
	 * Reset users token.
	 * 
	 * @param dbId user id
	 * @throws Exception exception
	 */
	public void resetToken(int dbId) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = instance.getConnection();
			// Now save edited data !
			stmt = conn.createStatement();
			final String stmtStr = "UPDATE users SET lasttoken='NONE' WHERE id=" + dbId;
			stmt.executeUpdate(stmtStr);
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
	 * @throws SQLException SQL exception
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
	 * @throws SQLException SQL exception
	 */
	public String getProperty(String name) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		String value = null;
		try {
			conn = instance.getConnection();
			stmt = conn.createStatement();
			final String stmtStr = "SELECT value FROM properties WHERE name='" + name +"'";
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
	 * @throws Exception exception
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
			final String stmtStr = "SELECT lang FROM users WHERE id=" + dbId;
			set = stmt.executeQuery(stmtStr);
			if (set.next())
				lang = set.getString("lang");
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
	 * @throws Exception exception
	 */
	public void updateLanguage(String lang, int dbId) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = instance.getConnection();
			stmt = conn.createStatement();
			final String stmtStr = "UPDATE users SET lang='"+lang+"' WHERE id=" + dbId;
			stmt.executeUpdate(stmtStr);
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
	}

	/**
	 * Describe table columns for given table.
	 * 
	 * @param table database table
	 * @return list of DB fields with table column descriptions
	 * @throws SQLException SQL exception
	 */
	public List<DBField> describeTable(String table) throws SQLException {
		String statement = null;
		if (isMariaDb || isMysqlDb) {
			statement = "DESC " + table;
		} else if (isH2Db) {
			statement = "SHOW columns FROM " + table;
		} else if (isOracleDb) {
			statement = "SELECT "
					  + "  column_name, "
					  + "  data_type, "
					  + "  nullable, "
					  + "  (select "
					  + "      'UNI' "
					  + "    FROM "
					  + "      user_constraints uc, "
					  + "      USER_IND_COLUMNS uic "
					  + "    WHERE "
					  + "      uc.table_name='"+table.toUpperCase()+"' "
					  + "        AND uic.table_name='"+table.toUpperCase()+"' "
					  + "        AND uc.constraint_type='U' "
					  + "        AND uic.COLUMN_NAME=utc.column_name "
					  + "        AND uc.constraint_name = uic.index_name) AS is_uique, "
					  + "  data_default "
					  + "FROM "
					  + "  user_tab_columns utc "
					  + "WHERE "
					  + "  table_name = '"+table.toUpperCase()+"'"
					  + "ORDER BY"
					  + "  column_id";
		} else if (isPostgreDb || isPostgreNGDb) {
			statement = "SELECT "
					  + "  column_name, "
					  + "  data_type, "
					  + "  is_nullable, " // 'NO' or 'YES'
					  + "  (SELECT "
					  + "      constraint_type "
					  + "    FROM "
					  + "      information_schema.table_constraints "
					  + "    WHERE "
					  + "      table_name = '"+table+"' "
					  + "    AND "
					  + "      constraint_name = '"+table+"_' || column_name || '_key'), " // 'UNIQUE' or NULL
					  + "  column_default "
					  + "FROM "
					  + "  information_schema.columns "
					  + "WHERE "
					  + " table_name = '"+table+"'";
		} else {
			statement = "DESC " + table; // Wild guess!
		}
		final List<DBField> fields = new ArrayList<DBField>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = instance.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(statement);
			while (rs.next()) {
				final String name = rs.getString(1).toLowerCase();
				final String type = rs.getString(2);
				final String nullable = rs.getString(3).toLowerCase();
				final String unique = rs.getString(4); // Unique 'UNI', Primary (PRI) or NULL!
				final String defVal = rs.getString(5); 
				final DBField dbField = new DBField(
					name,
					type,
					nullable.equals("yes") || nullable.equals("y") ? true : false,
					unique == null || !unique.toLowerCase().startsWith("uni") ? false : true, 
					defVal
				);
				fields.add(dbField);
			}			
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
			}
		}			
		return fields;
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
		return dataSourceDriverClassName;
	}

	public String getDataSourceClassName() {
		return dataSourceClassName;
	}
	
}
