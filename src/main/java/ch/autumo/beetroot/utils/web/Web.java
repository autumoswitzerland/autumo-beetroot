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
package ch.autumo.beetroot.utils.web;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import jakarta.servlet.ServletContext;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.database.DB;
import ch.autumo.beetroot.utils.security.SSL;


/**
 * Web helper methods.
 */
public class Web {

	private static final String REGEX_INPUT_PATTERN_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#+`~\\-_='@$!.,;:\u201D^\\(\\)\\[\\]\\|\\{\\}\\\\\\/%*<>?&])[^\\s]{8,}$";
	private static final String REGEX_INPUT_PATTERN_EMAIL = "[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}$";
	private static final String REGEX_INPUT_PATTERN_PHONE = "^(\\+?\\d{1,4})?\\d{10,14}$";


	private Web() {
	}

    /**
     * HTML escape (reserved characters only) value.
     *
     * @param input value to escape
     * @return escaped value
     */
    public static String escapeHtmlReserved(String input) {
        if (Strings.isBlank(input)) {
            return input;
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    /**
     * Full HTML escape value. Includes "Umlaute".
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
	 * @param replacements tags to replace if any
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
	 * @param replacements tags to replace if any
	 * @return prepared HTML
	 */
	public static String prepareHtmlWithLineBreaks(String html, boolean prettyPrint, List<SimpleEntry<String, String>> replacements, String... addTags) {

		final Document jsoupDoc = Jsoup.parse(html);
		final Document.OutputSettings outputSettings = new Document.OutputSettings();
		outputSettings.prettyPrint(prettyPrint);
		jsoupDoc.outputSettings(outputSettings);

		if (replacements!= null && replacements.isEmpty()) {
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

		String out = jsoupDoc.html().replace("\\n", "\n");
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
	 * @throws SQLException SQL exception
	 */
	public static String getHtmlDivType(ResultSetMetaData rsmd, int idx, String columnName) throws SQLException {

		int sqlType = rsmd.getColumnType(idx);
		int prec = rsmd.getPrecision(idx);

		final String name = columnName.toLowerCase();

		String divType = "text";
		if (DB.isSqlTextType(sqlType))
			divType = "text";
		else if (BeetRootDatabaseManager.getInstance().isOracleDb() && DB.isSqlNumberType(sqlType) && prec == 1) // oracle special case
			divType = "checkbox";
		else if (DB.isSqlNumberType(sqlType))
			divType = "number";
		else if (sqlType == Types.TIMESTAMP)
			divType = "datetime-local"; //HTML 5
		else if (sqlType == Types.DATE)
			divType = "date";
		else if (sqlType == Types.TIME)
			divType = "time";
		else if (DB.isSqlBooelanType(sqlType))
			divType = "checkbox";

		// Overwrite if matching
		if (name.equals("password"))
			divType = "password";
		else if (name.equals("email"))
			divType = "email";
		else if (name.equals("phone") || name.equals("phonenumber") || name.equals("tel") || name.equals("telephone"))
			divType = "tel";

		// User mappings if any, overwrite if matching
		final String userMapping = BeetRootConfigurationManager.getInstance().getHtmlInputMapType(name);
		if (userMapping != null)
			divType = BeetRootConfigurationManager.getInstance().getHtmlInputMapType(name);

		return divType;
	}

	/**
	 * Determine HTML input field type for SQL data type.
	 *
	 * @param rsmd result set meta data
	 * @param idx db column index
	 * @param columnName db column name
	 * @return type
	 * @throws SQLException SQL exception
	 */
	public static String getHtmlInputType(ResultSetMetaData rsmd, int idx, String columnName) throws SQLException {

		int sqlType = rsmd.getColumnType(idx);
		int prec = rsmd.getPrecision(idx);

		final String name = columnName.toLowerCase();

		String inputType = "text";
		if (DB.isSqlTextType(sqlType))
			inputType = "text";
		else if (BeetRootDatabaseManager.getInstance().isOracleDb() && DB.isSqlNumberType(sqlType) && prec == 1) // oracle special case
			inputType = "checkbox";
		else if (DB.isSqlNumberType(sqlType))
			inputType = "number";
		else if (sqlType == Types.TIMESTAMP)
			inputType = "datetime-local"; //HTML 5
		else if (sqlType == Types.DATE)
			inputType = "date";
		else if (sqlType == Types.TIME)
			inputType = "time";
		else if (DB.isSqlBooelanType(sqlType))
			inputType = "checkbox";

		// Overwrite if matching
		if (name.equals("password"))
			inputType = "password";
		else if (name.equals("email"))
			inputType = "email";
		else if (name.equals("phone") || name.equals("phonenumber") || name.equals("tel") || name.equals("telephone"))
			inputType = "tel";

		// User mappings if any, overwrite if matching
		final String userMapping = BeetRootConfigurationManager.getInstance().getHtmlInputMapType(name);
		if (userMapping != null)
			inputType = userMapping;

		return inputType;
	}

	/**
	 * Determine HTML input field pattern based on column name.
	 *
	 * @param idx db column index
	 * @param columnName db column name
	 * @return pattern
	 * @throws SQLException SQL exception
	 */
	public static String getHtmlInputPattern(int idx, String columnName) throws SQLException {

		final String name = columnName.toLowerCase();
		String pattern = null;

		if (name.equals("password"))
			pattern = REGEX_INPUT_PATTERN_PASSWORD;
		else if (name.equals("email"))
			pattern = REGEX_INPUT_PATTERN_EMAIL;
		else if (name.equals("phone") || name.equals("phonenumber") || name.equals("tel") || name.equals("telephone"))
			pattern = REGEX_INPUT_PATTERN_PHONE;

		// User mappings if any, overwrite if matching
		final String userMapping = BeetRootConfigurationManager.getInstance().getHtmlInputMapType(name);
		if (userMapping != null)
			pattern = BeetRootConfigurationManager.getInstance().getHtmlInputMapPattern(name);

		return pattern;
	}

	/**
	 * Pre-format values from database for HTML input values.
	 *
	 * @param databaseValueObject database value object
	 * @param sqlType SQL type, see {@link java.sql.Types}
	 * @return pre-formatted value for HTML
	 */
	public static String preFormatForHTML(Object databaseValueObject, int sqlType) {
		String preformattedVal = "";
		switch (sqlType) {
			case Types.TIMESTAMP:
				preformattedVal = Web.formatDateTimeForHTML((java.sql.Timestamp) databaseValueObject);
				break;
			case Types.DATE:
				preformattedVal = Web.formatDateForHTML((java.sql.Date) databaseValueObject);
				break;
			case Types.TIME:
				preformattedVal = Web.formatTimeForHTML((java.sql.Time) databaseValueObject);
				break;
			default:
				if (databaseValueObject != null)
					preformattedVal = databaseValueObject.toString().trim();
				break;
		}
		return preformattedVal;
	}

	/**
	 * Format SQL date.
	 *
	 * @param sqlDate SQL date
	 * @return formatted string for HTML input value
	 */
    protected static String formatDateForHTML(java.sql.Date sqlDate) {
        final LocalDate localDate = sqlDate.toLocalDate();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return localDate.format(formatter); // Format as YYYY-MM-DD
    }

	/**
	 * Format SQL time.
	 *
	 * @param sqlTime SQL time
	 * @return formatted string for HTML input value
	 */
    protected static String formatTimeForHTML(java.sql.Time sqlTime) {
    	final LocalTime localTime = sqlTime.toLocalTime();
    	final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return localTime.format(formatter); // Format as HH:MM
    }

	/**
	 * Format SQL time-stamp.
	 *
	 * @param sqlTimestamp SQL time-stamp
	 * @return formatted string for HTML input value
	 */
    protected static String formatDateTimeForHTML(java.sql.Timestamp sqlTimestamp) {
    	final LocalDateTime localDateTime = sqlTimestamp.toLocalDateTime();
    	final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return localDateTime.format(formatter); // Format as YYYY-MM-DDTHH:MM
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
            if (urlAddress.startsWith("https")) {
            	final HttpsURLConnection sconnection = (HttpsURLConnection) url.openConnection();
            	/**
            	 * Host-name verification when an SSL/HTTPS certificate is used?
            	 * Usually with self-signed certificates and on localhost this is
            	 * turned off, because the verification doesn't work.
            	 */
                if (BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_COM_HOSTNAME_VERIFY))
                	sconnection.setHostnameVerifier(new DefaultHostnameVerifier());
                else
                	sconnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            	final SSLSocketFactory factory = SSL.makeSSLSocketFactory();
            	sconnection.setSSLSocketFactory(factory);
            	connection = sconnection;
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod("GET");
            final int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        } finally {
        	if (connection != null)
        		connection.disconnect();
		}
	}

}
