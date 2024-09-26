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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import ch.autumo.beetroot.cache.FileCache;
import ch.autumo.beetroot.cache.FileCacheManager;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.users.User;
import ch.autumo.beetroot.handler.usersroles.UserRole;
import ch.autumo.beetroot.routing.Route;
import ch.autumo.beetroot.utils.bean.Beans;
import ch.autumo.beetroot.utils.common.Time;
import ch.autumo.beetroot.utils.database.DB;
import ch.autumo.beetroot.utils.security.Security;
import ch.autumo.beetroot.utils.web.Web;
import jakarta.activation.MimeType;


/**
 * Base handler - The "Heart" of beetRoot. A handler is mapped by a route
 * defined in 'cfg/routing.xml'.
 * <br><br>
 * Use {@link #setVar(String, Object)} in your overwritten methods {@link #render(BeetRootHTTPSession)}
 * to replace template variables.
 * <br><br>
 * Use {@link #setVarAll(String, Object)} in your overwritten methods {@link #renderAll(BeetRootHTTPSession)}
 * to replace variables within the whole HTML document.
 * <br><br>
 * Beside the CRUD methods
 * <ul>
 * <li>{@link #readData(BeetRootHTTPSession, int)} (index,view,edit),</li>
 * <li>{@link #saveData(BeetRootHTTPSession)} (add),</li>
 * <li>{@link #updateData(BeetRootHTTPSession, int)} (edit) and</li>
 * <li>{@link #deleteData(BeetRootHTTPSession, int)} (delete)</li>
 * </ul>
 * you usually need to overwrite the following methods:
 * <ul>
 * <li>{@link #getBeanClass()} (when serving entities),</li>
 * <li>{@link #getTitle(Session)} (Show specific title)</li>
 * <li>{@link #getRedirectHandler()} (for redirecting)</li>
 * <li>{@link #hasAccess(Session)} (authorization)</li>
 * </ul>
 * Depending on the handler, you might want to overwrite more methods. There are methods for
 * everything, and you should be able to do anything with the template engine. For a good 
 * overview, see users as well system handler source codes.
 */
public abstract class BaseHandler extends DefaultHandler implements Handler {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseHandler.class.getName());

	
	// The routes (without default routes)
	private static List<Route> routes = null;
	
	// Strings
	private static final String STR_EMAIL			= "email";
	private static final String STR_PASSWORD		= "password";
	private static final String STR_DEFAULT			= "default";
	private static final String URL_WEB_HTML_PREFIX	= "web/html/";
	private static final String MIME_TYPE_HTML		= "text/html";

	// Files
	private static final String FILE_HTML_ACTION_INDEX	= "index.html";
	private static final String FILE_HTML_ACTION_VIEW	= "view.html";
	private static final String FILE_HTML_ACTION_EDIT	= "edit.html";
	private static final String FILE_HTML_ACTION_ADD	= "add.html";
	private static final String FILE_CFG_COLUMNS		= "/columns.cfg";
	
	// Precision HTML input types
	protected static final List<String> PRECISION_INPUT_TYPES = Arrays.asList(STR_EMAIL, STR_PASSWORD, "search", "tel", "text", "url");
	
	
	// Link reference patterns 
	private static final Pattern PATTERN_HREF		= Pattern.compile("href=\\\"(?!#.*)(?!\\{.*)");
	private static final Pattern PATTERN_SRC		= Pattern.compile("src=\\\"(?!#.*)(?!\\{.*)");
	private static final Pattern PATTERN_ACTION		= Pattern.compile("action=\\\"(?!#.*)(?!\\{.*)");
	private static final Pattern PATTERN_LOCATION	= Pattern.compile("location='(?!#.*)(?!\\{.*)");
	
	// Pattern for script resources
	private static final Pattern PATTERN_SCRIPT	= Pattern.compile("\\{#script.*\\}");
	
	// Tags
	public static final String TAG_PREFIX_LANG		= "{$l.";
	private static final String TAG_IG 				= "{$id}";
	private static final String TAG_DBID			= "{$dbid}";
	private static final String TAG_DISPLAY_NAME	= "{$displayName}";
	private static final String TAG_CSRF_TOKEN		= "{$csrfToken}";
	private static final String TAG_TITLE			= "{$title}";
	private static final String TAG_USER			= "{$user}";
	private static final String TAG_USERFULL		= "{$userfull}";
	private static final String TAG_LANG			= "{$lang}";
	private static final String TAG_THEME			= "{$theme}";
	private static final String TAG_ANTITHEME		= "{$antitheme}";
	
	// Tags for sub-resource
	private static final String TAG_REDIRECT_INDEX		= "{$redirectIndex}";
	private static final String TAG_SEVERITY			= "{$severity}";
	private static final String TAG_MESSAGE				= "{$message}";
	private static final String TAG_USERINFO			= "{$userinfo}";
	private static final String TAG_USERLINK			= "{$userlink}";
	private static final String TAG_LANG_MENU_ENTRIES	= "{$lang_menu_entries}";
	private static final String TAG_ADMIN_MENU			= "{$adminmenu}";
	private static final String TAG_CB_LOGIC			= "{$checkBoxLogic}";
	private static final String TAG_LOGIN_OR_LOGOUT		= "{$loginorlogout}";
	
	// Additional patterns
	private static final String CHAR_SEMICOLON				= ";";
	private static final String CHAR_COLON					= ":";
	private static final String CHAR_RIGHT_CURLY_BRACKET	= "}";
	private static final String CHAR_SPACE					= " ";
	
	public static final int MSG_TYPE_INFO = 0;
	public static final int MSG_TYPE_WARN = 1;
	public static final int MSG_TYPE_ERR = -1;
	
	// Link reference reverse patterns
	private Pattern pattern_href_rev = null;
	private Pattern pattern_scr_rev;
	private Pattern pattern_action_rev;
	private Pattern pattern_location_rev;
	
	
	private StringBuilder buffer = new StringBuilder();	
	
	protected TreeMap<Integer, String> columns = null;
	protected Map<String, String> initialValues = null;
	protected String uniqueFields[] = null;
	protected List<String> transientFields = new ArrayList<>();
	
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
	
	private StringBuilder checkBoxLogic = new StringBuilder();

	private IfSectionHandler ish = null;
	
	private boolean redirectedMarker = false;

	// start time
	private long baseHandlerStart = 0;
	// measure duration time?
	@SuppressWarnings("unused")
	private boolean measureDuration = false;
	
	private final Map<String, String> vars = new HashMap<>();
	private final Map<String, String> varsAll = new HashMap<>();
	
	
	/**
	 * Base Handler.
	 */
	protected BaseHandler() {
	}

	/**
	 * Base Handler.
	 * 
	 * @param entity entity, plural &amp; lower-case; e.g. 'roles, users or properties'
	 */
	protected BaseHandler(String entity) {
		this.entity = entity;
	}
	
	
	/**
	 * Every handler MUST be initialized!
	 * 
	 * @param session session
	 */
	public void initialize(BeetRootHTTPSession session) {
		
		// IF section handler
		ish = new IfSectionHandler(this);
		
		servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
		if (servletName != null && servletName.length() != 0) {
			insertServletNameInTemplateRefs = true;
			pattern_href_rev = Pattern.compile("href=\\\"/"+servletName+"http");
			pattern_scr_rev = Pattern.compile("src=\\\"/"+servletName+"http");
			pattern_action_rev = Pattern.compile("action=\\\"/"+servletName+"http");
			pattern_location_rev = Pattern.compile("location='/"+servletName+"http");
		}
		
		
		// Get template resource
		final String templateResource = getResource();
		
		// Define action
		if (templateResource != null && templateResource.length() != 0) {
    		final String htmlAction = templateResource.substring(templateResource.lastIndexOf("/") + 1, templateResource.length());
    		switch (htmlAction) {
				case FILE_HTML_ACTION_INDEX:
					action = "index";
					break;
				case FILE_HTML_ACTION_VIEW:
					action = "view";
					break;
				case FILE_HTML_ACTION_EDIT:
					action = "edit";
					break;
				case FILE_HTML_ACTION_ADD:
					action = "add";
					break;
				default:
					final int idx = htmlAction.indexOf(".");
					if (idx == -1) {
						action = "null";
					} else {
						action = htmlAction.substring(0, idx);
					}
					break;
			}
		} else {
			action = "null";
		}
		
		
		// nothing further to do!
		if (entity == null || entity.length() == 0)
			return;
		
		
		// Create an empty bean to access static information generated by PLANT if necessary, e.g. foreign bean classes
		final Class<?> entityClass = this.getBeanClass();
		if (entityClass != null) {
			try {
				this.emptyBean = Beans.createBean(entityClass);
			} catch (Exception e) {
				LOG.error("Couldn't create empty bean, this might be an error!", e);
			}
		}
		
		
		// nothing further to do!
		if (this.hasNoColumnsConfig())
			return;
		
		
		/**
		 * Columns.cfg configuration.
		 */
		
		this.columns = new TreeMap<>();
		this.initialValues = new HashMap<>();
		
		final List<String> fallBackList = new ArrayList<>();
		
		Session userSession = session.getUserSession();
		String res = null;
		
		// Special case JSON: overwrite languages, not needed!
		if (session.getUri().endsWith(Constants.JSON_EXT)) {
			res = URL_WEB_HTML_PREFIX + entity + FILE_CFG_COLUMNS;
		} else {
			if (userSession == null)
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+FILE_CFG_COLUMNS, Web.normalizeUri(session.getUri()));
			else
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+FILE_CFG_COLUMNS, userSession);
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
			LOG.trace("File '{}' not found on server, looking further within archives...", filePath);
			try {
				fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		
		if (tryFurther) {
			tryFurther = false;
			LOG.trace("Resource '{}' doesn't exist, trying with default language '{}'!", res, LanguageManager.DEFAULT_LANG);
			if (!session.getUri().endsWith(Constants.JSON_EXT)) {
				if (userSession == null)
					res = LanguageManager.getInstance().getResource(URL_WEB_HTML_PREFIX+LanguageManager.DEFAULT_LANG+"/"+entity+FILE_CFG_COLUMNS, Web.normalizeUri(session.getUri()));
				else
					res = LanguageManager.getInstance().getResource(URL_WEB_HTML_PREFIX+LanguageManager.DEFAULT_LANG+"/"+entity+FILE_CFG_COLUMNS, userSession);
			}
			try {
				if (context == null)
					fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + res);
				else
					fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + res);
			} catch (IOException e) {
				LOG.trace("File '{}'not found on server, looking further within archives...", filePath);
				try {
					fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}			
				
			if (tryFurther) {
				tryFurther = false;
				LOG.trace("Resource '{}' doesn't exist, trying with NO language!", res);
				res = LanguageManager.getInstance().getResourceWithoutLang(URL_WEB_HTML_PREFIX+entity+FILE_CFG_COLUMNS, Web.normalizeUri(session.getUri()));
				try {
					if (context == null)
						fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + res);
					else
						fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + res);
				} catch (IOException e) {
					LOG.trace("File '{}'not found on server, looking further within archives...", filePath);
					try {
						fc = FileCacheManager.getInstance().findOrCreateByResource("/" + res);
					} catch (IOException e1) {
						LOG.debug("Resource '{}' doesn't exist, no columns used!", res);
						return; // !
					}
				}			
			}
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
		    			configPair[1] = configPair[1].replace(" ", "");
		    			uniqueFields = configPair[1].split(",");
		    			continue LOOP;
		    		}
		    		// transient (not store in DB)
		    		if (configPair[0].equals("transient")) {
		    			if (configPair[1] == null || configPair[1].equals(""))
			    			continue LOOP;
		    			configPair[1] = configPair[1].replace(" ", "");
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
			    			case FILE_HTML_ACTION_INDEX:
				    			if (configPair[0].startsWith("list.")) {
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + this.replaceLanguageVariablesNoEscape(configPair[1], session);
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
			    			case FILE_HTML_ACTION_VIEW:
				    			if (configPair[0].startsWith("view.")) {
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + this.replaceLanguageVariablesNoEscape(configPair[1], session);
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
			    			case FILE_HTML_ACTION_EDIT:
				    			if (configPair[0].startsWith("edit.")) {
				    				newCfgLine = configPair[0].substring(5, configPair[0].length()) + "=" + this.replaceLanguageVariablesNoEscape(configPair[1], session);
				    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				    		    	added = true;
				    			}
				    			break;
			    			case FILE_HTML_ACTION_ADD:
				    			if (configPair[0].startsWith("add.")) {
				    				newCfgLine = configPair[0].substring(4, configPair[0].length()) + "=" + this.replaceLanguageVariablesNoEscape(configPair[1], session);
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
		    	transientFields = new ArrayList<>();
		    
		    // no prefixes have been used -> column config is valid for all handlers! 
		    if (columns.size() == 0) {
		    	for (Iterator<String> iterator = fallBackList.iterator(); iterator.hasNext();) {
					final String newCfgLine = iterator.next();
    		    	columns.put(Integer.valueOf(++l), newCfgLine);
				}
		    }
		    
		} catch (Exception e) {
			// Not good !
			LOG.error("Couldn't read columns for entity '"+entity+"' from file '" + fc.getFullPath() + "'!\n"
					+ "Create this file and add such a line for every column you want to show:\n"
					+ "columnName=Name of Column on Web Page", e);
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
	 * Routes registration method.
	 * 
	 * @param routes all routes except default routes
	 */
	public static void registerRoutes(List<Route> routes) {
		BaseHandler.routes = routes;
	}

	/**
	 * Get a handler class by handler name.
	 * 
	 * @param handlerName handler name
	 * @return handler class or null if not found
	 */
	public final Class<?> getHandlerClass(String handlerName) {
		Class<?> clz = null;
		for (Iterator<Route> iterator = routes.iterator(); iterator.hasNext();) {
			final Route route = iterator.next();
			clz = route.getHandler();
			if (clz.getName().endsWith(handlerName))
				return clz;
		}
		LOG.error("No router for handler name '{}' found!", handlerName);
		return null;
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
			default: addWarningMessage(origMsg);
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
	public SortedMap<Integer, String> columns() {
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
		// We actually allow HTML here!
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
			
			final String[] col = getColumn(i);

			if (transientFields.contains(col[0]))
				continue LOOP;
			
			String val = session.getParms().get(col[0]);		
			
			val = DB.escapeValuesForDb(val);
			
			if (dbPwEnc && col[0].equalsIgnoreCase(STR_PASSWORD)) {
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

		// No PW update here!
		final boolean dbAutoMod = BeetRootConfigurationManager.getInstance().getYesOrNo("db_auto_update_modified");
		
		String clause = "";
		
		final Session userSession = session.getUserSession();
		final boolean currentUser = this.isCurrentUserUpdate(session);
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {

			final String col[] = getColumn(i);
			
			if (transientFields.contains(col[0]))
				continue LOOP;

			if (col[0].equalsIgnoreCase(STR_PASSWORD)) // passwords are not allowed to be updated!
				continue LOOP;
			
			
			String val = session.getParms().get(col[0]);
			val = DB.escapeValuesForDb(val);
			
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
				if (col[0].equals(STR_EMAIL)) {
					userSession.set(STR_EMAIL, val);
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
					LOG.info("Found "+getEntity()+" with same unique value(s) {}! Not {} the record.", uniqueText, operation);
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
		
		final List<Model> usersRoles = Model.where(UserRole.class, "user_id = ?", Integer.valueOf(currentUserId));
		for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
			final UserRole userRole = (UserRole) iterator.next();
			final Role role = (Role) Model.read(Role.class, userRole.getRoleId());
			roles += role.getName()+",";
			permissions += role.getPermissions()+",";
		}
		if (!usersRoles.isEmpty()) {
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
		final StringBuilder sb = new StringBuilder();
		
		// process JSON templates
		Scanner sc = null;
		try {
			sc = getNewScanner(currRessource);
			while (sc.hasNextLine()) {
				
				String text = sc.nextLine();
				
				// template specific variables
				this.render(session);
				this.renderAll(session);
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

		
	    // Language
		final Session userSession = session.getUserSession();
	    final User user = userSession.getUser();
	    final String lang = LanguageManager.getInstance().retrieveLanguage(session);
		userSession.setUserLang(lang);
		
		
		String userName = userSession.getUserName();
		if (userName != null && userName.indexOf("$") > 0)
			userName = userName.replace("$", "\\$");
		String userfull = userSession.getUserFullNameOrUserName();
		if (userfull != null && userfull.indexOf("$") > 0)
			userfull = userfull.replace("$", "\\$");
		String currRessource = LanguageManager.getInstance().getBlockResource(this.getLayout(userSession), userSession);
		String templateResource = getResource();
		
		// prepare text buffer
		final StringBuilder sb = new StringBuilder();
		
		// process templates
		Scanner sc = null;
		try {
			sc = getNewScanner(currRessource);
			LOOP: while (sc.hasNextLine()) {
				String text = sc.nextLine();
				// Remove lines?
				if(ish.continueRemoval(text, userSession, "overall"))
					continue LOOP;
				
				
				// 0. Layout templates and main template
				
				if (text.contains("{#head}")) {
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/head.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#head}", session, origId);
				} else if (text.contains("{#header}")) {
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/header.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#header}", session, origId);
				} else if (text.contains("{#langmenu}")) {
					if (this.showLangMenu(userSession)) {
						if (LanguageManager.getInstance().getConfiguredLanguages().length > 1) {
							currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/menu_lang.html", userSession);
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
				} else if (text.contains("{#thememenu}")) {
					if (user != null) {
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/menu_theme.html", userSession);
						text = parseAndGetSubResource(text, currRessource, "{#thememenu}", session, origId);
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
					currRessource = LanguageManager.getInstance().getBlockResource(this.getResource(), userSession);
					try {
						this.createTemplateContent(userSession, session);
						if (templateResource.endsWith(FILE_HTML_ACTION_INDEX)) {
							parseTemplateHead(buffer, "{$head}");
							parsePaginator(buffer, "{$paginator}", session); // if any, only index!
						}
						if (templateResource.endsWith("search.html")) {
							parseTemplateHead(buffer, "{$head}");
						}
						parseTemplateData(buffer, "{$data}");
						text = text.replace("{#template}", buffer.toString());
						// Provide a link to current logged in user for every template!
						if (text.contains(TAG_USERLINK)) {
							final Integer uid = userSession.getUserId();
							if (uid != null) {
								String usid = userSession.getModifyId(uid.intValue(), "users");
								if (usid == null)
									userSession.createIdPair(uid, "users");
								text = text.replace(TAG_USERLINK, "/"+lang+"/users/view?id=" + userSession.getModifyId(uid, "users")); 
							} else {
								text = text.replace(TAG_USERLINK, "#");
							}
						}
						if (text.contains(TAG_CSRF_TOKEN) && userSession != null) {
							final String formCsrfToken = userSession.getFormCsrfToken();
							if (formCsrfToken != null && formCsrfToken.length() != 0)
								text = text.replace(TAG_CSRF_TOKEN, formCsrfToken);
						}
						if (templateResource.endsWith(FILE_HTML_ACTION_VIEW) || 
							templateResource.endsWith(FILE_HTML_ACTION_EDIT) || 
							templateResource.endsWith(FILE_HTML_ACTION_ADD)) {
							// Here, the id's are written !
							// obfuscate it!
							String modifyID = userSession.getModifyId(origId, getEntity());
							if (modifyID == null) {
								userSession.createIdPair(origId, getEntity());
							}
							text = text.replace(TAG_IG, "" + modifyID);
							text = text.replace(TAG_DBID, "" + origId);
							if (displayNameValue != null)
								text = text.replace(TAG_DISPLAY_NAME, displayNameValue);
							else
								text = text.replace(TAG_DISPLAY_NAME, "" + origId);
						}
						// template specific variables
						this.render(session);
						
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
				} else if (text.contains("{#dialog}")) {
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/dialog.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#dialog}", session, origId);
				} else if (text.contains("{#script")) { // {#scriptXXX}
					final Matcher matcher = PATTERN_SCRIPT.matcher(text);
					if (matcher.find()) {
						final String scriptTag = matcher.group();
						final String fileName = scriptTag.substring(2, scriptTag.length() - 1);
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/" + fileName + ".html", userSession);
						text = parseAndGetSubResource(text, currRessource, scriptTag, session, origId);
					}
				}
				
				
				// 1. General variables!
				
				// title
				if (text.contains(TAG_TITLE))
					text = text.replace(TAG_TITLE, this.getTitle(userSession));
				// user
				if (user != null && text.contains(TAG_USER)) {
					text = text.replace(TAG_USER, userName);
				}
				// user full
				if (userfull != null && text.contains(TAG_USERFULL)) {
					text = text.replace(TAG_USERFULL, userfull);
				}
				// language
				if (text.contains(TAG_LANG)) {
					text = text.replace(TAG_LANG, lang);
				}				
				
				
				// 2. User settings variables!
				
				// theme
				if (text.contains(TAG_THEME)) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = text.replace(TAG_THEME, "dark");
					else
						text = text.replace(TAG_THEME, theme);
				}				
				if (text.contains(TAG_ANTITHEME)) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = text.replace(TAG_ANTITHEME, STR_DEFAULT);
					else
						if (theme.equals(STR_DEFAULT))
							text = text.replace(TAG_ANTITHEME, "dark");
						else
							text = text.replace(TAG_ANTITHEME, STR_DEFAULT);
				}
				
				
				// 3. Replace further overall variables!
				this.renderAll(session);
				String resRepl = this.replaceVariables(text, session);
				if (resRepl != null && resRepl.length() > 0)
					text = resRepl;

				
				// 4. Replace template language translations if any.
				//    It is important that this is last, possible value 
				//    place-holders should be replaced before translation
				//    takes them into account!
				//    E.g. -> {$l.transvar,{$var1},{$var1}}
				text = this.replaceLanguageVariables(text, session);
				
				
				// 5. Add servlet URL part.
				//    - href="
				//    - src="
				//    - action="
				//    - location='
				if (insertServletNameInTemplateRefs) {
					// repeat only the following part:
					text = PATTERN_HREF.matcher(text).replaceAll("href=\"/"+servletName);
					text = PATTERN_SRC.matcher(text).replaceAll("src=\"/"+servletName);
					text = PATTERN_ACTION.matcher(text).replaceAll("action=\"/"+servletName);
					text = PATTERN_LOCATION.matcher(text).replaceAll("location='/"+servletName);
					if (this.hasExternalLinks()) {
						// hack: we have to re-replace http and https links....
						text = pattern_href_rev.matcher(text).replaceAll("href=\"http");
						text = pattern_scr_rev.matcher(text).replaceAll("src=\"http");
						text = pattern_action_rev.matcher(text).replaceAll("action=\"http");
						text = pattern_location_rev.matcher(text).replaceAll("location='http");
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
	 * Not used by the extended role management. which is default!
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
	protected void parseAssociatedEntities(StringBuilder snippet, List<Model> list, BeetRootHTTPSession session) {
		this.parseAssociations("{$assignedRoles}", snippet, list, session);
	}
	
	/**
	 * Parse un-associated list.
	 * 
	 * @param snippet buffered code snippet
	 * @param list the list with the un-associated entities
	 * @param session beetRoot session
	 */
	protected void parseUnassociatedEntities(StringBuilder snippet, List<Model> list, BeetRootHTTPSession session) {
		this.parseAssociations("{$unassignedRoles}", snippet, list, session);
	}
	
	private void parseAssociations(String tag, StringBuilder snippet, List<Model> list, BeetRootHTTPSession session) {
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
	
	private void parseTemplateHead(StringBuilder template, String variable) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getHtmlHead());
	}
	
	private void parseTemplateData(StringBuilder template, String variable) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getHtmlData());
	}

	private void parsePaginator(StringBuilder template, String variable, BeetRootHTTPSession session) {
		final int idx = template.indexOf(variable);
		if (idx == -1)
			return;
		template.replace(idx, idx + variable.length(), this.getPaginator(session));
	}
	
	private String parseAndGetSubResource(String origText, String resource, String type, BeetRootHTTPSession session, int origId) throws FileNotFoundException {
		final Session userSession = session.getUserSession();
		final StringBuilder sb = new StringBuilder();
		String lang = userSession.getUserLang(); 
		String currRessource = LanguageManager.getInstance().getBlockResource(resource, userSession);		
		Scanner sc = getNewScanner(currRessource);
		LOOP: while (sc.hasNextLine()) {
			
			String text = sc.nextLine();
			
			// Remove lines?
			if(ish.continueRemoval(text, userSession, "subresource"))
				continue LOOP;

			switch (type) {
				case "{#head}":
					// title ->DONE in overall method
					break;
				case "{#message}":
					if (text.contains(TAG_SEVERITY)) {
						if (this.hasSuccessMessage())
							text = text.replace(TAG_SEVERITY, "success");
						else if (this.hasWarningMessage())
							text = text.replace(TAG_SEVERITY, "warning");
						else if (this.hasErrorMessage())
							text = text.replace(TAG_SEVERITY, "error");
					}
					if (text.contains(TAG_MESSAGE)) {
						if (this.hasSuccessMessage())
							text = text.replace(TAG_MESSAGE, this.successMessage);
						else if (this.hasWarningMessage())
							text = text.replace(TAG_MESSAGE, this.warningMessage);
						else if (this.hasErrorMessage())
							text = text.replace(TAG_MESSAGE, this.errorMessage);
						this.successMessage = null;
						this.warningMessage = null;
						this.errorMessage = null;
					}
					break;
				case "{#footer}":
					if (text.contains(TAG_USERINFO)) {
						final Integer uid = userSession.getUserId();
						if (uid != null) {
							String usid = userSession.getModifyId(uid.intValue(), "users");
							if (usid == null) {
								userSession.createIdPair(uid, "users");
							}
							String user = userSession.getUserFullNameOrUserName();
							if (user != null && user.indexOf("$") > 0)
								user = user.replace("$", "\\$");
							text = text.replace(TAG_USERINFO, 
									LanguageManager.getInstance().translate("base.name.user", userSession)
									+ ": <a class=\"hideprint\" href=\"/"+lang+"/users/view?id="
									+ userSession.getModifyId(uid, "users")+"\" rel=\"nofollow\">" 
									+ user + "</a> | ");
						} else {
							text = text.replace(TAG_USERINFO," ");
						}
					}
					break;
				case "{#script}":
					if (text.contains(TAG_REDIRECT_INDEX)) {
						//URL rewrite
						if (redirectedMarker) {
							if (insertServletNameInTemplateRefs)
								text = text.replace(TAG_REDIRECT_INDEX, "window.history.pushState({}, document.title, \"/"+servletName+"/"+lang+"/"+getEntity()+"/index\");");
							else
								text = text.replace(TAG_REDIRECT_INDEX, "window.history.pushState({}, document.title, \"/"+lang+"/"+getEntity()+"/index\");");
						} else {
							text = text.replace(TAG_REDIRECT_INDEX, " ");
						}
						redirectedMarker = false;
					} 
					if (text.contains(TAG_CB_LOGIC)) {
						
						if (checkBoxLogic.length() > 0) {
							text = text.replace(TAG_CB_LOGIC, checkBoxLogic.toString());
						} else {
							text = text.replace(TAG_CB_LOGIC, " ");
						}
						checkBoxLogic = new StringBuilder();
					}
					break;
				case "{#langmenu}":
					if (text.contains(TAG_LANG_MENU_ENTRIES)) {
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
							
							String language = BeetRootConfigurationManager.getInstance().getLanguage(langs[i]);
							if (language == null)
								language = langs[i].toUpperCase();
							
							if (i+1 == langs.length) {
								entries += "<a href=\"/"+langs[i]+"/"+route+"\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".png\" alt=\""+language+"\">"+language+"</a>\n";
							} else {
								entries += "<a href=\"/"+langs[i]+"/"+route+"\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".png\" alt=\""+language+"\">"+language+"</a>\n";
								entries += "<hr class=\"menusep\">\n";
							}
						}
						text = text.replace(TAG_LANG_MENU_ENTRIES, entries);
					} else {
						text = text.replace(TAG_LANG_MENU_ENTRIES, " ");
					}
					break;
				case "{#menu}":
					final List<String> userroles = userSession.getUserRoles();
					if (text.contains(TAG_ADMIN_MENU)) {
						if (userroles.contains("Administrator")) {
							String adminMenu = parseAndGetSubResource(text, "web/html/:lang/blocks/adminmenu.html", "{$adminmenu}", session, origId);
							text = text.replace(TAG_ADMIN_MENU, adminMenu);
						}
						else
							text = text.replace(TAG_ADMIN_MENU, " ");
					}
					// Show login or logout?
					if (text.contains(TAG_LOGIN_OR_LOGOUT)) {
						if (userroles.size() > 0)
							text = text.replace(TAG_LOGIN_OR_LOGOUT, "<a href=\"/{$lang}/users/logout\">"+LanguageManager.getInstance().translate("base.name.logout", userSession)+"</a>");
						else
							text = text.replace(TAG_LOGIN_OR_LOGOUT, "<a href=\"/{$lang}/users/login\">"+LanguageManager.getInstance().translate("base.name.login", userSession)+"</a>");
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
		return origText.replace(type, sb.toString());
	}

	private void addLine(String line) {
		buffer.append(line + "\n");
	}

	private void createTemplateContent(Session userSession, BeetRootHTTPSession session) throws Exception {
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

	private Scanner getNewScannerForSnippet(String resource, String originalResource) throws FileNotFoundException {
		return getNewScanner(resource, originalResource);
	}

	private Scanner getNewScanner(Session userSession) throws FileNotFoundException {
		return getNewScanner(LanguageManager.getInstance().getResource(this.getResource(), userSession));
	}

	/**
	 * Getting a new scanner for a web resource (HTML template) to parse.
	 * 
	 * @param resource resource string, e.g. 'web/html/en/&lt;entity&gt;/index.html'
	 * @return file scanner for reading lines
	 * @throws FileNotFoundException if file is not found
	 */
	protected Scanner getNewScanner(String resource) throws FileNotFoundException {
		return this.getNewScanner(resource, null);
	}
	
	/**
	 * Getting a new scanner for a web resource (HTML template) to parse.
	 * 
	 * @param resource resource string, e.g. 'web/html/en/&lt;entity&gt;/index.html'
	 * @param originalResource resource string, e.g. 'web/html/:lang/&lt;entity&gt;/index.html';
	 * 			useful for looking up snippets
	 * @return file scanner for reading lines
	 * @throws FileNotFoundException if file is not found
	 */
	protected Scanner getNewScanner(String resource, String originalResource) throws FileNotFoundException {
		
		if (originalResource == null)
			originalResource = this.getResource();
		
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
			LOG.trace("File '{}'not found on server, looking further within archives...", filePath);
			try {
				fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		if (tryFurther) {
			tryFurther = false;
			LOG.trace("Resource '{}' doesn't exist, trying default language '{}'!", resource, LanguageManager.DEFAULT_LANG);
			resource = LanguageManager.getInstance().getResourceByLang(originalResource, LanguageManager.DEFAULT_LANG);
			try {
				if (context == null )
					fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + resource);
				else
					fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + resource);
			} catch (IOException e) {
				LOG.trace("File '{}'not found on server, looking further within archives...", filePath);
				try {
					fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}
			if (tryFurther) {
				tryFurther = false;
				LOG.trace("Resource '{}' doesn't exist, trying NO language!", resource);
				resource = LanguageManager.getInstance().getResourceWithoutLang(originalResource, LanguageManager.DEFAULT_LANG);
				try {
					if (context == null )
						fc = FileCacheManager.getInstance().findOrCreate(BeetRootConfigurationManager.getInstance().getRootPath() + resource);
					else
						fc = FileCacheManager.getInstance().findOrCreate(Web.getRealPath(context) + resource);
				} catch (IOException e) {
					LOG.trace("File '{}'not found on server, looking further within archives...", filePath);
					try {
						fc = FileCacheManager.getInstance().findOrCreateByResource("/" + resource);
					} catch (IOException e1) {
						LOG.error("No resource has been found for '{}' after trying to load it differently! "
								+ "This will lead to an exception and you quite surely missed to add this resource to you app.", filePath);
						tryFurther = false;
					}
				}				
			}			
		}
		
		if (fc == null) {
			throw new FileNotFoundException("File/resource '"+resource+"' not found!");
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
	protected StringBuilder readSnippetResource(String resource, Session userSession) throws FileNotFoundException {
		final StringBuilder sb = new StringBuilder();
		final String res = LanguageManager.getInstance().getResource(resource, userSession);
		final Scanner sc = this.getNewScannerForSnippet(res, resource);
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
		return MIME_TYPE_HTML;
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
		final Session userSession = currentSession.getUserSession();
		//cookies.set("__SESSION_ID__", cookies.read("__SESSION_ID__"), 1);

		try {
			// access control
			if (!this.hasAccess(userSession)) {
				return serveDefaultRedirectHandler(
								(BeetRootHTTPSession)session, 
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
							return serveHandler(session, this.getHandlerClass("LogoutHandler"), response);
						if (reset)
							return serveHandler(session, this.getHandlerClass("LogoutHandler"), response);
						if (response == null || response.getStatus() == HandlerResponse.STATE_OK) {// Ok in this case
							String m = LanguageManager.getInstance().translate("base.info.updated", userSession, getUpperCaseEntity());
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
							String m = LanguageManager.getInstance().translate("base.info.deleted", userSession, getUpperCaseEntity());
							// Measure-point
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {
							return serveRedirectHandler((BeetRootHTTPSession)session, response.getMessage(), MSG_TYPE_ERR);
						}
					}
					
					// Reset original ID, important!
					if (!retryCall && !requestCall) // special cases that need the right id in the readData-method!
						origId = -1;
				}
			}
			
			
			// ======== B. HTTP Get (read) ========
			
			// Read data
			final HandlerResponse response = this.readData((BeetRootHTTPSession) session, origId);
			
			
			// ======== C. Handler Response Handling ======
			
			// Change redirect
			if (session.getUri().endsWith("/users/change") && response != null)
				return serveHandler(session, this.getHandlerClass("LogoutHandler"), response);
			
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
				new MimeType(mime); // throws an exception
				if (!file.exists())
					throw new FileNotFoundException("File '"+file.getName()+"' doesn't exist (Download)!");
		        final Response downloadResponse = Response.newFixedLengthResponse(getStatus(), mime, new FileInputStream(file), file.length());
		        downloadResponse.addHeader("Content-disposition", "attachment; filename=" +file.getName());
				return downloadResponse;
			}
			
			// Custom HTTP response, e.g., JSON etc.
			if (this.isCustomResponse()) {
				return response.getHttpResponse();
			}
			
			
			// ======== D. We can have an error response here; so show it and don't proceed with parsing ======
			if (response!= null && response.getStatus() == HandlerResponse.STATE_NOT_OK) {
				final String t = response.getTitle() + ": ";
				final String m = response.getMessage();
				String e = null;
				final Exception ex = response.getException();
				if (ex != null) 
					e = ExceptionUtils.getStackTrace(ex);
				String text = t + m;
				if (e != null)
					text = text + "<br><pre style=\"box-shadow: none; border:0; font-size: 14px;\">"+e+"</pre>";
				return serveHandler(session, new ErrorHandler(Status.NOT_FOUND, LanguageManager.getInstance().translate("base.err.template.title", userSession), text), response);
			}
			
			
			// ======== E. Get HTML: Parse templates ======
			
			final String getHtml = getText((BeetRootHTTPSession)session, origId);
			
			// Template error !
			if (getHtml.startsWith("NOTFOUND:")) {
				final String t = LanguageManager.getInstance().translate("base.err.template.parsing.title", userSession);
				final String m = LanguageManager.getInstance().translate("base.err.resource.msg", userSession, getHtml.split(":")[1]);
				HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, t);
				return serveHandler(session, new ErrorHandler(Status.NOT_FOUND, LanguageManager.getInstance().translate("base.err.template.title", userSession), t+"<br>"+m), errStat);
			}
			else if (getHtml.startsWith("PARERROR:")) {
				
				final String t = LanguageManager.getInstance().translate("base.err.template.parsing.title", userSession);
				final String m = LanguageManager.getInstance().translate("base.err.template.parsing.msg", userSession, getHtml.split(":")[1]);
				HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, t);
				return serveHandler(session, new ErrorHandler(Status.BAD_REQUEST, LanguageManager.getInstance().translate("base.err.template.title", userSession), t+"<br>"+m), errStat);
			}

			
			// ======== F. Create final response ==========
			
	        return Response.newFixedLengthResponse(getStatus(), getMimeType(), getHtml);

	        
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
							+"[title][message], but it has '{}'; correct the return value of "
							+"'getCustomizedExceptionInformation' in your code!", custExInfo.length);	
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
	 * Overwrite this if your handler has a custom response; e.g.,
	 * a JSON response without any further HTML content.
	 * Such a response is usually used for live searches, etc.
	 * 
	 * @return <code>true</code> if it is a custom response
	 */
	protected boolean isCustomResponse() {
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
			return new ArrayList<String>();
		strs[1] = strs[1].replace(CHAR_SEMICOLON, "");
		strs[1] = strs[1].replace(CHAR_COLON, "");
		strs[1] = strs[1].replace(CHAR_RIGHT_CURLY_BRACKET, "");
		strs[1] = strs[1].replace(CHAR_SPACE, "");
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
		final Object obj = construct(session, handlerClass, entity, msg);
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize(session);
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
	 * @param handlerClass handler class
	 * @param stat status
	 * @return response
	 * @throws Exception
	 */
	private Response serveHandler(IHTTPSession session, Class<?> handlerClass, HandlerResponse stat) throws Exception {
		final BaseHandler handler = (BaseHandler) handlerClass.getDeclaredConstructor().newInstance();
		return this.serveHandler(session, handler, stat);
	}
	
	/**
	 * Used for login and logout handler.
	 * 
	 * @param session HTTP session
	 * @param handler handler
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
		// Important!
		handler.initialize((BeetRootHTTPSession)session);
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), handler.getText((BeetRootHTTPSession)session, -1));
	}

	/**
	 * Used when an handler access failed!
	 * 
	 * @param session HTTP session
	 * @param msg message
	 * @param messageType message type
	 * @return response
	 * @throws Exception
	 */
	private Response serveDefaultRedirectHandler(			
			BeetRootHTTPSession session, 
			String msg, int messageType) throws Exception {
		final Session userSession = session.getUserSession();
		userSession.removeAllIds(); // important, we need to generate new ones!
		final Object obj = construct(session, getDefaultHandlerClass(), getDefaultHandlerEntity(), msg);
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
        	LOG.error("*** NOTE *** : You might have forgotten to define a default handler and entioty in the configuration!");
        	throw ex;
        }
		// Language is re-written per redirect script
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
		final String ent = this.getEntity();
		final Object obj = construct(session, getRedirectHandler(), ent, msg, messageType);
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize(session);
        try {
        	// set current page if any
            final String page = (String) userSession.get("page-"+ent);
    		if (page != null) {
    			session.overwriteParameter("page", page);
    			// consume!
    			userSession.remove("page-"+ent);
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
	 * Construct a handler with success message.
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
            constructor.setAccessible(true); // Yes, we do this
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
				+ "<meta charset=\"utf-8\">\n"
				+ "<meta http-equiv=\"Refresh\" content=\"0; url=" + sn + userSession.getUserLang() + "/" + url + "\" />\n"
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
	public final BeetRootHTTPSession getCurrentSession() {
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
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + newValue + matcher.group(3));
        }
        matcher.appendTail(sb);
        return sb.toString();			
	}
	
	/**
	 * Overwrite to set your template variables.
	 * 
	 * Only use the pure names without bracket-limiters
	 * and $-sign; e.g., In template '{$name}' -> 'name' as variable.
	 * 
	 * Example: <code>setVar("name", "Gandalf")</code>.
	 * 
	 * @param session HTTP session
	 */
	public void render(BeetRootHTTPSession session) {
	}
	
	/**
	 * Overwrite to set your variables for the whole HTML page.
	 * 
	 * Only use the pure names without bracket-limiters
	 * and $-sign; e.g., In template '{$name}' -> 'name' as variable.
	 * 
	 * Example: <code>setVarAll("name", "The Almighty")</code>.
	 * 
	 * @param session HTTP session
	 */
	public void renderAll(BeetRootHTTPSession session) {
	}
	
	/**
	 * Set a template variable. Only use the pure names without bracket-limiters
	 * and $-sign; e.g., In template '{$name}' -> 'name' as variable.
	 *  
	 * @param variable template variable without brackets and '$'.
	 * @param replacement replacement text
	 */
	public final void setVar(String variable, Object replacement) {
		if (variable != null)
			vars.put(variable, replacement.toString());
	}
	
	/**
	 * Set a global variable. Variables are replace within the whole HTML page!
	 * Only use the pure names without bracket-limiters
	 * and $-sign; e.g., In template '{$name}' -> 'name' as variable.
	 *  
	 * @param variable template variable without brackets and '$'.
	 * @param replacement replacement text
	 */
	public final void setVarAll(String variable, Object replacement) {
		if (variable != null)
			varsAll.put(variable, replacement.toString());
	}
	
	/**
	 * Replace some more variables in template.
	 * If returning null, then nothing is replaced.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	private String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		for (Map.Entry<String, String> entry : vars.entrySet()) {
			final String key = entry.getKey();
			final String val = entry.getValue();
			if (val != null && text.indexOf("{$"+key+"}") != -1) {
				text = text.replace("{$"+key+"}", val);
			}
		}
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
	private String replaceVariables(String text, BeetRootHTTPSession session) {
		for (Map.Entry<String, String> entry : varsAll.entrySet()) {
			final String key = entry.getKey();
			final String val = entry.getValue();
			if (val != null && text.indexOf("{$"+key+"}") != -1) {
				text = text.replace("{$"+key+"}", val);
			}
		}
		return text;
	}

	/**
	 * Replace language variables within the whole page if any. Translations will be
	 * HTML escaped with the following characters "<>&\'".
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	private String replaceLanguageVariables(String text, BeetRootHTTPSession session) {
		return this.replaceLanguageVariablesInt(text, session, false);
	}
	
	/**
	 * Replace language variables within the whole page if any. Translations will NOT
	 * be HTML escaped at all.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or null
	 */
	private String replaceLanguageVariablesNoEscape(String text, BeetRootHTTPSession session) {
		return this.replaceLanguageVariablesInt(text, session, false);
	}
	
	/**
	 * Replace language variables within the whole page if any.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @param escape if true, basic HTML escaping is applied
	 * @return parsed text or null
	 */
	private String replaceLanguageVariablesInt(String text, BeetRootHTTPSession session, boolean escape) {
		// Only when switched on!
		if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
			int idx = -1;
			while ((idx = text.indexOf(TAG_PREFIX_LANG)) != -1) {
				final int pos1 = idx + TAG_PREFIX_LANG.length();
				final int pos2 = text.indexOf("}", idx + TAG_PREFIX_LANG.length());
				int posC = text.indexOf(",", idx + TAG_PREFIX_LANG.length());
				// if a comma is found outside the tag it refers not ro a replace variable!
				if (posC > pos2)
					posC = -1;
				String totrans = null; 
				String subValues = null; 
				String subValuesArr[] = null; 
				if (posC == -1) {
					totrans = text.substring(pos1, pos2); // no values to replace
				}
				else {
					totrans = text.substring(pos1, posC);
					subValues = text.substring(posC + 1, pos2);
					if (subValues.length() > 0) {
						subValuesArr = subValues.trim().split("\\s*,\\s*");
					}
				}
				String trans = "";
				if (totrans.length() > 0)
					trans = LanguageManager.getInstance().translateTemplate(totrans.trim(), session.getUserSession(), subValuesArr, escape);
				text = text.substring(0, idx) + trans + text.substring(pos2 + 1);
			}
		}
		return text;
	}
	
	/**
	 * Get HTML paginator code. Must only be implemented by index handlers
	 * and is only called if there's a "{$paginator}" tag in a template.
	 * @param session HTTP session  
	 * @return HTML paginator code
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
	 * Show language menu?
	 * 
	 * @param userSession user session, possible even
	 * a temporary session from a not logged in user
	 * 
	 * @return true if language menu should be shown
	 */
	public boolean showLangMenu(Session userSession) {
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
	 * Add proper logic for check-boxes.
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

	/**
	 * Login marker method.
	 * 
	 * @param redirectLogin redirected from login?
	 */
	protected void loginMarker(boolean redirectLogin) {
	}
	
	@SuppressWarnings("unused")
	private void processTime() {
		// stop stop-watch and measure
		final long handlerEnd = System.currentTimeMillis();
		final long duration = handlerEnd - baseHandlerStart;
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

		private static final String ROLE = "role";
		private static final String NOT_ROLE = "!role";
		private static final String ENTITY = "entity";
		private static final String NOT_ENTITY = "!entity";
		private static final String ACTION = "action";
		private static final String NOT_ACTION = "!action";
		
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
		 * @param layer 'overall', 'template' or 'sub-resource'
		 * @return true if removal should continue, otherwise false
		 */
		protected boolean continueRemoval(String text, Session userSession, String layer) {
			boolean continueRemoval = false;
			// Get states
			Map<String, Boolean> currStates = ifTagStates.get(layer);
			// Add initial states if not present for layer
			if (currStates == null) {
				currStates = new HashMap<String, Boolean>();
				currStates.put(ROLE, Boolean.FALSE);
				currStates.put(NOT_ROLE, Boolean.FALSE);
				currStates.put(ENTITY, Boolean.FALSE);
				currStates.put(NOT_ENTITY, Boolean.FALSE);
				currStates.put(ACTION, Boolean.FALSE);
				currStates.put(NOT_ACTION, Boolean.FALSE);
				ifTagStates.put(layer, currStates);
			}
			
			
			// deal with role-specific sections
			if (text.contains("$endif-role")) {
				currStates.put(ROLE, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(ROLE).booleanValue())
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
					currStates.put(ROLE, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			if (text.contains("$endif-!role")) {
				currStates.put(NOT_ROLE, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(NOT_ROLE).booleanValue())
				continueRemoval = true;
			if (text.contains("$if-!role")) {
				final List<String> roles = userSession.getUserRoles();
				final List<String> tempRoles = handler.getIfValuesFromTemplate(text);
				boolean roleAvailable = false;
				final Iterator<String> iterator = tempRoles.iterator();
				while (!roleAvailable && iterator.hasNext()) {
					final String tempRole = iterator.next();
					roleAvailable = roles.contains(tempRole);
				}
				if (roleAvailable)
					currStates.put(NOT_ROLE, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			
			// deal with entity-specific sections
			if (text.contains("$endif-entity")) {
				currStates.put(ENTITY, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(ENTITY).booleanValue())
				continueRemoval = true;
			if (text.contains("$if-entity")) {
				final List<String> entities = handler.getIfValuesFromTemplate(text);
				if (!entities.contains(entity))
					currStates.put(ENTITY, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			if (text.contains("$endif-!entity")) {
				currStates.put(NOT_ENTITY, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(NOT_ENTITY).booleanValue())
				continueRemoval = true;
			if (text.contains("$if-!entity")) {
				final List<String> entities = handler.getIfValuesFromTemplate(text);
				if (!entities.contains(entity))
					currStates.put(NOT_ENTITY, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			
			// deal with action-specific sections
			if (text.contains("$endif-action")) {
				currStates.put(ACTION, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(ACTION).booleanValue())
				continueRemoval = true;
			if (text.contains("$if-action")) {
				final List<String> actions = handler.getIfValuesFromTemplate(text);
				if (!actions.contains(action))
					currStates.put(ACTION, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			if (text.contains("$endif-!action")) {
				currStates.put(NOT_ACTION, Boolean.FALSE);
				continueRemoval = true;
			}
			if (currStates.get(NOT_ACTION).booleanValue())
				continueRemoval = true;
			if (text.contains("$if-!action")) {
				final List<String> actions = handler.getIfValuesFromTemplate(text);
				if (actions.contains(action))
					currStates.put(NOT_ACTION, Boolean.TRUE); // start removal
				continueRemoval = true;
			}
			
			return continueRemoval;
		}		
	}

}
