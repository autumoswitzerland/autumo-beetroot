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
package ch.autumo.beetroot.mailing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;

/**
 * Javax Mailer.
 */
public class JavaxMailer extends AbstractMailer {

	private final static Logger LOG = LoggerFactory.getLogger(JavaxMailer.class.getName());
	
	@Override
	public void mail(String[] to, String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {

		final Properties props = super.getProperties();
		props.put("mail.from", from);
		
		String msname = BeetRootDatabaseManager.getProperty("mail.session.name");
		if (msname == null || msname.length() == 0) {
			msname = BeetRootConfigurationManager.getInstance().getString("mail_session_name");
			if (msname == null || msname.length() == 0)
				msname = "beetRootMailSession";
		}
		
		final InitialContext ic = new InitialContext();
		final Session initSession = (Session) ic.lookup(msname);
		@SuppressWarnings("static-access")
		final Session mailSession = initSession.getInstance(props);
		
		if (auth) {
		
			mailSession.setPasswordAuthentication(
						new URLName("smtp", host, -1, null, user, null),
						new PasswordAuthentication(user, password)
					);		
		}

		final MimeMessage message = new MimeMessage(mailSession);
		
		String from = BeetRootDatabaseManager.getProperty("mail.mailer");
		from = from == null ? BeetRootConfigurationManager.getInstance().getString("mail_from") : from; 
		
		message.setFrom(new InternetAddress(from));

		// process receivers
		for (int i = 0; i < to.length; i++) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to[i]));
			LOG.info("Sending mail to '"+to[i]+"'.");
		}

		message.setSubject(subject);

		final Multipart multipart = new MimeMultipart();
		
		// txt must be first always !
		Arrays.sort(mailformats, Collections.reverseOrder());
		
		BodyPart messageBodyPart = null;
		for (int i = 0; i < mailformats.length; i++) {
			
			String template = super.loadTemplate(templateName, session, mailformats[i]);		
			template = super.replaceAllVariables(template, variables, mailformats[i]);
			
			messageBodyPart = new MimeBodyPart();
			if (mailformats[i].toLowerCase().equals("html"))
				messageBodyPart.setContent(template, "text/html");
			else
				messageBodyPart.setText(template);
			
			multipart.addBodyPart(messageBodyPart);
		}
		
		message.setContent(multipart);
		message.saveChanges();

		// Send message
		Transport.send(message);
	}

}
