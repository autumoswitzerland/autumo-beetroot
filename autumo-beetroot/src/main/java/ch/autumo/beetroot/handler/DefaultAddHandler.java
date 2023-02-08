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
import ch.autumo.beetroot.utils.Utils;

/**
 * Default handler for 'web/html/<entity>/add.html' templates.
 * 
 * This one must be overwritten, because of mandatory db fields
 * that might not be show on the GUI. The values to insert into
 * database must be all defined if they are not nullable!
 */
public abstract class DefaultAddHandler extends BaseHandler {

	private final static Logger LOG = LoggerFactory.getLogger(DefaultAddHandler.class.getName());
	
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
		if (_method != null && _method.equals("RETRY")) {

			final Connection conn = BeetRootDatabaseManager.getInstance().getConnection();
			final Statement stmt = conn.createStatement();
			
			// we only need the result set for the column meta data
			stmt.setFetchSize(1);
			
			String stmtStr = "SELECT " + super.getColumnsForSql() + " FROM " + this.entity;
			final ResultSet set = stmt.executeQuery(stmtStr);

			LOOP: for (int i = 1; i <= columns().size(); i++) {
				
				final String col[] = getColumn(i);
				
				final String guiColTitle = col[1];
				if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
					continue LOOP;
				
				htmlData += this.extractSingleInputDiv(session, params, set, col[0], guiColTitle, i);
			}
			return null;
		}
		
		// NORMAL case: first call case
		final Connection conn = BeetRootDatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement();
		
		String stmtStr = "SELECT " + super.getColumnsForSql() + " FROM " + this.entity; //NO SEMICOLON + ";";
		final ResultSet set = stmt.executeQuery(stmtStr); // NOTE: call only for types, make this better!
		
		LOOP: for (int i = 1; i <= columns().size(); i++) {
			
			final String col[] = getColumn(i);
			
			final String guiColTitle = col[1];
			if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
				continue LOOP;
			
			htmlData += extractSingleInputDiv(session, set, col[0], guiColTitle, i);
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
			String columns = getColumnsForSql();
			String values = getInsertValues(session);
			
			final Map<String, Object> mandatory = getAddMandatoryFields();
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
							values += ", " + Utils.nowTimeStamp();
						else
							values += ", '" + Utils.nowTimeStamp() + "'";
					}
					else
						values += ", '"+val+"'";
				}
				else
					values += ", null";
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
			
		} finally {
			if (keySet != null)
				keySet.close();
			if (stmt != null)
				stmt.close();
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
	 * @param set result set
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/<entity>/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract <div>...</div>
	 * @throws Exception
	 */
	private String extractSingleInputDiv(BeetRootHTTPSession session, ResultSet set, String columnName, String guiColName, int idx) throws Exception {
		
		return this.extractSingleInputDiv(session, "", set, columnName, guiColName, idx);
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param data repost data
	 * @param set result set, even when empty, data is taken from the map (retry)
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/<entity>/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract <div>...</div>
	 * @throws Exception
	 */
	private String extractSingleInputDiv(BeetRootHTTPSession session, Map<String, String> data, ResultSet set, String columnName, String guiColName, int idx) throws Exception {
		
		return this.extractSingleInputDiv(session, data.get(columnName), set, columnName, guiColName, idx);
	}

	private String extractSingleInputDiv(BeetRootHTTPSession session, String val, ResultSet set, String columnName, String guiColName, int idx) throws Exception {
		
		String result = "";
		boolean isCheck = false;
		final ResultSetMetaData rsmd = set.getMetaData();
		final String inputType = Utils.getHtmlInputType(rsmd, idx, columnName);
		final String divType = Utils.getHtmlDivType(rsmd, idx, columnName);
		if (inputType == "checkbox")
			isCheck = true;
		
		
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
		
		result += "<label for=\""+columnName+"\">"+guiColName+"</label>\n"; 
		
		if (isCheck) {
			
			if (val.equals("true") || val.equals("1")) {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"true\" checked>\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"true\" />";
			}
			else {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"false\">\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"false\" />";
			}
			
			// Must !
			super.addCheckBox(session, columnName);
		} 
		
		if (!isCheck) {
		
			final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
			if (jsPwValidator && columnName.equals("password")) {
				
				result += "<div id=\"password\" data-lang=\""+session.getUserSession().getUserLang()+"\"></div>";
				
			} else if (getEntity().equals("users") && columnName.toLowerCase().equals("role")) {
				
				final String roles[] = BeetRootConfigurationManager.getInstance().getAppRoles();
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (int i = 0; i < roles.length; i++) {
					if (val.equals(roles[i]))
						result += "    <option value=\""+roles[i]+"\" selected>"+roles[i]+"</option>\n";
					else
						result += "    <option value=\""+roles[i]+"\">"+roles[i]+"</option>\n";
				}
				result += "</select>";
				
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
				
			} else {
			
				if (nullable == ResultSetMetaData.columnNoNulls) {
					
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\" required=\"required\"\n";
					result += "    data-validity-message=\"This field cannot be left empty\" oninvalid=\"this.setCustomValidity(''); if (!this.value) this.setCustomValidity(this.dataset.validityMessage)\"\n";
					result += "    oninput=\"this.setCustomValidity('')\"\n"; 
					result += "    id=\""+columnName+"\" aria-required=\"true\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
					
				} else {
					
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\"\n";
					result += "    id=\""+columnName+"\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
				}
			}
		}
		
		
		result += "</div>\n";
		return result;
	}
	
	/**
	 * Is this column a HTML select field?
	 * 
	 * @param columnName column name
	 * @returntruew if so
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
