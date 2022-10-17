/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.UtilsException;
import ch.autumo.beetroot.handler.DefaultViewHandler;

/**
 * Users view handler.
 */
public class UsersViewHandler extends DefaultViewHandler {

	private final static Logger LOG = LoggerFactory.getLogger(UsersViewHandler.class.getName());
	
	private String userName = null;
	
	public UsersViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		final User user = (User) entity;
		
		switch (columnName) {
		
			case "username"		: userName = set.getString(columnName); 
								  return "<td>"+userName+"</td>";
								  
			case "email"		: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "role"			: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			
			case "two_fa"		: return set.getBoolean(columnName) ? "<td>Yes</td>" : "<td>No</td>";
			case "created"		: return "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			case "modified"		: return "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			
			// transient field 'code'
			case "code"			: return "<td>"+this.get2FAQRImage(session, user)+"</td>";
			
			default				: return "<td>"+set.getObject(columnName)+"</td>";
		}
	}

	@Override
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		// user name
		return text.replaceAll("\\{\\$userName\\}", userName);
	}

	@Override
	public Class<?> getBeanClass() {
		return User.class;
	}
	
	private String get2FAQRImage(BeetRootHTTPSession session, User user) {
		
		// QR code for Google Authenticator 2FA
		String absPath = null;
		String tempFileName = null;
		String barCode = null;
		
		try {

			// Generate bar code from user shown, not logged in !!!!
			barCode = Utils.getGoogleAuthenticatorBarCode(user.getSecretkey(), user.getEmail());
			absPath = Utils.createQRCode(barCode, Constants.QR_IMG_SIZE, Constants.QR_IMG_SIZE);
			tempFileName = absPath.substring(absPath.lastIndexOf(Utils.FILE_SEPARATOR) + 1, absPath.length());
			
			final String title = LanguageManager.getInstance().translate("base.2fa.title.text", SessionManager.getInstance().findOrCreate(session));			
			
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
