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

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Status;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;

/**
 * Default error 404 handler.
 */
public class Error404Handler extends BaseHandler {

	public Error404Handler() {
	}
	
	@Override
	public String getEntity() {
		return "Error";
	}

	@Override
	public void render(BeetRootHTTPSession session) {
		setVar("title", LanguageManager.getInstance().translate("base.err.srv.404.title", session.getUserSession()));
		setVar("message", " ");
	}

	@Override
	public  String getResource() {
		return "web/html/:lang/error.html";
	}

	@Override
	public boolean showMenu(Session userSession) {
		return userSession.getUserRoles().size() > 0;
	}

	@Override
	public IStatus getStatus() {
		return Status.NOT_FOUND;
	}
	
}
