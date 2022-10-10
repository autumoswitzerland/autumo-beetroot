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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.GUIDGenerator;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.mailing.MailerFactory;

/**
 * Default login handler.
 */
public class ResetHandler extends BaseHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(ResetHandler.class.getName());
	
	private String entity = null;
	
	public ResetHandler(String entity) {
		this.entity = entity;
	}
	
	@Override
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = session.getUserSession();
		
		String email = session.getParms().get("email");
		if (email == null || email.length() == 0) {

			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, "No email address gathered!");
		}
		
		email = email.trim();

		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null;
		String token = null; 
				
		try {
			
			conn = BeetRootDatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
			
			String stmtStr = "SELECT id FROM users WHERE email='" + email + "'";;
			set = stmt.executeQuery(stmtStr);
			boolean found = set.next();
			
			if (!found) {
	
				set.close();
				stmt.close();
				conn.close();
				
				LOG.warn("PW reset: User with email '"+email+"' not found in database!");
				// be silent !
				//return new HandlerStatus(HandlerStatus.STATE_NOT_OK, "User not found in database!");
				return new HandlerResponse(HandlerResponse.STATE_NOT_OK);
			}
			
			int userid = set.getInt(1);
			token = GUIDGenerator.generate();
			
			if (BeetRootDatabaseManager.getInstance().isOracleDb())
				stmtStr = "UPDATE users SET lasttoken='" + token + "', modified=" + Utils.nowTimeStamp() + " WHERE id=" + userid;
			else
				stmtStr = "UPDATE users SET lasttoken='" + token + "', modified='" + Utils.nowTimeStamp() + "' WHERE id=" + userid;
			stmt.executeUpdate(stmtStr);
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		String baseUrl = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_URL);
		String baseUrlPort = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_PORT);
		String link = null;
		
		String lang = userSession.getUserLang();
		
		if (baseUrlPort != null) {
			if (super.insertServletNameInTemplateRefs)
				link = baseUrl + ":" + baseUrlPort + "/" + super.servletName + "/" + lang + "/users/change?token=" + token;
			else
				link = baseUrl + ":" + baseUrlPort + "/" + lang + "/users/change?token=" + token;
		}
		else {
			if (super.insertServletNameInTemplateRefs)
				link = baseUrl + "/" + super.servletName + "/" + lang + "/users/change?token=" + token;
			else
				link = baseUrl + "/" + lang + "/users/change?token=" + token;
		}

		final Map<String, String> variables = new HashMap<String, String>();
		
		variables.put("title", LanguageManager.getInstance().translate("base.mail.reset.title", userSession));
		
		variables.put("subtitle", LanguageManager.getInstance().translate("base.mail.reset.subtitle", userSession));
		variables.put("message", LanguageManager.getInstance().translate("base.mail.reset.msg", userSession));
		variables.put("link", link);
		
		MailerFactory.getInstance().mail(new String[] {email}, LanguageManager.getInstance().translate("base.mail.reset.title", userSession), variables, "reset", session);
		
		return new HandlerResponse(HandlerResponse.STATE_OK, "Mail sent!");
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return LogoutHandler.class;
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/users/reset.html";
	}

	@Override
	public String getEntity() {
		return entity;
	}

	@Override
	public boolean showMenu(Session userSession) {
		return false;
	}
	
}
