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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import jakarta.servlet.ServletContext;



/**
 * Utils.
 */
public class Utils {

	/** Allowed text mime types. */
	public static List<String> mimeTextList;
	/** Allowed octet mime types. */
	public static List<String> mimeOctetList;
	/** Allowed archive mime types. */
	public static List<String> mimeArchiveList;
	
	/**
	 * OS.
	 */
	public static final String OS = System.getProperty("os.name").toLowerCase();
	
	/**
	 * User home directory.
	 */
    public static final String USER_HOME = System.getProperty("user.home");
	
    /**
     * System-specific file separator.
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
    /**
     * System-specific separator.
     */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

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
		
		final Connection conn = DatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement();
		
		String stmtStr = "SELECT settings FROM users WHERE id="+userSession.getUserId();
		final ResultSet set = stmt.executeQuery(stmtStr);
		
		set.next(); // one record !
		
		final String settingsString = set.getString(1);

		set.close();
		stmt.close();
		conn.close();
		
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
		
		final Connection conn = DatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement();
		
		String stmtStr = "UPDATE users SET settings='"+settingsStr+"' WHERE id=" + userSession.getUserId();
		stmt.executeUpdate(stmtStr);
		
		stmt.close();
		conn.close();
	}
	
	/**
	 * Select a record of type clz (entity class).
	 * 
	 * @param clz entity class
	 * @param id DB record id
	 * @return entity bean
	 * @throws SQLException
	 */
	public static Entity selectRecord(Class<?> clz, int id) throws SQLException {
		
		final String c = clz.getName().toLowerCase();
		String table = c.substring(c.lastIndexOf(".") + 1, c.length());
		if (table.endsWith("y"))
			table = (table.substring(0, table.length() - 1)) + "ies";
		else
			table += "s";
		
		final Connection conn = DatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement();
		
		String stmtStr = "SELECT * FROM " + table + " WHERE id="+id;
		final ResultSet set = stmt.executeQuery(stmtStr);

		set.next(); // one record !
		
		final Entity entity = createBean(clz, set);
		
		set.close();
		stmt.close();
		conn.close();
		
		return entity;
	}	
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @return entity bean or null
	 * @throws SQLException
	 */
	public static Entity createBean(Class<?> beanClass, ResultSet set) throws SQLException {
		return createBean(beanClass, set, new BeanProcessor());
	}
	
	/**
	 * Create bean.
	 * 
	 * @param beanClass bean class, must be of type  {@link Entity}.
	 * @param set result set at current position the data is taken from
	 * @param processor bean processor
	 * @return entity bean or null
	 * @throws SQLException
	 */
	public static Entity createBean(Class<?> beanClass, ResultSet set, BeanProcessor processor) throws SQLException {
		
		Entity entity = null;
		if (beanClass != null)
			entity = (Entity) processor.toBean(set, beanClass);
		
		return entity;
	}
	
	/**
	 * Normalize URI.
	 * @param uri URI
	 * @return normalized URI
	 */
    public static String normalizeUri(String uri) {
        if (uri == null)
            return uri;
        if (uri.startsWith("/"))
        	uri = uri.substring(1);
        if (uri.endsWith("/"))
        	uri = uri.substring(0, uri.length() - 1);
        return uri;
    }
    
	/**
	 * Is supported text mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeText(String mimeType) {
		
		if (mimeTextList == null)
			mimeTextList = ConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_text");
		return mimeTextList.contains(mimeType);
	}

	/**
	 * Is supported octet-stream mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeOctet(String mimeType) {
		if (mimeOctetList == null)
			mimeOctetList = ConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_octet");
		return mimeOctetList.contains(mimeType);
	}
	
	/**
	 * Is supported archive mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeArchive(String mimeType) {
		if (mimeArchiveList == null)
			mimeArchiveList = ConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_archive");
		return mimeArchiveList.contains(mimeType);
	}
	
	/**
	 * Configure the log4j2 framework specifically with a log configuration file.
	 * 
	 * @param path a path to the configuration
	 * @throws IOException
	 */
	public static void configureLog4j2(String path) throws IOException {
		final File cf = new File(path);
		if (cf.exists()) {
			Configurator.initialize("BeetRootConfig", null, cf.toURI());
		}
		else {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			ConfigurationSource cs = new ConfigurationSource(is);
			Configurator.initialize(Thread.currentThread().getContextClassLoader(), cs);
		}
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
		if (value) {
			if (DatabaseManager.getInstance().isMariaDb() || DatabaseManager.getInstance().isMysqlDb() || DatabaseManager.getInstance().isOracleDb())
				val = "1";
			else
				val = "true";
		} else {
			if (DatabaseManager.getInstance().isMariaDb() || DatabaseManager.getInstance().isMysqlDb() || DatabaseManager.getInstance().isOracleDb())
				val = "0";
			else 
				val = "false";
		}
		return val;
	}
	
	/**
	 * Get servlets context's real path.
	 * 
	 * @param context servlet context
	 * @return real path
	 */
	public static String getRealPath(ServletContext context) {
		
		String cp = context.getRealPath("/");
		if (!cp.endsWith(Utils.FILE_SEPARATOR))
			cp += Utils.FILE_SEPARATOR;
		
		return cp;
	}
	
	/**
	 * Determine HTML div field type for SQL data type.
	 * 
	 * @param rsmd result set meta data
	 * @param idx db column index
	 * @param columnName db column name
	 * @return type
	 * @throws SQLException
	 */
	public static String getHtmlDivType(ResultSetMetaData rsmd, int idx, String columnName) throws SQLException {

		int sqlType = rsmd.getColumnType(idx);
		
		String divType = "text";
		if (Utils.isSqlTextType(sqlType))
			divType = "text";
		if (Utils.isSqlNumberType(sqlType))
			divType = "number";
		
		if (sqlType == Types.TIMESTAMP)
			divType = "datetime-local"; //HTML 5
		if (sqlType == Types.DATE)
			divType = "date";
		if (sqlType == Types.TIME)
			divType = "time";
		
		if (columnName.toLowerCase().equals("password"))
			divType = "password";
		if (columnName.equals("email"))
			divType = "email";
		if (Utils.isSqlBooelanType(sqlType)) {
			divType = "checkbox";
		}		
		return divType;
		
		// Unused list:
		/*
		input[type="month"]
		input[type="week"]
		input[type="search"]
		input[type="tel"]
		input[type="url"]
		input[type="color"]
		*/
	}
	
	/**
	 * Determine HTML input field type for SQL data type.
	 * 
	 * @param rsmd result set meta data
	 * @param idx db column index
	 * @param columnName db column name
	 * @return type
	 * @throws SQLException
	 */
	public static String getHtmlInputType(ResultSetMetaData rsmd, int idx, String columnName) throws SQLException {
		
		// NOTICE split into more types possibly, but override this method 
		// and customize what is necessary is the way... THIS IS THE WAY!
		// But this is already sufficient for most cases
		
		int sqlType = rsmd.getColumnType(idx);
		
		String inputType = "text";
		if (Utils.isSqlTextType(sqlType))
			inputType = "text";
		if (Utils.isSqlNumberType(sqlType))
			inputType = "number";
		
		if (sqlType == Types.TIMESTAMP)
			inputType = "datetime-local"; //HTML 5
		if (sqlType == Types.DATE)
			inputType = "date";
		if (sqlType == Types.TIME)
			inputType = "time";
		
		if (columnName.toLowerCase().equals("password"))
			inputType = "password";
		if (columnName.equals("email"))
			inputType = "email";
		if (Utils.isSqlBooelanType(sqlType)) {
			inputType = "checkbox";
		}
		return inputType;
		
		// Full list:
		/*
		<input type="text">
		<input type="date">
		<input type="time">
		<input type="datetime-local">
		<input type="email">
		<input type="number">
		<input type="password">
		<input type="checkbox">
		
		<input type="radio">
		<input type="range">
		
		<input type="tel">
		<input type="url">
		<input type="search">
		
		<input type="file">
		<input type="color">
		<input type="image">
		
		<input type="month">
		<input type="reset">
		<input type="week">
		
		<input type="hidden">
		<input type="submit">
		<input type="button">
		 */
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
		return value;
	}

	/**
	 * Get a timestamp representation that can be shown in GUI
	 * 
	 * @param tmestamp from DB
	 * @return timestamp representable date
	 */
	public static String getGUIDate(Timestamp tsFromDb) {

		// Oh boy...
		final Instant instant = tsFromDb.toInstant();
		final Instant instantTruncated = instant.with(ChronoField.NANO_OF_SECOND, 0);
		final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());
		final String output = formatter.format(instantTruncated);
		
		return output.replace("T", " ");
	}
	
	/**
	 * Get a timestamp representation that can be stored in db.
	 * 
	 * @return timestamp string
	 */
	public static String nowTimeStamp() {
		
		final Timestamp ts = new Timestamp(System.currentTimeMillis());
		return ts.toLocalDateTime().toString();
	}
    
	/**
	 * Generates a CSRF token.
	 * 
	 * @param secureApplication a secure appp is needed!
	 * @return The generated CSRF.
	 * @throws UtilsException
	 */
	public static String generateCSRFToken(SecureApplication secureApplication) throws UtilsException {
		final String guid = GUIDGenerator.generate();
		return encodeBase64_PBE_MD5_DES(guid, secureApplication);
	}

	/**
	 * 
	 * Encode password for DB and config. It is the algorithm 3, see 'encoder.sh' from ifaceX.
	 * 
	 * @param pw password
	 * @param secureApplication secure application
	 * @return encoded PW
	 * @throws UtilsException
	 */
	public static String encodePassword(String pw, SecureApplication secureApplication) throws UtilsException {
		return encodeBase64_PBE_MD5_DES(pw, secureApplication);
	}
	
	private static String encodeBase64_PBE_MD5_DES(String data, SecureApplication app) throws UtilsException {
    	
        try {
	        //Key generation for enc and desc
	        KeySpec keySpec = new PBEKeySpec(app.getUniqueSecurityKey().toCharArray(), KEYDATA.SALT_1, KEYDATA.ITER_1);
	        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
	        // Prepare the parameter to the ciphers
	        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(KEYDATA.SALT_1, KEYDATA.ITER_1);
	
	        //Enc process
	        Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
	        ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
	        String charSet = "UTF-8";
	        byte[] in = data.getBytes(charSet);
	        byte[] out = ecipher.doFinal(in);
	        String encStr = new String(Base64.encodeBase64(out));
	        return encStr;
        } catch (Exception e) {
			throw new UtilsException("Couldn't encode password/key!", e);
        }
    }	

	/**
	 * 
	 * Decode password for DB and config. It is the algorithm 3, see 'encoder.sh' from ifaceX.
	 * 
	 * @param pw password
	 * @param secureApplication secure application
	 * @return decoded PW
	 * @throws UtilsException
	 */
	public static String decodePassword(String pw, SecureApplication secureApplication) throws UtilsException {
		return decodeBase64_PBE_MD5_DES(pw, secureApplication);
	}
	
	private static String decodeBase64_PBE_MD5_DES(String data, SecureApplication app) throws UtilsException {
    	
        try {
	        //Key generation for enc and desc
	        KeySpec keySpec = new PBEKeySpec(app.getUniqueSecurityKey().toCharArray(), KEYDATA.SALT_1, KEYDATA.ITER_1);
	        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
	        // Prepare the parameter to the ciphers
	        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(KEYDATA.SALT_1, KEYDATA.ITER_1);
	        //Decryption process; same key will be used for decr
	        Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
	        dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	        byte[] enc = Base64.decodeBase64(data);
	        byte[] utf8 = dcipher.doFinal(enc);
	        String charSet = "UTF-8";
	        String plainStr = new String(utf8, charSet);
	        return plainStr;
        } catch (Exception e) {
			throw new UtilsException("Couldn't decode password/key!", e);
        }
	}	

    /**
     * Exit console program, due to unprocessable error.
     */
	public static void fatalExit() {
		exit(-1);
	}
    /**
     * Exit console program, due desired end.
     */
	public static void normalExit() {
		exit(0);
	}
    /**
     * Exit console program.
     */
	public static void exit(int code) {
		System.exit(code);
	}
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public final static class KEYDATA {
		private static final byte[] SALT_1 = {
				(byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
				(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
			};
		public static final int LEN_3 = 16;	
		public static final int LEN_4 = 32;
		private static final int ITER_1 = 19;	
	}

	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	
}
