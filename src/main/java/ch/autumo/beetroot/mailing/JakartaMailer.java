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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;


/**
 * Jakarta Mailer.
 */
public class JakartaMailer extends AbstractMailer {

	private static final Logger LOG = LoggerFactory.getLogger(JakartaMailer.class.getName());

	/**
	 * Create a custom mail session with authentication or none.
	 *  
	 * @param auth authentication?
	 * @param user user or null (no authentication)
	 * @param password password or null (no authentication)
	 * @return mail session
	 * @throws Exception
	 */
	private Session createCustomMailSession(boolean auth, String user, String password) throws Exception {
	    Authenticator authenticator = null;
	    if (auth) {
	        authenticator = new Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(user, password);
	            }
	        };
	    }
	    Session mailSession = null;
	    if (auth) {
	    	mailSession = Session.getInstance(super.initialize(), authenticator);
	    	LOG.info("Custom Mail-session with authentication has been created.");	    	
	    } else {
	    	mailSession = Session.getDefaultInstance(super.initialize());
	    	LOG.info("Custom Mail-session without authentication has been created.");
	    }
	    return mailSession;
	}

	/**
	 * Looks up the JNDI mail session context.
	 *
	 * @param jndiContextName the JNDI context name (or mail session name)
	 * @return the mail session, configured with fallback credentials if needed
	 * @throws NamingException if the JNDI lookup fails
	 * @throws NumberFormatException if the port property is invalid
	 */
	private Session lookupJndiMailSession(String jndiContextName) throws Exception {
		// We still load properties from default configuration,
		// because we still need some value, e.g., 'from'
		// and possibly credentials
		super.initialize();
		Session session = null;
	    try {
            // Retrieve the JNDI Mail Session
		    final InitialContext ic = new InitialContext();
		    session = (Session) ic.lookup(jndiContextName);
		    if (session == null) {
		    	throw new NamingException("Mail session not found in JNDI context: " + jndiContextName);
		    }
            final Properties jndiProps = session.getProperties();
            if (Boolean.parseBoolean(jndiProps.getProperty("mail.smtp.auth", "false"))) {
                configureAuthentication(session, jndiProps);
            }
	    } catch (NamingException e) {
	        LOG.error("Failed to look up JNDI context '{}': {}", jndiContextName, e.getMessage(), e);
	        throw e;
	    } catch (NumberFormatException e) {
	        LOG.error("Invalid port format in JNDI properties for '{}': {}", jndiContextName, e.getMessage(), e);
	        throw e;
	    }
	    return session;
	}
	
	/**
	 * Configures SMTP authentication for the mail session if JNDI credentials are incomplete.
	 * 
	 * @param session the JNDI mail session
	 * @param jndiProps the JNDI mail session properties
	 * @throws NamingException if SMTP host is missing
	 */
	private void configureAuthentication(Session session, Properties jndiProps) throws NamingException {
	    final String smtpHost = jndiProps.getProperty("mail.smtp.host");
	    if (smtpHost == null) {
	        throw new NamingException("SMTP host property is missing in JNDI properties.");
	    }
	    String jndiUsername = jndiProps.getProperty("mail.smtp.user");
	    String jndiPassword = jndiProps.getProperty("mail.smtp.password");
	    if (jndiUsername == null || jndiPassword == null) {
	        LOG.info("Falling back to external configuration for SMTP credentials.");
	        session.setPasswordAuthentication(
	            new URLName(
	                "smtp",
	                smtpHost,
	                Integer.parseInt(jndiProps.getProperty("mail.smtp.port", String.valueOf(DEFAULT_SMTP_PORT))),
	                null,
	                user,
	                password
	            ),
	            new PasswordAuthentication(user, password)
	        );
	    } else {
	        LOG.warn(
	            "SMTP credentials found in JNDI context. For enhanced security, define them externally: '{}'.",
	            BeetRootConfigurationManager.getInstance().getConfigFileName()
	        );
	    }
	}
	
	/**
	 * Send MIME message.
	 * 
	 * @param message MIME message
	 * @param to email receivers
	 * @param subject email subject
	 * @param variables variables to parse in templates
	 * @param templateName template name
	 * @param session HTTP session or null if not called within a servlet context (default language used)
	 * @throws Exception exception
	 */	
	private void send(MimeMessage message, String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {
		message.setFrom(new InternetAddress(from));
		// Process recipients
        for (String recipient : to) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            LOG.info("Sending mail to '{}'.", recipient);
        }
		message.setSubject(subject, "UTF-8");
		final Multipart multipart = new MimeMultipart();
		// TXT must be first always !
		Arrays.sort(mailformats, Collections.reverseOrder());
		for (String format : mailformats) {
            final String template = loadTemplateWithVariables(templateName, session, variables, format);
            final MimeBodyPart messageBodyPart = new MimeBodyPart();
            if (format.equalsIgnoreCase("html")) {
                messageBodyPart.setContent(template, "text/html; charset=UTF-8");
            } else {
                messageBodyPart.setText(template, "UTF-8");
            }
            multipart.addBodyPart(messageBodyPart);
        }
		message.setContent(multipart);
		message.saveChanges();
		// Send message
		Transport.send(message);
	}
	
	@Override
	public void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {
		Session mailSession = null;
		try {
			String jndiName = BeetRootDatabaseManager.getInstance().getProperty("mail.session.name");
            if (jndiName == null || jndiName.isEmpty()) {
            	jndiName = BeetRootConfigurationManager.getInstance().getString("mail_session_name");
                if (jndiName == null || jndiName.isEmpty()) {
                    // No JNDI session name provided, create a custom session programmatically
                	mailSession = this.createCustomMailSession(auth, user, password);
                } else {
                    // JNDI mail session
                    mailSession = this.lookupJndiMailSession(jndiName);
                    LOG.info(
                    		"External Mail-session (JNDI) '{}' has been configured from configuration file '{}'.", 
                    		jndiName,
                    		BeetRootConfigurationManager.getInstance().getConfigFileName()
                    	);
                }
            } else {
                // Retrieve session name from database and create JNDI context
                mailSession = this.lookupJndiMailSession(jndiName);
                LOG.info("External Mail-session (JNDI) '{}' has been configured from database.", jndiName);
            }
		
            this.send(new MimeMessage(mailSession), to, subject, variables, templateName, session);
            
        } catch (NamingException e) {
            LOG.error("Error looking up mail session via JNDI.", e);
            throw new RuntimeException("JNDI lookup failed for mail session", e);
        } catch (Exception e) {
            LOG.error("Error configuring mail session.", e);
            throw new RuntimeException("Error configuring mail session", e);
        }
	}

}
