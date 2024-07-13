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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.crud.EventHandler;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.Time;
import ch.autumo.beetroot.utils.Web;

/**
 * Default handler for 'web/html/&lt;entity&gt;/add.html' templates.
 * 
 * This one must be overwritten, because of mandatory db fields
 * that might not be show on the GUI. The values to insert into
 * database must be all defined if they are not nullable!
 */
public abstract class DefaultAddHandler extends BaseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAddHandler.class.getName());
	
	private Map<String, Class<?>> refs = null;

	
	public DefaultAddHandler(String entity) {
		super(entity);
	}

	public DefaultAddHandler(String entity, String errMsg) {
		super(entity);
		this.addErrorMessage(errMsg);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		// RETRY case!
		final Map<String, String> params = session.getParms();
		final String _method = params.get("_method");

		// Foreign relations?
		refs = Beans.getForeignReferences(super.getEmptyBean());
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		try {
		
			if (_method != null && _method.equals("RETRY")) {
	
				conn = BeetRootDatabaseManager.getInstance().getConnection();
				stmt = conn.createStatement();
				
				// we only need the result set for the column meta data
				stmt.setFetchSize(1);
				
				String stmtStr = "SELECT " + super.getColumnsForSql() + " FROM " + this.entity;
				set = stmt.executeQuery(stmtStr);
	
				LOOP: for (int i = 1; i <= columns().size(); i++) {
					
					final String col[] = getColumn(i);
					
					final String guiColTitle = col[1];
					if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
						continue LOOP;
					
					htmlData += this.extractSingleInputDiv(session, params, set.getMetaData(), col[0], guiColTitle, i);
				}
				set.close();
				stmt.close();
				conn.close();
				return null;
			}
			
			// NORMAL case: first call case
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
			
			// we only need the result set for the column meta data
			stmt.setFetchSize(1);
			
			String stmtStr = "SELECT " + super.getColumnsForSql() + " FROM " + this.entity; //NO SEMICOLON + ";";
			set = stmt.executeQuery(stmtStr); // NOTE: call only for types, make this better!
			
			LOOP: for (int i = 1; i <= columns().size(); i++) {
				
				final String col[] = getColumn(i);
				
				final String guiColTitle = col[1];
				if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
					continue LOOP;
				
				htmlData += extractSingleInputDiv(session, set.getMetaData(), col[0], guiColTitle, i);
			}
			
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}		
		return null;
	}

	@Override
	public HandlerResponse saveData(BeetRootHTTPSession session) throws Exception {
		
		// Unique fields test!
		final HandlerResponse response = super.uniqueTest(session, "SELECT id FROM "+getEntity()+" WHERE ", "saving");
		if (response != null && response.getStatus() == HandlerResponse.STATE_NOT_OK) {
			return response;
		}
				
		Connection conn = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt2 = null;
		ResultSet keySet = null;
		int savedId = -1;
		
		try {
		
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// Now save data !
			String columns = getColumnsForSql(true);
			String values = getInsertValues(session);
			
			final Map<String, Object> mandatory = getAddMandatoryFields();
			if (mandatory != null) {
				final Set<String> cols = mandatory.keySet();
				for (Iterator<String> iterator = cols.iterator(); iterator.hasNext();) {
					
					final String col = (String) iterator.next();
					
					columns += ", " + col;
					
					final Object obj = mandatory.get(col);
					
					String val = null;
					if (obj != null)
						val = obj.toString();
					
					if (val != null) {
						if (val.equalsIgnoreCase("NOW()")) {
							if (BeetRootDatabaseManager.getInstance().isOracleDb())
								values += ", " + Time.nowTimeStamp();
							else
								values += ", '" + Time.nowTimeStamp() + "'";
						}
						else
							values += ", '"+val+"'";
					}
					else
						values += ", null";
				}
			}
			
			final String entity = getEntity();
			
			//NO SEMICOLON
			stmt = conn.prepareStatement("INSERT INTO "+entity+" (" + columns + ") VALUES (" + values + ")", Statement.RETURN_GENERATED_KEYS);
			stmt.executeUpdate();
			
			// Get generated key
			if (BeetRootDatabaseManager.getInstance().isOracleDb()) {
				
				stmt2 = conn.prepareStatement("select "+entity+"_seq.currval from dual");
				keySet = stmt2.executeQuery();
				boolean found = keySet.next();
				if (!found) {
					final Session userSession = session.getUserSession();
					return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.savedid", userSession));
				}				

				savedId = (int) keySet.getLong(1);
				
			} else {
				
				keySet = stmt.getGeneratedKeys();
				boolean found = keySet.next();
				if (!found) {
					final Session userSession = session.getUserSession();
					return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.savedid", userSession));
				}				
				
				savedId = keySet.getInt(1);
			}
		
			LOG.debug("Record '"+savedId+"' in '"+entity+"'.");
			
			// Notify listeners
			EventHandler.getInstance().notifyAfterCreate(getBeanClass(), savedId);
			
		} finally {
			if (keySet != null)
				keySet.close();
			if (stmt != null)
				stmt.close();
			if (stmt2 != null)
				stmt2.close();
			if (conn != null)
				conn.close();
		}
		
		final HandlerResponse okResponse = new HandlerResponse(HandlerResponse.STATE_OK, savedId);
		return okResponse; // ok
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param rsmd result set meta data
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract &lt;div&gt;...&lt;/div&gt;
	 * @throws Exception exception
	 */
	protected String extractSingleInputDiv(BeetRootHTTPSession session, ResultSetMetaData rsmd, String columnName, String guiColName, int idx) throws Exception {
		return this.extractSingleInputDiv(session, "", rsmd, columnName, guiColName, idx);
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * Called in the retry case; 'data' contains cached data from previous user input.
	 *  
	 * @param session HTTP session
	 * @param data repost data (retry data)
	 * @param rsmd result set meta data
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract &lt;div&gt;...&lt;/div&gt;
	 * @throws Exception exception
	 */
	protected String extractSingleInputDiv(BeetRootHTTPSession session, Map<String, String> data, ResultSetMetaData rsmd, String columnName, String guiColName, int idx) throws Exception {
		String val = data.get(columnName);
		if (val == null) // TRANSIENT values
			val = "";
		return this.extractSingleInputDiv(session, val, rsmd, columnName, guiColName, idx);
	}

	private String extractSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd, String columnName, String guiColName, int idx) throws Exception {
		
		// No show!
		if (guiColName.equals(Constants.GUI_COL_NO_SHOW))
			return "";
		
		String result = "";
		boolean isCheck = false;
		
		String inputType = null;
		String divType = null;
		String pattern = null;
		
		// If we have a reference table
		Class<?> entityClass = null;
		if (refs != null)
			entityClass = refs.get(columnName);
		
		if (entityClass != null) {
			// it is a foreign key!
			inputType = "select";
			divType = "select";
		} else {
			// standard columns
			inputType = Web.getHtmlInputType(rsmd, idx, columnName);
			divType = Web.getHtmlDivType(rsmd, idx, columnName);
			if (inputType == "checkbox")
				isCheck = true;
			pattern = Web.getHtmlInputPattern(idx, columnName);
		}
		
		
		// Initial values!
		// If this method is entered the first time - hence, not a RETRY case-
		// the value is always empty "". Therefore, we can use initial values if
		// configured in 'columns.cfg'
		if (super.initValuesSize() > 0) {
			// we have at least one!
			String initVal = super.initialValue(columnName);
			if (initVal != null && initVal.length() > 0)
				val = initVal;
		}
		
		
		int nullable = rsmd.isNullable(idx);
		int precision = rsmd.getPrecision(idx);
		
		if (nullable == ResultSetMetaData.columnNoNulls)
			result += "<div class=\"input "+divType+" required\">\n";
		else
			result += "<div class=\"input "+divType+"\">\n";
		
		
		// 1. Label
		if (isCheck)
			result += "<label for=\"cb_"+columnName+"\">"+guiColName+"</label>\n";
		else {
			if (entityClass != null)
				guiColName = Helper.adjustRefDisplayName(guiColName);
			result += "<label for=\""+columnName+"\">"+guiColName+"</label>\n";
		}
		
		// 2. Input
		if (isCheck) {
			
			if (val.equals("true") || val.equals("1")) {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"true\" checked>\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"true\">";
			}
			else {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"false\">\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"false\">";
			}
			
			// Must !
			super.addCheckBox(session, columnName);
			
		} else {
		
			// All other
			
			// Custom fields/divs, e.g. for custom user roles
			final String extra = this.extractCustomSingleInputDiv(session, val, rsmd, columnName, guiColName, idx);
			if (extra != null && extra.length() != 0) {
				result += extra; 
				result += "</div>\n";
				return result;
			}
			
			// x. Special case password
			final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
			if (jsPwValidator && columnName.equals("password")) {
				
				result += "<div id=\"password\" data-lang=\""+session.getUserSession().getUserLang()+"\"></div>";
				
			// a. Special case Users
			} else if (getEntity().equals("users") && columnName.toLowerCase().equals("role")) {
				
				final String roles[] = this.getSimpleManagementUserRoles();
				
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (int i = 0; i < roles.length; i++) {
					final String trRole = LanguageManager.getInstance().translateOrDefVal("role."+roles[i], roles[i], session.getUserSession());
					if (val.equals(roles[i]))
						result += "    <option value=\""+roles[i]+"\" selected>"+trRole+"</option>\n";
					else
						result += "    <option value=\""+roles[i]+"\">"+trRole+"</option>\n";
				}
				result += "</select>";
				
			// c. Foreign key boxes
			} else if (entityClass != null) {
				
				final Map<Integer, String> entries = DB.getDisplayValues(entityClass);
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (Integer id : entries.keySet()) {
					final int i = id.intValue();
					final String displayValue = entries.get(id);
					if (!val.equals("") && id.equals(Integer.valueOf(val)))
						result += "    <option value=\""+i+"\" selected>"+displayValue+"</option>\n";
					else
						result += "    <option value=\""+i+"\">"+displayValue+"</option>\n";
			    }					
				result += "</select>";	
					
			// b. Custom select boxes
			} else if (this.isSelect(columnName)) {
				
				final String entries[] = this.getSelectValues(columnName);
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (int i = 0; i < entries.length; i++) {
					if (val.equals(entries[i]))
						result += "    <option value=\""+entries[i]+"\" selected>"+entries[i]+"</option>\n";
					else
						result += "    <option value=\""+entries[i]+"\">"+entries[i]+"</option>\n";
				}
				result += "</select>";				
				
			// d. Other standard input types 
			} else {
				
				// Pattern?
				String patternAttr = "";
				if (pattern != null)
					patternAttr = "pattern=\""+pattern+"\"";
				
				if (nullable == ResultSetMetaData.columnNoNulls) {
					
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\" required=\"required\" "+patternAttr+"\n";
					result += "    data-validity-message=\"This field cannot be left empty\" oninvalid=\"this.setCustomValidity(''); if (!this.value) this.setCustomValidity(this.dataset.validityMessage)\"\n";
					result += "    oninput=\"this.setCustomValidity('')\"\n";
					if (super.isPrecisionInputType(inputType))
						result += "    id=\""+columnName+"\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
					else
						result += "    id=\""+columnName+"\" value=\""+val+"\">\n";
				} else {
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\" "+patternAttr+"\n";
					if (super.isPrecisionInputType(inputType))
						result += "    id=\""+columnName+"\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
					else
						result += "    id=\""+columnName+"\" value=\""+val+"\">\n";
				}
			}
		}
		
		
		result += "</div>\n";
		return result;
	}
	
	/**
	 * Overwrite this method, if you need to add a custom field (HTML 'div'); e.g. when multiple user roles are used;
	 * in this case the 'div' is more likely consisting of 2 role assignment boxes instead of a simple input-'div'
	 * or use it for any custom 'div'. The 'div' is guaranteed to be inserted in the column-order as defined in the
	 * 'columns.cfg'.
	 * <br><br>
	 * The return value of this method is essential:<br>
	 * <ul>
	 * <li>Returns the data (including an empty character string): The HTML data is inserted into the template and further 
	 * parsing of the columns for the HTML input elements is completed.</li>
	 * <li>If 'null' is returned, the search for matching input elements for the current columns is continued, even if it is a 
	 * transient column! Transient columns should be parsed in this method!</li>
	 * </ul>
	 *   
	 * @param session HTTP session
	 * @param val repost data (only available in retry case)
	 * @param rsmd result set meta data
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract &lt;div&gt;...&lt;/div&gt;
	 * @throws Exception exception
	 */
	public String extractCustomSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd,
			String columnName, String guiColName, int idx) throws Exception {
		return "";
	}
	
	/**
	 * Is this column a HTML select field?
	 * 
	 * @param columnName column name
	 * @return true if so
	 */
	protected boolean isSelect(String columnName) {
		return false;
	}
	
	/**
	 * Get values for column name
	 * @param columnName column name
	 * @return select values
	 */
	protected String[] getSelectValues(String columnName) {
		return null;
	}
	
	@Override
	public String formatSingleValueForDB(BeetRootHTTPSession session, String val, String columnname) {
		return val;
	}
	
	/**
	 * Get additional mandatory fields of the table
	 * that are not present and mandatory fields in the
	 * GUI.
	 * 
	 * They are usually the DB NOT NULL fields.
	 * Return the column/value pair within a map.
	 * 
	 * The value must be an object that is representable
	 * as a string!
	 *
	 * @return column/value pair map
	 */
	public abstract Map<String, Object> getAddMandatoryFields();
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/add.html";
	}

	/**
	 * Get bean entity class that has been generated trough PLANT, 
	 * self-written or null (then null in extract calls too).
	 * 
	 * @return bean entity class
	 */
	public Class<?> getBeanClass() {
		return null;
	}
	
}
