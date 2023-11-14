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
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;

/**
 * Default delete handler.
 */
public class DefaultDeleteHandler extends BaseHandler {
	
	public DefaultDeleteHandler(String entity) {
		super(entity);
	}

	@Override
	public HandlerResponse deleteData(BeetRootHTTPSession session, int id) throws Exception {

		Connection conn = null;
		Statement stmt = null;
		
		try {
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			
			// Delete data !
			stmt = conn.createStatement();
			
			String stmtStr = "DELETE FROM "+getEntity()+" WHERE id=" + id;
			stmt.executeUpdate(stmtStr);
		
		} catch (SQLIntegrityConstraintViolationException ex) {
			
			// In this case, the entity references another
			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, LanguageManager.getInstance().translate("base.error.handler.delete.integrity", session.getUserSession()));
			
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return null;
	}
	
	@Override
	public String getResource() {
		return null;
	}

}
