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

import java.util.HashMap;
import java.util.Map;

import org.passay.RuleResult;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultAddHandler;
import ch.autumo.beetroot.handler.HandlerResponse;
import ch.autumo.beetroot.utils.Utils;

/**
 * Users add handler. 
 */
public class UsersAddHandler extends DefaultAddHandler {
	
	public UsersAddHandler(String entity) {
		super(entity);
	}

	public UsersAddHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public HandlerResponse saveData(BeetRootHTTPSession session) throws Exception {
		
		final String pass = session.getParms().get("password");
		
		final boolean jsPwValidator = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WEB_PASSWORD_VALIDATOR);
		if (jsPwValidator) {
			final RuleResult rr = PasswordHelper.isValid(pass);
			if (!rr.isValid())
				return new HandlerResponse(HandlerResponse.STATE_NOT_OK, PasswordHelper.getHTMLMessages(rr));
		}
		
		return super.saveData(session);
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return UsersIndexHandler.class;
	}

	@Override
	public Map<String, Object> getAddMandatoryFields() {
		
		final Map<String, Object> mand = new HashMap<String, Object>();
		
		mand.put("created",  "NOW()");
		mand.put("modified", "NOW()");
		mand.put("secretkey", Utils.createSecretUserKey());
	    
		return mand;
	}

	@Override
	public Class<?> getBeanClass() {
		return User.class;
	}

	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("Administrator");
	}
	
	@Override
	public String getTitle(Session userSession) {
		return LanguageManager.getInstance().translate("base.name.users", userSession);
	}
	
}