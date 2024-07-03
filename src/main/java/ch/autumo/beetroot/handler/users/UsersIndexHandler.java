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
package ch.autumo.beetroot.handler.users;

import java.sql.ResultSet;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultIndexHandler;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.Time;

/**
 * Users index handler. 
 */
public class UsersIndexHandler extends DefaultIndexHandler {

	public UsersIndexHandler(String entity) {
		super(entity);
	}

	public UsersIndexHandler(String entity, String msg) {
		
		super(entity);
		
		super.addSuccessMessage(msg);
		super.redirectedMarker(true);
	}
	
	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		switch (columnName) {
			case "username"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "email"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "role"			: String r = DB.getValue(set, columnName);
									return "<td>" + LanguageManager.getInstance().translateOrDefVal("role."+r, r, session.getUserSession()) + "</td>";
			case "two_fa"		: return set.getBoolean(columnName) ? 
									"<td>" + LanguageManager.getInstance().translate("base.switch.yes", session.getUserSession()) + "</td>" : 
									"<td>" + LanguageManager.getInstance().translate("base.switch.no", session.getUserSession()) + "</td>";
			case "created"		: return "<td>" + Time.getGUIDate(set.getTimestamp(columnName)) + "</td>";
			case "modified"		: return "<td>" + Time.getGUIDate(set.getTimestamp(columnName)) + "</td>";
			default				: return "<td>" + set.getObject(columnName) + "</td>";
		}		
	}

	@Override
	public boolean deleteAllowed(Session userSession) {
		return userSession.getUserRoles().contains("Administrator");
	}
	
	@Override
	public Class<?> getBeanClass() {
		return User.class;
	}

	@Override
	public String getTitle(Session userSession) {
		return LanguageManager.getInstance().translate("base.name.users", userSession);
	}
	
}
