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
import ch.autumo.beetroot.Session;


/**
 * Default error handler.
 */
public class ErrorHandler extends BaseHandler {

	private Status status = null;
	private String title = null;
	private String message = null;
	
	public ErrorHandler(Status status, String title, String message) {
		this.status = status;
		this.title = title;
		this.message = message;
	}
	
	@Override
	public String getEntity() {
		return "Error";
	}

	@Override
	public void render(BeetRootHTTPSession session) {
		setVar("title", this.title);
		if (title != null && title.length() != 0)
			setVar("message", this.message);
		else
			setVar("message", "");
	}

	@Override
	public  String getResource() {
		return "web/html/:lang/error.html";
	}

	@Override
	public IStatus getStatus() {
		return status;
	}

	@Override
	public boolean showMenu(Session userSession) {
		return userSession.getUserRoles().size() > 0;
	}
	
}
