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
import java.util.Calendar;
import java.util.Date;

import org.passay.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.Utils;

/**
 * Default login handler.
 */
public class ChangeHandler extends BaseHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(ChangeHandler.class.getName());
	
	private String entity = null;
	
	private int userid = -1;
	
	public ChangeHandler(String entity) {
		this.entity = entity;
	}

	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {

		// email brought us here with a token or a cancel!
		
		String cancel = session.getParms().get("cancel");
		String token = session.getParms().get("token");
		
		
		if (cancel != null && cancel.length() != 0) {
			
			final Session s = session.getUserSession();
			userid = ((Integer) s.get("resetid")).intValue();
			
			Connection conn = null;
			Statement stmt = null;
			
			try {
				
				conn = BeetRootDatabaseManager.getInstance().getConnection();
				stmt = conn.createStatement();
			
				String stmtStr = null;
				if (BeetRootDatabaseManager.getInstance().isOracleDb())
					stmtStr = "UPDATE users SET lasttoken='NONE', modified=" + Utils.nowTimeStamp() + " WHERE id=" + userid;
				else
					stmtStr = "UPDATE users SET lasttoken='NONE', modified='" + Utils.nowTimeStamp() + "' WHERE id=" + userid;
				stmt.executeUpdate(stmtStr);
			
			} finally {
				
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			
			return new HandlerResponse(HandlerResponse.STATE_WARNING, "Password reset canceled.");
		}
		
		if (token != null && token.length() != 0) {
			
			token = token.trim();
			
			Connection conn = null;
			Statement stmt = null;
			ResultSet set = null;
			Date modified = null;
			
			try {
				
				conn = BeetRootDatabaseManager.getInstance().getConnection();
				stmt = conn.createStatement();
			
				LOG.debug("Reset token to lookup in DB: "+token);
				
				String stmtStr = "SELECT id, modified FROM users WHERE lasttoken='" + token + "'";
				
				set = stmt.executeQuery(stmtStr);
				boolean found = set.next();
			
				if (!found) {
	
					set.close();
					stmt.close();
					conn.close();
					
					userid = -1;
					LOG.debug("Invalid token used: "+token);
					return new HandlerResponse(HandlerResponse.STATE_NOT_OK, "This token is invalid!");
				}
				
				userid = set.getInt(1);
				modified = set.getTimestamp(2);

			} finally {
				
				if (set != null)
					set.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			
			Calendar cal = Calendar.getInstance(); 
			cal.setTime(modified); 
			cal.add(Calendar.DATE, 1);
			final Date date = cal.getTime();
			
			// 24 hour link is invalid!
			if (new Date(System.currentTimeMillis()).after(date)) {
				
				LOG.debug("Invalid token used: "+token);
				
				BeetRootDatabaseManager.getInstance().resetToken(userid);

				userid = -1;
				
				return new HandlerResponse(HandlerResponse.STATE_NOT_OK, "Password reset link is not valid anymore, 24 hours have passed.");
			}
			
			LOG.debug("Reset token found, user id: "+userid);
			
			final Session s = SessionManager.getInstance().findOrCreate(session);
			s.set("resetid", Integer.valueOf(userid));
		}
		
		return null;
	}

	@Override
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		
		String pass = session.getParms().get("password");
		
		final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
		if (jsPwValidator) {
			final RuleResult rr = PasswordHelper.isValid(pass);
			if (!rr.isValid())
				return new HandlerResponse(HandlerResponse.STATE_NOT_OK, PasswordHelper.getHTMLMessages(rr));
		}
		
		try {
			if (pass != null && pass.length() != 0) { // && suserid != null && suserid.length() != 0) {
				
				if (BeetRootConfigurationManager.getInstance().getYesOrNo("db_pw_encoded")) {
					pass = Utils.encode(pass, SecureApplicationHolder.getInstance().getSecApp());
				};
				
				final Session s = session.getUserSession();
				userid = ((Integer) s.get("resetid")).intValue();
				
				Connection conn = null;
				Statement stmt = null;
				
				try {
					conn = BeetRootDatabaseManager.getInstance().getConnection();
					stmt = conn.createStatement();
				
					String stmtStr = "UPDATE users SET password='" + pass + "', lasttoken='NONE' WHERE id=" + userid;
					stmt.executeUpdate(stmtStr);
				
				} finally {
					
					if (stmt != null)
						stmt.close();
					if (conn != null)
						conn.close();
				}
				
				return new HandlerResponse(HandlerResponse.STATE_OK, "Password reset!");
			}
		} catch (Exception e) {
			return new HandlerResponse(HandlerResponse.STATE_NOT_OK, "Password reset failed!");
		}
		
		return new HandlerResponse(HandlerResponse.STATE_NOT_OK, "Password reset failed!");
	}
	
	@Override
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		
		final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
		if (text.indexOf("{$passElem}") != -1) {
			if (jsPwValidator)
				text = text.replace("{$passElem}", "<div id=\"password\" data-lang=\""+session.getUserSession().getUserLang()+"\"></div>");
			else
				text = text.replace("{$passElem}", "<input type=\"password\" name=\"password\" id=\"password\" />");
		}
		return text;
	}

	@Override
	public Class<?> getRedirectHandler() {
		return LoginHandler.class;
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/users/change.html";
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
