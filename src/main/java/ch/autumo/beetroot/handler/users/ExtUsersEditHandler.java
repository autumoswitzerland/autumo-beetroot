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

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.usersroles.UserRole;


/**
 * Users edit handler for extended roles. 
 */
public class ExtUsersEditHandler extends UsersEditHandler {

	public ExtUsersEditHandler(String entity) {
		super(entity);
	}

	public ExtUsersEditHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {

		// 1. Update entity
		HandlerResponse response = super.updateData(session, id); 
		if (response != null ) {
			// not null is bad
			return response;
		}
		
		// 2. delete all relations
		List<Model> usersRoles = UserRole.where(UserRole.class, "user_id = ?", Integer.valueOf(id));
		for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();)
			iterator.next().delete();
		
		// 2. Save relation
		final String assignedIds = session.getParms().get("assignedIds");
		if (assignedIds != null && assignedIds.length() > 0) {
			final String roleIds[] = assignedIds.split(",");
			for (int i = 0; i < roleIds.length; i++) {
				final UserRole ur = new UserRole();
				ur.setUserId(id);
				ur.setRoleId(Integer.valueOf(roleIds[i]));
				int urid = ur.save().intValue();
				// pseudo id -2 is valid at this point (for many-to many tables that have no id)
				if (urid == -1) {
					return new HandlerResponse(
							HandlerResponse.STATE_NOT_OK, 
							LanguageManager.getInstance().translate("base.error.handler.savedid.rel", 
							session.getUserSession())
						);
				}
			}
			// Refresh current's user roles if they have been updated
			if (super.isCurrentUserUpdate(session))
				super.refreshUserRoles(id, session);
		}
		
		return null;
	}

	@Override
	public String extractCustomSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd,
			String columnName, String guiColName, int idx) throws Exception {
		
		if (columnName.equals("roles")) {
			
			final List<Model> usersRoles = UserRole.where(UserRole.class, "user_id = ?", Integer.valueOf(super.getCurrentEntityDbId()));
			final List<Model> unassociatedRoles = Role.listAll(Role.class);
			final List<Model> associatedRoles = new ArrayList<Model>();
			
			for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
				final UserRole userRole = (UserRole) iterator.next();
				final Role role = (Role) userRole.getAssociatedReference(Role.class);
				associatedRoles.add(role);
				unassociatedRoles.remove(role);
			}
			
			final StringBuffer snippet = super.readSnippetResource("web/html/:lang/users/snippets/roles.html", session.getUserSession());
			
			super.parseAssociatedEntities(snippet, associatedRoles);
			super.parseUnassociatedEntities(snippet, unassociatedRoles);
			
			return snippet.toString();
		}
		
		return "";
	}

	@Override
	public Class<?> getRedirectHandler() {
		return ExtUsersIndexHandler.class;
	}
	
}
