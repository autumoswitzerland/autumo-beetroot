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

import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.BaseHandler;

/**
 * Default OTP handler.
 */
public class OtpHandler extends BaseHandler {

	private String entity = null;
	
	public OtpHandler() {
		this.entity = "2FA Code";
	}
	
	public OtpHandler(String entity) {
		this.entity = entity;
	}
	
	public OtpHandler(String entity, String errMsg) {
		this.entity = entity;
		this.addErrorMessage(errMsg);
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/users/otp.html";
	}

	@Override
	public String getEntity() {
		return entity;
	}

	@Override
	public boolean showMenu(Session userSession) {
		return false;
	}

	@Override
	public boolean showLangMenu(Session userSession) {
		return false;
	}
	
}
