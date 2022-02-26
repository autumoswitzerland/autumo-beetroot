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
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.dbutils.BeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.Utils;

/**
 * Default handler for 'web/html/<entity>/index.html' templates.
 */
public class DefaultIndexHandler extends BaseHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(DefaultIndexHandler.class.getName());
	
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
			maxRecPerPage = ConfigurationManager.getInstance().getInt(Constants.KEY_WEB_MAX_RECORDS_PER_PAGE);
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
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = SessionManager.getInstance().findOrCreate(session);
		String lang = LanguageManager.getInstance().getLanguage(userSession);
		
		
		// delete ids from user session
		userSession.removeAllIds();
		
		//?sort=name&amp;direction=asc
		final String sortField = session.getParms().get("sort");
		final String sortDir = session.getParms().get("direction");
		
		final String pg = session.getParms().get("page");
		if (pg != null && pg.length() != 0)
			try {
				page = Integer.valueOf(pg).intValue();
			} catch (Exception e) {
				LOG.warn("Couldn't parse page number, using page 1!", e);
				page = 1;
			}
		
		final Connection conn = DatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		
		String stmtStr = "SELECT id, "+super.getColumnsForSql()+" FROM " + this.entity;
		
		// take care of user data !
		if (userSession != null && getEntity().equals("users")) {
			final String username = userSession.getUserName();
			final String userrole = userSession.getUserRole();
			if (username != null && username.length() != 0 && (userrole == null || !userrole.equals("Administrator")))
				stmtStr += " WHERE username='"+username+"'";	
		}
		
		if (sortField != null && sortField.length() != 0)
			stmtStr += " ORDER BY " + sortField;
		if (sortDir != null && sortDir.length() != 0)
			stmtStr += " " + sortDir.toUpperCase();
		
		stmtStr += ";";
		
		final ResultSet set = stmt.executeQuery(stmtStr);
		
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
			
			final Entity entity = Utils.createBean(getBeanClass(), set, processor);
			
			// NOTE: We could deliver the whole bean which could be extracted by the
			// user with HTML and bean property-tags and waive the 'columns.cfg'-approach,
			// but then we would do it the way every web-framework does it and we want to
			// have only the tags {$head} and {$data} that generate the fields and table data
			// entries in every view.
			
			// columns
			htmlData += "<tr>";
			for (int i = 1; i <= columns().size(); i++) {
				
				final String cfgLine = columns().get(Integer.valueOf(i));
				final String params[] = cfgLine.split("=");
				int dbIdx = i + 1; // because of additional id!
				htmlData += extractSingleTableData(set, params[0].trim(), dbIdx, entity)+ "\n";
				
			}
			
			// Actions !
			htmlData += "<td class=\"actions\">\n";
			htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/view?id="+modifyID+"\">"+LanguageManager.getInstance().translate("base.name.view", userSession)+"</a>\n";
			htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/edit?id="+modifyID+"\">"+LanguageManager.getInstance().translate("base.name.edit", userSession)+"</a>\n";
			htmlData += "<form name=\"post_"+getEntity()+"_delete_"+modifyID+"\" style=\"display:none;\" method=\"post\" action=\"/"+getEntity()+"/delete?id="+modifyID+"\">\n";
			htmlData += "<input type=\"hidden\" name=\"_method\" value=\"POST\"/>\n";
			
			if (ConfigurationManager.getInstance().useCsrf()) {
				
				final String formCsrfToken = userSession.getFormCsrfToken();
				htmlData += "<input type=\"hidden\" name=\"_csrfToken\" autocomplete=\"off\" value=\""+formCsrfToken+"\"/>\n";
			}
			
			htmlData += "</form>\n";
			htmlData += "<a href=\"/"+lang+"/"+getEntity()+"/delete?id="+modifyID+"\" data-confirm-message=\""
							+ LanguageManager.getInstance().translate("base.operation.delete.ask", userSession, idr) 
							+ "\" onclick=\"if (confirm(this.dataset.confirmMessage)) { document.post_"+getEntity()+"_delete_"+modifyID+".submit(); } event.returnValue = false; return false;\">"
							+ LanguageManager.getInstance().translate("base.name.delete", userSession)+"</a>\n";
			htmlData += "</td>\n";
			htmlData += "</tr>\n";
			counter++;
			
			/*
<form name="post_tasks_delete_{$id}" style="display:none;" method="post" action="/tasks/delete?id={$id}">
<a href="/tasks/delete?id={$id}" class="side-nav-item" data-confirm-message="Wollen Sie # {$dbid} wirklich löschen?" onclick="if (confirm(this.dataset.confirmMessage)) { document.post_tasks_delete_{$id}.submit(); } event.returnValue = false; return false;">Task löschen</a>
			 */
		}
		
		set.close();
		stmt.close();
		conn.close();
		
		// table head
		for (int i = 1; i <= columns().size(); i++) {
			
			final String col[] = getColumn(i);
			
			if (sortField != null && sortField.length() != 0) {
				
				if (sortField.equals(col[0])) {
					if (sortDir != null && sortDir.length() != 0 && sortDir.equals("asc"))
						htmlHead += "<th><a class=\"asc\" href=\"/"+getEntity()+"?sort="+col[0]+"&amp;direction=desc\">"+col[1]+"</a></th>\n";
					else if (sortDir != null && sortDir.length() != 0 && sortDir.equals("desc"))
						htmlHead += "<th><a class=\"desc\" href=\"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+col[1]+"</a></th>\n";
				} else {
					htmlHead += "<th><a href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+col[1]+"</a></th>\n";
				}
			} else {
				htmlHead += "<th><a href=\"/"+lang+"/"+getEntity()+"?sort="+col[0]+"&amp;direction=asc\">"+col[1]+"</a></th>\n";
			}
		}
		
		return null;
	}
	
	/**
	 * Extract one single table data field from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param set database result set pointing to current record
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param dbIdx SQL result set column index
	 * @param entity whole entity bean
	 * @return html data extract <td>...</td>
	 * @throws Exception
	 */
	public String extractSingleTableData(ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		final Object o = set.getObject(idx);
		
		String val = null;
		if (o == null || o.toString().equals("null"))
			val = "";
		else
			val = o.toString();
		
		return "<td>" + val + "</td>";
	}
	
	/**
	 * Get whole index paginator.
	 */
	@Override
	public String getPaginator(BeetRootHTTPSession session) {
		
		final Session userSession = SessionManager.getInstance().findOrCreate(session);
		final LanguageManager lm = LanguageManager.getInstance();
		String lang = lm.getLanguage(userSession);
		
		String options = "";
		
		if (page > 1)
			options += "				<li class=\"first\"><a href=\"/"+lang+"/"+getEntity()+"/index?page=1\">&lt;&lt; first</a></li>\n";            
		
		if (page == 1)
			options += "				<li class=\"prev disabled\"><a href=\"\" onclick=\"return false;\">&lt; previous</a></li>\n";
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
			options += "				<li class=\"next disabled\"><a href=\"\" onclick=\"return false;\">next &gt;</a></li>\n";
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
