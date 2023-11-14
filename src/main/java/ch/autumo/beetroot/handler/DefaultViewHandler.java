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
import java.sql.Statement;
import java.util.Map;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.utils.Utils;

/**
 * Default handler for 'web/html/<entity>/view.html' templates.
 */
public class DefaultViewHandler extends BaseHandler {
	
	private Map<String, Class<?>> refs = null;
	private String displayField = null;
	
	public DefaultViewHandler(String entity) {
		super(entity);
	}

	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = session.getUserSession();
		final String lang = LanguageManager.getInstance().getLanguage(userSession);
		
		Connection conn = null;
		Statement stmt = null;
		
		// Foreign relations?
		refs = Utils.getForeignReferences(super.getEmptyBean());
		
		try {
		
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
			
			String stmtStr = "SELECT id, "+super.getColumnsForSql()+" FROM " + this.entity + " WHERE id="+id;
			final ResultSet set = stmt.executeQuery(stmtStr);
	
			set.next(); // one record !
			
			final Entity entity = Utils.createBean(getBeanClass(), set);
			displayField = Utils.getDisplayField(entity);
			
			this.prepare(session, entity);
			
			LOOP: for (int i = 1; i <= columns().size(); i++) {
				
				final String col[] = getColumn(i);
				int dbIdx = i + 1; // because of additional id!
				
				final String guiColTitle = col[1];
				if (guiColTitle != null && guiColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
					continue LOOP;
				
				String val = null;
				final Object o = set.getObject(dbIdx);
				if (o == null || o.toString().equals("null"))
					val = "";
				else
					val = o.toString();
				
				if (this.displayField != null && col[0].equals(this.displayField)) {
					super.registerDisplayField(val);
				}
				
				// If we have a reference table
				Class<?> entityClass = null;
				if (refs != null)
					entityClass = refs.get(col[0]);
				
				
				// it's a foreign key
				if (entityClass != null) {
					
					final int refDbIdx = Integer.valueOf(val).intValue();
					final Map.Entry<Integer, String> e = Utils.getDisplayValue(entityClass, refDbIdx);
					val = e.getValue();
					
					final String displayName = Utils.adjustRefDisplayName(col[1]);
					final String foreignEntity = Utils.classToTable(entityClass);

					String foreignModifyID = userSession.getModifyId(refDbIdx, foreignEntity);
					if (foreignModifyID == null)
						foreignModifyID = userSession.createIdPair(refDbIdx, foreignEntity);

					String valLink = "<a href=\"/"+lang+"/"+foreignEntity+"/view?id="+foreignModifyID+"\">" + val + "</a>\n";
					htmlData += "<tr><th>"+displayName+"</th><td>" + valLink + "</td></tr>\n";
					
				} else {
					
					htmlData += "<tr><th>"+col[1]+"</th>" + extractSingleTableData(session, set, col[0], dbIdx, entity) + "</tr>\n";		
				}
			}		
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
	 * Extract one single table data field from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param set database result set pointing to current record
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param dbIdx SQL result set column index
	 * @param entity whole entity bean
	 * @return html data extract <td>...</td>
	 * @throws Exception
	 */
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		if (transientFields.contains(columnName))
			return "<td></td>"; // only a specific user implementation knows what to do with transient fields

		final Object o = set.getObject(idx);
		String val = null;
		
		if (o == null || o.toString().equals("null"))
			val = "";
		else
			val = o.toString();
		
		val = Utils.escapeHtml(val);
		
		return "<td>" + val + "</td>";
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/view.html";
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
