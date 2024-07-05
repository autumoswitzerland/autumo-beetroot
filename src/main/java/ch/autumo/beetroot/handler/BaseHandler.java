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
package ch.autumo.beetroot.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;
import org.nanohttpd.router.RouterNanoHTTPD.UriResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.cache.FileCache;
import ch.autumo.beetroot.cache.FileCacheManager;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.users.LogoutHandler;
import ch.autumo.beetroot.handler.usersroles.UserRole;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.Security;
import ch.autumo.beetroot.utils.Time;
import ch.autumo.beetroot.utils.Web;
import jakarta.activation.MimeType;


/**
 * Base handler - The "Heart" of beetRoot.
 */
public abstract class BaseHandler extends DefaultHandler implements Handler {
	
	private final static Logger LOG = LoggerFactory.getLogger(BaseHandler.class.getName());

	// Precision HTML input types
	protected static List<String> PRECISION_INPUT_TYPES = Arrays.asList(new String[] {"email", "password", "search", "tel", "text", "url"});
	
	// link reference patterns 
	private static Pattern PATTERN_HREF = Pattern.compile("href=\\\"(?!#.*)(?!\\{.*)");
	private static Pattern PATTERN_SRC = Pattern.compile("src=\\\"(?!#.*)(?!\\{.*)");
	private static Pattern PATTERN_ACTION = Pattern.compile("action=\\\"(?!#.*)(?!\\{.*)");
	private static Pattern PATTERN_LOCATION = Pattern.compile("location='(?!#.*)(?!\\{.*)");

	// link reference reverse patterns
	private static Pattern PATTERN_HREF_REV;
	private static Pattern PATTERN_SRC_REV;
	private static Pattern PATTERN_ACTION_REV;
	private static Pattern PATTERN_LOCATION_REV;
	
	// Get text patterns
	private static Pattern PATTERN_ID = Pattern.compile("\\{\\$id\\}");
	private static Pattern PATTERN_DBID = Pattern.compile("\\{\\$dbid\\}");
	private static Pattern PATTERN_DISPLAY_NAME = Pattern.compile("\\{\\$displayName\\}");
	private static Pattern PATTERN_CSRF_TOKEN = Pattern.compile("\\{\\$csrfToken\\}");
	private static Pattern PATTERN_TITLE = Pattern.compile("\\{\\$title\\}");
	private static Pattern PATTERN_USER = Pattern.compile("\\{\\$user\\}");
	private static Pattern PATTERN_USERFULL = Pattern.compile("\\{\\$userfull\\}");
	private static Pattern PATTERN_LANG = Pattern.compile("\\{\\$lang\\}");
	private static Pattern PATTERN_THEME = Pattern.compile("\\{\\$theme\\}");
	private static Pattern PATTERN_ANTITHEME = Pattern.compile("\\{\\$antitheme\\}");
	
	// sub-resource patterns
	private static Pattern PATTERN_REDIRECT_INDEX = Pattern.compile("\\{\\$redirectIndex\\}");
	//private static Pattern PATTERN_CHECK_BOX_LOGIC = Pattern.compile("\\{\\$checkBoxLogic\\}");
	private static Pattern PATTERN_SEVERITY = Pattern.compile("\\{\\$severity\\}");
	private static Pattern PATTERN_MESSAGE = Pattern.compile("\\{\\$message\\}");
	private static Pattern PATTERN_USERINFO = Pattern.compile("\\{\\$userinfo\\}");
	private static Pattern PATTERN_USERLINK = Pattern.compile("\\{\\$userlink\\}");
	private static Pattern PATTERN_LANG_MENU_ENTRIES = Pattern.compile("\\{\\$lang_menu_entries\\}");
	//private static Pattern PATTERN_ADMIN_MENU = Pattern.compile("\\{\\$adminmenu\\}");
	//private static Pattern PATTERN_LOGIN_OR_LOGOUT = Pattern.compile("\\{\\$loginorlogout\\}");
	
	// Additional patterns
	private static Pattern PATTERN_SEMICOLON = Pattern.compile(";");
	private static Pattern PATTERN_COLON = Pattern.compile(":");
	private static Pattern PATTERN_RIGHT_CURLY_BRACKET = Pattern.compile("}");
	private static Pattern PATTERN_SPACE = Pattern.compile(" ");
	
	public static final int MSG_TYPE_INFO = 0;
	public static final int MSG_TYPE_WARN = 1;
	public static final int MSG_TYPE_ERR = -1;
	
	private StringBuffer buffer = new StringBuffer();
	
	protected TreeMap<Integer, String> columns = null;
	protected Map<String, String> initialValues = null;
	protected String uniqueFields[] = null;
	protected List<String> transientFields = new ArrayList<String>();
	
	protected Entity emptyBean = null;
	
	private String displayNameValue = null;
	
	private int currentEntityDbId = -1;
	
	protected String entity = null;
	protected String action = null;
	protected String htmlHead = "";
	protected String htmlData = "";
	
	protected boolean insertServletNameInTemplateRefs = false;
	protected String servletName = null;
	
	private String successMessage = null;
	private String warningMessage = null;
	private String errorMessage = null;
	
	private int messageType = MSG_TYPE_INFO;
	
	private BeetRootHTTPSession currentSession = null;
	
	private StringBuffer checkBoxLogic = new StringBuffer();

	private IfSectionHandler ish = null;
	
	private boolean redirectedMarker = false;
	//private boolean loginMarker = false;

	// start time
	private long baseHandlerStart = 0;
	// measure duration time?
	@SuppressWarnings("unused")
	private boolean measureDuration = false;
	
	
	/**
	 * Base Handler.
	 */
	public BaseHandler() {
	}

	/**
	 * Base Handler.
	 * 
	 * @param entity entity, plural & lower-case; e.g. 'roles, users or properties'
	 */
	public BaseHandler(String entity) {
		this.entity = entity;
	}
	
	
	/**
	 * Every handler MUST be initialized!
	 * 
	 * @param session session
	 */
	public void initialize(BeetRootHTTPSession session) {
		
		// if (measureDuration) baseHandlerStart  = System.currentTimeMillis();
		
		// IF section handler
		ish = new IfSectionHandler(this);
		
		servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
		
		if (servletName != null && servletName.length() != 0) {
			
			insertServletNameInTemplateRefs = true;
			
			PATTERN_HREF_REV = Pattern.compile("href=\\\"/"+servletName+"http");
			PATTERN_SRC_REV = Pattern.compile("src=\\\"/"+servletName+"http");
			PATTERN_ACTION_REV = Pattern.compile("action=\\\"/"+servletName+"http");
			PATTERN_LOCATION_REV = Pattern.compile("location='/"+servletName+"http");
		}
		
		// nothing to do!
		if (entity == null || entity.length() == 0)
			return;
		
		// Create an empty bean to access static information generated by PLANT if necessary, e.g. foreign bean classes
		final Class<?> entityClass = this.getBeanClass();
		if (entityClass != null)
			try {
				this.emptyBean = Beans.createBean(entityClass);
			} catch (Exception e) {
				LOG.error("Couldn't create empty bean, this might be an error!", e);
			}
		
		// nothing to do!
		if (this.hasNoColumnsConfig())
			return;
		
		this.columns = new TreeMap<Integer, String>();
		this.initialValues = new HashMap<String, String>();
		
		final List<String> fallBackList = new ArrayList<String>();
		
		Session userSession = SessionManager.getInstance().findOrCreate(session);
		String res = null;
		
		// Special case JSON: overwrite languages, not needed!
		if (session.getUri().endsWith(Constants.JSON_EXT)) {
			res = "web/html/"+entity+"/columns.cfg";
		} else {
			if (userSession == null)
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+"/columns.cfg", Web.normalizeUri(session.getUri()));
			else
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+"/columns.cfg", userSession);
		}
		
    	FileCache fc = null;
    	String filePath = null;
    	boolean tryFurther = false;
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		
		try {
			if (context == null)
				fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + res);
			else
				fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + res);
		} catch (IOException e) {
			LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
			try {
				fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		
		if (tryFurther) {
			tryFurther = false;
			LOG.trace("Resource '" + res + "' doesn't exist, trying with default language '"+LanguageManager.DEFAULT_LANG+"'!");
			if (!session.getUri().endsWith(Constants.JSON_EXT)) {
				if (userSession == null)
					res = LanguageManager.getInstance().getResource("web/html/"+LanguageManager.DEFAULT_LANG+"/"+entity+"/columns.cfg", Web.normalizeUri(session.getUri()));
				else
					res = LanguageManager.getInstance().getResource("web/html/"+LanguageManager.DEFAULT_LANG+"/"+entity+"/columns.cfg", userSession);
			}
			try {
				if (context == null)
					fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + res);
				else
					fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + res);
			} catch (IOException e) {
				LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
				try {
					fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}			
				
			if (tryFurther) {
				tryFurther = false;
				LOG.trace("Resource '"+res+"' doesn't exist, trying with NO language!");
				res = LanguageManager.getInstance().getResourceWithoutLang("web/html/"+entity+"/columns.cfg", Web.normalizeUri(session.getUri()));
				try {
					if (context == null)
						fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + res);
					else
						fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + res);
				} catch (IOException e) {
					LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
					} catch (IOException e1) {
						LOG.debug("Resource '"+res+"' doesn't exist, no columns used!");
						return; // !
					}
				}			
			}
		}

		
		// Get template resource
		final String templateResource = getResource();
		
		// Define action
		if (templateResource != null && templateResource.length() != 0) {
    		final String htmlAction = templateResource.substring(templateResource.lastIndexOf("/") + 1, templateResource.length());
    		switch (htmlAction) {
				case "index.html":
					action = "index";
					break;
				case "view.html":
					action = "view";
					break;
				case "add.html":
					action = "add";
					break;
				case "edit.html":
					action = "edit";
					break;
				default:
					action = "null";
					break;
			}
		} else {
			action = "null";
		}
    		
		
		BufferedReader br = null;
		try {
			if (fc.isCached())
				br = new BufferedReader(new StringReader(fc.getTextData()));
			else
				br = new BufferedReader(new InputStreamReader(fc.getData(), fc.getEncoding()));
			
		    String line;
		    int l = 0;
		    
		    LOOP: while ((line = br.readLine()) != null) {
		    	
		    	//line = line.replaceAll("\n", "");
		    	final String cfgLine = line.trim();
		    	if (cfgLine.length() != 0 && cfgLine.indexOf("=") != -1) {
		    		
		    		String configPair[] = cfgLine.split("=");
		    		configPair[0] = configPair[0].trim();
		    		if (configPair.length > 1)
		    			configPair[1] = configPair[1].trim();
		    		else {
		    			configPair = new String[] {configPair[0], null};
		    		}
		    		
		    		String newCfgLine = null;
		    		boolean added = false;
		    		
		    		// First check for special configurations
		    		// unique
		    		if (configPair[0].equals("unique")) {
		    			if (configPair[1] == null || configPair[1].equals(""))
			    			continue LOOP;
		    			configPair[1].replace(" ", "");
		    			uniqueFields = configPair[1].split(",");
		    			continue LOOP;
		    		}
		    		// transient (not store in DB)
		    		if (configPair[0].equals("transient")) {
		    			if (configPair[1] == null || configPair[1].equals(""))
			    			continue LOOP;
		    			configPair[1].replace(" ", "");
		    			transientFields =  Arrays.asList(configPair[1].split(","));
		    			continue LOOP;
		    		}
		    		
		    		// If no config prefix is defined, all config lines are taken!
		    		// This makes sense if one doesn't need to distinguish between
		    		// list and single records data field names aka GUI column names.
		    		if (templateResource != null && templateResource.length() != 0) {
		    			
			    		final String htmlAction = templateResource.substring(templateResource.lastIndexOf("/") + 1, templateResource.length());
			    		switch (htmlAction) {

			    			case "index.json":
			    				
				    			if (configPair[0].startsWith("list_json.")) {
				    				
				    				newCfgLine = configPair[0].substring(10, configPair[0].length()) + "=" + configPair[1];
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
				    			
			    			case "index.html":
			    				
				    			if (configPair[0].startsWith("list.")) {
				    				
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + configPair[1];
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
				    			
			    			case "view.html":
					    		
				    			if (configPair[0].startsWith("view.")) {
				    				
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + configPair[1];
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
				    			
			    			case "edit.html":
				    			
				    			if (configPair[0].startsWith("edit.")) {
				    				
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + configPair[1];
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
				    			
			    			case "add.html":
			    		
				    			if (configPair[0].startsWith("add.")) {
				    				
				    				newCfgLine = configPair[0].substring(4, configPair[0].length()) + "=" + configPair[1];
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    		    	
				    			} else if (configPair[0].startsWith("init.")) { // initial values set in add template :)
				    				
				    		    	initialValues.put(configPair[0].substring(5, configPair[0].length()), configPair[1]);
				    		    	added = true;
				    			}
				    			break;
				    			
			    			default:
			    		    	columns.put(Integer.valueOf(++l), cfgLine.trim());
			    		    	added = true;
			    		    	break;
			    		}
			    		
			    		if (!added)
			    			fallBackList.add(cfgLine.trim());
		    		}
		    		
		    	}
		    }
		    
		    if (uniqueFields == null)
		    	uniqueFields = new String[] {};
		    if (transientFields == null)
		    	transientFields = new ArrayList<String>();
		    
		    // no prefixes have been used -> column config is valid for all handlers! 
		    if (columns.size() == 0) {
		    	for (Iterator<String> iterator = fallBackList.iterator(); iterator.hasNext();) {
					final String newCfgLine = (String) iterator.next();
    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				}
		    }
		    
		} catch (Exception e) {
			
			// Not good !
			
			LOG.error("Couldn't read columns for entity '"+entity+"' from file '" + fc.getFullPath() + "'!\n"
					+ "Create this file and add such a line for every column you want to show:\n"
					+ "columnName=Name of Column on Web Page", e);
			
			//OSUtils.fatalExit();
			
		} finally {
			
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LOG.warn("Couldn't close file reader for resource '" + fc.getFullPath() + "'!", e);
				}
			}
		}	
	}
	
	/**
	 * Get bean entity class that has been generated trough PLANT, 
	 * overwritten or null.
	 * 
	 * @return bean entity class
	 */
	public Class<?> getBeanClass() {
		return null;
	}

	/**
	 * Get an ampty bean of the entity type that is processed in this handler.
	 * Can be used to access static information that has been created by PLANT.
	 * (E.g. foreign related classes).
	 * 
	 * @return entity bean
	 */
	public final Entity getEmptyBean() {
		return this.emptyBean;
	}
	
	/**
	 * Get set message type, default it is info/success type.
	 * 
	 * @return message type
	 */
	public int getMessageType() {
		return messageType;
	}

	/**
	 * Set message type. Overwrites any message type set before, e.g.
	 * adding a message through constructor. But the original message 
	 * is kept if any available.
	 * 
	 * @param messageType message type
	 */
	public void setMessageType(int messageType) {
		
		String origMsg = null;
		if (hasSuccessMessage()) {
			origMsg = successMessage;
			successMessage = null;
		}
		if (hasWarningMessage()) {
			origMsg = warningMessage;
			warningMessage = null;
		}
		if (hasErrorMessage()) {
			origMsg = errorMessage;
			errorMessage = null;
		}
		
		switch (messageType) {
			case MSG_TYPE_INFO: addSuccessMessage(origMsg); break; 
			case MSG_TYPE_WARN: addWarningMessage(origMsg); break; 
			case MSG_TYPE_ERR: addErrorMessage(origMsg); break; 
		}
		
		this.messageType = messageType;
	}

	/**
	 * Get columns map. Entries:
	 * 	'[1] [colName1=GUI Col Name 1]'.
	 * 	'[2] [colName2=GUI Col Name 2]'.
	 *  ...
	 * 
	 * @return colum map
	 */
	public TreeMap<Integer, String> columns() {
		return columns;
	}

	/**
	 * Get unique fields.
	 * 
	 * @return unique fields
	 */
	public String[] uniqueFields() {
		return uniqueFields;
	}
	
	/**
	 * Amount of columns,
	 * 
	 * @return amount of columns
	 */
	public int columnsSize() {
		return columns.size();
	}
	
	/**
	 * Access column values. '[1] [colName=GUI Col Name 1]',
	 * index starts with 1 in 'columns.cfg'!
	 * 
	 * @param idx columns index from 'columns.cfg'
	 * @return column values
	 */
	public String[] getColumn(int idx) {
		
		final String cfgLine = columns.get(Integer.valueOf(idx));
		final String params[] = cfgLine.split("=");
		final String colName = params[0].trim();
		String guiColName = params[1];
		
		// We don't want this! We leave full control to 'columns.cfg'
		//guiColName = TextUtils.escapeHtml(guiColName);
		
		return new String[] {colName, guiColName};
	}
	
	/**
	 * Get SQL fields, e.g. 'col1, col2, col3' for queries (seleczs).
	 * 
	 * @return SQL query fields
	 */
	public String getColumnsForSql() {
		return this.getColumnsForSql(false);
	}
	
	/**
	 * Get SQL fields, e.g. 'col1, col2, col3'.
	 * 
	 * @param forModification true for updates/inserts
	 * @return SQL query fields
	 */
	public String getColumnsForSql(boolean forModification) {

		String queryfields = "";
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {
			
			final String cfgLine = columns.get(Integer.valueOf(i));
			final String params[] = cfgLine.split("=");
			final String colName = params[0].trim();
			
			if (transientFields.contains(colName)) {
				if (!forModification) {
					if (columns.size() == i)
						queryfields += "'TRANSIENT'";
					else
						queryfields += "'TRANSIENT', ";
				}
				continue LOOP;
			}
			
			if (columns.size() == i)
				queryfields += colName;
			else
				queryfields += colName + ", ";
		}
		
		// Can happen if we have a transient field at the end!
		if (queryfields.endsWith(", "))
			queryfields = queryfields.substring(0, queryfields.length() - 2);
		
		return queryfields;
	}	
	
	/**
	 * Get transient fields
	 * @return transient fields
	 */
	public List<String> getTransientFields() {
		return this.transientFields;
	}
	
	/**
	 * Get SQL insert values.
	 * 
	 * @param session HTTP session
	 * @return SQL insert values
	 * @throws Exception exception
	 */
	public String getInsertValues(BeetRootHTTPSession session) throws Exception {
		
		final boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
		
		String clause = "";
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {
			
			final String col[] = getColumn(i);

			if (transientFields.contains(col[0]))
				continue LOOP;
			
			String val = session.getParms().get(col[0]);		
			
			val = DB.escapeValuesForDb(val);
			
			if (dbPwEnc && col[0].equalsIgnoreCase("password")) {
				val = Security.hashPw(val);
			}

			// Informix wants 't' or 'f'
			if (val.equalsIgnoreCase("true")) {
				val = "1";
			}
			if (val.equalsIgnoreCase("false")) {
				val = "0";
			}
			
			// if there's really a column in the GUI that is mapped 
			// to the db column 'created', overwrite it!
			if (col[1].equalsIgnoreCase("created")) {
				if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
					if (columns.size() == i)
						clause += Time.nowTimeStamp();
					else
						clause += Time.nowTimeStamp() + ", ";
				} else {
					if (columns.size() == i)
						clause += "'" + Time.nowTimeStamp() + "'";
					else
						clause += "'" + Time.nowTimeStamp() + "', ";
				}
				// continue here with for-loop. otherwise we would get errors!
				continue LOOP;
			}
			
			val = this.formatSingleValueForDB(session, val, col[0]);
			
			if (columns.size() == i)
				clause += "'"+val+"'";
			else
				clause += "'"+val+"', ";
		}
		
		// Can happen if we have a transient field at the end!
		if (clause.endsWith(", "))
			clause = clause.substring(0, clause.length() - 2);

		return clause;
	}

	/**
	 * Get SQL update set clause. Passwords will NOT be updated!
	 * 
	 * @param session HTTP session
	 * @return SQL update clause
	 * @throws Exception exception
	 */
	public String getUpdateSetClause(BeetRootHTTPSession session) throws Exception {
		return this.getUpdateSetClause(session, null);
	}
	
	/**
	 * Get SQL update set clause. Passwords will NOT be updated!
	 * 
	 * @param session HTTP session
	 * @param onOffMapName name of on/off value map if any, otherwise null.
	 * @return SQL update clause
	 * @throws Exception exception
	 */
	public String getUpdateSetClause(BeetRootHTTPSession session, String onOffMapName) throws Exception {

		//final boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
		final boolean dbAutoMod = BeetRootConfigurationManager.getInstance().getYesOrNo("db_auto_update_modified");
		
		String clause = "";
		
		final Session userSession = session.getUserSession();
		final boolean currentUser = this.isCurrentUserUpdate(session);
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {

			final String col[] = getColumn(i);
			
			if (transientFields.contains(col[0]))
				continue LOOP;

			if (col[0].equalsIgnoreCase("password")) // passwords are not allowed to be updated!
				continue LOOP;
			
			
			String val = session.getParms().get(col[0]);
			val = DB.escapeValuesForDb(val);
			
			/*
			if (dbPwEnc && col[0].equals("password")) {
				val = Utils.encode(val, SecureApplicationHolder.getInstance().getSecApp());
			}
			*/
			
			// Informix wants 't' or 'f'
			if (val.equalsIgnoreCase("true")) {
				if (onOffMapName != null) {
					final String exists = (String) session.getUserSession().getMapValue(onOffMapName, col[0]);
					if (exists != null)
						val = Constants.ON;
					else
						val = "1";
				} else {
					val = "1";
				}
			}
			if (val.equalsIgnoreCase("false")) {
				if (onOffMapName != null) {
					final String exists = (String) session.getUserSession().getMapValue(onOffMapName, col[0]);
					if (exists != null)
						val = Constants.OFF;
					else
						val = "0";
				} else {
					val = "0";
				}
			}

			// Only the logged in user must be updated with new session data if data is changed
			if (currentUser) {
				if (col[0].equals("username")) {
					userSession.set("username", val);
				}
				if (col[0].equals("role")) {
					userSession.set("userrole", val);
				}
				if (col[0].equals("firstname")) {
					userSession.set("firstname", val);
				}
				if (col[0].equals("lastname")) {
					userSession.set("lastname", val);
				}
				if (col[0].equals("email")) {
					userSession.set("email", val);
				}
				if (col[0].equals("two_fa")) {
					userSession.set("two_fa", val);
				}
			}
			
			val = this.formatSingleValueForDB(session, val, col[0]);
			
			if (columns.size() == i)
				clause += col[0] + "='"+val+"'";
			else
				clause += col[0] + "='"+val+"', ";
		}

		// Can happen if we have a transient field at the end!
		if (clause.endsWith(", "))
			clause = clause.substring(0, clause.length() - 2);
		
		// Doesn't matter, if 'modified' is configured in 'colums.cfg' or not
		// And we assume the column exists as specified by design!
		// But we don't update it, if the user chooses to modify it by himself (GUI).
		if (dbAutoMod && clause.indexOf("modified=") != 1 ) {
			if (BeetRootDatabaseManager.getInstance().isOracleDb())
				clause += ", modified=" + Time.nowTimeStamp() + "";
			else
				clause += ", modified='" + Time.nowTimeStamp() + "'";
		}
		
		return clause;
	}

	/**
	 * Is the current user being updated?
	 * 
	 * @param session HTTP session
	 * @return true is so
	 */
	protected boolean isCurrentUserUpdate(BeetRootHTTPSession session) {
		Session userSession = session.getUserSession();
		int uid = userSession.getUserId();
		int origId = -1; 
		if (entity.equals("users")) { // we have a user entity in process
			final String obfSessId = session.getParms().get("id");
			origId = userSession.getOrigId(obfSessId, getEntity());
			if (origId == uid)
				return true;
		}
		return false;
	}
	
	/**
	 * Check if unique fields are unique.
	 * 
	 * @param session sessions
	 * @param preSql pre-parsed SQL without unique fields
	 * @param operation saved or updated
	 * @return response or null, null means success, response's status must be checked!
	 * @throws Exception exception
	 */
	public HandlerResponse uniqueTest(BeetRootHTTPSession session, String preSql, String operation) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		
		try {

			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// Unique fields test!
			if (uniqueFields.length != 0) {
				
				String uniqueText = "";
				boolean foundOneAtLeast = false;
				final List<String> foundPairs = new ArrayList<String>();
				
				for (int i = 0; i < uniqueFields.length; i++) {
					
					String stmtStr = preSql;
				
					String val = session.getParms().get(uniqueFields[i]);
					stmtStr += uniqueFields[i] + "='"+val+"'";
					//NO SEMICOLON
					//stmtStr += ";";
					stmt = conn.createStatement();
					// we only need the result set for the column meta data
					stmt.setFetchSize(1);
					set = stmt.executeQuery(stmtStr);
					
					boolean found = set.next();
					
					set.close();
					stmt.close();
					
					if (found) {
						foundOneAtLeast = true;
						foundPairs.add(uniqueFields[i] + "='"+val+"'");
					}
				}
				
				if (foundOneAtLeast) {
					
					conn.close();
	
					int i = 1;
					for (Iterator<String> iterator = foundPairs.iterator(); iterator.hasNext();) {
						String fp = (String) iterator.next();
						if (i == foundPairs.size())
							uniqueText +=  fp;
						else
							uniqueText +=  fp+"', ";
						i++;
					}
					
					// We have at least one with the same values!
					LOG.info("Found "+getEntity()+" with same unique value(s) " + uniqueText + "! Not "+operation+" the record.");
					return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.unique", userSession, getEntity(), uniqueText));
				}
			}
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return null; // ok !
	}
	
	/**
	 * Refresh user roles and permissions for current user.
	 * 
	 * @param currentUserId current user id of the currently logged-in user
	 * @param session HTTP session
	 */
	protected final void refreshUserRoles(int currentUserId, BeetRootHTTPSession session) {
		String roles = "";
		String permissions = "";
		
		final List<Model> usersRoles = UserRole.where(UserRole.class, "user_id = ?", Integer.valueOf(currentUserId));
		for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
			final UserRole userRole = (UserRole) iterator.next();
			final Role role = (Role) Role.read(Role.class, userRole.getRoleId());
			roles += role.getName()+",";
			permissions += role.getPermissions()+",";
		}
		if (usersRoles.size() > 0) {
			roles = roles.substring(0, roles.length() - 1);
			if (permissions.endsWith(","))
				permissions = permissions.substring(0, permissions.length() - 1);
		}
		
		session.getUserSession().set("userroles", roles.replaceAll("\\s", "").toLowerCase());
		session.getUserSession().set("userpermissions", permissions.replaceAll("\\s", "").toLowerCase());		
	}
	
	/**
	 * Process JSON templates.
	 * 
	 * @param session beetRoot session
	 * @return result
	 */
	private String processJSON(BeetRootHTTPSession session) {
		
		String currRessource =  this.getResource();
		// this is no web-page call, so we have to get language from URL if any 
		currRessource = LanguageManager.getInstance().getResource(currRessource, session.getUri());		

		// prepare text buffer
		final StringBuffer sb = new StringBuffer();
		
		// process JSON templates
		Scanner sc = null;
		try {
			sc = getNewScanner(currRessource);
			while (sc.hasNextLine()) {
				
				String text = sc.nextLine();
				
				//text = this.preParse(text, session);
				
				// template specific variables
				final String res = this.replaceTemplateVariables(text, session);
				if (res != null && res.length() != 0)
					text = res;

				sb.append(text + "\n");
				
				parseTemplateData(sb, "{$data}");
				parsePaginator(sb, "{$paginator}", session);
			}
			
		} catch (FileNotFoundException e) {
			
			final String err = "Web resource '" + currRessource + "' not found!";
			LOG.error(err, e);
			return "NOTFOUND:" + currRessource;
			
		} catch (Exception ex) {
			
			final String err = "Web resource '" + currRessource + "' parsing error!";
			LOG.error(err, ex);
			return "PARERROR:" + currRessource + ":" + ex.getMessage();
			
		} finally {
			if (sc!= null)
				sc.close();
		}
		
		return sb.toString();			
	}
	
	/**
	 * Process handlers to get the whole HTML page.
	 * 
	 * @param session beetRoot session
	 * @param origId original DB id
	 * @return whole parsed HTML page
	 * @throws Exception exception
	 */
	public String getText(BeetRootHTTPSession session, int origId) throws Exception {

		// Special case: JSON REST
		if (session.getUri().endsWith(Constants.JSON_EXT)) {
			return this.processJSON(session);
		}
		
		// leftover messages?
		String msg = session.getParms().get("msg");
		if (msg != null && msg.length() != 0) {
			String sev = session.getParms().get("sev");
			msg = URLDecoder.decode(msg, StandardCharsets.UTF_8.toString());
			if (sev!= null && sev.length() != 0) {
				switch (sev) {
					case "i":
						this.addSuccessMessage(msg);
						break;
					case "w":
						this.addWarningMessage(msg);
						break;
					case "e":
						this.addErrorMessage(msg);
						break;
					default:
						this.addWarningMessage(msg);
				}
			} else 
				this.addWarningMessage(msg);
		}

		final Session userSession = session.getUserSession();
		String lang = LanguageManager.getInstance().getLanguage(userSession);
		String user = userSession.getUserName();
		if (user != null && user.indexOf("$") > 0)
			user = user.replace("$", "\\$");
		String userfull = userSession.getUserFullNameOrUserName();
		if (userfull != null && userfull.indexOf("$") > 0)
			userfull = userfull.replace("$", "\\$");
		String currRessource = LanguageManager.getInstance().getBlockResource(this.getLayout(userSession), userSession);
		String templateResource = getResource();
		
		// prepare text buffer
		final StringBuffer sb = new StringBuffer();
		
		// process templates
		Scanner sc = null;
		try {
			
			sc = getNewScanner(currRessource);

			LOOP: while (sc.hasNextLine()) {
				
				String text = sc.nextLine();
				
				// Remove lines?
				if(ish.continueRemoval(text, userSession, "overall"))
					continue LOOP;
				
				// layout templates and main template
				if (text.contains("{#head}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/head.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#head}", session, origId);
					
				} else if (text.contains("{#header}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/header.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#header}", session, origId);

				} else if (text.contains("{#langmenu}")) {
					
					if (this.showMenu(userSession)) {
						
						if (LanguageManager.getInstance().getConfiguredLanguages().length > 1) {
						
							currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/lang_menu.html", userSession);
							text = parseAndGetSubResource(text, currRessource, "{#langmenu}", session, origId);
							
						} else {
							text = "";
						}
						
					} else {
						text = "";
					}

				} else if (text.contains("{#menu}")) {
					
					if (this.showMenu(userSession)) {
						
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/menu.html", userSession);
						text = parseAndGetSubResource(text, currRessource, "{#menu}", session, origId);
						
					} else {
						text = "";
					}

				} else if (text.contains("{#message}")) {
					
					if (this.hasAnyMessage()) {
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/message.html", userSession);
						text = parseAndGetSubResource(text, currRessource, "{#message}", session, origId);
					} else {
						text = "";
					}

				} else if (text.contains("{#template}")) {
				
					try {
						
						this.createTemplateContent(userSession, session);
						
						if (templateResource.endsWith("index.html")) {
							parseTemplateHead(buffer, "{$head}");
							parsePaginator(buffer, "{$paginator}", session); // if any, only index!
						}
						if (templateResource.endsWith("search.html")) {
							parseTemplateHead(buffer, "{$head}");
						}
						
						parseTemplateData(buffer, "{$data}");
						
						text = text.replace("{#template}", buffer.toString());

						// Provide a link to current logged in user for every template!
						if (text.contains("{$userlink}")) {
							final Integer uid = userSession.getUserId();
							if (uid != null) {
								String usid = userSession.getModifyId(uid.intValue(), "users");
								if (usid == null)
									userSession.createIdPair(uid, "users");
								text = PATTERN_USERLINK.matcher(text).replaceFirst(
										"/"+lang+"/users/view?id=" + userSession.getModifyId(uid, "users"));
							} else {
								text = PATTERN_USERLINK.matcher(text).replaceFirst("#");
							}
						}
						
						if (text.contains("{$csrfToken}")) {
							
							if (userSession != null) {
								
								final String formCsrfToken = userSession.getFormCsrfToken();
								
								if (formCsrfToken != null && formCsrfToken.length() != 0)
									text = PATTERN_CSRF_TOKEN.matcher(text).replaceAll(formCsrfToken);
							}
						}
						
						if (templateResource.endsWith("view.html") || 
							templateResource.endsWith("edit.html") || 
							templateResource.endsWith("add.html")) {
							
							// here the id's are written !
							
							// obfuscate it!
							String modifyID = userSession.getModifyId(origId, getEntity());
							if (modifyID == null) {
								userSession.createIdPair(origId, getEntity());
							}
							text = PATTERN_ID.matcher(text).replaceAll("" + modifyID);
							text = PATTERN_DBID.matcher(text).replaceAll("" + origId);

							if (displayNameValue != null)
								text = PATTERN_DISPLAY_NAME.matcher(text).replaceAll(displayNameValue);
							else
								text = PATTERN_DISPLAY_NAME.matcher(text).replaceAll(""+origId);
						}
						
						// template specific variables
						final String res = this.replaceTemplateVariables(text, session);
						if (res != null && res.length() != 0)
							text = res;
						
						buffer.delete(0, buffer.length()); // buffer consumed!						
						
					} catch (Exception e) {
						
						final String err = "Error Parsing Template! - Exception:\n"+e.getMessage();
						LOG.error(err, e);
						return "PARERROR:" + currRessource + ":" + e.getMessage();
					}
					
				} else if (text.contains("{#footer}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/footer.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#footer}", session, origId);
					
				} else if (text.contains("{#script}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/script.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#script}", session, origId);
					
				}
					
				
				// General variables!
				
				// title
				if (text.contains("{$title}"))
					text = PATTERN_TITLE.matcher(text).replaceAll(this.getTitle(userSession));

				// user
				if (user != null && text.contains("{$user}")) {
					text = PATTERN_USER.matcher(text).replaceAll(user);
				}

				// user full
				if (userfull != null && text.contains("{$userfull}")) {
					text = PATTERN_USERFULL.matcher(text).replaceAll(userfull);
				}
				
				// language
				if (text.contains("{$lang}")) {
					text = PATTERN_LANG.matcher(text).replaceAll(lang);
				}				
				
				
				// User settings variables!
				
				// theme
				if (text.contains("{$theme}")) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = PATTERN_THEME.matcher(text).replaceAll("dark");
					else
						text = PATTERN_THEME.matcher(text).replaceAll(theme);
				}				
				if (text.contains("{$antitheme}")) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = PATTERN_ANTITHEME.matcher(text).replaceAll("default");
					else
						if (theme.equals("default"))
							text = PATTERN_ANTITHEME.matcher(text).replaceAll("dark");
						else
							text = PATTERN_ANTITHEME.matcher(text).replaceAll("default");
				}
				
				
				// replace further overall variables!
				String resRepl = this.replaceVariables(text, session);
				if (resRepl != null && resRepl.length() > 0)
					text = resRepl;

				// replace template language translations if any
				text = this.replaceLanguageVariables(text, session);
				
				
				// Add servlet URL part.
				// - href="
				// - src="
				// - action="
				// - location='
				if (insertServletNameInTemplateRefs) {
					// repeat only the following part:
					text = PATTERN_HREF.matcher(text).replaceAll("href=\"/"+servletName);
					text = PATTERN_SRC.matcher(text).replaceAll("src=\"/"+servletName);
					text = PATTERN_ACTION.matcher(text).replaceAll("action=\"/"+servletName);
					text = PATTERN_LOCATION.matcher(text).replaceAll("location='/"+servletName);
					if (this.hasExternalLinks()) {
						// hack: we have to re-replace http and https links....
						text = PATTERN_HREF_REV.matcher(text).replaceAll("href=\"http");
						text = PATTERN_SRC_REV.matcher(text).replaceAll("src=\"http");
						text = PATTERN_ACTION_REV.matcher(text).replaceAll("action=\"http");
						text = PATTERN_LOCATION_REV.matcher(text).replaceAll("location='http");
					}
				}

				
				sb.append(text + "\n");
			}
		
		} catch (FileNotFoundException e) {
			
			final String err = "Web resource '" + currRessource + "' not found!";
			LOG.error(err, e);
			return "NOTFOUND:" + currRessource;
			
		} catch (Exception ex) {
			
			final String err = "Web resource '" + currRessource + "' parsing error!";
			LOG.error(err, ex);
			return "PARERROR:" + currRessource + ":" + ex.getMessage();
			
		} finally {
			if (sc!= null)
				sc.close();
		}
		
		return sb.toString();		
	}

	/**
	 * Register a display name value to be show where defined in template.
	 * 
	 * @param displayNameValue display name value
	 */
	public void registerDisplayField(String displayNameValue) {
		this.displayNameValue = displayNameValue;
	}
	
	/**
	 * Overwrite if you want to have a special layout 
	 * for this handler.
	 * 
	 * @param userSession user session
	 * @return layout file path; example:
	 * 			'web/html/:lang/blocks/mylayout.html'
	 */
	public String getLayout(Session userSession) {
		// default layout
		return "web/html/:lang/blocks/layout.html";
	}

	/** 
	 * Page title show left above the navigation area.
	 * If not overwritten, the entity name is shown
	 * starting with an upper-case letter!
	 * 
	 * @param userSession user session
	 * @return page title
	 */
	public String getTitle(Session userSession) {
		return getUpperCaseEntity();		
	}

	/**
	 * Overwrite if this handler serves templates with external
	 * links (starting with 'http' or 'https').
	 * 
	 * @return true if   
	 */
	public boolean hasExternalLinks() {
		return false;
	}
	
	/**
	 * Retrieve user roles for simple user role management. These roles are read from the application 
	 * configuration (beetroog.cfg -&gt; 'web_roles') and translated in the web masks if a translation 
	 * is available.
	 * 
	 * If you want to use your own user/role or ACL setup; e.g. reading roles from 
	 * your own database table, you be better off overwriting
	 * {@link #extractCustomSingleInputDiv(BeetRootHTTPSession, String, ResultSetMetaData, String, String, int)}
	 * and {@link #useExternalRoles()} = true; in this case this method isn't 
	 * called at all!
	 * 
	 * Note: The extended user roles management is activated by default, so these roles aren't used!
	 * 
	 * @return user roles
	 */
	public String[] getSimpleManagementUserRoles() {
		return BeetRootConfigurationManager.getInstance().getAppRoles();
	}
	
	/**
	 * Parse associated list.
	 * 
	 * @param snippet buffered code snippet
	 * @param list the list with the associated entities
	 * @param session beetRoot session
	 */
	protected void parseAssociatedEntities(StringBuffer snippet, List<Model> list, BeetRootHTTPSession session) {
		this.parseAssociations("{$assignedRoles}", snippet, list, session);
	}
	
	/**
	 * Parse un-associated list.
	 * 
	 * @param snippet buffered code snippet
	 * @param list the list with the un-associated entities
	 * @param session beetRoot session
	 */
	protected void parseUnassociatedEntities(StringBuffer snippet, List<Model> list, BeetRootHTTPSession session) {
		this.parseAssociations("{$unassignedRoles}", snippet, list, session);
	}
	
	private void parseAssociations(String tag, StringBuffer snippet, List<Model> list, BeetRootHTTPSession session) {
		final int idx = snippet.indexOf(tag);
		if (idx == -1)
			return;
		String txt = "";
		for (Iterator<Model> iterator = list.iterator(); iterator.hasNext();) {
			final Model model = iterator.next();
			String roleVal = model.getDisplayValue();
			if (model.modelClass().equals(Role.class))
				roleVal = LanguageManager.getInstance().translateOrDefVal("role."+roleVal, roleVal, session.getUserSession());
			txt += "<li class=\"list-group-item\" draggable=\"true\" ondragstart=\"drag(event)\" data-id=\""+model.getId()+"\">"+roleVal+"</li>\n";
		}
		snippet.replace(idx, idx + tag.length(), txt);
	}
	
	private void parseTemplateHead(StringBuffer template, String variable) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getHtmlHead());
	}
	
	private void parseTemplateData(StringBuffer template, String variable) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getHtmlData());
	}

	private void parsePaginator(StringBuffer template, String variable, BeetRootHTTPSession session) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getPaginator(session));
	}
	
	private String parseAndGetSubResource(String origText, String resource, String type, BeetRootHTTPSession session, int origId) throws FileNotFoundException {
		
		final Session userSession = session.getUserSession();
		final StringBuffer sb = new StringBuffer();
		
		String lang = LanguageManager.getInstance().getLanguage(userSession);
		String currRessource = LanguageManager.getInstance().getBlockResource(resource, userSession);		

		Scanner sc = getNewScanner(currRessource);
		
		LOOP: while (sc.hasNextLine()) {
			
			String text = sc.nextLine();
			//text = this.preParse(text, session);
			
			// Remove lines?
			if(ish.continueRemoval(text, userSession, "subresource"))
				continue LOOP;

			switch (type) {
			
				case "{#head}":

					// DONE in overal method
					//if (text.contains("{$title}"))
					//	text = text.replace("{$title}", getUpperCaseEntity());
					break;

				case "{#message}":

					if (text.contains("{$severity}")) {
						
						if (this.hasSuccessMessage())
							text = PATTERN_SEVERITY.matcher(text).replaceFirst("success");
						else if (this.hasWarningMessage())
							text = PATTERN_SEVERITY.matcher(text).replaceFirst("warning");
						else if (this.hasErrorMessage())
							text = PATTERN_SEVERITY.matcher(text).replaceFirst("error");
					}
					
					if (text.contains("{$message}")) {
						if (this.hasSuccessMessage())
							text = PATTERN_MESSAGE.matcher(text).replaceFirst(this.successMessage);
						else if (this.hasWarningMessage())
							text = PATTERN_MESSAGE.matcher(text).replaceFirst(this.warningMessage);
						else if (this.hasErrorMessage())
							text = PATTERN_MESSAGE.matcher(text).replaceFirst(this.errorMessage);
						
						this.successMessage = null;
						this.warningMessage = null;
						this.errorMessage = null;
					}
					
					break;
					
				case "{#footer}":

					if (text.contains("{$userinfo}")) {
						
						final Integer uid = userSession.getUserId();
						if (uid != null) {
							
							String usid = userSession.getModifyId(uid.intValue(), "users");
							if (usid == null) {
								userSession.createIdPair(uid, "users");
							}

							String user = userSession.getUserFullNameOrUserName();
							if (user != null && user.indexOf("$") > 0)
								user = user.replace("$", "\\$");
							text = PATTERN_USERINFO.matcher(text).replaceFirst(
									LanguageManager.getInstance().translate("base.name.user", userSession)
									+ ": <a class=\"hideprint\" href=\"/"+lang+"/users/view?id="
									+ userSession.getModifyId(uid, "users")+"\" rel=\"nofollow\">" 
									+ user + "</a> | ");
						} else {
							
							text = PATTERN_USERINFO.matcher(text).replaceFirst(" ");
						}
					}
					
					break;
				
				case "{#script}":
					
					if (text.contains("{$redirectIndex}")) {
						
						//URL rewrite
						if (redirectedMarker) {
							if (insertServletNameInTemplateRefs)
								text = PATTERN_REDIRECT_INDEX.matcher(text).replaceFirst("window.history.pushState({}, document.title, \"/"+servletName+"/"+lang+"/"+getEntity()+"/index\");");
							else
								text = PATTERN_REDIRECT_INDEX.matcher(text).replaceFirst("window.history.pushState({}, document.title, \"/"+lang+"/"+getEntity()+"/index\");");
						} else {
							text = PATTERN_REDIRECT_INDEX.matcher(text).replaceFirst(" ");
						}
						redirectedMarker = false;
					} 
					
					if (text.contains("{$checkBoxLogic}")) {
						
						if (checkBoxLogic.length() > 0) {
							text = text.replace("{$checkBoxLogic}", checkBoxLogic.toString());
							//text = PATTERN_CHECK_BOX_LOGIC.matcher(text).replaceFirst(checkBoxLogic.toString());
						} else {
							text = text.replace("{$checkBoxLogic}", " ");
							//text = PATTERN_CHECK_BOX_LOGIC.matcher(text).replaceFirst(" ");
						}
						checkBoxLogic = new StringBuffer();
					}
					
					break;
					
				case "{#langmenu}":
					
					if (text.contains("{$lang_menu_entries}")) {
						
						// We determine the right route according to the web resource path -> generic!
						// We do not language re-route with post or put retry post data, this would be a safety issue!
						String route = "home";
						final String res = this.getResource();
						if (res == null) {
							route = "home";
						} else {
							final int i = res.indexOf(":lang");
							if (i == -1) {
								route = "home"; // wrong
							} else {
								route = res.substring(i + 6);
								final int j = route.indexOf(".");
								if (j != -1) {
									route = route.substring(0, j);
								}
							}
						}
						if (origId > 0) {
							final String modifyID = userSession.getModifyId(origId, getEntity());
							route = route + "?id=" + modifyID;
						}
						
						String entries = "";
						final String langs[] = LanguageManager.getInstance().getConfiguredLanguages();
						
						for (int i = 0; i < langs.length; i++) {
							if (i+1 == langs.length) {
								entries += "<a href=\"/"+langs[i]+"/"+route+"\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".gif\" alt=\""+langs[i].toUpperCase()+"\">"+langs[i].toUpperCase()+"</a>\n";
							} else {
								entries += "<a href=\"/"+langs[i]+"/"+route+"\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".gif\" alt=\""+langs[i].toUpperCase()+"\">"+langs[i].toUpperCase()+"</a>\n";
								entries += "<hr class=\"menusep\">\n";
							}
						}
						text = PATTERN_LANG_MENU_ENTRIES.matcher(text).replaceFirst(entries);
					} else {
						text = PATTERN_LANG_MENU_ENTRIES.matcher(text).replaceFirst(" ");
					}
					
					break;
					
				case "{#menu}":
					
					final List<String> userroles = userSession.getUserRoles();

					/**
					// This is only a cosmetic precaution, menus shouldn't
					// be shown anyways without a logged-in user.
					if (userrole == null && text.contains("<a href=") || text.contains("<hr")) {
						text  = " ";
					}
					*/
					
					if (text.contains("{$adminmenu}")) {
						
						if (userroles.contains("Administrator")) {
							String adminMenu = parseAndGetSubResource(text, "web/html/:lang/blocks/adminmenu.html", "{$adminmenu}", session, origId);
							text = text.replace("{$adminmenu}", adminMenu);
							//text = PATTERN_ADMIN_MENU.matcher(text).replaceFirst(adminMenu);
						}
						else
							text = text.replace("{$adminmenu}", " ");
							//text = PATTERN_ADMIN_MENU.matcher(text).replaceFirst(" ");
					}
					
					// Show login or logout?
					if (text.contains("{$loginorlogout}")) {
						if (userroles.size() > 0)
							text = text.replace("{$loginorlogout}", "<a href=\"/{$lang}/users/logout\">"+LanguageManager.getInstance().translate("base.name.logout", userSession)+"</a>");
							//text = PATTERN_LOGIN_OR_LOGOUT.matcher(text).replaceFirst("<a href=\"/{$lang}/users/logout\">"+LanguageManager.getInstance().translate("base.name.logout", userSession)+"</a>");
						else
							text = text.replace("{$loginorlogout}", "<a href=\"/{$lang}/users/login\">"+LanguageManager.getInstance().translate("base.name.login", userSession)+"</a>");
							//text = PATTERN_LOGIN_OR_LOGOUT.matcher(text).replaceFirst("<a href=\"/{$lang}/users/login\">"+LanguageManager.getInstance().translate("base.name.login", userSession)+"</a>");
					}
					
					break;
					
				
				case "{#adminmenu}":
					// we need to to nada, just all liens should be added, that's all!
					break;
				
					
				default:
					break;
			}
			
			sb.append(text + "\n");
		}	

		sc.close();
		
		// PATTERN ?
		return origText.replace(type, sb.toString());
	}

	private void addLine(String line) {
		buffer.append(line + "\n");
	}

	private void createTemplateContent(Session userSession, BeetRootHTTPSession session) {
		Scanner sc = null;
		try {
			sc = getNewScanner(userSession);
		} catch (FileNotFoundException e) {
			final String err = "Web resource '" + getResource() + "' not found!";
			LOG.error(err, e);
			addLine("<h1>"+err+"</h1>");
		}

		LOOP: while (sc.hasNextLine()) {
			String text = sc.nextLine();
			// Remove lines?
			if(ish.continueRemoval(text, userSession, "template"))
				continue LOOP;
			//addLine(this.preParse(text, session));
			addLine(text);
		}
		sc.close();
	}	
	
	private boolean hasAnyMessage() {
		return hasSuccessMessage() || hasWarningMessage() || hasErrorMessage();
	}
	
	private boolean hasSuccessMessage() {
		return this.successMessage != null;
	}

	private boolean hasWarningMessage() {
		return this.warningMessage != null;
	}
	
	private boolean hasErrorMessage() {
		return this.errorMessage != null;
	}
	
	private String getUpperCaseEntity() {
		final String e = getEntity();
		if (e == null || e.length() == 0)
			return "";
		final String s1 = (e.charAt(0) + "").toUpperCase();
		return s1 + e.substring(1);
	}

	private Scanner getNewScanner(Session userSession) throws FileNotFoundException {
		return getNewScanner(LanguageManager.getInstance().getResource(this.getResource(), userSession));
	}
	
	/**
	 * Getting a new scanner for web a resource (HTML template) to parse.
	 * 
	 * @param resource resource string, e.g. 'web/html/:lang/&lt;entity&gt;/index.html'.
	 * @return file scanner for reading lines
	 * @throws FileNotFoundException if file is not found
	 */
	protected Scanner getNewScanner(String resource) throws FileNotFoundException {
		
    	String filePath = null;
    	FileCache fc = null;
    	boolean tryFurther = false;
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		
		try {
			if (context == null )
				fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + resource);
			else
				fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + resource);
		} catch (IOException e) {
			LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
			try {
				fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		
		if (tryFurther) {
			tryFurther = false;
			LOG.trace("Resource '"+resource+"' doesn't exist, trying default language '"+LanguageManager.DEFAULT_LANG+"'!");
			resource = LanguageManager.getInstance().getResourceByLang(this.getResource(), LanguageManager.DEFAULT_LANG);
			try {
				if (context == null )
					fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + resource);
				else
					fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + resource);
			} catch (IOException e) {
				LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
				try {
					fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}
			
			if (tryFurther) {
				tryFurther = false;
				LOG.trace("Resource '"+resource+"' doesn't exist, trying NO language!");
				resource = LanguageManager.getInstance().getResourceWithoutLang(this.getResource(), LanguageManager.DEFAULT_LANG);
				try {
					if (context == null )
						fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + resource);
					else
						fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + resource);
				} catch (IOException e) {
					LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
					} catch (IOException e1) {
						LOG.error("No resource has been found for '"+filePath+"' after trying to load it differently! "
								+ "This will lead to an exception and you quite surely missed to add this resource to you app.");
						tryFurther = false;
					}
				}				
			}			
		}		
		
		try {
			
			if (fc.isCached())
				return new Scanner(fc.getTextData());
			else 
				return new Scanner(fc.getData());
			
		} catch (IOException e) {
			throw new FileNotFoundException("File/resource '"+fc.getFullPath()+"' not found! Exception: " + e.getMessage());
		}				
	}
	
	/**
	 * Read snippet resource; e.g., 'web/html/:lang/users/snippets/roles.html'.
	 * 
	 * @param resource resource file
	 * @param userSession user session
	 * @return buffer with resource
	 * @throws FileNotFoundException if file is not found
	 */
	protected StringBuffer readSnippetResource(String resource, Session userSession) throws FileNotFoundException {
		final StringBuffer sb = new StringBuffer();
		final String res = LanguageManager.getInstance().getResource(resource, userSession);
		final Scanner sc = this.getNewScanner(res);
		while (sc.hasNextLine()) {
			sb.append(sc.nextLine() + "\n");
		}
		return sb;
	}
	
	/**
	 * Return size of initial value list for add template.
	 * 
	 * @return initial values size
	 */
	protected int initValuesSize() {
		return initialValues.size();
	}
	
	/**
	 * Holds initial add values from 'columns.cfg'.
	 * Example
	 * 
	 * init.colName1=0
	 * init.colName2=Change this!
	 * 
	 * @param colName column name
	 * @return initial value for add template
	 */
	protected String initialValue(String colName) {
		String initVal = (String) initialValues.get(colName);
		if (initVal == null)
			initVal ="";
		
		return initVal;
	}
	
	@Override
	public IStatus getStatus() {
		return Status.OK;
	}

	@Override
	public String getMimeType() {
		return "text/html";
	}
	
	@Override
	public final String getText() {
		return null;
	}

	@Override
	public final InputStream getData() {
		return null;
	}
	
	/**
	 * MAIN method for base handler.
	 */
	@Override
    public final Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		
		this.currentSession = (BeetRootHTTPSession)session;
		
		// a new session is created after socket timeout or because of something else (?), that's how it is! 
		// After that, the obfuscated modify IDs are invalid! We could logout or do some magic.
		final Session userSession = SessionManager.getInstance().findOrCreate(currentSession);
		//cookies.set("__SESSION_ID__", cookies.read("__SESSION_ID__"), 1);

		
		try {
		
			// access control
			if (!this.hasAccess(userSession)) {
				Map<String, String> params = session.getParms();
				return serveDefaultRedirectHandler(
										(BeetRootHTTPSession)session, 
										params, 
										LanguageManager.getInstance().translate("base.error.noaccess.msg", userSession), 
										MSG_TYPE_ERR
									);
			}

			
			final Method method = session.getMethod();
			int origId = -1;
			
			final String modifyID = session.getParms().get("id");
			if (modifyID != null && modifyID.length() != 0) {
				
				// translate ID again
				origId = userSession.getOrigId(modifyID, getEntity());
				if (origId == -1) {
	
					// NOTICE someone requested an id, but the form is invalid (session invalidation somehow)
					// -> we should bring him back to default page or do nothing!
					final String err = "Security Warning - Couldn't translate the posted record ID while executing '"+getResource()+"! Session might have been invalidated.";
					LOG.warn(err);
					
					return refresh((BeetRootHTTPSession)session, LanguageManager.getInstance().translate("base.info.session.inv", userSession));
				}
			}
			
			
			// Set current DB id of entity, it is -1 for index.html pages or if it is a new entity!
			this.setCurrentEntityDbId(origId);
		
			
			// ======== A. HTTP Posts ========
			
			// working...
			if (Method.POST.equals(method)) { 
				
				final boolean reset = session.getUri().endsWith("/users/reset");
				final boolean change = session.getUri().endsWith("/users/change");
				final boolean add = session.getUri().endsWith("/"+getEntity()+"/add");
				
				if (origId != -1 || change || reset || add ) { // + edit or delete --> origId != -1

					final String putOrPostMethod = session.getParms().get("_method");
					final boolean requestCall = putOrPostMethod != null && putOrPostMethod.length() != 0 && putOrPostMethod.equals("REQUEST");
					
					final String parmMethod = urlParams.get("_method");
					final boolean retryCall = parmMethod != null && parmMethod.length() != 0 && parmMethod.equals("RETRY");
					
					
					// ======== 1. Retry call test =================
					
					if (retryCall) {
						
						// Retry when adding 
						
						// Failed 'add' retry case; do nothing and read the formular again
						// We have to add the RETRY again to the session params!
						final List<String> retry = new ArrayList<String>();
						retry.add("RETRY");
						session.getParameters().put("_method", retry);
							
						// create a new ID pair, if somehow the orig has been lost, shouldn't happen actually!
						userSession.createIdPair(origId, getEntity());
						
					// ======== 2. Request call test ================
						
					} else if (requestCall) {
						
						// A request call is a call that wants to read data beside the default
						// index and view handler!

						// we simply let the code run further till handler read function
						// -> used for reads that need a post form
						
					// ======== 3. Main HTTP: No method (save) ======
						
					} else if (putOrPostMethod == null || putOrPostMethod.length() == 0) { 
						
						// add with id -> save
						HandlerResponse response = this.saveData((BeetRootHTTPSession) session);
						
						if (response == null || (response.getStatus() == HandlerResponse.STATE_OK && response.getType() == HandlerResponse.TYPE_FORM)) { // Ok in this case
							
							String m = LanguageManager.getInstance().translate("base.info.saved", userSession, getUpperCaseEntity());
							
							//if (measureDuration) this.processTime();
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}

						if (response.getStatus() == HandlerResponse.STATE_OK && response.getType() == HandlerResponse.TYPE_FILE_UPLOAD) {
							
							String m = response.getMessage();
							if (m == null)
								m = LanguageManager.getInstance().translate("base.info.stored0", userSession, getUpperCaseEntity());
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {

							Map<String, String> params = session.getParms();
							params.put("_method", "RETRY");
							// NOTICE: special case, don't use this method anywhere else, HTTP method isn't changed here!
							return serveHandler((BeetRootHTTPSession)session, getEntity(), this.getClass(), params, response.getMessage(), MSG_TYPE_ERR);
						}

						
					// ======== 4. Main HTTP: PUT (update) ==========
						
					} else if (putOrPostMethod.equals("PUT")) {  // and password reset
						
						// edit with id -> update
						HandlerResponse response = this.updateData((BeetRootHTTPSession)session, origId);
						
						if (change)
							return serveHandler(session, new LogoutHandler(), response);
						if (reset)
							return serveHandler(session, new LogoutHandler(), response);

						if (response == null || response.getStatus() == HandlerResponse.STATE_OK) {// Ok in this case
							
							//String m = LanguageManager.getInstance().translate("base.info.updated", userSession, origId, getUpperCaseEntity());
							String m = LanguageManager.getInstance().translate("base.info.updated", userSession, getUpperCaseEntity());
							
							//if (measureDuration) this.processTime();
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {

							Map<String, String> params = session.getParms();
							params.put("_method", "RETRY");
							
							return serveHandler((BeetRootHTTPSession)session, getEntity(), this.getClass(), params, response.getMessage(), MSG_TYPE_ERR);
						}

						
					// ======== 5. Main HTTP: POST (delete) ========
						
					} else if (putOrPostMethod.equals("POST")) {
						
						// delete with id
						HandlerResponse response = this.deleteData((BeetRootHTTPSession)session, origId);

						if (response == null || response.getStatus() == HandlerResponse.STATE_OK) { // Ok in this case
							
							//String m = LanguageManager.getInstance().translate("base.info.deleted", userSession, origId, getUpperCaseEntity());
							String m = LanguageManager.getInstance().translate("base.info.deleted", userSession, getUpperCaseEntity());
							
							// if (measureDuration) this.processTime();
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {
							return serveRedirectHandler((BeetRootHTTPSession)session, response.getMessage(), MSG_TYPE_ERR);
						}
					}
					
					// Soooo important !!!
					if (!retryCall && !requestCall) // special cases that need the right id in the readData-method!
						origId = -1;
				}
			}
			
			
			// ======== B. HTTP Get (read) ========
			
			// read data
			final HandlerResponse response = this.readData((BeetRootHTTPSession) session, origId);
			
			
			// ======== C. Handler Response Handling ======
			
			// change redirect
			if (session.getUri().endsWith("/users/change") && response != null)
				return serveHandler(session, new LogoutHandler(), response);

			// For possible special cases allow no content response, but route somewhere specific
			final String route = this.isNoContentResponseButRoute(userSession);
			if (route != null && route.length() != 0)
				return this.refreshRoute((BeetRootHTTPSession) session, route, null);
			
			// For possible special cases allow no content response
			if (this.isNoContentResponse())
				return this.refresh((BeetRootHTTPSession) session, null);
			
			// Add flash messages
			if (response != null) {
				switch (response.getStatus()) {
					case HandlerResponse.STATE_OK:
						addSuccessMessage(response.getMessage());
						break;
					case HandlerResponse.STATE_NOT_OK:
						addErrorMessage(response.getMessage());
						break;
					case HandlerResponse.STATE_WARNING:
						addWarningMessage(response.getMessage());
						break;
					default:
						break;
				}
			}
			
			// Download handling
			if (response != null && response.getType() == HandlerResponse.TYPE_FILE_DOWNLOAD) {
				
				final File file = response.getDownloadFile();
				final String mime = response.getDownloadFileMimeType();
				
				new MimeType(mime);
				
				if (!file.exists())
					throw new FileNotFoundException("File '"+file.getName()+"' doesn't exist (Download)!");
		        
		        final Response downloadResponse = Response.newFixedLengthResponse(getStatus(), mime, new FileInputStream(file), file.length());
		        downloadResponse.addHeader("Content-disposition", "attachment; filename=" +file.getName());
				return downloadResponse;
			}
			
			
			// ======== D. Get HTML: Parse templates ======
			
			String getHtml = getText((BeetRootHTTPSession)session, origId);
			
			// Template error !
			if (getHtml.startsWith("NOTFOUND:")) {
				
				String t = LanguageManager.getInstance().translate("base.err.template.parsing.title", userSession)+"<br><br>";
				String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, getHtml.split(":")[1]);
				HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, t);
				return serveHandler(session, new ErrorHandler(Status.NOT_FOUND, LanguageManager.getInstance().translate("base.err.template.title", userSession), t+m), errStat);
			}
			else if (getHtml.startsWith("PARERROR:")) {
				
				String t = LanguageManager.getInstance().translate("base.err.template.parsing.title", userSession)+"<br><br>";
				String m = LanguageManager.getInstance().translate("base.err.template.parsing.msg", userSession, getHtml.split(":")[1]);
				HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, t);
				return serveHandler(session, new ErrorHandler(Status.NOT_FOUND, LanguageManager.getInstance().translate("base.err.template.title", userSession), t+m), errStat);
			}

			
			// ======== E. Create final response ==========
			
			//if (measureDuration) this.processTime();
			
			final Response theResponse = Response.newFixedLengthResponse(getStatus(), getMimeType(), getHtml);
			
			/**
			// CORS, e.g. Tomcat CORS https://medium.com/@tarang.chikhalia/how-to-enable-cors-origin-in-apache-tomcat-e0042eae5017
			if (session.getUri().endsWith(Constants.JSON_EXT)) {
				theResponse.addHeader("Access-Control-Allow-Origin", "*");
				theResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
				theResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
				theResponse.addHeader("Access-Control-Allow-Credentials", "true");
				theResponse.addHeader("Access-Control-Max-Age", "3600");				
			}
			*/
			
	        return theResponse;
        
		} catch (Exception e) {
		
			// The framework user might have messed up things!
			String res = getResource();
			if (res == null)
				res = session.getUri();
			String err1 = this.getTemplateEngineErrorTitle(userSession, res); 
			String err2 = err1 + "<br><br>" + getTemplateEngineErrorMessage(userSession, res);
			
			LOG.error(err1, e);
			
			final String custExInfo[] = this.getCustomizedExceptionInformation(userSession);
			if (custExInfo != null) {
				if (custExInfo.length == 2) {
					err1 = custExInfo[0];
					err2 = custExInfo[1];
				} else {
					LOG.warn("Your customized exception handler information needs 2 arguments: "
							+"[title][message], but it has '"+custExInfo.length+"'; "
							+"correct the return value of 'getCustomizedExceptionInformation' in your code!");	
				}
			}
			
			final HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, err1);
			try {
				
				return serveHandler(session, new ErrorHandler(Status.INTERNAL_ERROR, err1, err2), errStat);
				
			} catch (Exception excalibur) {
				
				// At this point we cannot do anything anymore ... *sniff*
				LOG.error("TECH-FATAL:"+excalibur.getMessage(), excalibur);
				excalibur.printStackTrace();
				return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/html", err1);
			}			
		}
    }
	
	/**
	 * Is it a retry call?
	 * 
	 * @param session HTTP session
	 * @return true if so
	 */
	public boolean isRetryCall(BeetRootHTTPSession session) {
		final String parmMethod = session.getParms().get("_method");
		return parmMethod != null && parmMethod.length() != 0 && parmMethod.equals("RETRY");		
	}
	
	/**
	 * Set current DB id of entity processed, for 'index.html' pages
	 * or other pages not showing an existing entity it is '-1'.
	 * 
	 * @param origId original DB id of entity
	 */
	public final void setCurrentEntityDbId(int origId) {
		this.currentEntityDbId = origId;
	}

	/**
	 * Get current DB id of entity processed, for 'index.html' pages
	 * or other pages not showing an existing entity it is '-1'.
	 * 
	 * @return original DB id of entity
	 */
	public final int getCurrentEntityDbId() {
		return this.currentEntityDbId;
	}
	
	/**
	 * Overwrite this method, if you want to have a customized 
	 * handler exception title and message.
	 * 
	 * @param userSession user session
	 * @return array of two values containing title[0] and text[1]
	 */
	protected String[] getCustomizedExceptionInformation(Session userSession) {
		return null;
	}
	
	/**
	 * Overwrite this if your handler doesn't have an output.
	 * 
	 * When a new route (e.g. 'users/index') is returned, that new
	 * route is loaded by a script refresh. If <code>null</code> is
	 * returned, nothing happens. 
	 * 
	 * @param userSession user session
	 * @return <code>null</code> or a new route
	 */
	protected String isNoContentResponseButRoute(Session userSession) {
		return null;
	}
	
	/**
	 * Overwrite this if your handler doesn't have an output.
	 * A no content response (HTTP 204) with no content will be 
	 * generated and the current page will be refreshed!
	 * 
	 * @return <code>true</code> if a script refresh on the
	 * 		default index handler should be made.
	 */
	protected boolean isNoContentResponse() {
		return false;
	}

	/**
	 * Overwrite this if your handler has no columns config
	 * configuration.
	 * 
	 * @return <code>true</code> if no columns configuration must
	 * 		be read
	 */
	protected boolean hasNoColumnsConfig() {
		return false;
	}
	
	/**
	 * Get title for general template engine error.
	 * 
	 * @param userSession user session
	 * @param resource template resource
	 * @return title
	 */
	protected String getTemplateEngineErrorTitle(Session userSession, String resource) {
		return LanguageManager.getInstance().translate("base.err.template.execute.title", userSession, resource);
	}
	
	/**
	 * Get message for general template engine error.
	 * 
	 * @param userSession user session
	 * @param resource template resource
	 * @return message
	 */
	protected String getTemplateEngineErrorMessage(Session userSession, String resource) {
		return LanguageManager.getInstance().translate("base.err.template.msg", userSession);
	}
	
	/**
	 * Get roles, entities or actions from template behind an "if"-tag.
	 * 
	 * @param line text line within template with roles, entities or actions
	 * @return all values
	 */
	private List<String> getIfValuesFromTemplate(String line) {
		String strs[] = line.split("=", 2);
		if (strs.length != 2)
			return Arrays.asList(new String[] {});
		strs[1] = PATTERN_SEMICOLON.matcher(strs[1]).replaceAll("");
		strs[1] = PATTERN_COLON.matcher(strs[1]).replaceAll("");
		strs[1] = PATTERN_RIGHT_CURLY_BRACKET.matcher(strs[1]).replaceAll("");
		strs[1] = PATTERN_SPACE.matcher(strs[1]).replaceAll("");
		strs[1] = strs[1].trim().toLowerCase();
		final String roles[] = strs[1].split(",");
		return Arrays.asList(roles);
	}
	
	/**
	 * Special case: Used for update/save retry cases when web from data was invalid!
	 * 
	 * @param session HTTP session
	 * @param entity entity
	 * @param handlerClass handler class
	 * @param newParams new parameters
	 * @param msg message
	 * @param messageType message type
	 * @return response
	 * @throws Exception
	 */
	private Response serveHandler(
			BeetRootHTTPSession session, 
			String entity, 
			Class<?> handlerClass, 
			Map<String, String> newParams, 
			String msg, int messageType) throws Exception {

		Object obj = construct(session, handlerClass, entity, msg);
		
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
        
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize((BeetRootHTTPSession)session);
        handler.setMessageType(messageType);
        
        final UriResource ur = new UriResource(null, handlerClass, entity);
        final UriResponder responder = ((UriResponder) handler);
        
        final Response response = responder.get(ur, newParams, (org.nanohttpd.protocols.http.IHTTPSession) session);
		return response;
	}
	
	/**
	 * Used for login and logout handler.
	 * 
	 * @param session HTTP session
	 * @param handler login or logout handler
	 * @param stat status
	 * @return response
	 * @throws Exception
	 */
	private Response serveHandler(IHTTPSession session, BaseHandler handler, HandlerResponse stat) throws Exception {
		
		if (stat != null) {
			
			switch (stat.getStatus()) {
			
			case HandlerResponse.STATE_OK:
				handler.addSuccessMessage(stat.getMessage());
				break;
			case HandlerResponse.STATE_NOT_OK:
				handler.addErrorMessage(stat.getMessage());
				break;
			case HandlerResponse.STATE_WARNING:
				handler.addWarningMessage(stat.getMessage());
				break;

			default:
				break;
			}
		}
		
		// Important !!
		handler.initialize((BeetRootHTTPSession)session);
		
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), handler.getText((BeetRootHTTPSession)session, -1));
	}

	/**
	 * Used when an handler access failed!
	 * 
	 * @param session HTTP session
	 * @param newParams new parameters
	 * @param msg message
	 * @param messageType message type
	 * @return response
	 * @throws Exception
	 */
	private Response serveDefaultRedirectHandler(			
			BeetRootHTTPSession session, 
			Map<String, String> newParams, 
			String msg, int messageType) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		userSession.removeAllIds(); // important, we need to generate new ones!
		
		Object obj = construct(session, getDefaultHandlerClass(), getDefaultHandlerEntity(), msg);
		
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
        
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize(session);
        handler.setMessageType(messageType);
		
		// read index data
        try {
        	
        	handler.readData(session, -1);
        	
        } catch (Exception ex) {
        	LOG.error("*** NOTE *** : You might have forgotten to define a default handler and entioty in teh configuration!");
        	throw ex;
        }
		// lang is re-written per redirect script
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), handler.getText(session, -1));		
	}	

	/**
	 * Used for standard redirects with a success message.
	 * 
	 * @param session HTTP session
	 * @param msg success message
	 * @return response
	 * @throws Exception
	 */
	private Response serveRedirectHandler(BeetRootHTTPSession session, String msg) throws Exception {
		return serveRedirectHandler(session, msg, MSG_TYPE_INFO);
	}
	
	/**
	 * Used for standard redirects with a message.
	 * 
	 * @param session HTTP session
	 * @param msg message
	 * @param messageType message type
	 * @return response
	 * @throws Exception
	 */
	private Response serveRedirectHandler(BeetRootHTTPSession session, String msg, int messageType) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		userSession.removeAllIds(); // important, we need to generate new ones!
		
		final String entity = this.getEntity();
		Object obj = construct(session, getRedirectHandler(), entity, msg, messageType);
		
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
        
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize(session);
		
        try {

        	// set current page if any
            final String page = (String) userSession.get("page-"+entity);
    		if (page != null) {
    			session.overwriteParameter("page", page);
    			// consume!
    			userSession.remove("page-"+entity);
    		}
    		
    		// read index data
        	handler.readData(session, -1);
        	
        } catch (Exception ex) {
        	
        	LOG.error("*** NOTE *** : You might have forgotten to overwrite a handler, so beetRoot can choose the right redirect handler for an entity!");
        	LOG.error("    -> This is especially necessary, if you have defined transient colums in 'columns.cfg' !");
        	
        	throw ex;
        }
        
		return Response.newFixedLengthResponse(Status.OK, getMimeType(), handler.getText(session, -1));
	}

	/**
	 * Construct a handler with sucess message.
	 * 
	 * @param session HTTP session
	 * @param handlerClass handler class
	 * @param entity entity
	 * @param msg message
	 * @return response
	 * @throws Exception
	 */
	private Object construct(BeetRootHTTPSession session, Class<?> handlerClass, String entity, String msg) throws Exception {
		return this.construct(session, handlerClass, entity, msg, MSG_TYPE_INFO);
	}
	
	/**
	 * Construct a handler with message.
	 * 
	 * @param session HTTP session
	 * @param handlerClass handler class
	 * @param entity entity
	 * @param msg message
	 * @param messageType message type
	 * @return response
	 * @throws Exception
	 */
	private Object construct(BeetRootHTTPSession session, Class<?> handlerClass, String entity, String msg, int messageType) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		Constructor<?> constructor = null;
        final Constructor<?> constructors[] = handlerClass.getDeclaredConstructors();
        
		int ip = 3;
		if (messageType == MSG_TYPE_INFO)
			ip = 2;
		
        for (int i = 0; i < constructors.length; i++) {
			int pc = constructors[i].getParameterCount();
			if (pc == ip) {
				constructor = constructors[i];
				break;
			}
		}
        
        try {
        	
            constructor.setAccessible(true);
            
		} catch (Exception e) {
			
			String err = "Handler constructor error! - No implementation found for handler class '"+handlerClass.toString()+"' with "+ip+" parameters.";
			LOG.error(err, e);
			
			String t = "<h1>"+LanguageManager.getInstance().translate("base.err.handler.construct.title", userSession)+"</h1>";
			String m = "<p>"+LanguageManager.getInstance().translate("base.err.handler.construct.msg", userSession, handlerClass.toString(), ip, e.getMessage())+"</p>";
										
			return Response.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/html", t+m);
			
		}
        BaseHandler handler = null;
        try {
        	
    		if (messageType == MSG_TYPE_INFO)
    			handler = (BaseHandler) constructor.newInstance(entity, msg);
    		else
    			handler = (BaseHandler) constructor.newInstance(entity, msg, messageType);
            
		} catch (Exception e) {

			String err = "Handler error! - No implementation found for handler class '"+handlerClass.toString()+"'!";
			LOG.error(err, e);
			
			String t = "<h1>"+LanguageManager.getInstance().translate("base.err.handler.impl.title", userSession)+"</h1>";
			String m = "<p>"+LanguageManager.getInstance().translate("base.err.handler.impl.msg", userSession, handlerClass.toString(), e.getMessage())+"</p>";
			
			return Response.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/html", t+m);
		}	
        
        return handler;
	}

	/**
	 * Refresh a route; used for no content handlers!
	 * 
	 * @param session HTTP session
	 * @param route new URI route
	 * @param msg success message
	 * @return response
	 */
	private Response refreshRoute(BeetRootHTTPSession session, String route, String msg) {
		
		if (route.startsWith("/"))
			route = route.substring(1, route.length());
		
		String url = route;
		final Session userSession = session.getUserSession();
		
		// Page ?
        final String page = (String) userSession.get("page-"+entity);
		if (page != null) {
			url = Web.enrichQuery(url, "page", page);
			// consume!
			userSession.remove("page-"+entity);
		}

		// Message?
		if (msg != null && msg.length() !=0 ) {
			try {
				url = Web.enrichQuery(url, "msg", msg);
			} catch (Exception e) {
				url = Web.enrichQuery(url, "msg", LanguageManager.getInstance().translate("base.info.session.inv.refresh", userSession));
			}
			url = Web.enrichQuery(url, "sev", "w");
		}
		
		String sn = "/";
		if (insertServletNameInTemplateRefs)
			sn = "/" + servletName + "/";
		
		final String refreshText = 
				  "<!DOCTYPE html>\n"
				+ "<html lang=\"en\">\n"
				+ "<head>\n"
				+ "	<meta charset=\"utf-8\">\n"
				+ "	<meta http-equiv=\"Refresh\" content=\"0; url=" + sn + userSession.getUserLang() + "/" + url + "\" />\n"
				+ "</head>\n"
				+ "</html>\n"
				+ "";
		
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), refreshText);
	}
	
	/**
	 * Refresh the index-route. Used if modify IDs have an inconsistent state.
	 * 
	 * @param session HTTP session
	 * @param msg message
	 * @return response
	 */
	private Response refresh(BeetRootHTTPSession session, String msg) {
		return this.refreshRoute(session, getDefaultHandlerEntity() + "/index", msg);
	}
	
	/**
	 * Get default handler entity from configuration.
	 */
	private String getDefaultHandlerEntity() {
		String entity;
		try {
			entity = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_ENTITY);
			return entity;
		} catch (Exception e) {
	    	LOG.warn("Couldn't load default handler entity!", e);
			return "NONE";
		}
	}
	
	/**
	 * Get default handler class from configuration.
	 */
	private Class<?> getDefaultHandlerClass() {
		String clz;
		try {
			clz = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WEB_DEFAULT_HANDLER);
			return Class.forName(clz);
		} catch (Exception e) {
	    	LOG.warn("Couldn't load default handler class!", e);
			return null;
		}
	}
	
	/**
	 * Mark this handler as it has been redirected from
	 * a data modifying handler.
	 * 
	 * @param redirected true if redirected
	 */
	protected void redirectedMarker(boolean redirected) {
		this.redirectedMarker = redirected;
	}
	
	/**
	 * Overwrite this method, to specifically extend handler authorization, 
	 * e.g. with more app user roles.
	 *  
	 * @param userSession user session
	 * @return true if access is allowed, otherwise false
	 */
	public boolean hasAccess(Session userSession) {
		return true;
	}
	
	/**
	 * Read data from DB that must be filled when the template is parsed.
	 * 
	 * Used by index and view handlers.
	 * 
	 * @param session HTTP session
	 * @param id db record id &gt; 0 if a single record should be read otherwise &lt; 0;
	 * @return response or null, null means success, response's status must be checked!
	 * @throws Exception exception
	 */
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		return null;
	}

	/**
	 * Save data to DB.
	 * 
	 * Override for add handlers.
	 * 
	 * @param session HTTP session
	 * @return response or null, null means success, response's status 
	 * 			must be checked and must hold the id of the saved record!
	 * @throws Exception exception
	 */
	public HandlerResponse saveData(BeetRootHTTPSession session) throws Exception {
		return null;
	}

	/**
	 * Update data in DB.
	 * 
	 * Override for edit handlers.
	 * 
	 * @param id db record id
	 * @param session HTTP session
	 * @return response or null, null means success, response's status 
	 * 			must be checked!
	 * @throws Exception exception
	 */
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		return null;
	}

	/**
	 * Delete data from DB.
	 * 
	 * Override for delete handlers.
	 * 
	 * @param id db record id
	 * @param session HTTP session
	 * @return response or null, null means success, response's status must be checked!
	 * @throws Exception exception
	 */
	public HandlerResponse deleteData(BeetRootHTTPSession session, int id) throws Exception {
		return null;
	}
	
	/**
	 * Overwrite to get the right re-route/redirect index handler
	 * after modifying data. It must be of the same entity as the 
	 * last executing handler!
	 * 
	 * @return redirect index handler
	 */
	public Class<?> getRedirectHandler() {
		return DefaultIndexHandler.class;
	}
	
	/**
	 * Get current HTTP session process.
	 * 
	 * @return HTTP session
	 */
	final public BeetRootHTTPSession getCurrentSession() {
		return currentSession;
	}

	/**
	 * Get web entity (use plural, e.g. 'tasks', 'users'.
	 * 
	 * @return web entity
	 */
	public String getEntity() {
		return entity;
	}
		
	/**
	 * Get HTML head if there's any.
	 * 
	 * @return HTML head
	 */
	public String getHtmlHead() {
		return this.htmlHead;
	}
	
	/**
	 * Get HTML data.
	 * 
	 * @return HTML data
	 */
	public String getHtmlData() {
		return this.htmlData;
	}

	/**
	 * Get HTML head if there's any.
	 * 
	 * @param line HTML line
	 */
	public void addHtmlHeadLine(String line) {
		this.htmlHead += line + "\n";
	}
	
	/**
	 * Get HTML data.
	 * 
	 * @param line HTML line
	 */
	public void addHtmlDataLine(String line) {
		this.htmlData += line + "\n";
	}

	/**
	 * This replaces a value in a HTML element with a specific ID.
	 * 
	 * @param text text to parse and return
	 * @param id HTML element with specific ID
	 * @return patched HTML
	 */
	public static String patchInputValue(String text, String id, String newValue) {
		final String regex = "(<[^>]*\\sid\\s*=\\s*['\"]" + Pattern.quote(id) + "['\"][^>]*\\svalue\\s*=\\s*\")(.*?)(\")";
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(text);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + newValue + matcher.group(3));
        }
        matcher.appendTail(sb);
        return sb.toString();			
	}
	
	/**
	 * Replace some more variables in template.
	 * If returning null, then nothing is replaced.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		return text;
	}
	
	/**
	 * Replace some more variables within the whole page.
	 * If returning null, then nothing is replaced.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	public String replaceVariables(String text, BeetRootHTTPSession session) {
		return text;
	}
	
	/**
	 * Replace language variables within the whole page if any.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	private String replaceLanguageVariables(String text, BeetRootHTTPSession session) {
		// Only when switched on!
		if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
			int idx = -1;
			while ((idx = text.indexOf("{$lang.")) != -1) {
				final int pos1 = idx + 7;
				final int pos2 = text.indexOf("}");
				String totrans = text.substring(pos1, pos2);
				String trans = "";
				if (totrans.length() > 0)
					trans = LanguageManager.getInstance().translateTemplate(totrans.trim(), session.getUserSession());
				text = text.substring(0, idx) + trans + text.substring(pos2 + 1);
			}
		}
		return text;
	}
	
	/**
	 * Get paginator html code. Must only be implemented by index handlers
	 * and is only called if there's a {$paginator} tag in a template.
	 * @param session HTTP session  
	 * @return html paginator code
	 */
	public String getPaginator(BeetRootHTTPSession session) {
		throw new IllegalStateException("This method should not be called without a routed index template handler!");
	}
	
	/**
	 * Show template menu?
	 * 
	 * @param userSession user session, possible even
	 * a temporary session from a not logged in user
	 * 
	 * @return true if a menu should be shown
	 */
	public boolean showMenu(Session userSession) {
		return true;
	}
	
	/**
	 * Add a success message to show.
	 * 
	 * @param message message
	 */
	public void addSuccessMessage(String message) {
		if (message != null)
			this.successMessage = message.replace("$", "\\$");
		else
			this.successMessage = null;
	}

	/**
	 * Add a warning message to show.
	 * 
	 * @param message message
	 */
	public void addWarningMessage(String message) {
		if (message != null)
			this.warningMessage = message.replace("$", "\\$");
		else
			this.warningMessage = null;
	}
	
	/**
	 * Add an error message to show.
	 * 
	 * @param message message
	 */
	public void addErrorMessage(String message) {
		if (message != null)
			this.errorMessage = message.replace("$", "\\$");
		else
			this.errorMessage = null;
	}

	/**
	 * Get web resource file as it lies on the file system 
	 * relatively to the started server process.
	 * 
	 * @return web resource
	 */
	public abstract String getResource();

	/**
	 * Add proper logic for checkboxes.
	 * 
	 * @param session HTTP session
	 * @param columnName column / input name
	 */
	protected void addCheckBox(BeetRootHTTPSession session, String columnName) {
		checkBoxLogic.append(""
				+ "$('#cb_"+columnName+"').change(function() {\n"
				+ "	if ($(this).is(':checked')) {\n"
				+ "		$('#"+columnName+"').val(\"true\");\n"
				+ "	} else {\n"
				+ "		$('#"+columnName+"').val(\"false\");\n"
				+ "	}\n"
				+ "});\n");
	}
	
	/**
	 * Is the given HTML input type a precision input type?
	 * They are allowed to have a 'maxLength' attribute.
	 * 
	 * @param inputType HTML input type
	 * @return true, if it is a precision HTML input type
	 */
	protected boolean isPrecisionInputType(String inputType) {
		return PRECISION_INPUT_TYPES.contains(inputType.toLowerCase());
	}
	
	/**
	 * Format single value before update / insert into DB.
	 * 
	 * @param session HTTP session
	 * @param val value
	 * @param columnname column name
	 * @return formatted value
	 */
	public String formatSingleValueForDB(BeetRootHTTPSession session, String val, String columnname) {
		throw new IllegalAccessError("This method should never be called in this context!");
	}

	protected void loginMarker(boolean redirectLogin) {
		//this.loginMarker = redirectLogin;
	}
	
	@SuppressWarnings("unused")
	private void processTime() {
		// stop stop-watch and measure
		final long ifaceXEnd = System.currentTimeMillis();
		final long duration = ifaceXEnd - baseHandlerStart;
		final String durStr = "BEETROOT handler process time: " + Time.getReadableDuration(duration, TimeUnit.HOURS);
		LOG.info(durStr);
	}

	/**
	 * State handler for role, entity and action "IFs" for
	 * the overall, the template and the template sub-resource layer.
	 * 
	 * Note: Authorization cascades in the HTML views only works in the order:
	 * role -> entity -> action.
	 *  
	 * Nasty stuff.
	 */
	protected final class IfSectionHandler {

		private Map<String, Map<String, Boolean>> ifTagStates = new HashMap<String, Map<String, Boolean>>();
		private BaseHandler handler = null;
		
		/**
		 * Create an "if"-section handler.
		 * 
		 * @param handler base handler
		 */
		protected IfSectionHandler(BaseHandler handler) {
			this.handler= handler;
		}
		
		/**
		 * Continue removal of lines?
		 * 
		 * @param text current line
		 * @param userSession user session
		 * @param layer 'overall', 'template' or 'subresource'
		 * @return true if removal should continue, otherwise false
		 */
		protected boolean continueRemoval(String text, Session userSession, String layer) {
			
			boolean continueRemoval = false;

			// Get states
			Map<String, Boolean> currStates = ifTagStates.get(layer);
			
			// TODO we have to check all roles
			
			// Add initial states if not present for layer
			if (currStates == null) {
				currStates = new HashMap<String, Boolean>();
				currStates.put("role", Boolean.FALSE);
				currStates.put("entity", Boolean.FALSE);
				currStates.put("action", Boolean.FALSE);
				ifTagStates.put(layer, currStates);
			}
			
			// deal with role-specific sections
			if (text.contains("$endif-role")) {
				currStates.put("role", Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get("role").booleanValue())
				continueRemoval = true;
			if (text.contains("$if-role")) {
				final List<String> roles = userSession.getUserRoles();
				final List<String> tempRoles = handler.getIfValuesFromTemplate(text);
				boolean roleAvailable = false;
				final Iterator<String> iterator = tempRoles.iterator();
				while (!roleAvailable && iterator.hasNext()) {
					final String tempRole = iterator.next();
					roleAvailable = roles.contains(tempRole);
				}
				if (!roleAvailable)
					currStates.put("role", Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			// deal with entity-specific sections
			if (text.contains("$endif-entity")) {
				currStates.put("entity", Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get("entity").booleanValue())
				continueRemoval = true;
			if (text.contains("$if-entity")) {
				final List<String> entities = handler.getIfValuesFromTemplate(text);
				if (!entities.contains(entity))
					currStates.put("entity", Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			// deal with action-specific sections
			if (text.contains("$endif-action")) {
				currStates.put("action", Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get("action").booleanValue())
				continueRemoval = true;
			if (text.contains("$if-action")) {
				final List<String> actions = handler.getIfValuesFromTemplate(text);
				if (!actions.contains(action))
					currStates.put("action", Boolean.TRUE); // start removal
				continueRemoval = true;
			}
				
			return continueRemoval;
		}		
	}

}


