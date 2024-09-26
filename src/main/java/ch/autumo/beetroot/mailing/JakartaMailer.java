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
package ch.autumo.beetroot.mailing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;


/**
 * Jakarta Mailer.
 */
public class JakartaMailer extends AbstractMailer {

	private static final Logger LOG = LoggerFactory.getLogger(JakartaMailer.class.getName());

	@Override
	public void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {

		final Properties props = super.getProperties();

		jakarta.mail.Session mailSession = null;

		if (auth) {
			mailSession = jakarta.mail.Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password);
				}
			});
		}
		else {
			mailSession = jakarta.mail.Session.getDefaultInstance(props);
		}

		final MimeMessage message = new MimeMessage(mailSession);
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
			template = super.replaceAllLanguageVariables(template, session, mailformats[i]); 
			
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
