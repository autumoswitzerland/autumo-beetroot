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
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultEditHandler;
import ch.autumo.beetroot.handler.HandlerResponse;

/**
 * Users edit handler. 
 */
public class UsersEditHandler extends DefaultEditHandler {
	
	public UsersEditHandler(String entity) {
		super(entity);
	}
	
	public UsersEditHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}

	@Override
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		
		/** Password updates not allowed
		final String pass = session.getParms().get("password");< o d		
		final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
		if (jsPwValidator) {
			final RuleResult rr = PasswordHelper.isValid(pass);
			if (!rr.isValid())
				return new HandlerResponse(HandlerResponse.STATE_NOT_OK, PasswordHelper.getHTMLMessages(rr));
		}
		*/
		
		return super.updateData(session, id);
	}

	@Override
	public Class<?> getRedirectHandler() {
		return UsersIndexHandler.class;
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
