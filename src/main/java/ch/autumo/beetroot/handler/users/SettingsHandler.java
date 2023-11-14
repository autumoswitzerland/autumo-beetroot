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
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.handler.NoContentAndConfigHandler;
import ch.autumo.beetroot.utils.Utils;

/**
 * Settings handler.
 */
public class SettingsHandler extends NoContentAndConfigHandler {

	public SettingsHandler(String entity) {
		super(entity);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = session.getUserSession();
		boolean somethingChanged = false;
	
		// theme
		String key = session.getParms().get("theme");
		if (key != null && key.length() != 0) {
			if (!key.equals(userSession.getUserSetting("theme"))) {
				userSession.addOrUpdateUserSetting("theme", key);
				somethingChanged = true;
			}
		}
		
		if (somethingChanged)
			Utils.storeUserSettings(userSession);
		
		return null;
	}

}
