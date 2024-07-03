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

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.utils.DB;
import ch.autumo.beetroot.utils.TwoFA;

/**
 * New QR Code handler.
 */
public class NewQRCodeHandler extends BaseHandler {

	private int uid = -1;
	
	public NewQRCodeHandler(String entity) {
		this.entity = entity;
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final String newSecretUserKey = TwoFA.createSecretUserKey();
		DB.updateSecretUserKey(id, newSecretUserKey);
		
		uid = id;
		
		return null;
	}
	
	@Override
	protected String isNoContentResponseButRoute(Session userSession) {
		return "users/view?id="+userSession.getModifyId(uid, entity);
	}
	
	@Override
	public String getResource() {
		// since no output is created, no resource is necessary
		return null;
	}

	@Override
	public String getEntity() {
		return this.entity;
	}

	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRoles().contains("Administrator");
	}
	
}
