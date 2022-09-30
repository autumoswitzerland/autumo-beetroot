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
import ch.autumo.beetroot.Utils;

/**
 * Default JSON-REST handler for listing entities.
 */
public class DefaultRESTIndexHandler extends BaseHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(DefaultRESTIndexHandler.class.getName());
	
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
	public DefaultRESTIndexHandler(String entity) {
		
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
	public DefaultRESTIndexHandler(String entity, String msg) {
		this(entity);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		//?sort=name&amp;direction=asc
		final String sortField = session.getParms().get("sort");
		final String sortDir = session.getParms().get("direction");
		
		final String pg = session.getParms().get("page");
		if (pg != null && pg.length() != 0) {
			try {
				page = Integer.valueOf(pg).intValue();
			} catch (Exception e) {
				LOG.warn("Couldn't parse page number, using page 1!", e);
				page = 1;
			}
		}

		final String fs = session.getParms().get("fetchsize");
		if (fs != null && fs.length() != 0) {
			try {
				maxRecPerPage = Integer.valueOf(fs).intValue();
			} catch (Exception e) {
				LOG.warn("Couldn't parse fetch size number, using fetch size '"+maxRecPerPage+"'!", e);
			}
		}
		
		
		Connection conn = null;
		Statement stmt = null;
		
		try {

			conn = DatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			String stmtStr = "SELECT id, "+super.getColumnsForSql()+" FROM " + this.entity;

			
			if (sortField != null && sortField.length() != 0)
				stmtStr += " ORDER BY " + sortField;
			if (sortDir != null && sortDir.length() != 0)
				stmtStr += " " + sortDir.toUpperCase();
			
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
				
				final Entity entity = Utils.createBean(getBeanClass(), set, processor);
				this.prepare(session, entity);
				
				// columns
				htmlData += "        {\n";
				LOOP: for (int i = 1; i <= columns().size(); i++) {
					
					final String cfgLine = columns().get(Integer.valueOf(i));
					final String params[] = cfgLine.split("=");
					int dbIdx = i + 1; // because of additional id!
					
					final String jsonColTitle = params[0].trim();
					if (jsonColTitle != null && jsonColTitle.equals(Constants.GUI_COL_NO_SHOW)) // NO_SHOW option
						continue LOOP;
					
					if (i == columns().size())
						htmlData += extractSingleTableData(session, set, jsonColTitle, dbIdx, entity)+ "\n";
					else
						htmlData += extractSingleTableData(session, set, jsonColTitle, dbIdx, entity)+ ",\n";
				}
				
				if (counter + 1 == totalShown)
					htmlData += "        }";
				else
					htmlData += "        },\n";
				
				counter++;
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
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param dbIdx SQL result set column index
	 * @param entity whole entity bean
	 * @return JSON data extract <td>...</td>
	 * @throws Exception
	 */
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		if (transientFields.contains(columnName))
			return ""; // only a specific user implementation knows what to do with transient fields
		
		final Object o = set.getObject(idx);
		
		String val = null;
		if (o == null || o.toString().equals("null"))
			val = "";
		else
			val = o.toString();
		
		return "            \"" + columnName + "\": \"" + val + "\"";
	}
	
	/**
	 * Get whole index paginator.
	 */
	@Override
	public String getPaginator(BeetRootHTTPSession session) {
		
		String json = "";
		json += "    \"paginator\": {\n";            
		json += "        \"itemsPerPage\": "+totalShown+",\n";            
		json += "        \"itemsTotal\": "+rowCount+",\n";            
		json += "        \"lastPage\": "+pages+"\n";            
		json += "    }";            
		return json;
	}
	
	@Override
	public  String getResource() {
		return "web/html/:lang/"+entity+"/index.json";
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
	
	@Override
	public String getMimeType() {
		return "application/json";
	}
	
}
