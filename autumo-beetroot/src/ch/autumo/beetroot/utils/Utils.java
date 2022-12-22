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
package ch.autumo.beetroot.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
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
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.security.SecureApplication;
import de.taimos.totp.TOTP;


/**
 * Utils - A long story.
 */
public class Utils {

	/** alpha-numeric HEX characters */
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	
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
	 * Get Windows APPDATA directory.
	 */
    public static final String WIN_APPDATA_FOLDER = System.getenv("APPDATA" /*"LOCALAPPDATA"*/);
	
    
    
	// General
	//------------------------------------------------------------------------------
	
	/**
	 * Bytes 2 Hex.
	 * 
	 * @param bytes bytes
	 * @return Hex representation
	 */
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}	
	
	
	
    // OS
	//------------------------------------------------------------------------------
    
	/**
	 * Is Windows?
	 * 
	 * @return true if so
	 */
	public static boolean isWindows() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	/**
	 * Is Mac?
	 * 
	 * @return true if so
	 */
	public static boolean isMac() {
		return SystemUtils.IS_OS_MAC;
	}

	/**
	 * Is Unix?
	 * 
	 * @return true if so
	 */
	public static boolean isUnix() {
		return SystemUtils.IS_OS_UNIX;
	}

	/**
	 * Is Solaris?
	 * 
	 * @return true if so
	 */
	public static boolean isSolaris() {
		return SystemUtils.IS_OS_SOLARIS;
	}
	
	/**
	 * Get operating system.
	 * 
	 * @return true if so
	 */
	public static String getOS() {
		return OS;	
	}
	
	/**
	 * Get temporary directory.
	 * 
	 * @return temporary directory
	 */
	public static String getTemporaryDirectory() {

		String dir = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_TMP_DIR);
		
		if (dir != null && dir.length() == 0)
			dir = System.getProperty("java.io.tmpdir");
		
		if (dir == null)
			dir = System.getProperty("java.io.tmpdir");
		
		if (!dir.endsWith(FILE_SEPARATOR))
			dir += FILE_SEPARATOR;
		
		return dir;
	}


	
	// Desktop
	//------------------------------------------------------------------------------
	
    /**
     * Get properties path.
     * 
     * @param appName app name
     * @return properties path
     */
    public static String getDesktopPropertiesPath(String appName) {
        if (isMac())
            return USER_HOME + FILE_SEPARATOR + "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR + "autumo" + FILE_SEPARATOR + appName + FILE_SEPARATOR;
        if (isWindows())
            return WIN_APPDATA_FOLDER + FILE_SEPARATOR + "autumo" + FILE_SEPARATOR + appName + FILE_SEPARATOR;
        if (isUnix())
            return USER_HOME + FILE_SEPARATOR + "." + appName + FILE_SEPARATOR;
        return USER_HOME + FILE_SEPARATOR;
    }
			
	

	// HTML / URL / URI
	//------------------------------------------------------------------------------

    /**
     * HTML escape value.
     * 
     * @param value to escape
     * @return value escaped value
     */
    public static String escapeHtml(String value) {
    	return StringEscapeUtils.escapeHtml4(value);
    }
	
	/**
	 * Enrich URL with parameters.
	 * 
	 * @param url url
	 * @param name parameter name
	 * @param value parameter value
	 * @return new url
	 */
	public static String enrichQuery(String url, String name, Object value) {
		StringBuilder queryString = new StringBuilder();
		if (url.contains("?")) {
			queryString.append("&");
		} else {
			queryString.append("?");
		}
		try {
			queryString
				.append(URLEncoder.encode(name, "UTF-8"))
				.append("=")
				.append(URLEncoder.encode((value == null) ? "" : value.toString(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		url += queryString.toString();
		return url;
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
	 * Remove unnecessary HTML tags, but preserve line-breaks.
	 * 
	 * @param html the input HTML
	 * @param prettyPrint use pretty-print?
	 * @return prepared HTML
	 */
	public static String prepareHtmlWithLineBreaks(String html, boolean prettyPrint) {
		return prepareHtmlWithLineBreaks(html, prettyPrint, null, (String[]) null);
	}
	
	/**
	 * Remove unnecessary HTML tags, but preserve line-breaks.
	 * 
	 * @param html the input HTML
	 * @param prettyPrint use pretty-print?
	 * @param addTags additional tags that should be kept if any
	 * @return prepared HTML
	 */
	public static String prepareHtmlWithLineBreaks(String html, boolean prettyPrint, String... addTags) {
		return prepareHtmlWithLineBreaks(html, prettyPrint, null, addTags);
	}

	/**
	 * Remove unnecessary HTML tags, but preserve line-breaks.
	 * 
	 * @param html the input HTML
	 * @param prettyPrint use pretty-print?
	 * @param replaceTags tags to replace if any
	 * @return prepared HTML
	 */
	public static String prepareHtmlWithLineBreaks(String html, boolean prettyPrint, List<SimpleEntry<String, String>> replacements) {
		return prepareHtmlWithLineBreaks(html, prettyPrint, replacements, (String[]) null);
	}
	
	/**
	 * Remove unnecessary HTML tags, but preserve line-breaks.
	 * 
	 * @param html the input HTML
	 * @param prettyPrint use pretty-print?
	 * @param addTags additional tags that should be kept if any
	 * @param replaceTags tags to replace if any
	 * @return prepared HTML
	 */
	public static String prepareHtmlWithLineBreaks(String html, boolean prettyPrint, List<SimpleEntry<String, String>> replacements, String... addTags) {
		
		final Document jsoupDoc = Jsoup.parse(html);
		final Document.OutputSettings outputSettings = new Document.OutputSettings();
		outputSettings.prettyPrint(prettyPrint);
		jsoupDoc.outputSettings(outputSettings);
		
		if (replacements!= null && replacements.size() > 0) {
			for (Iterator<SimpleEntry<String, String>> iterator = replacements.iterator(); iterator.hasNext();) {
				
				final SimpleEntry<String, String> simpleEntry = iterator.next();
				final Elements elements= jsoupDoc.getElementsByTag(simpleEntry.getKey());
				
				for (Iterator<Element> iterator2 = elements.iterator(); iterator2.hasNext();) {
					final Element element = iterator2.next();
					final Node c = element.childNode(0);
					final Element newElement = new Element(Tag.valueOf(simpleEntry.getValue()), "");
					newElement.append(c.toString());
					element.replaceWith(newElement);
				}
			}
		}
		
		jsoupDoc.select("br").before("\\n");
		jsoupDoc.select("p").before("\\n");
		
		String out = jsoupDoc.html().replaceAll("\\\\n", "\n");
		String strWithNewLines = null;
		if (addTags != null && addTags.length > 0)
			strWithNewLines = Jsoup.clean(out, "", Safelist.basic().addTags(addTags), outputSettings);
		else
			strWithNewLines = Jsoup.clean(out, "", Safelist.basic(), outputSettings);
		
		return strWithNewLines;
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
		int prec = rsmd.getPrecision(idx);
		
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
		
		// oracle special case
		if (BeetRootDatabaseManager.getInstance().isOracleDb() && Utils.isSqlNumberType(sqlType) && prec == 1) {
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
		int prec = rsmd.getPrecision(idx);
		
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
		
		// oracle special case
		if (BeetRootDatabaseManager.getInstance().isOracleDb() && Utils.isSqlNumberType(sqlType) && prec == 1) {
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
	
	
	
	// 2FA / OTP
	//------------------------------------------------------------------------------
	
	/**
	 * Generate a secret user key with specific length.
	 * Note: Only initially for every user once!
	 * 
	 * @return secret user key
	 */
	public static String createSecretUserKey() {
		return generateSecretUserKey(Constants.SECRET_USER_KEY_DEFAULT_LEN);
	}		
	
	/**
	 * Generate a secret user key with specific length.
	 * Note: Only initially for every user once!
	 * 
	 * @param len length
	 * @return secret user key
	 */
	private static String generateSecretUserKey(int len) {
	    final SecureRandom random = new SecureRandom();
	    final byte[] bytes = new byte[len];
	    random.nextBytes(bytes);
	    final Base32 base32 = new Base32();
	    return base32.encodeToString(bytes);
	}	

	/**
	 * Create 5-digit TOTP (time-based one-time password) code
	 * for a user secret key.
	 * 
	 * @param secretUserKey secret user key
	 * @return code
	 */
	public static String create6DigitTOTPCode(String secretUserKey) {
	    final Base32 base32 = new Base32();
	    final byte[] bytes = base32.decode(secretUserKey);
	    final String hexKey = Hex.encodeHexString(bytes);
	    return TOTP.getOTP(hexKey);
	}

	/**
	 * Create Google Authenticator bar code.
	 * 
	 * @param secretUserKey secret user key
	 * @param email email of user
	 * @return bar code
	 * @throws UtilsException
	 */
	public static String getGoogleAuthenticatorBarCode(String secretUserKey, String email) throws UtilsException  {
	    try {
	    	final String issuer = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_APP_NAME); // app name !
	        return "otpauth://totp/"
	                + URLEncoder.encode(issuer + ":" + email, "UTF-8").replace("+", "%20")
	                + "?secret=" + URLEncoder.encode(secretUserKey, "UTF-8").replace("+", "%20")
	                + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
	    } catch (UnsupportedEncodingException e) {
	        throw new UtilsException("Couldn't create Google authenticator bar code!", e);
	    }
	}
	
	/**
	 * Create QR code.
	 * 
	 * @param barCodeData Google authenticator bar code.
	 * @param height height of QR code image
	 * @param width width of QR code image
	 * @return path to QR code image file
	 * @throws UtilsException
	 */
	public static String createQRCode(String barCodeData, int height, int width) throws UtilsException {
		
	    BitMatrix matrix;
		try {
			matrix = new MultiFormatWriter().encode(barCodeData, BarcodeFormat.QR_CODE, width, height);
		} catch (WriterException e) {
			throw new UtilsException("Couldn't create QR matrix for bar code", e);
		}
		
		File png = null;
		String absPath = null;
		
		String prefix = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_TMP_FILE_PREFIX);
		if (prefix == null || prefix.length() == 0)
			prefix = "beetrootweb-";
		
		try {
			
			png = File.createTempFile(prefix, ".png", new File(getTemporaryDirectory()));
			png.deleteOnExit();
	        absPath = png.getAbsolutePath();
			final FileOutputStream out = new FileOutputStream(png);
	        MatrixToImageWriter.writeToStream(matrix, "png", out);
	        
	    } catch (Exception e) {
			throw new UtilsException("Couldn't write QR matrix to: '"+absPath+"'!", e);
		}		
		
		return absPath;
	}
	
	
	
	// DB
	//------------------------------------------------------------------------------
	
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
			return escapeHtml(v);
		
		return v;
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

		final String table = classToTable(clz);
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
	 * @param clz entity class
	 * @param id DB record id
	 * @return entity bean
	 * @throws SQLException
	 */
	public static Entity selectRecord(Class<?> clz, int id) throws SQLException {
		
		final String table = classToTable(clz);
		
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
			entity = createBean(clz, set);
		
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
	 * Class to DB table.
	 * @param clz class
	 * @return name of table in DB
	 */
	public static String classToTable(Class<?> clz) {
		
		final String c = clz.getName().toLowerCase();
		String table = c.substring(c.lastIndexOf(".") + 1, c.length());
		if (table.endsWith("y"))
			table = (table.substring(0, table.length() - 1)) + "ies";
		else
			table += "s";
		
		return table;
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
	
	
	
	// MIME types
	//------------------------------------------------------------------------------
	
	/**
	 * Is supported text mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeText(String mimeType) {
		
		if (mimeTextList == null)
			mimeTextList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_text");
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
			mimeOctetList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_octet");
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
			mimeArchiveList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_archive");
		return mimeArchiveList.contains(mimeType);
	}


		
	// Time
	//------------------------------------------------------------------------------
	
	/**
	 * Get readable duration.
	 * 
	 * @param durationInMilliseconds duration in ms
	 * @param printUpTo The maximum timeunit that should be printed
	 * @return readable duration
	 */
	public static String getReadableDuration(long durationInMilliseconds, TimeUnit printUpTo) {
		
		long dy = TimeUnit.MILLISECONDS.toDays(durationInMilliseconds);
		long allHours = TimeUnit.MILLISECONDS.toHours(durationInMilliseconds);
		long allMinutes = TimeUnit.MILLISECONDS.toMinutes(durationInMilliseconds);
		long allSeconds = TimeUnit.MILLISECONDS.toSeconds(durationInMilliseconds);
		long allMilliSeconds = TimeUnit.MILLISECONDS.toMillis(durationInMilliseconds);
		
		final long hr = allHours - TimeUnit.DAYS.toHours(dy);
		final long min = allMinutes - TimeUnit.HOURS.toMinutes(allHours);
		final long sec = allSeconds - TimeUnit.MINUTES.toSeconds(allMinutes);
		final long ms = allMilliSeconds - TimeUnit.SECONDS.toMillis(allSeconds);
		
		switch (printUpTo) {
			case DAYS: return String.format("%d Days %d Hours %d Minutes %d Seconds %d Milliseconds", dy, hr, min, sec, ms);
			case HOURS: return String.format("%d Hours %d Minutes %d Seconds %d Milliseconds", hr, min, sec, ms);
			case MINUTES: return String.format("%d Minutes %d Seconds %d Milliseconds", min, sec, ms);
			case SECONDS: return String.format("%d Seconds %d Milliseconds", sec, ms);
			case MILLISECONDS: return String.format("%d Milliseconds", ms);
			default: return String.format("%d Days %d Hours %d Minutes %d Seconds %d Milliseconds", dy, hr, min, sec, ms);
		}
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
	 * Get a time-stamp representation that can be stored in DB.
	 * 
	 * Note that this code returns a 'to_timestamp'-call when you are using
	 * an Oracle database, hence that value cannot be enclosed with
	 * apostrophes '...'; in case of Oracle it looks like this:
	 * 
	 * "to_timestamp('2022-12-21 23:59:59.999', 'YYYY-MM-DD HH24:MI:SS.FF')"
	 * 
	 * and in case of all other databases:
	 * 
	 * "2022-12-21 23:59:59.999"
	 * 
	 * @return time-stamp a time-stamp representation that works with used DB
	 */
	public static String nowTimeStamp() {
		return Utils.timeStamp(new Date());
	}
    
	/**
	 * Get a time-stamp representation that can be stored in DB.
	 * 
	 * Note that this code returns a 'to_timestamp'-call when you are using
	 * an Oracle database, hence that value cannot be enclosed with
	 * apostrophes '...'; in case of Oracle it looks like this:
	 * 
	 * "to_timestamp('2022-12-21 23:59:59.999', 'YYYY-MM-DD HH24:MI:SS.FF')"
	 * 
	 * and in case of all other databases:
	 * 
	 * "2022-12-21 23:59:59.999"
	 * 
	 * @param date create time-stamp out of given date
	 * @return time-stamp a time-stamp representation that works with used DB
	 */
	public static String timeStamp(Date date) {
		
		String ts_str = null;
		
		final Timestamp ts = new Timestamp(date.getTime());
		ts_str = ts.toLocalDateTime().toString();

		if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
			ts_str = ts_str.replace("T", " ");
			ts_str = "to_timestamp('"+ts_str+"', 'YYYY-MM-DD HH24:MI:SS.FF')";
		}
		
		return ts_str;
	}


	
	// JVM exits.
	//------------------------------------------------------------------------------
	
    /**
     * Exit console program, due to unprocessable error.
     */
	public static void fatalExit() {
		exit(1);
	}
    /**
     * Exit console program, due to an error.
     */
	public static void errorExit() {
		exit(1);
	}
    /**
     * Exit because of invalid argument use.
     */
	public static void invalidArgumentsExit() {
		exit(128);
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

		
	
	// Encoding / Decoding
	//------------------------------------------------------------------------------
	
	/**
	 * Generates a CSRF token.
	 * 
	 * @param secureApplication a secure app is needed!
	 * @return The generated CSRF.
	 * @throws UtilsException
	 */
	public static String generateCSRFToken(SecureApplication secureApplication) throws UtilsException {
		final String guid = GUIDGenerator.generate();
		return encodeBase64_PBE_MD5_DES(guid, secureApplication);
	}

	/**
	 * 
	 * Encode data. It is the algorithm 2.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return encoded PW
	 * @throws UtilsException
	 */
	public static String encode(String data, SecureApplication secureApplication) throws UtilsException {
		return encodeBase64_SHA256_AES(data, secureApplication);
	}

	/**
	 * 
	 * Decode data. It is the algorithm 1.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return decoded PW
	 * @throws UtilsException
	 */
	public static String decode(String data, SecureApplication secureApplication) throws UtilsException {
		return decodeBase64_SHA256_AES(data, secureApplication);
	}

	/**
	 * 
	 * Encode com data.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return encoded data
	 * @throws UtilsException
	 */
	public static String encodeCom(String data, SecureApplication secureApplication) throws UtilsException {
		return encodeBase64_SHA3_256_AES(data, secureApplication);
	}
	
	/**
	 * 
	 * Decode com data.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return decoded data
	 * @throws UtilsException
	 */
	public static String decodeCom(String data, SecureApplication secureApplication) throws UtilsException {
		return decodeBase64_SHA3_256_AES(data, secureApplication);
	}	
	
	
	
	// Internal encoding / decoding
	//------------------------------------------------------------------------------
	
	private static String encodeBase64_SHA256_AES(String data, SecureApplication app) throws UtilsException {
    	byte[] key = null;
    	byte[] encrypted = null;
		try {
			key = app.getUniqueSecurityKey().getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance("SHA-256");
	    	key = sha.digest(key);
	    	//key = Arrays.copyOf(key, KEYDATA.LEN_3); 
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
	    	encrypted = cipher.doFinal(data.getBytes());
		} catch (Exception e) {
			throw new UtilsException("Couldn't encode password/key!", e);
		}
		String result;
    	//----
    	//BASE64Encoder encoder = new BASE64Encoder();
    	//result = encoder.encode(encrypted);
    	result = Base64.encodeBase64String(encrypted);
    	//----
    	return result;
    }	
	
	private static String encodeBase64_SHA3_256_AES(String data, SecureApplication app) throws UtilsException {
    	byte[] key = null;
    	byte[] encrypted = null;
		try {
			key = (app.getUniqueSecurityKey()).getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance("SHA3-256");
	    	key = sha.digest(key);
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
	    	encrypted = cipher.doFinal(data.getBytes());
		} catch (Exception e) {
			throw new UtilsException("Couldn't encode password/key!", e);
		}
    	return Base64.encodeBase64String(encrypted);
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

	private static String decodeBase64_SHA256_AES(String data, SecureApplication app) throws UtilsException {
    	byte[] key = null;
    	byte[] cipherData = null;
		try {
			key = app.getUniqueSecurityKey().getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance("SHA-256");
	    	key = sha.digest(key);
	    	//key = Arrays.copyOf(key, KEYDATA.LEN_3); 
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	byte[] crypted;	    	//----
	    	//BASE64Decoder decoder = new BASE64Decoder();
	    	//crypted = decoder.decodeBuffer(data);
	    	crypted = Base64.decodeBase64(data);
	    	//----
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
	    	cipherData = cipher.doFinal(crypted);
		} catch (Exception e) {
			throw new UtilsException("Couldn't decode password/key!", e);
		}
    	return new String(cipherData);
    }
	
	private static String decodeBase64_SHA3_256_AES(String data, SecureApplication app) throws UtilsException {
    	
    	byte[] key = null;
    	byte[] cipherData = null;
		try {
			key = (app.getUniqueSecurityKey()).getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance("SHA3-256");
	    	key = sha.digest(key);
	    	//key = Arrays.copyOf(key, KEYDATA.LEN_3); 
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	byte[] crypted;	    	//----
	    	//BASE64Decoder decoder = new BASE64Decoder();
	    	//crypted = decoder.decodeBuffer(data);
	    	crypted = Base64.decodeBase64(data);
	    	//----
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
	    	cipherData = cipher.doFinal(crypted);
		} catch (Exception e) {
			throw new UtilsException("Couldn't decode password/key!", e);
		}
    	return new String(cipherData);
    }
	
	@SuppressWarnings("unused")
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
	 * Internal keys.
	 */
	public final static class KEYDATA {
		private static final byte[] SALT_1 = {
				(byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
				(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
			};
		public static final int LEN_3 = 16;	
		public static final int LEN_4 = 32;
		private static final int ITER_1 = 19;	
	}

	
	
	//------------------------------------------------------------------------------
	
	/*
	public static void main(String[] args) throws Exception {
		BeetRootConfigurationManager.getInstance().initialize();
		String e = encodeCom("This would be crazy!", SecureApplicationHolder.getInstance().getSecApp());
		System.out.println("ENC:"+e);
		String d = decodeCom(e, SecureApplicationHolder.getInstance().getSecApp());
		System.out.println("DEC:"+d);
	}
	*/
	
}
