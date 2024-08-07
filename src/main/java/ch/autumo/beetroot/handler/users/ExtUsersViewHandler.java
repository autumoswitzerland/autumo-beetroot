/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
import java.util.Iterator;
import java.util.List;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.usersroles.UserRole;
import ch.autumo.beetroot.utils.common.Time;
import ch.autumo.beetroot.utils.database.DB;

/**
 * Users view handler for extended roles. 
 */
public class ExtUsersViewHandler extends UsersViewHandler {

	public ExtUsersViewHandler(String entity) {
		super(entity);
	}
	
	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		final User user = (User) entity;
		
		String strRoles = "";
		if (columnName.equals("roles")) {
			final List<Model> usersRoles = UserRole.where(UserRole.class, "user_id = ?", Integer.valueOf(entity.getId()));
			for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
				final UserRole userRole = (UserRole) iterator.next();
				final Role role = (Role) userRole.getAssociatedReference(Role.class);
				String name = role.getName();
				name = LanguageManager.getInstance().translateOrDefVal("role."+name, name, session.getUserSession());
				strRoles += name + ", ";
			}
			if (usersRoles.size() > 0)
				strRoles = strRoles.substring(0, strRoles.length() - 2);
		}
		
		switch (columnName) {
		
			case "username"		: userName = DB.getValue(set, columnName); 
								  return "<td>"+userName+"</td>";
			case "email"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "phone"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "roles"		: return "<td>" + strRoles + "</td>";
			case "two_fa"		: return set.getBoolean(columnName) ? 
										"<td class=\"yesStatus\"></td>" : 
										"<td class=\"noStatus\"></td>";
			case "created"		: return "<td>"+Time.getGUIDate(set.getTimestamp(columnName))+"</td>";
			case "modified"		: return "<td>"+Time.getGUIDate(set.getTimestamp(columnName))+"</td>";
			// transient field 'code'
			case "code"			: return "<td>"+this.get2FAQRImage(session, user)+"</td>";
			default				: return "<td>"+DB.getValue(set, columnName)+"</td>";
		}
	}

	@Override
	public Class<?> getRedirectHandler() {
		return ExtUsersIndexHandler.class;
	}
	
}
