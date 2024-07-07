/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.call;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;

/**
 * TWILIO Caller.
 */
public class TwilioCaller implements Caller {

	protected static final Logger LOG = LoggerFactory.getLogger(TwilioCaller.class.getName());
	
	private String accountSid = null;
	private String authToken = null;
	private String phoneNumber = null;
	
	@Override
	public void init() throws Exception {
		
		final boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
		
		accountSid = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("call_twilio_account_sid", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("call_twilio_account_sid");
		
		authToken = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("sms_twilio_auth_token", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("call_twilio_auth_token");

		phoneNumber = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("call_twilio_phone_number", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("call_twilio_phone_number");
	}
	
	@Override
	public void call(String toNumber, String text) throws Exception {
		
		Twilio.init(accountSid, authToken);

		Call call = Call.creator(
	    		new PhoneNumber(toNumber),
	    		new PhoneNumber(phoneNumber), 
	    		new Twiml("<Response><Say>"+text+"</Say></Response>"))
			.create();
	    
	    LOG.info("Call made to '" + toNumber + "', SID="+call.getSid());
	}

}
