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

import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultViewHandler;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.common.Time;
import ch.autumo.beetroot.utils.database.DB;
import ch.autumo.beetroot.utils.web.TwoFA;

/**
 * Users view handler.
 */
public class UsersViewHandler extends DefaultViewHandler {

	private static final Logger LOG = LoggerFactory.getLogger(UsersViewHandler.class.getName());
	
	protected String userName = null;
	
	public UsersViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		final User user = (User) entity;
		switch (columnName) {
			case "username"		: userName = DB.getValue(set, columnName); 
								  return "<td>"+userName+"</td>";
			case "email"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "phone"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "role"			: String r = DB.getValue(set, columnName);
									return "<td>" + LanguageManager.getInstance().translateOrDefVal("role."+r, r, session.getUserSession()) + "</td>";
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
	public void render(BeetRootHTTPSession session) {
		// user name
		setVar("userName", userName);
	}

	@Override
	public Class<?> getBeanClass() {
		return User.class;
	}
	
	/**
	 * Return the while image tag for the 2FA QR code image
	 * 
	 * @param session HTTP session
	 * @param user user
	 * @return HTML image tag
	 */
	protected String get2FAQRImage(BeetRootHTTPSession session, User user) {
		// QR code for Google Authenticator 2FA
		String absPath = null;
		String tempFileName = null;
		String barCode = null;
		try {
			// Generate bar code from user shown, not logged in !!!!
			barCode = TwoFA.getGoogleAuthenticatorBarCode(user.getSecretkey(), user.getEmail());
			absPath = TwoFA.createQRCode(barCode, Constants.QR_IMG_SIZE, Constants.QR_IMG_SIZE);
			tempFileName = absPath.substring(absPath.lastIndexOf(Helper.FILE_SEPARATOR) + 1, absPath.length());
			final String title = LanguageManager.getInstance().translate("base.2fa.title.text", session.getUserSession());			
			return "<img src=\"/tmp/"+tempFileName+"\" title=\""+title+"\">";
		} catch (UtilsException e) {
			LOG.error("Couldn't serve QR code for user '"+userName+"'!", e);
		}
		return "notfound.png";
	}

	@Override
	public String getTitle(Session userSession) {
		return LanguageManager.getInstance().translate("base.name.users", userSession);
	}
	
}
