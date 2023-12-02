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

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import ch.autumo.beetroot.BeetRootDatabaseManager;


/**
 * Web helper methods.
 */
public class Web {

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
		if (!cp.endsWith(Helper.FILE_SEPARATOR))
			cp += Helper.FILE_SEPARATOR;
		
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
		if (DB.isSqlTextType(sqlType))
			divType = "text";
		if (DB.isSqlNumberType(sqlType))
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

		if (DB.isSqlBooelanType(sqlType)) {
			divType = "checkbox";
		}		
		
		// oracle special case
		if (BeetRootDatabaseManager.getInstance().isOracleDb() && DB.isSqlNumberType(sqlType) && prec == 1) {
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
		if (DB.isSqlTextType(sqlType))
			inputType = "text";
		if (DB.isSqlNumberType(sqlType))
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
		
		if (DB.isSqlBooelanType(sqlType)) {
			inputType = "checkbox";
		}
		
		// oracle special case
		if (BeetRootDatabaseManager.getInstance().isOracleDb() && DB.isSqlNumberType(sqlType) && prec == 1) {
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
	 * Ping a web-site.
	 * 
	 * @param urlAddress URL address
	 * @return true if ping was successful
	 */
	public static boolean pingWebsite(String urlAddress) {
		HttpURLConnection connection = null;
		try {
			final URL url = new URL(urlAddress);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            final int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        } finally {
        	connection.disconnect();
		}
	}
	
}
