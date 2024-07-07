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
import java.util.List;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.handler.roles.Role;
import ch.autumo.beetroot.handler.usersroles.UserRole;

/**
 * Users add handler for extended roles. 
 */
public class ExtUsersAddHandler extends UsersAddHandler {

	public ExtUsersAddHandler(String entity) {
		super(entity);
	}

	public ExtUsersAddHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}

	@Override
	public HandlerResponse saveData(BeetRootHTTPSession session) throws Exception {

		// 1. Save entity
		final HandlerResponse response = super.saveData(session); 
		if (response != null && response.getStatus() == HandlerResponse.STATE_NOT_OK) {
			return response;
		}
		
		// Save worked, we have a save-id!
		final int saveId = response.getSavedId();
		
		// 2. Save relation
		final String assignedIds = session.getParms().get("assignedIds");
		if (assignedIds != null && assignedIds.length() > 0) {
			final String roleIds[] = assignedIds.split(",");
			for (int i = 0; i < roleIds.length; i++) {
				final UserRole ur = new UserRole();
				ur.setUserId(saveId);
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
		}
		
		return response;
	}
	
	@Override
	public String extractCustomSingleInputDiv(BeetRootHTTPSession session, String val, ResultSetMetaData rsmd,
			String columnName, String guiColName, int idx) throws Exception {
		
		if (columnName.equals("roles")) {
			
			final List<Model> associatedRoles = new ArrayList<Model>(); // empty
			final List<Model> unassociatedRoles = Role.listAll(Role.class);
			
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
			}			
			
			final StringBuilder snippet = super.readSnippetResource("web/html/:lang/users/snippets/roles.html", session.getUserSession());
			super.parseAssociatedEntities(snippet, associatedRoles, session);
			super.parseUnassociatedEntities(snippet, unassociatedRoles,session);
			return snippet.toString();
		}
		
		return "";
	}
	
	@Override
	public void render(BeetRootHTTPSession session) {
		if (super.isRetryCall(session)) {
			final String assignedIds = session.getParms().get("assignedIds");
			setVar("assignedIds", assignedIds);
		} else {
			setVar("assignedIds", "");
		}
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return ExtUsersIndexHandler.class;
	}
	
}
