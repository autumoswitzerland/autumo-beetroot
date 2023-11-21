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
package ch.autumo.beetroot.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Session;


/**
 * DB helper methods.
 */
public class DB {

    /**
     * Maximum of referenced records to be loaded.
     */
    public static int maxRefRecords = 200;
	
	/**
	 * Sorting of foreign entities by ID.
	 */
	public static int SORT_BY_ID = 0;
	
	/**
	 * Sorting of foreign entities by display value.
	 */
	public static int SORT_BY_VALUE = 1;
	
	/**
	 * Get display values: <ID:displayValue>.
	 * Max. 200 records to be returned.
	 * 
	 * @param entityClass class
	 * @return entries
	 * @throws Exception
	 */
	public static Map<Integer, String> getDisplayValues(Class<?> entityClass) throws Exception {
		maxRefRecords = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_WEB_MAX_REF_REC, 200);
		return getDisplayValues(entityClass, maxRefRecords);
	}

	/**
	 * Get display values: <ID:displayValue>.
	 * Max. 200 records to be returned.
	 * 
	 * @param entityClass class
	 * @param amount max. amount of records to be loaded
	 * @return entries
	 * @throws SQLException
	 */
	public static Map<Integer, String> getDisplayValues(Class<?> entityClass, int amount) throws Exception {
		return getDisplayValues(entityClass, amount, SORT_BY_VALUE);
	}
	
	/**
	 * Get display values: <ID:displayValue>.
	 * 
	 * @param entityClass class
	 * @param amount max. amount of records to be loaded
	 * @param sortType sort entries by ID or by values, 
	 * 			see {@value #SORT_BY_ID} and {@value #SORT_BY_VALUE}
	 * @return entries
	 * @throws Exception
	 */
	public static Map<Integer, String> getDisplayValues(Class<?> entityClass, int amount, int sortType) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		
		final String displayColumn =  Beans.getDisplayField(Beans.createBean(entityClass));
		String orderFiled = (sortType == SORT_BY_VALUE) ? displayColumn : "id"; 
		final String table = Beans.classToTable(entityClass);
		
		final Map<Integer, String> map = new HashMap<Integer, String>();
		try {
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			String stmtStr = null;
			if (BeetRootDatabaseManager.getInstance().isOracleDb())
				stmtStr = "SELECT id, " + displayColumn + " FROM " + table + " ORDER BY " + orderFiled + " OFFSET 0 ROWS FETCH NEXT " + amount + " ROWS ONLY";
			else
				stmtStr = "SELECT id, " + displayColumn + " FROM " + table + " ORDER BY " + orderFiled + " LIMIT " + amount;
				
			set = stmt.executeQuery(stmtStr);
	
			while (set.next())
				map.put(Integer.valueOf(set.getInt(1)), set.getString(2));
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
		
		return map;		
	}
	
	/**
	 * Get display value: <ID:displayValue>.
	 * 
	 * @param entityClass class
	 * @param id id
	 * @return entry
	 * @throws Exception
	 */
	public static Map.Entry<Integer, String> getDisplayValue(Class<?> entityClass, int id) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;

		final String displayColumn =  Beans.getDisplayField(Beans.createBean(entityClass));
		final String table = Beans.classToTable(entityClass);
		
		Map.Entry<Integer, String> entry = null;
		try {
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			String stmtStr = "SELECT id, " + displayColumn + " FROM " + table + " WHERE id = " + id;
			set = stmt.executeQuery(stmtStr);
	
			// One record!
			set.next();
			
			entry = Map.entry(Integer.valueOf(set.getInt(1)), set.getString(2));
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
		
		return entry;		
	}

	/**
	 * Escape single value for DB.
	 * 
	 * @param value value
	 * @return escaped value
	 */
	public static String escapeValuesForDb(String value) {
		
		if (value == null)
			return null;
		
		// escape quote with another quote for DB
		int q = value.indexOf("'");
		if (q != -1) {
			value = value.replaceAll("'", "''");
		}
		q = value.indexOf("\\");
		if (q != -1) {
			value = value.replace("\\", "\\\\");
		}
		return value;
	}
	
	/**
	 * Update secret user key.
	 * 
	 * @param userId DB user id
	 * @param newSecretUserKey new secret user key
	 * @throws SQLException
	 */
	public static void updateSecretUserKey(int userId, String newSecretUserKey) throws SQLException {
		
		Connection conn = null;
		Statement stmt = null;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			String stmtStr = "UPDATE users SET secretkey='"+newSecretUserKey+"' WHERE id=" + userId;
			stmt.executeUpdate(stmtStr);
		
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}		
	}	
	
	/**
	 * Load user settings map into user session.
	 *  
	 * @param userSession user session
	 * @return user settings map
	 * @throws SQLException
	 */
	public static Map<String, String> loadUserSettings(Session userSession) throws SQLException {

		Map<String, String> map = userSession.getUserSettings();
		if (map != null)
			return map;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		String settingsString = null;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			String stmtStr = "SELECT settings FROM users WHERE id="+userSession.getUserId();
			set = stmt.executeQuery(stmtStr);
			
			set.next(); // one record !
			settingsString = set.getString(1);
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
		
		if (settingsString == null || settingsString.length() == 0) {
			map = new HashMap<String, String>();
			userSession.setUserSettings(map);
			return map;
		}
		
		final String pairs[] = settingsString.replace(" ", "").trim().split(",");
		final Map<String, String> settingsMap = new HashMap<String, String>();
		for (int i = 0; i < pairs.length; i++) {
			String pair[] = pairs[i].split("=");
			settingsMap.put(pair[0], pair[1]);
		}
		
		userSession.setUserSettings(settingsMap);
		return settingsMap;
	}

	/**
	 * Store user setting from user session settings.
	 * 
	 * @param userSession user session
	 * @throws SQLException
	 */
	public static void storeUserSettings(Session userSession) throws SQLException {
		
		final Map<String, String> map = userSession.getUserSettings();
		if (map == null)
			return;
		
		String settingsStr = "";
		
		final Set<String> keys = map.keySet();
		int i = 1;
		int s = keys.size();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String val = map.get(key);
			if (i == s)
				settingsStr += (key+"="+val);
			else
				settingsStr += (key+"="+val+",");
			i++;
		}
		
		Connection conn = null;
		Statement stmt = null;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			String stmtStr = "UPDATE users SET settings='"+settingsStr+"' WHERE id=" + userSession.getUserId();
			stmt.executeUpdate(stmtStr);
		
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
	}
    
	/**
	 * Count rows of type clz (entity class).
	 * @param clz entity class
	 * @return amount of rows or -1 if something bad happens
	 * @throws SQLException
	 */
	public static int countRows(Class<?> clz) throws SQLException {

		final String table = Beans.classToTable(clz);
		return countRows(table);
	}
	
	/**
	 * Count rows of table.
	 * @param table table DB name
	 * @return amount of rows or -1 if something bad happens
	 * @throws SQLException
	 */
	public static int countRows(String table) throws SQLException {

		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		int amount = -1;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			set = stmt.executeQuery("SELECT count(*) FROM " + table);
			
			if(!set.next()) {
				
				set.close();
				stmt.close();
				conn.close();
				return -1;
			}
			
			amount =  set.getInt(1);
		
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
	 * Select a record of type clz (entity class).
	 * 
	 * @param entityClass entity class
	 * @param id DB record id
	 * @return entity bean
	 * @throws SQLException
	 */
	public static Entity selectRecord(Class<?> entityClass, int id) throws SQLException {
		
		final String table = Beans.classToTable(entityClass);
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		Entity entity = null;
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			String stmtStr = "SELECT * FROM " + table + " WHERE id="+id;
			set = stmt.executeQuery(stmtStr);
	
			set.next(); // one record !
			entity = Beans.createBean(entityClass, set);
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
		
		return entity;
	}
	
	/**
	 * Select a records of type entityClass (entity class).
	 * Max. 200 records to be returned.
	 * 
	 * @param entityClass class
	 * @return entity beans
	 * @throws Exception
	 */
	public static List<Entity> selectRecords(Class<?> entityClass) throws Exception {
		maxRefRecords = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_WEB_MAX_REF_REC, 200);
		return selectRecords(entityClass, maxRefRecords);
	}

	/**
	 * Select a records of type entityClass (entity class).
	 * Max. 200 records to be returned.
	 * 
	 * @param entityClass class
	 * @param amount max. amount of records to be loaded
	 * @return entity beans
	 * @throws Exception
	 */
	public static List<Entity> selectRecords(Class<?> entityClass, int amount) throws Exception {
		return selectRecords(entityClass, amount, SORT_BY_VALUE);
	}
	
	/**
	 * Select a records of type entityClass (entity class).
	 * 
	 * @param entityClass entity class
	 * @param amount max. amount of records to be loaded
	 * @param sortType sort entries by ID or by values, 
	 * 			see {@value #SORT_BY_ID} and {@value #SORT_BY_VALUE}
	 * @return entity beans
	 * @throws Exception
	 */
	public static List<Entity> selectRecords(Class<?> entityClass, int amount, int sortType) throws Exception {
		
		final String displayColumn = Beans.getDisplayField(Beans.createBean(entityClass));
		String orderFiled = (sortType == SORT_BY_VALUE) ? displayColumn : "id"; 
		final String table = Beans.classToTable(entityClass);
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		List<Entity> entities = new ArrayList<Entity>();
		
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
		
			String stmtStr = null;
			if (BeetRootDatabaseManager.getInstance().isOracleDb())
				stmtStr = "SELECT * FROM " + table + " ORDER BY " + orderFiled + " OFFSET 0 ROWS FETCH NEXT " + amount + " ROWS ONLY";
			else
				stmtStr = "SELECT * FROM " + table + " ORDER BY " + orderFiled + " LIMIT " + amount;
				
			set = stmt.executeQuery(stmtStr);
	
			while(set.next())
				entities.add(Beans.createBean(entityClass, set));
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();    	
		}
		
		return entities;
	}

	/**
	 * Access result set value and HTML escape it.
	 * 
	 * @param set result set
	 * @param dbColumnName db column name
	 * @return escaped db value
	 * @throws SQLException
	 */
	public static String getValue(ResultSet set, String dbColumnName) throws SQLException {

		String v = set.getString(dbColumnName);
		if (v != null && v.length() != 0)
			return Web.escapeHtml(v);
		
		return v;
	}
	
	/**
	 * Check if this SQL type is a html-input text type.
	 * 
	 * @param sqlType SQL type
	 * @return true if it is a html-input text type.
	 */
	public static boolean isSqlTextType(int sqlType) {
		for (int i = 0; i < Constants.SQL_TEXT_TYPES.length; i++) {
			if (Constants.SQL_TEXT_TYPES[i] == sqlType)
				return true;
		}
		return false;
	}

	/**
	 * Check if this SQL type is a html-input number type.
	 * 
	 * @param sqlType SQL type
	 * @return true if it is a html-input number type.
	 */
	public static boolean isSqlNumberType(int sqlType) {
		for (int i = 0; i < Constants.SQL_NUMBER_TYPES.length; i++) {
			if (Constants.SQL_NUMBER_TYPES[i] == sqlType)
				return true;
		}
		return false;
	}
	
	/**
	 * Check if this SQL type is a html-input date type.
	 * 
	 * @param sqlType SQL type
	 * @return true if it is a html-input date type.
	 */
	public static boolean isSqlDateTimeType(int sqlType) {
		for (int i = 0; i < Constants.SQL_DATE_TYPES.length; i++) {
			if (Constants.SQL_DATE_TYPES[i] == sqlType)
				return true;
		}
		return false;
	}
	
	/**
	 * Check if this SQL type is a html-input binary type.
	 * 
	 * @param sqlType SQL type
	 * @return true if it is a html-input binary type.
	 */
	public static boolean isSqlBinaryType(int sqlType) {
		for (int i = 0; i < Constants.SQL_BINARY_TYPES.length; i++) {
			if (Constants.SQL_BINARY_TYPES[i] == sqlType)
				return true;
		}
		return false;
	}

	/**
	 * Check if this SQL type is a html-input boolean type.
	 * 
	 * @param sqlType SQL type
	 * @return true if it is a html-input boolean type.
	 */
	public static boolean isSqlBooelanType(int sqlType) {
		for (int i = 0; i < Constants.SQL_BOOLEAN_TYPES.length; i++) {
			if (Constants.SQL_BOOLEAN_TYPES[i] == sqlType)
				return true;
		}
		return false;
	}	
	
	/**
	 * Get correct DB value for a boolean.
	 * @param value boolean value
	 * @return DB boolean value as string
	 */
	public static String getBooleanDatabaseMappingValue(boolean value) {
		
    	String val = null;
    	
    	// Informix uses 't' or 'f'
		if (value) {
			val = "1";
		} else {
			val = "0";
		}
		return val;
	}
	
}
