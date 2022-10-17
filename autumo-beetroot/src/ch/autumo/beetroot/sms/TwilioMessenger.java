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
package ch.autumo.beetroot.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.SecureApplicationHolder;

/**
 * Twilio SMS Messenger
 */
public class TwilioMessenger implements Messenger {

	protected final static Logger LOG = LoggerFactory.getLogger(TwilioMessenger.class.getName());
	
	private String accountSid = null;
	private String serviceSid = null;
	private String authToken = null;
	
	@Override
	public void init() throws Exception {
		
		final boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
		
		accountSid = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("sms_twilio_account_sid", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("sms_twilio_account_sid");
		serviceSid = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("sms_twilio_service_sid", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("sms_twilio_service_sid");
		authToken = (pwEncoded) ?
				BeetRootConfigurationManager.getInstance().getDecodedString("sms_twilio_auth_token", SecureApplicationHolder.getInstance().getSecApp()) :
				BeetRootConfigurationManager.getInstance().getString("sms_twilio_auth_token");
	}
	
	@Override
	public void sms(String toNumber, String text) throws Exception {
		
		Twilio.init(accountSid, authToken);

	    final Message message = Message.creator(
	    		new PhoneNumber(toNumber),
	    		serviceSid, 
	    		text).create();

	    
	    LOG.info("Message sent to '" + toNumber + "', SID="+message.getSid());
	}

}
