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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.mailing.MailerFactory;
import ch.autumo.beetroot.utils.GUIDGenerator;
import ch.autumo.beetroot.utils.Time;

/**
 * Default login handler.
 */
public class ResetHandler extends BaseHandler {

	protected static final Logger LOG = LoggerFactory.getLogger(ResetHandler.class.getName());
	
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
				stmtStr = "UPDATE users SET lasttoken='" + token + "', modified=" + Time.nowTimeStamp() + " WHERE id=" + userid;
			else
				stmtStr = "UPDATE users SET lasttoken='" + token + "', modified='" + Time.nowTimeStamp() + "' WHERE id=" + userid;
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
