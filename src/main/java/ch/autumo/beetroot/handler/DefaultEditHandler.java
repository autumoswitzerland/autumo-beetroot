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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Map;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.crud.EventHandler;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.bean.Beans;
import ch.autumo.beetroot.utils.database.DB;
import ch.autumo.beetroot.utils.web.Web;

/**
 * Default handler for 'web/html/&lt;entity&gt;/edit.html' templates.
 */
public class DefaultEditHandler extends BaseHandler {

	private static final String ON_OFF_MAP_NAME = "OnOffCols";
	
	private Map<String, Class<?>> refs = null;
	
	
	public DefaultEditHandler(String entity) {
		super(entity);
	}

	public DefaultEditHandler(String entity, String errMsg) {
		super(entity);
		this.addErrorMessage(errMsg);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		// Remove On/Off map from session if still present
		session.getUserSession().removeMap(ON_OFF_MAP_NAME + "." + super.getEntity());
		
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
			
			String stmtStr = "SELECT id, " + super.getColumnsForSql() + " FROM " + this.entity + " WHERE id=" + id;
			set = stmt.executeQuery(stmtStr);
			final ResultSetMetaData metaData = set.getMetaData();
	
			set.next(); // one record !
			
			final Model entity = Beans.createBean(getBeanClass(), set);
			if (entity.getDisplayValue() != null) {
				super.registerDisplayField(entity.getDisplayValue());
			} else {
				super.registerDisplayField(""+id);
			}	
			
			this.prepare(session, entity);
			
			LOOP: for (int i = 1; i <= columns().size(); i++) {
				
				final String col[] = getColumn(i);
				int dbIdx = i + 1; // because of additional id!
				int sqlType = metaData.getColumnType(dbIdx);
				
				final String guiColTitle = col[1];
				if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
					continue LOOP;
				
				htmlData += extractSingleInputDiv(session, set, entity, col[0], guiColTitle, sqlType, dbIdx);		
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
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		
		// Unique fields test!
		final HandlerResponse status = super.uniqueTest(session, "SELECT id FROM "+getEntity()+" WHERE id!="+id+" AND ", "updating");
		if (status != null) {
			// it's a bad status
			status.setId(id);
			return status;
		}
		
		// Notify listeners
		if (EventHandler.getInstance().notifyBeforeUpdate(getBeanClass(), id)) {
			// Abort?
			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.update.abort", session.getUserSession(), getEntity(), id));
		}
		
		Connection conn = null;
		Statement stmt = null;
		
		try {
		
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// Now save edited data !
			stmt = conn.createStatement();
			String stmtStr = "UPDATE "+getEntity()+" SET "+this.getUpdateSetClause(session, ON_OFF_MAP_NAME + "." + super.getEntity())+" WHERE id=" + id;
			session.getUserSession().removeMap(ON_OFF_MAP_NAME + "." + super.getEntity()); // clear map here
			stmt.executeUpdate(stmtStr);

			// Notify listeners
			EventHandler.getInstance().notifyAfterUpdate(getBeanClass(), id);
			
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return null;
	}
	
	/**
	 * Prepare call to to something with the entity bean if necessary.
	 * 
	 * @param session HTTP session
	 * @param entity entity bean
	 */
	public void prepare(BeetRootHTTPSession session, Entity entity) {
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param set result set holding one record
	 * @param entity entity bean
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param sqlType SQL type, see {@link java.sql.Types}
	 * @param idx SQL result set column index
	 * @return html data extract &lt;div&gt;...&lt;/div&gt;
	 * @throws Exception exception
	 */
	protected String extractSingleInputDiv(BeetRootHTTPSession session, ResultSet set, Entity entity, String columnName, String guiColName, int sqlType, int idx) throws Exception {
		final Object dbObj = set.getObject(idx);
		final String preformattedVal = Web.preFormatForHTML(dbObj, sqlType);
		final String val = this.formatSingleValueForGUI(session, dbObj, preformattedVal, columnName, sqlType, idx, entity);
		return this.extractSingleInputDiv(session, val, set.getMetaData(), columnName, guiColName, idx, false); // true); // ->no PW from DB anymore
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * Called in the retry case; 'data' contains cached data from previous user input.
	 *  
	 * @param session HTTP session
	 * @param data repost data
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
		// Note: We cannot set date/time objects (date time, time-stamp) to null in the GUI,
		// so even these values will be an empty string.
		return this.extractSingleInputDiv(session, val, rsmd, columnName, guiColName, idx, false);
	}
	
	private String extractSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd, String columnName, String guiColName, int idx, boolean pwFromDb) throws Exception {

		// No show!
		if (guiColName.equals(Constants.GUI_COL_NO_SHOW))
			return "";
		
		// No PWs anymore
		if (columnName.toLowerCase().equals("password"))
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
		
		
		int nullable = rsmd.isNullable(idx);
		int precision = rsmd.getPrecision(idx);
		
		if (nullable == ResultSetMetaData.columnNoNulls)
			result += "<div class=\"input "+divType+" required\">\n";
		else
			result += "<div class=\"input "+divType+"\">\n";

		/**
		// we have to decode the password for edit, even it is obfuscated by stars
		// -> if the user presses save it would be double-encoded otherwise!
		if (pwFromDb && inputType.equals("password") && BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_DB_PW_ENC)) {
			val = Utils.decode(val, SecureApplicationHolder.getInstance().getSecApp());
		}
		*/
		
		
		// A. On/Off switches
		if (val.equalsIgnoreCase(Constants.ON) || val.equalsIgnoreCase(Constants.OFF)) {
			
			// Add On/Off column to map
			session.getUserSession().setMapValue(ON_OFF_MAP_NAME + "." + super.getEntity(), columnName, val);
			
			result += "<label for=\"cb_"+columnName+"\">"+guiColName+"</label>\n";

			if (val.equalsIgnoreCase("On")) {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"true\" checked>\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"true\">";
			}
			else {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"false\">\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"false\">";
			}
			
			// Must !
			super.addCheckBox(session, columnName);

		// B. All others
		} else {
		
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

				// Check-Boxes
				
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
				if (extra != null && extra.length() > 0) {
					result += extra; 
					result += "</div>\n";
					return result;
				}

				// a. Special case Users
				if (getEntity().equals("users") && columnName.toLowerCase().equals("role")) {
					
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
					
				// c. Foreign key boxes
				} else if (entityClass != null) {
					
					final Map<Integer, String> entries = DB.getDisplayValues(entityClass);
					result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
					for (Integer id : entries.keySet()) {
						final int i = id.intValue();
						final String displayValue = entries.get(id);
						if (id.equals(Integer.valueOf(val)))
							result += "    <option value=\""+i+"\" selected>"+displayValue+"</option>\n";
						else
							result += "    <option value=\""+i+"\">"+displayValue+"</option>\n";
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
	 * @return html data extract &lt;div&gt;...&lt;/div&gt;, empty string or null
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
	 * @return select vaues
	 */
	protected String[] getSelectValues(String columnName) {
		return null;
	}

	/**
	 * Format value for GUI.
	 * 
	 * @param session HTTP session
	 * @param dbObject DB object 
	 * @param preformattedValue pre-formatted database value 
	 * @param columnName DB column name
	 * @param sqlType SQL type, see {@link java.sql.Types}
	 * @param dbIdx SQL result set column index
	 * @param entity whole entity bean
	 * @return formated value for given column-name or DB index 
	 */
	public String formatSingleValueForGUI(BeetRootHTTPSession session, Object dbObject, String preformattedValue, String columnName, int sqlType, int dbIdx, Entity entity) {
		return preformattedValue;
	}
	
	@Override
	public String formatSingleValueForDB(BeetRootHTTPSession session, String val, String columnname) {
		return val;
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/edit.html";
	}

	/**
	 * Get bean entity class that has been generated trough PLANT, 
	 * overwritten or null.
	 * 
	 * @return bean entity class
	 */
	@Override
	public Class<?> getBeanClass() {
		return null;
	}
	
}
