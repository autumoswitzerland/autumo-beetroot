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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.utils.BeanProcessor;
import ch.autumo.beetroot.utils.Beans;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.Web;

/**
 * Default handler for 'web/html/&lt;entity&gt;/index.html' templates.
 */
public class DefaultIndexHandler extends BaseHandler {

	protected static final Logger LOG = LoggerFactory.getLogger(DefaultIndexHandler.class.getName());
	
	private Map<String, Class<?>> refs = null;
	
	private int maxRecPerPage = 20;
	
	private int page = 1;
	private int pages = 0;
	private int rowCount = 0;
	private int totalShown = 0;

	/**
	 * New default index handler.
	 * 
	 * @param entity entity
	 */
	public DefaultIndexHandler(String entity) {
		
		super(entity);
		
		final String err = "Couldn't read max records per page, using 20.'";
		try {
			maxRecPerPage = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_WEB_MAX_RECORDS_PER_PAGE);
			if (maxRecPerPage == -1) {
				maxRecPerPage = 20;
				LOG.warn(err);
			}
		} catch (Exception e) {
			maxRecPerPage = 20;
			LOG.warn(err, e);
		}
	}

	/**
	 * New default index handler.
	 * 
	 * @param msg message
	 * @param entity entity
	 */
	public DefaultIndexHandler(String entity, String msg) {
		
		this(entity);
		
		super.addSuccessMessage(msg);
		super.redirectedMarker(true);
	}

	/**
	 * New default index handler.
	 * 
	 * @param msg message
	 * @param entity entity
	 * @param messageType messagetype
	 */
	public DefaultIndexHandler(String entity, String msg, int messageType) {
		
		this(entity);
		
		switch (messageType) {
			case MSG_TYPE_INFO:
				super.addSuccessMessage(msg);
				break;
			case MSG_TYPE_WARN:
				super.addWarningMessage(msg);
				break;
			case MSG_TYPE_ERR:
				super.addErrorMessage(msg);
				break;
			default:
				super.addWarningMessage(msg);
				break;
		}

		super.redirectedMarker(true);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = SessionManager.getInstance().findOrCreate(session);
		String lang = LanguageManager.getInstance().getLanguage(userSession);
		
		// delete IDs from user session
		userSession.removeAllIds();
		
		//?sort=name&amp;direction=asc
		final String sortField = session.getParms().get("sort");
		final String sortDir = session.getParms().get("direction");
		
		String pg = session.getParms().get("page");
		if (pg == null)
			pg = (String) userSession.get("page-"+this.entity);
		
		if (pg != null && pg.length() != 0) {
			try {
				page = Integer.valueOf(pg).intValue();
				// set current page to user session
				userSession.set("page-"+this.entity, pg);
			} catch (Exception e) {
				LOG.warn("Couldn't parse page number, using page 1!", e);
				page = 1;
			}
		}
		
		// Foreign relations?
		refs = Beans.getForeignReferences(super.getEmptyBean());

		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		try {

			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// NOTE: TYPE_SCROLL_INSENSITIVE is very slow with Oracle Developer Database.
			// Even when the logic is changed to use ROWNUM with the BETWEEN keyword to select
			// records in a specific range. It seems cursors are slowed down, maybe it's a
			// limitation/feature of the Oracle database that comes with the Developer VM;
			// this is usually peanuts for Oracle databases.
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			String stmtStr = "SELECT id, "+super.getColumnsForSql()+" FROM " + this.entity;
			
			// take care of user data !
			if (userSession != null && getEntity().equals("users")) {
				final String username = userSession.getUserName();
				final List<String> userroles = userSession.getUserRoles();
				if (!userroles.contains("Administrator"))
					stmtStr += " WHERE username='"+username+"'";	
			}
			
			if (sortField != null && sortField.length() != 0)
				stmtStr += " ORDER BY " + sortField;
			if (sortDir != null && sortDir.length() != 0)
				stmtStr += " " + sortDir.toUpperCase();
			
			// NO SEMICOLON
			//stmtStr += ";";
			
			set = stmt.executeQuery(stmtStr);
			
	        if (set.last()) { 
	        	rowCount = set.getRow();
	          	set.beforeFirst();
	        }
	        
	        if (rowCount < maxRecPerPage)
	        	pages = 1;
	        else {
	        	pages = rowCount / maxRecPerPage;
	            if (rowCount % maxRecPerPage > 0)
	            	pages++;
	        }
	        
	        // set before first record of current page!
	        set.absolute((page - 1) * maxRecPerPage);
	        
	        if (rowCount >= maxRecPerPage) {
	        	set.setFetchSize(maxRecPerPage); 
	        }
			
	        totalShown = maxRecPerPage;
	        if (rowCount < maxRecPerPage)
	        	totalShown = rowCount;
	        	
			int counter = 0;
			
			final BeanProcessor processor = new BeanProcessor();
			
			// table data
			while (set.next() && counter < maxRecPerPage) {
				
				// id
				int idr = set.getInt("id");
				
				userSession.createIdPair(idr, getEntity());
				String modifyID = userSession.getModifyId(idr, getEntity());
				
				final Entity entity = Beans.createBean(getBeanClass(), set, processor);
				this.prepare(session, entity);
				
				// NOTE: We could deliver the whole bean which could be extracted by the
				// user with HTML and bean property-tags and waive the 'columns.cfg'-approach,
				// but then we would do it the way every web-framework does it and we want to
				// have only the tags {$head} and {$data} that generate the fields and table data
				// entries in every view.
				
				// columns
				htmlData += "<tr>";
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
					
					// If we have a reference table
					Class<?> entityClass = null;
					if (refs != null)
						entityClass = refs.get(col[0]);
					
					
					// it's a foreign key
					if (entityClass != null) {
						
						final int refDbIdx = Integer.valueOf(val).intValue();
						final Map.Entry<Integer, String> e = DB.getDisplayValue(entityClass, refDbIdx);
						val = e.getValue();
						
						final String foreignEntity = Beans.classToTable(entityClass);
						String foreignModifyID = userSession.getModifyId(refDbIdx, foreignEntity);
						if (foreignModifyID == null)
							foreignModifyID = userSession.createIdPair(refDbIdx, foreignEntity);

						final String valLink = "<a href=\"/"+lang+"/"+foreignEntity+"/view?id="+foreignModifyID+"\">" + val + "</a>\n";
						htmlData += "<td>" + valLink + "</td>";
						
					} else {
						
						String td = extractSingleTableData(session, set, col[0], dbIdx, entity) + "\n";
						if (td.indexOf("null") != -1)
							td = td.replace("null", "");
						
						htmlData += td;
					}
				}
				
				// generate actions
				htmlData += this.generateActionsTableData(userSession, getEntity(), entity, modifyID, idr, lang);
				
				htmlData += "</tr>\n";
				
				counter++;
			}
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		final List<String> transientFields = super.getTransientFields();
		
		// table head
		HEAD: for (int i = 1; i <= columns().size(); i++) {
			
			final String col[] = getColumn(i);
			
			// No show option!
			if (col[1].equals(Constants.GUI_COL_NO_SHOW))
				continue HEAD;
			
			String displayName = col[1];
			if (refs != null)
				if(refs.get(col[0]) != null)
					displayName = Helper.adjustRefDisplayName(displayName);
			
			if (sortField != null && sortField.length() != 0) {
				
				if (sortField.equals(col[0])) {
					if (sortDir != null && sortDir.length() != 0 && sortDir.equals("asc")) {
						if (transientFields.contains(col[0]))
							htmlHead += "<th>"+displayName+"</th>\n";
						else
							htmlHead += "<th><a class=\"asc\" href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=desc\">"+displayName+"</a></th>\n";
					}
					else if (sortDir != null && sortDir.length() != 0 && sortDir.equals("desc")) {
						if (transientFields.contains(col[0]))
							htmlHead += "<th>"+displayName+"</th>\n";
						else
							htmlHead += "<th><a class=\"desc\" href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+displayName+"</a></th>\n";
					}
				} else {
					if (transientFields.contains(col[0]))
						htmlHead += "<th>"+displayName+"</th>\n";
					else
						htmlHead += "<th><a href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+displayName+"</a></th>\n";
				}
			} else {
				if (transientFields.contains(col[0]))
					htmlHead += "<th>"+displayName+"</th>\n";
				else
					htmlHead += "<th><a href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+displayName+"</a></th>\n";
			}
		}
		
		return null;
	}
	
	/**
	 * Create actions table data. This must return a HTML &lt;td&gt;...&lt;/td&gt; section
	 * with all actions possible on the index page. the actions possibly returned might 
	 * depend on the user's role.
	 * 
	 * This method is internally called by the {@link #readData(BeetRootHTTPSession, int)}
	 * method. 
	 * 
	 * @param userSession user session
	 * @param entity entity string
	 * @param entityObj entity object
	 * @param modifyID obfuscated modify id used action links
	 * @param dbId internal DB id, don't write it out!
	 * @param lang user's language
	 * @return HTML data 
	 */
	public String generateActionsTableData(Session userSession, String entity, Entity entityObj, String modifyID, int dbId, String lang) {
		
		String htmlData ="";
		
		// Actions !
		htmlData += "<td class=\"actions\">\n";
		
		// VIEW
		htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/view?id="+modifyID+"\">"+LanguageManager.getInstance().translate("base.name.view", userSession)+"</a>\n";
		
		// EDIT
		if (this.changeAllowed(userSession))
			htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/edit?id="+modifyID+"\">"+LanguageManager.getInstance().translate("base.name.edit", userSession)+"</a>\n";
		
		// DELETE
		if (this.deleteAllowed(userSession)) {
			htmlData += "<form name=\"post_"+getEntity()+"_delete_"+modifyID+"\" style=\"display:none;\" method=\"post\" action=\"/"+getEntity()+"/delete?id="+modifyID+"\">\n";
			htmlData += "<input type=\"hidden\" name=\"_method\" value=\"POST\">\n";
			if (BeetRootConfigurationManager.getInstance().useCsrf()) {
				
				final String formCsrfToken = userSession.getFormCsrfToken();
				htmlData += "<input type=\"hidden\" name=\"_csrfToken\" value=\""+formCsrfToken+"\">\n";
			}
			htmlData += "</form>\n";
			htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/delete?id="+modifyID+"\" data-confirm-message=\""
							+ LanguageManager.getInstance().translate("base.operation.delete.ask", userSession, this.getDeleteName(entityObj)) 
							+ "\" onclick=\"if (confirm(this.dataset.confirmMessage)) { document.post_"+getEntity()+"_delete_"+modifyID+".submit(); } event.preventDefault();\">"
							+ LanguageManager.getInstance().translate("base.name.delete", userSession)+"</a>\n";
		}
		
		final String addHtml = this.addAdditionalActions(userSession, entity, modifyID, dbId, lang);
		if (addHtml != null && addHtml.length() !=0)
			htmlData += addHtml;
		
		htmlData += "</td>\n";
		return htmlData;
	}
	
	/**
	 * Get the name/id that should be shown in
	 * the delete confirmation dialog.
	 * 
	 * @param entityObj entity
	 * @return id/name of delete object
	 */
	public String getDeleteName(Entity entityObj) {
		return "" + entityObj.getId();
	}
	
	/**
	 * Add additional actions.
	 *
	 * @param userSession user session
	 * @param entity entity string
	 * @param modifyID obfuscated modify id used action links
	 * @param dbId internal DB id, don't write it out!
	 * @param lang user's language
	 * @return HTML data or null
	 */	
	public String addAdditionalActions(Session userSession, String entity, String modifyID, int dbId, String lang) {
		return null;
	}

	/**
	 * Determine if change actions are shown on index page.
	 * 
	 * @param userSession user session
	 * @return true or false
	 */
	public boolean changeAllowed(Session userSession) {
		return true;
	}

	/**
	 * Determine if delete actions are shown on index page.
	 * 
	 * @param userSession user session
	 * @return true or false
	 */
	public boolean deleteAllowed(Session userSession) {
		return true;
	}
	
	/**
	 * Prepare call to to something with the current entity bean 
	 * processed in the list if necessary. Called before all 
	 * {@link #extractSingleTableData(BeetRootHTTPSession, ResultSet, String, int, Entity)}
	 * calls.
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
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param idx SQL result set column index
	 * @param entity whole entity bean
	 * @return html data extract &lt;td&gt;...&lt;/td&gt;
	 * @throws Exception exception
	 */
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		if (transientFields.contains(columnName))
			return "<td></td>"; // only a specific user implementation knows what to do with transient fields
		
		String val = null;
		
		// Custom field data, e.g. for custom user roles
		val = this.extractCustomSingleTableData(session, set, columnName, idx, entity);
		if (val != null)
			return "<td>" + val + "</td>";

		final Object o = set.getObject(idx);
		
		if (o == null || o.toString().equals("null"))
			val = "";
		else
			val = o.toString();

		// Special case Users
		if (getEntity().equals("users") && columnName.toLowerCase().equals("role")) {
			val = LanguageManager.getInstance().translateOrDefVal("role."+val, val, session.getUserSession());
		}
		
		val = Web.escapeHtmlReserved(val);

		return "<td>" + val + "</td>";
	}
	
	/**
	 * Overwrite this method, if you need to add a custom data; e.g. when multiple user roles are used;
	 * in this case it is more likely that you combine more values that just one field value or use it 
	 * for any custom value. The value is guaranteed to be inserted in the column-order as defined in the
	 * 'columns.cfg'.
	 *   
	 * @param session HTTP session
	 * @param rsmd result set meta data
	 * @param columnName column name as configured in 'web/&lt;entity&gt;/columns.cfg'
	 * @param idx SQL result set column index
	 * @param entity whole entity bean
	 * @return html data extract &lt;td&gt;...&lt;/td&gt;
	 * @throws Exception exception
	 */
	public String extractCustomSingleTableData(BeetRootHTTPSession session, ResultSet rsmd,
			String columnName, int idx, Entity entity) throws Exception {
		return null;
	}
	
	/**
	 * Get whole index paginator.
	 * 
	 * @param session HTTP session
	 */
	@Override
	public String getPaginator(BeetRootHTTPSession session) {
		
		final Session userSession = session.getUserSession();
		final LanguageManager lm = LanguageManager.getInstance();
		String lang = lm.getLanguage(userSession);
		
		String options = "";
		
		if (page > 1)
			options += "				<li class=\"first\"><a href=\"/"+lang+"/"+getEntity()+"/index?page=1\">&lt;&lt; first</a></li>\n";            
		
		if (page == 1)
			options += "				<li class=\"prev disabled\"><a href=\"\" onclick=\"event.preventDefault();\">&lt; previous</a></li>\n";
		else if (page > 1)
			options += "				<li class=\"prev\"><a rel=\"prev\" href=\"/"+lang+"/"+getEntity()+"/index?page="+(page-1)+"\">&lt; previous</a></li>\n";            
		
		
		for (int i = 0; i < pages; i++) {
			int onpage = i+1;
			if (onpage == page)
				options += "				<li class=\"active\"><a href=\"\">"+onpage+"</a></li>\n";
			else
				options += "				<li><a href=\"/"+lang+"/"+getEntity()+"?page="+onpage+"\">"+onpage+"</a></li>\n";
		}
		
		
		if (page == pages)
			options += "				<li class=\"next disabled\"><a href=\"\" onclick=\"event.preventDefault();\">next &gt;</a></li>\n";
		else if (page < pages)
			options += "				<li class=\"next\"><a rel=\"next\" href=\"/"+lang+"/"+getEntity()+"/index?page="+(page+1)+"\">next &gt;</a></li>\n";            

		if (page < pages)
			options += "				<li class=\"last\"><a href=\"/"+lang+"/"+getEntity()+"/index?page="+pages+"\">last &gt;&gt;</a></li>\n";            
		
		
		return 	  "    <div class=\"paginator\">\n"
				+ "        <ul class=\"pagination\">\n"
				+ options
				+ "			</ul>\n"
				+ "        <p>" + lm.translate("base.name.page", userSession) + " "+page+" " + lm.translate("base.name.of", userSession)+" "+pages+" | "
								+ lm.translate("base.name.showing", userSession)+" "+totalShown + " "
								+ lm.translate("base.name.recordsoutof", userSession)+" " + rowCount + " " + lm.translate("base.name.total", userSession)+"</p>\n"
				+ "    </div>\n";
	}
	
	@Override
	public  String getResource() {
		return "web/html/:lang/"+entity+"/index.html";
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
