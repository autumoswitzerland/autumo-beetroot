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
package ch.autumo.beetroot.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

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
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.cache.FileCache;
import ch.autumo.beetroot.cache.FileCacheManager;
import ch.autumo.beetroot.handler.users.LogoutHandler;
import jakarta.activation.MimeType;

/**
 * Base handler - The "Heart" of beetRoot.
 */
public abstract class BaseHandler extends DefaultHandler implements Handler {
	
	private final static Logger LOG = LoggerFactory.getLogger(BaseHandler.class.getName());

	public static final int MSG_TYPE_INFO = 0;
	public static final int MSG_TYPE_WARN = 1;
	public static final int MSG_TYPE_ERR = -1;
	
	private StringBuffer buffer = new StringBuffer();
	
	protected TreeMap<Integer, String> columns = null;
	protected String uniqueFields[] = null;
	protected List<String> transientFields = new ArrayList<String>();
	protected String entity = null;
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
	
	private boolean ifroleactive = false;
	private boolean ifroleactive_sub = false;
	private boolean ifroleactive_templ = false;
	
	private boolean redirectedMarker = false;
	//private boolean loginMarker = false;

	
	public BaseHandler() {
	}

	public BaseHandler(String entity) {
		
		this.entity = entity;
	}
	
	/**
	 * Every handler MUST be initialized!
	 * 
	 * @param session session
	 */
	public void initialize(BeetRootHTTPSession session) {
		
		servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
		if (servletName != null && servletName.length() != 0)
			insertServletNameInTemplateRefs = true; 
		
		// nothing to do!
		if (entity == null || entity.length() == 0)
			return;
		
		// nothing to do!
		if (this.hasNoColumnsConfig())
			return;
		
		this.columns = new TreeMap<Integer, String>();
		
		final List<String> fallBackList = new ArrayList<String>();
		
		Session userSession = SessionManager.getInstance().findOrCreate(session);
		String res = null;
		
		// Special case JSON: overwrite languages, not needed!
		if (session.getUri().endsWith(Constants.JSON_EXT)) {
			res = "web/html/"+entity+"/columns.cfg";
		} else {
			if (userSession == null)
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+"/columns.cfg", Utils.normalizeUri(session.getUri()));
			else
				res = LanguageManager.getInstance().getResource("web/html/:lang/"+entity+"/columns.cfg", userSession);
		}
		
    	FileCache fc = null;
		String prePath = "";
    	String filePath = null;
    	boolean tryFurther = false;
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();

    	if (context != null)
    		prePath = Utils.getRealPath(context);
		
		try {
			filePath = prePath + res;
			fc = FileCacheManager.getInstance().findOrCreate(filePath);
		} catch (IOException e) {
			LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
			try {
				filePath = "/" + res;
				fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		
		if (tryFurther) {
			
			tryFurther = false;
			
			LOG.trace("Resource '" + res + "' doesn't exist, trying with default language '"+LanguageManager.DEFAULT_LANG+"'!");
			
			// Special case JSON: overwrite languages, not needed!
			if (session.getUri().endsWith(Constants.JSON_EXT)) {
				res = "web/html/"+entity+"/columns.cfg";
			} else {
				if (userSession == null)
					res = LanguageManager.getInstance().getResource("web/html/"+LanguageManager.DEFAULT_LANG+"/"+entity+"/columns.cfg", Utils.normalizeUri(session.getUri()));
				else
					res = LanguageManager.getInstance().getResource("web/html/"+LanguageManager.DEFAULT_LANG+"/"+entity+"/columns.cfg", userSession);
			}
			
			try {
				filePath = prePath + res;
				fc = FileCacheManager.getInstance().findOrCreate(filePath);
			} catch (IOException e) {
				LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
				try {
					filePath = "/" + res;
					fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}			
				
			if (tryFurther) {
				
				tryFurther = false;
				
				LOG.trace("Resource '"+res+"' doesn't exist, trying with NO language!");
				res = LanguageManager.getInstance().getResourceWithoutLang("web/html/"+entity+"/columns.cfg", Utils.normalizeUri(session.getUri()));
				
				try {
					filePath = prePath + res;
					fc = FileCacheManager.getInstance().findOrCreate(filePath);
				} catch (IOException e) {
					LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						filePath = "/" + res;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
					} catch (IOException e1) {
						LOG.debug("Resource '"+res+"' doesn't exist, no columns used!");
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
		    	
		    	//line = line.replaceAll("\n", "");
		    	final String cfgLine = line.trim();
		    	if (cfgLine.length() != 0 && cfgLine.indexOf("=") != -1) {
		    		
		    		final String configPair[] = cfgLine.split("=");
		    		
		    		configPair[0] = configPair[0].trim();
		    		configPair[1] = configPair[1].trim();
		    		
		    		String newCfgLine = null;
		    		boolean added = false;
		    		
		    		// First check for special configurations
		    		// unique
		    		if (configPair[0].equals("unique")) {
		    			
		    			configPair[1].replace(" ", "");
		    			uniqueFields = configPair[1].split(",");
		    			continue LOOP;
		    		}
		    		// transient (not store in DB)
		    		if (configPair[0].equals("transient")) {
		    			
		    			configPair[1].replace(" ", "");
		    			transientFields =  Arrays.asList(configPair[1].split(","));
		    			continue LOOP;
		    		}
		    		
		    		// If no config prefix is defined, all config lines are taken!
		    		// This makes sense if one doesn't need to distinguish between
		    		// list and single records data field names aka GUI column names.
		    		String templateResource = getResource();
		    		if (templateResource != null && templateResource.length() != 0) {
		    			
			    		templateResource = templateResource.substring(templateResource.lastIndexOf("/") + 1, templateResource.length());
			    		
			    		switch (templateResource) {

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
				    			}
				    			break;
				    			
			    			default:
			    		    	columns.put(Integer.valueOf(++l), cfgLine);
			    		    	added = true;
			    		    	break;
			    		}
			    		
			    		if (!added)
			    			fallBackList.add(cfgLine);
		    		}
		    		
		    	}
		    }
		    
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
	 * Access column values. '[1] [colName=GUI Col Name 1]'
	 * Idx starts wiht 1!
	 * 
	 * @return colum values
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
	 * Get SQL fields, e.g. 'col1, col2, col3'.
	 * 
	 * @return SQL query fields
	 */
	public String getColumnsForSql() {

		String queryfields = "";
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {
			
			final String cfgLine = columns.get(Integer.valueOf(i));
			final String params[] = cfgLine.split("=");
			final String colName = params[0].trim();
			
			if (transientFields.contains(colName))
				continue LOOP;
			
			if (columns.size() == i)
				queryfields += colName;
			else
				queryfields += colName + ", ";
		}
		
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
	 */
	public String getInsertValues(BeetRootHTTPSession session) throws Exception {
		
		final boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
		
		String clause = "";
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {
			
			final String col[] = getColumn(i);

			if (transientFields.contains(col[0]))
				continue LOOP;
			
			String val = session.getParms().get(col[0]);		
			
			val = Utils.escapeValuesForDb(val);
			
			if (dbPwEnc && col[0].equals("password")) {
				val = Utils.encode(val, SecureApplicationHolder.getInstance().getSecApp());
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
			if (col[1].equals("created")) {
				if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
					if (columns.size() == i)
						clause += Utils.nowTimeStamp();
					else
						clause += Utils.nowTimeStamp() + ", ";
				} else {
					if (columns.size() == i)
						clause += "'" + Utils.nowTimeStamp() + "'";
					else
						clause += "'" + Utils.nowTimeStamp() + "', ";
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

		return clause;
	}
	
	/**
	 * Get SQL update set clause.
	 * 
	 * @param session HTTP session
	 * @return SQL update clause
	 */
	public String getUpdateSetClause(BeetRootHTTPSession session) throws Exception {

		final boolean dbPwEnc = BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded");
		final boolean dbAutoMod = BeetRootConfigurationManager.getInstance().getYesOrNo("db_auto_update_modified");
		
		final Session userSession = session.getUserSession();
		
		String clause = "";
		
		int uid = userSession.getUserId();
		int origId = -1; 
		boolean currentUser = false;
		if (entity.equals("users")) { // we have a user entity in process
			final String obfSessId = session.getParms().get("id");
			origId = userSession.getOrigId(obfSessId, getEntity());
			if (origId == uid)
				currentUser = true;
		}
		
		LOOP: for (int i = 1; i <= columns.size(); i++) {

			final String col[] = getColumn(i);
			
			if (transientFields.contains(col[0]))
				continue LOOP;
			
			String val = session.getParms().get(col[0]);
			
			val = Utils.escapeValuesForDb(val);
			
			if (dbPwEnc && col[0].equals("password")) {
				val = Utils.encode(val, SecureApplicationHolder.getInstance().getSecApp());
			}
			
			// Informix wants 't' or 'f'
			if (val.equalsIgnoreCase("true")) {
				val = "1";
			}
			if (val.equalsIgnoreCase("false")) {
				val = "0";
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

		// Doesn't matter, if 'modified' is configured in 'colums.cfg' or not
		// And we assume the column exists as specified by design!
		// But we don't update it, if the user choses to modify it by himself (GUI).
		if (dbAutoMod && clause.indexOf("modified=") != 1) {
			if (BeetRootDatabaseManager.getInstance().isOracleDb())
				clause += ", modified=" + Utils.nowTimeStamp() + "";
			else
				clause += ", modified='" + Utils.nowTimeStamp() + "'";
		}
		
		return clause;
	}

	/**
	 * Check if unique fields are unique.
	 * 
	 * @param session sessions
	 * @param preSql pre-parsed SQL without unique fields
	 * @param operation saved or updated
	 * @return response or null, null means success, response's status must be checked!
	 * @throws Exception
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
	 * Process JSON templates.
	 * 
	 * @param session beetRoot session
	 * @return result
	 */
	private String processJSON(BeetRootHTTPSession session) {
		
		final String currRessource =  this.getResource();

		// prepare text buffer
		final StringBuffer sb = new StringBuffer();
		
		// process JSON templates
		Scanner sc = null;
		try {
			sc = getNewScanner(currRessource);
			while (sc.hasNextLine()) {
				
				String text = sc.nextLine();
				
				// custom parse?
				text = parse(text, session);
				
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
	 * @param session beetroot session
	 * @param origId original DB id
	 * @return whole parsed HTML page
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
		String userfull = userSession.getUserFullNameOrUserName();
		String currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/layout.html", userSession);
		String templateResource = getResource();
		
		// prepare text buffer
		final StringBuffer sb = new StringBuffer();
		
		// process templates
		Scanner sc = null;
		try {
			
			sc = getNewScanner(currRessource);

			LOOP: while (sc.hasNextLine()) {
				
				String text = sc.nextLine();

				
				// deal with role-specific sections
				
				if (text.contains("$endifrole")) {
					ifroleactive = false;
					continue LOOP;
				}
				if (ifroleactive)
					continue LOOP;
				
				if (text.contains("$ifrole")) {
					final List<String> roles = this.getRolesFromTemplate(text);
					if (!roles.contains(userSession.getUserRole()))
						ifroleactive = true;
					
					continue LOOP;
				}

				
				// layout templates and main template
				if (text.contains("{#head}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/head.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#head}", session);
					
				} else if (text.contains("{#header}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/header.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#header}", session);

				} else if (text.contains("{#langmenu}")) {
					
					if (this.showMenu(userSession)) {
						
						if (LanguageManager.getInstance().getConfiguredLanguages().length > 1) {
						
							currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/lang_menu.html", userSession);
							text = parseAndGetSubResource(text, currRessource, "{#langmenu}", session);
							
						} else {
							text = "";
						}
						
					} else {
						text = "";
					}

				} else if (text.contains("{#menu}")) {
					
					if (this.showMenu(userSession)) {
						
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/menu.html", userSession);
						text = parseAndGetSubResource(text, currRessource, "{#menu}", session);
						
					} else {
						text = "";
					}

				} else if (text.contains("{#message}")) {
					
					if (this.hasAnyMessage()) {
						currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/message.html", userSession);
						text = parseAndGetSubResource(text, currRessource, "{#message}", session);
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
						
						if (text.contains("{$csrfToken}")) {
							
							if (userSession != null) {
								
								final String formCsrfToken = userSession.getFormCsrfToken();
								
								if (formCsrfToken != null && formCsrfToken.length() != 0)
									text = text.replaceAll("\\{\\$csrfToken\\}", formCsrfToken);
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
							
							text = text.replaceAll("\\{\\$id\\}", "" + modifyID);
							text = text.replaceAll("\\{\\$dbid\\}", "" + origId);
						}

						// language
						if (text.contains("{$lang}")) {
							text = text.replaceAll("\\{\\$lang\\}", lang);
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
					text = parseAndGetSubResource(text, currRessource, "{#footer}", session);
					
				} else if (text.contains("{#script}")) {
					
					currRessource = LanguageManager.getInstance().getBlockResource("web/html/:lang/blocks/script.html", userSession);
					text = parseAndGetSubResource(text, currRessource, "{#script}", session);
					
				} else if (text.contains("{$redirectIndex}")) {
					
					//URL rewrite
					if (redirectedMarker) {
						if (insertServletNameInTemplateRefs)
							text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/"+servletName+"/"+lang+"/"+getEntity()+"/index\");");
						else
							text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/"+lang+"/"+getEntity()+"/index\");");
					}
					/** Unused
					else {
						if (loginMarker)
							text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/\" + \"users/login\");");
						else
							text = text.replace("{$redirectIndex}", " ");
					}
					*/
					
					//loginMarker = false;
					redirectedMarker = false;
					
				} else if (text.contains("{$checkBoxLogic}")) {
					
					if (checkBoxLogic.length() > 0) {
						text = text.replace("{$checkBoxLogic}", checkBoxLogic.toString());
					} else {
						text = text.replace("{$checkBoxLogic}", " ");
					}
					checkBoxLogic = new StringBuffer();
				}
				
				
				// General variables!
				
				// title
				if (text.contains("{$title}"))
					text = text.replaceAll("\\{\\$title\\}", getUpperCaseEntity());

				// CSRF token
				if (text.contains("{$csrfToken}")) {
					
					final String formCsrfToken = userSession.getFormCsrfToken();
					if (formCsrfToken != null && formCsrfToken.length() != 0) {
						text = text.replaceAll("\\{\\$csrfToken\\}", formCsrfToken);
					}
				}

				// user
				if (user != null && text.contains("{$user}")) {
					text = text.replaceAll("\\{\\$user\\}", user);
				}

				// user full
				if (userfull != null && text.contains("{$userfull}")) {
					text = text.replaceAll("\\{\\$userfull\\}", userfull);
				}
				
				// language
				if (text.contains("{$lang}")) {
					text = text.replaceAll("\\{\\$lang\\}", lang);
				}				
				
				
				// User settings variables!
				
				// theme
				if (text.contains("{$theme}")) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = text.replaceAll("\\{\\$theme\\}", "dark");
					else
						text = text.replaceAll("\\{\\$theme\\}", theme);
				}				
				if (text.contains("{$antitheme}")) {
					final String theme = userSession.getUserSetting("theme");
					if (theme == null)
						text = text.replaceAll("\\{\\$antitheme\\}", "default");
					else
						if (theme.equals("default"))
							text = text.replaceAll("\\{\\$antitheme\\}", "dark");
						else
							text = text.replaceAll("\\{\\$antitheme\\}", "default");
				}
				
				
				// Add servlet URL part.
				// href="/
				// src="/
				// action="/
				if (insertServletNameInTemplateRefs) {
					// NOTE: this only handles one occurrence, if there is another link without
					// 'http' or 'https' on the same line it is not handled yet :(
					// But external links should be on one line without any other references
					text = text.replaceAll("href=\\\"", "href=\"/"+servletName);
					text = text.replaceAll("src=\\\"", "src=\"/"+servletName);
					text = text.replaceAll("action=\\\"", "action=\"/"+servletName);
					
					// hack: we have to re-replace http and https links....
					text = text.replaceAll("href=\\\"/"+servletName+"http", "href=\"http");
					text = text.replaceAll("src=\\\"/"+servletName+"http", "src=\"http");
					text = text.replaceAll("action=\\\"/"+servletName+"http", "action=\"http");
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
	
	private String parseAndGetSubResource(String origText, String resource, String type, BeetRootHTTPSession session) throws FileNotFoundException {
		
		final Session userSession = session.getUserSession();
		final StringBuffer sb = new StringBuffer();
		
		String lang = LanguageManager.getInstance().getLanguage(userSession);
		String currRessource = LanguageManager.getInstance().getBlockResource(resource, userSession);		

		Scanner sc = getNewScanner(currRessource);
		
		LOOP: while (sc.hasNextLine()) {
			
			String text = sc.nextLine();
			
			
			// deal with role-specific sections
			
			if (text.contains("$endifrole")) {
				ifroleactive_sub = false;
				continue LOOP;
			}
			if (ifroleactive_sub)
				continue LOOP;
			
			if (text.contains("$ifrole")) {
				final List<String> roles = this.getRolesFromTemplate(text);
				if (!roles.contains(userSession.getUserRole()))
					ifroleactive_sub = true;
				
				continue LOOP;
			}

			
			switch (type) {
			
				case "{#head}":

					if (text.contains("{$title}"))
						text = text.replace("{$title}", getUpperCaseEntity());
					break;

				case "{#message}":

					if (text.contains("{$severity}")) {
						
						if (this.hasSuccessMessage())
							text = text.replace("{$severity}", "success");
						else if (this.hasWarningMessage())
							text = text.replace("{$severity}", "warning");
						else if (this.hasErrorMessage())
							text = text.replace("{$severity}", "error");
					}
					
					if (text.contains("{$message}")) {
						if (this.hasSuccessMessage())
							text = text.replace("{$message}", this.successMessage);
						else if (this.hasWarningMessage())
							text = text.replace("{$message}", this.warningMessage);
						else if (this.hasErrorMessage())
							text = text.replace("{$message}", this.errorMessage);
						
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

							final String user = userSession.getUserFullNameOrUserName();
							
							text = text.replace("{$userinfo}", 
									LanguageManager.getInstance().translate("base.name.user", userSession)
									+ ": <a class=\"hideprint\" href=\"/"+lang+"/users/view?id="
									+ userSession.getModifyId(uid, "users")+"\" rel=\"nofollow\">" 
									+ user + "</a> | ");
						} else {
							
							text = text.replace("{$userinfo}", " ");
						}
					}
					
					break;
				
				case "{#script}":
					
					if (text.contains("{$redirectIndex}")) {
						
						//URL rewrite
						if (redirectedMarker) {
							if (insertServletNameInTemplateRefs)
								text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/"+servletName+"/"+lang+"/"+getEntity()+"/index\");");
							else
								text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/"+lang+"/"+getEntity()+"/index\");");
						} else {
							text = text.replace("{$redirectIndex}", " ");
						}
						redirectedMarker = false;
		
						/** Unused
						else {
							if (loginMarker)
								text = text.replace("{$redirectIndex}", "window.history.pushState({}, document.title, \"/\" + \"users/login\");");
							else
								text = text.replace("{$redirectIndex}", " ");
						}
						loginMarker = false;
						*/
					} 
					
					if (text.contains("{$checkBoxLogic}")) {
						
						if (checkBoxLogic.length() > 0) {
							text = text.replace("{$checkBoxLogic}", checkBoxLogic.toString());
						} else {
							text = text.replace("{$checkBoxLogic}", " ");
						}
						checkBoxLogic = new StringBuffer();
					}
					
					break;
					
				case "{#langmenu}":
					
					if (text.contains("{$lang_menu_entries}")) {
						
						String entries = "";
						final String langs[] = LanguageManager.getInstance().getConfiguredLanguages();
						for (int i = 0; i < langs.length; i++) {
							if (i+1 == langs.length) {
								entries += "<a href=\"/"+langs[i]+"/"+getEntity()+"/index\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".gif\">"+langs[i].toUpperCase()+"</a>\n";
							} else {
								entries += "<a href=\"/"+langs[i]+"/"+getEntity()+"/index\"><img class=\"imglang\" src=\"/img/lang/"+langs[i]+".gif\">"+langs[i].toUpperCase()+"</a>\n";
								entries += "<hr class=\"menusep\">\n";
							}
						}
						text = text.replace("{$lang_menu_entries}", entries);
					} else {
						text = text.replace("{$lang_menu_entries}", " ");
					}
					
					break;
					
				case "{#menu}":
					
					final String userrole = userSession.getUserRole();

					/**
					// This is only a cosmetic precaution, menus shouldn't
					// be shown anyways without a logged-in user.
					if (userrole == null && text.contains("<a href=") || text.contains("<hr")) {
						text  = " ";
					}
					*/
					
					if (text.contains("{$adminmenu}")) {
						
						if (userrole != null && userrole.length() != 0 && userrole.equalsIgnoreCase("Administrator")) {
							String adminMenu = parseAndGetSubResource(text, "web/html/:lang/blocks/adminmenu.html", "{$adminmenu}", session);
							text = text.replace("{$adminmenu}", adminMenu);
						}
						else
							text = text.replace("{$adminmenu}", " ");
					}
					
					// Show login or logout?
					if (text.contains("{$loginorlogout}")) {
						if (userrole != null)
							text = text.replace("{$loginorlogout}", "<a href=\"/{$lang}/users/logout\">"+LanguageManager.getInstance().translate("base.name.logout", userSession)+"</a>");
						else
							text = text.replace("{$loginorlogout}", "<a href=\"/{$lang}/users/login\">"+LanguageManager.getInstance().translate("base.name.login", userSession)+"</a>");
					}
					
					break;
					
				case "{$adminmenu}":
					
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
			
			// deal with role-specific sections
			
			if (text.contains("$endifrole")) {
				ifroleactive_templ = false;
				continue LOOP;
			}
			if (ifroleactive_templ)
				continue LOOP;
			
			if (text.contains("$ifrole")) {
				final List<String> roles = this.getRolesFromTemplate(text);
				if (!roles.contains(userSession.getUserRole()))
					ifroleactive_templ = true;
				
				continue LOOP;
			}
			
			addLine(parse(text, session));
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
	 * @param resource resource string, e.g. 'web/html/:lang/<entity>/index.html'.
	 * @return file scanner for reading lines
	 * @throws FileNotFoundException
	 */
	protected Scanner getNewScanner(String resource) throws FileNotFoundException {
		
		String prePath = "";
    	String filePath = null;
    	FileCache fc = null;
    	boolean tryFurther = false;
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
    	if (context != null)
    		prePath = Utils.getRealPath(context);
		
		try {
			filePath = prePath + resource;
			fc = FileCacheManager.getInstance().findOrCreate(filePath);
		} catch (IOException e) {
			//LOG.info("File '" + filePath + "'not found on server, looking further within archives...");
			try {
				filePath = "/" + resource;
				fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
			} catch (IOException e1) {
				tryFurther = true;
			}
		}
		
		if (tryFurther) {

			tryFurther = false;
			
			LOG.trace("Resource '"+resource+"' doesn't exist, trying default language '"+LanguageManager.DEFAULT_LANG+"'!");
			resource = LanguageManager.getInstance().getResourceByLang(this.getResource(), LanguageManager.DEFAULT_LANG);

			try {
				filePath = prePath + resource;
				fc = FileCacheManager.getInstance().findOrCreate(filePath);
			} catch (IOException e) {
				LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
				try {
					filePath = "/" + resource;
					fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
				} catch (IOException e1) {
					tryFurther = true;
				}
			}
			
			if (tryFurther) {

				tryFurther = false;
				
				LOG.trace("Resource '"+resource+"' doesn't exist, trying NO language!");
				resource = LanguageManager.getInstance().getResourceWithoutLang(this.getResource(), LanguageManager.DEFAULT_LANG);
				
				try {
					filePath = prePath + resource;
					fc = FileCacheManager.getInstance().findOrCreate(filePath);
				} catch (IOException e) {
					LOG.trace("File '" + filePath + "'not found on server, looking further within archives...");
					try {
						filePath = "/" + resource;
						fc = FileCacheManager.getInstance().findOrCreateByResource(filePath);
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
		
			
			// ======== A. HTTP Posts ========
			
			// working...
			if (Method.POST.equals(method)) { 
				
				final boolean reset = session.getUri().endsWith("/users/reset");
				final boolean change = session.getUri().endsWith("/users/change");
				final boolean add = session.getUri().endsWith("/"+getEntity()+"/add");
				
				if (origId != -1 || change || reset || add ) {
				
					// we have an id, ergo it is a CUD operation
				
					final String _method = session.getParms().get("_method");
					final String upMethod = urlParams.get("_method");
					final boolean retryCall = upMethod != null && upMethod.length() != 0 && upMethod.equals("RETRY");
					final boolean requestCall = _method != null && _method.length() != 0 && _method.equals("REQUEST");
					
					
					// ======== 1. Retry call test =================
					
					if (retryCall) {
						
						// Failed 'add'; do nothing and read the formular again
						// We have to add the RETRY again to the session params!
						final List<String> retry = new ArrayList<String>();
						retry.add("RETRY");
						session.getParameters().put("_method", retry);
							
						// create a new ID pair, id somehow the orig has been lost, shouldn't happen actually!
						userSession.createIdPair(origId, getEntity());

						
					// ======== 2. Request call test ================
						
					} else if (requestCall) {
						
						// we simply let the code run further till handler read function
						// -> used for reads that need a post form

						
					// ======== 3. Main HTTP: No method (save) ======
						
					} else if (_method == null || _method.length() == 0) { 
						
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
						
					} else if (_method.equals("PUT")) {  // and password reset
						
						// edit with id -> update
						HandlerResponse response = this.updateData((BeetRootHTTPSession)session, origId);
						
						if (change)
							return serveHandler(session, new LogoutHandler(), response);
						if (reset)
							return serveHandler(session, new LogoutHandler(), response);

						if (response == null || response.getStatus() == HandlerResponse.STATE_OK) {// Ok in this case
							
							//String m = LanguageManager.getInstance().translate("base.info.updated", userSession, origId, getUpperCaseEntity());
							String m = LanguageManager.getInstance().translate("base.info.updated", userSession, getUpperCaseEntity());
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {

							Map<String, String> params = session.getParms();
							params.put("_method", "RETRY");
							
							return serveHandler((BeetRootHTTPSession)session, getEntity(), this.getClass(), params, response.getMessage(), MSG_TYPE_ERR);
						}

						
					// ======== 5. Main HTTP: POST (delete) ========
						
					} else if (_method.equals("POST")) {
						
						// delete with id
						HandlerResponse response = this.deleteData((BeetRootHTTPSession)session, origId);

						if (response == null || response.getStatus() == HandlerResponse.STATE_OK) { // Ok in this case
							
							//String m = LanguageManager.getInstance().translate("base.info.deleted", userSession, origId, getUpperCaseEntity());
							String m = LanguageManager.getInstance().translate("base.info.deleted", userSession, getUpperCaseEntity());
							return serveRedirectHandler((BeetRootHTTPSession)session, m);
						}
						
						if (response.getStatus() == HandlerResponse.STATE_NOT_OK) {
							//Not really reachable, only exception could be thrown, e.g. the record doesn't exist!
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
			
	        return Response.newFixedLengthResponse(getStatus(), getMimeType(), getHtml);
        
		} catch (Exception e) {
		
			// The framework user might have messed up things!
			String res = getResource();
			final String err1 = getTemplateEngineErrorTitle(userSession, res); 
			final String err2 = err1 + "<br><br>" + getTemplateEngineErrorMessage(userSession, res); 
			
			LOG.error(err1, e);
			
			final HandlerResponse errStat = new HandlerResponse(HandlerResponse.STATE_NOT_OK, err1);
			try {
				
				return serveHandler(session, new ErrorHandler(Status.INTERNAL_ERROR, LanguageManager.getInstance().translate("base.err.template.title", userSession), err2), errStat);
				
			} catch (Exception excalibur) {
				
				// At this point we cannot do anything anymore ... *sniff*
				LOG.error("TECH-FATAL:"+excalibur.getMessage(), excalibur);
				excalibur.printStackTrace();
				return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/html", err1);
			}			
		}
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
	 * Get roles from template.
	 * 
	 * @param roleLine text line within template with roles
	 * @return all roles
	 */
	private List<String> getRolesFromTemplate(String roleLine) {
		
		String strs[] = roleLine.split("=", 2);
		if (strs.length != 2)
			return Arrays.asList(new String[] {});
		
		strs[1] = strs[1].replaceAll(";", "");
		strs[1] = strs[1].replaceAll(":", "");
		strs[1] = strs[1].replaceAll("}", "");
		strs[1] = strs[1].replaceAll(" ", "");
		strs[1] = strs[1].trim();
		
		final String roles[] = strs[1].split(","); 		
		return Arrays.asList(roles);
	}
	
	/** Special case serve; only use with care - retry uses it */
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
        
        final Response response = responder.get(ur, newParams, (org.nanohttpd.protocols.http.IHTTPSession)session);
		return response;
	}
	
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
	
	private Response serveRedirectHandler(BeetRootHTTPSession session, String msg) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		userSession.removeAllIds(); // important, we need to generate new ones!
		
		Object obj = construct(session, getRedirectHandler(), getEntity(), msg);
		
		if (!(obj instanceof BaseHandler)) {
			return (Response) obj;
		}
        
		final BaseHandler handler = (BaseHandler) obj;
        handler.initialize(session);
		
		// read index data
        try {
        	
        	handler.readData(session, -1);
        	
        } catch (Exception ex) {
        	
        	LOG.error("*** NOTE *** : You might have forgotten to overwrite a handler, so beetRoot can choose the right redirect handler for an entity!");
        	LOG.error("    -> This is especially necessary, if you have defined transient colums in 'columns.cfg' !");
        	
        	throw ex;
        }
		// lang is re-written per redirect script
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), handler.getText(session, -1));		
	}
		
	private Object construct(BeetRootHTTPSession session, Class<?> handlerClass, String entity, String msg) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		Constructor<?> constructor = null;
        final Constructor<?> constructors[] = handlerClass.getDeclaredConstructors();
		int ip = 2;
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
        	
            handler = (BaseHandler) constructor.newInstance(entity, msg);
            
		} catch (Exception e) {

			String err = "Handler error! - No implementation found for handler class '"+handlerClass.toString()+"'!";
			LOG.error(err, e);
			
			String t = "<h1>"+LanguageManager.getInstance().translate("base.err.handler.impl.title", userSession)+"</h1>";
			String m = "<p>"+LanguageManager.getInstance().translate("base.err.handler.impl.msg", userSession, handlerClass.toString(), e.getMessage())+"</p>";
			
			return Response.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/html", t+m);
		}	
        
        return handler;
	}

	private Response refreshRoute(BeetRootHTTPSession session, String route, String msg) {
		
		if (route.startsWith("/"))
			route = route.substring(1, route.length());
		
		final Session userSession = session.getUserSession();
		
		if (msg != null && msg.length() !=0 ) {
			
			try {
				
				msg = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString());
				if (msg.contains("?"))
					msg = "&msg="+msg+"&sev=w";
				else
					msg = "?msg="+msg+"&sev=w";
				
			} catch (UnsupportedEncodingException e) {
				
				// zzz....
				if (msg.contains("?"))
					msg = "&msg="+LanguageManager.getInstance().translate("base.info.session.inv.refresh", userSession)+"&sev=w";
				else
					msg = "?msg="+LanguageManager.getInstance().translate("base.info.session.inv.refresh", userSession)+"&sev=w";
			}
		}
		
		if (msg == null)
			msg = "";
		
		String sn = "/";
		if (insertServletNameInTemplateRefs)
			sn = "/" + servletName + "/";
		
		String refreshText = 
				  "<!DOCTYPE html>\n"
				+ "<html lang=\"en\">\n"
				+ "<head>\n"
				+ "	<meta charset=\"utf-8\">\n"
				+ "	<meta http-equiv=\"Refresh\" content=\"0; url=" + sn + userSession.getUserLang() + "/" + route + msg + "\" />\n"
				+ "</head>\n"
				+ "</html>\n"
				+ "";
		
        return Response.newFixedLengthResponse(Status.OK, getMimeType(), refreshText);
	}
	
	private Response refresh(BeetRootHTTPSession session, String msg) {
		
		return this.refreshRoute(session, getDefaultHandlerEntity() + "/index", msg);
	}
	
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
	
	/** Unused atm */
	protected void loginMarker(boolean redirectLogin) {
		//this.loginMarker = redirectLogin;
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
	 * @throws Exception
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
	 * @throws Exception
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
	 * @throws Exception
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
	 * @throws Exception
	 */
	public HandlerResponse deleteData(BeetRootHTTPSession session, int id) throws Exception {
		return null;
	}
	
	/**
	 * Overwrite to get the right re-route/redirect index handler
	 * after modifying data. It must be of the same entity as the 
	 * last executing handler!
	 * 
	 * @pram msg message
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
	 * @return HTML head
	 */
	public void addHtmlHeadLine(String line) {
		this.htmlHead += line + "\n";
	}
	
	/**
	 * Get HTML data.
	 * 
	 * @return HTML data
	 */
	public void addHtmlDataLine(String line) {
		this.htmlData += line + "\n";
	}

	/**
	 * Replace some more variables in template.
	 * Returning <code>null<code> is valid, then 
	 * nothing is replaced.
	 * 
	 * @param text text to parse and return
	 * @param session HTTP session
	 * @return parsed text or <code>null<code>
	 */
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
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
	 * Parse one html line from templates (customization).
	 * 
	 * Note: This method is called before the engine replaces
	 * standard tags! 
	 *  
	 * @param line html line
	 * @param session HTTP session
	 * @return new html line or lines.
	 */
	public String parse(String line, BeetRootHTTPSession session) {
		return line;
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
	 * @param message
	 */
	public void addSuccessMessage(String message) {
		
		this.successMessage = message;
	}

	/**
	 * Add a warning message to show.
	 * 
	 * @param message
	 */
	public void addWarningMessage(String message) {
		
		this.warningMessage = message;
	}
	
	/**
	 * Add an error message to show.
	 * 
	 * @param message
	 */
	public void addErrorMessage(String message) {
		
		this.errorMessage = message;
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
	
}
