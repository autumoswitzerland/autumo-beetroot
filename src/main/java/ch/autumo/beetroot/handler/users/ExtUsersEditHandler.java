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

import java.sql.Connection;
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
import ch.autumo.beetroot.utils.database.DB;


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

		final HandlerResponse response = super.updateData(session, id); 
		if (response != null ) {
			// not null is bad
			return response;
		}
		
		if (!session.getUserSession().getUserRoles().contains("Administrator"))
			return null; // We are done here! Non-Admins are not allowed to update roles!
		
		// This is done very detailed here. To write less code, you
		// could delete all users-roles-associations and only save
		// the associated relations in a global TX.
		
		boolean update = false;
		
		// 0. Load relations
		final List<Model> usersRoles = Model.where(UserRole.class, "user_id = ?", Integer.valueOf(id));
		
		
		// 1. Create a global TX
		final Connection conn = DB.newGlobalConnection();
		
		
		// 2. Save relations if necessary
		final String assignedIds = session.getParms().get("assignedIds");
		if (assignedIds != null && assignedIds.length() > 0) {
			final String roleIds[] = assignedIds.split(",");
			for (int i = 0; i < roleIds.length; i++) {
				final UserRole ur = new UserRole();
				ur.setUserId(id);
				ur.setRoleId(Integer.valueOf(roleIds[i]));
				if (!usersRoles.contains(ur)) {
					int urid = ur.save(conn).intValue();
					// pseudo id -2 is valid at this point (for many-to many tables that have no id)
					if (urid == -1) {
						return new HandlerResponse(
								HandlerResponse.STATE_NOT_OK, 
								LanguageManager.getInstance().translate("base.error.handler.savedid.rel", 
								session.getUserSession())
							);
					}
					update = true;
				}
			}
		}
		
		// 3. Delete relations if necessary
		final String availableIds = session.getParms().get("availableIds");
		if (availableIds != null && availableIds.length() > 0) {
			final String roleIds[] = availableIds.split(",");
			for (int i = 0; i < roleIds.length; i++) {
				final UserRole ur = new UserRole();
				ur.setUserId(id);
				ur.setRoleId(Integer.valueOf(roleIds[i]));
				if (usersRoles.contains(ur)) {		
					ur.delete(conn);
					update = true;
				}
			}

		}
		
		
		// 3. Commit
		conn.commit();
		DB.retireGlobalConnection(conn);

		
		// 4. Refresh current's user roles if they have been updated
		if (update && super.isCurrentUserUpdate(session))
			super.refreshUserRoles(id, session);
		
		
		return null;
	}

	@Override
	public String extractCustomSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd,
			String columnName, String guiColName, int idx) throws Exception {
		
		if (columnName.equals("roles")) {

			final List<Model> unassociatedRoles = Role.listAll(Role.class);
			final List<Model> associatedRoles = new ArrayList<Model>();
			
			// We have to deal with possible retry cases here, since roles is a transient filed (see 'columns.cfg')
			if (super.isRetryCall(session)) {
				// Retry case
				final String assignedIds = session.getParms().get("assignedIds");
				if (assignedIds != null && assignedIds.length() > 0) {
					final String roleIds[] = assignedIds.split(",");
					for (int i = 0; i < roleIds.length; i++) {
						final Role assoCrole = (Role) Role.read(Role.class, Integer.valueOf(roleIds[i]));
						associatedRoles.add(assoCrole);
						unassociatedRoles.remove(assoCrole);
					}
				}
			} else {
				final List<Model> usersRoles = Model.where(UserRole.class, "user_id = ?", Integer.valueOf(super.getCurrentEntityDbId()));
				for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
					final UserRole userRole = (UserRole) iterator.next();
					final Role role = (Role) userRole.getAssociatedReference(Role.class);
					associatedRoles.add(role);
					unassociatedRoles.remove(role);
				}
			}

			// Only admins are allowed to update roles!
			if (session.getUserSession().getUserRoles().contains("Administrator")) {
				final StringBuilder snippet = super.readSnippetResource("web/html/:lang/users/snippets/roles.html", session.getUserSession());
				super.parseAssociatedEntities(snippet, associatedRoles, session);
				super.parseUnassociatedEntities(snippet, unassociatedRoles, session);
				return snippet.toString();
			} else {
				String roles = "";
				for (Iterator<Model> iterator = associatedRoles.iterator(); iterator.hasNext();) {
					final Role role = (Role) iterator.next();
					roles += role.getName() + ", ";
				}
				if (roles.endsWith(", "))
					roles = roles.substring(0, roles.length() - 2);
				if (roles.length() == 0)
					roles = "-";
				return roles;
			}
		}
		
		return "";
	}

	@Override
	public void render(BeetRootHTTPSession session) {
		
		// Only admins are allowed to update roles!
		if (!session.getUserSession().getUserRoles().contains("Administrator")) {
			return;
		}
		
		if (super.isRetryCall(session)) {
			final String assignedIds = session.getParms().get("assignedIds");
			setVar("assignedIds", assignedIds);
		} else {
			final List<Model> usersRoles = Model.where(UserRole.class, "user_id = ?", Integer.valueOf(super.getCurrentEntityDbId()));
			String assignedIds = "";
			for (Iterator<Model> iterator = usersRoles.iterator(); iterator.hasNext();) {
				final UserRole userRole = (UserRole) iterator.next();
				assignedIds += userRole.getRoleId()+","; 
			}
			if (assignedIds.endsWith(","))
				assignedIds = assignedIds.substring(0, assignedIds.length() - 1);
			setVar("assignedIds", assignedIds);
		}
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return ExtUsersIndexHandler.class;
	}

}
