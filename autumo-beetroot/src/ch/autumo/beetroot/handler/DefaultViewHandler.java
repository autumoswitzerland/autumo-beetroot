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

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Utils;

/**
 * Default handler for 'web/html/<entity>/view.html' templates.
 */
public class DefaultViewHandler extends BaseHandler {
	
	public DefaultViewHandler(String entity) {
		super(entity);
	}

	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Connection conn = DatabaseManager.getInstance().getConnection();
		final Statement stmt = conn.createStatement();
		
		String stmtStr = "SELECT id, "+super.getColumnsForSql()+" FROM " + this.entity + " WHERE id="+id;
		final ResultSet set = stmt.executeQuery(stmtStr);

		set.next(); // one record !
		
		final Entity entity = Utils.createBean(getBeanClass(), set);
		
		for (int i = 1; i <= columns().size(); i++) {
			
			final String col[] = getColumn(i);
			int dbIdx = i + 1; // because of additional id!
			htmlData += "<tr><th>"+col[1]+"</th>"+extractSingleTableData(set, col[0], dbIdx, entity)+"</tr>\n";		
		}		
		set.close();
		stmt.close();
		conn.close();
		
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
