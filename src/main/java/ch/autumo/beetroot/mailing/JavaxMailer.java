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

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;

/**
 * Javax Mailer.
 */
public class JavaxMailer extends AbstractMailer {

	private static final Logger LOG = LoggerFactory.getLogger(JavaxMailer.class.getName());
	
	@Override
	public void mail(String[] to, String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {
		final Properties props = super.getProperties();
		props.put("mail.from", from);
		String msname = BeetRootDatabaseManager.getInstance().getProperty("mail.session.name");
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
		String from = BeetRootDatabaseManager.getInstance().getProperty("mail.mailer");
		from = from == null ? BeetRootConfigurationManager.getInstance().getString("mail_from") : from; 
		message.setFrom(new InternetAddress(from));
		// process receivers
		for (int i = 0; i < to.length; i++) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to[i]));
			LOG.info("Sending mail to '{}'.", to[i]);
		}
		message.setSubject(subject, "UTF-8");
		final Multipart multipart = new MimeMultipart();
		// TXT must be first always !
		Arrays.sort(mailformats, Collections.reverseOrder());
		MimeBodyPart messageBodyPart = null;
		for (int i = 0; i < mailformats.length; i++) {
			String template = super.loadTemplate(templateName, session, mailformats[i]);		
			template = super.replaceAllVariables(template, variables, mailformats[i]);
			template = super.replaceAllLanguageVariables(template, session, mailformats[i]); 
			messageBodyPart = new MimeBodyPart();
			if (mailformats[i].equalsIgnoreCase("html"))
				messageBodyPart.setContent(template, "text/html; charset=UTF-8");
			else
				messageBodyPart.setText(template, "UTF-8");
			multipart.addBodyPart(messageBodyPart);
		}
		message.setContent(multipart);
		message.saveChanges();
		// Send message
		Transport.send(message);
	}

}
