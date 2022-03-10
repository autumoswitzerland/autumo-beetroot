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
package ch.autumo.beetroot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Mailer.
 */
public class Mailer {

	private final static Logger LOG = LoggerFactory.getLogger(Mailer.class.getName());

	
	private static String loadTemplate(String templateName, BeetRootHTTPSession session, String extension) throws Exception {
		
		Session userSession = session.getUserSession();
		String file = null;
		
		if (userSession == null)
			file = LanguageManager.getInstance().getResource("web/"+extension+"/:lang/email/" + templateName + "." + extension, Utils.normalizeUri(session.getUri()));
		else
			file = LanguageManager.getInstance().getResource("web/"+extension+"/:lang/email/" + templateName + "." + extension, userSession);

		final ServletContext context = ConfigurationManager.getInstance().getServletContext();
		File f = null;	
		InputStream is = null;
		boolean streamIt = false;
		String cp = "";
		if (context != null) {
			
			cp = context.getRealPath("/");
			if (!cp.endsWith(Utils.FILE_SEPARATOR))
				cp += Utils.FILE_SEPARATOR;

			f = new File(cp + file);
			if (!f.exists()) {
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + file);
				streamIt = true;
			}
		}
		else
			f = new File(file);
		
		if (!f.exists() || (streamIt && is == null)) {
			
			streamIt = false;
			
			LOG.warn("Resource '"+file+"' doesn't exist, looking up with default language '"+LanguageManager.DEFAULT_LANG+"'!");
			if (userSession == null)
				file = LanguageManager.getInstance().getResource("web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension, session.getUri());
			else
				file = LanguageManager.getInstance().getResource("web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension, userSession);

			if (context != null) {
				
				f = new File(cp + file);
				if (!f.exists()) {
					is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + file);
					streamIt = true;
				}
			}
			else
				f = new File(file);	
			
			if (!f.exists() || (streamIt && is == null)) {
				
				streamIt = false;
				
				LOG.warn("Resource '"+file+"' doesn't exist, trying with NO language!");
				file = LanguageManager.getInstance().getResourceWithoutLang("web/"+extension+"/email/"+templateName+"."+extension, session.getUri());
				
				if (context != null) {
					
					f = new File(cp + file);
					if (!f.exists()) {
						is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + file);
						streamIt = true;
					}
				}
				else
					f = new File(file);
				
				if (!f.exists() || (streamIt && is == null)) {
					LOG.warn("Resource '"+file+"' doesn't exist, that's an error!");
					throw new FileNotFoundException("No email template for name '"+templateName+"' found at all!");
				}
			}
		}		
		
		BufferedReader br = null; 
		if (context != null && is != null) {
			br = new BufferedReader(new InputStreamReader(is));
		} else {
			br = new BufferedReader(new FileReader(cp + file));
		}
		StringBuffer sb = new StringBuffer();
	    String line;
	    while ((line = br.readLine()) != null)
	    	sb.append(line+"\n");
	    br.close();
		
		return sb.toString();
	}
	
	private static String replaceAllVariables(String template, Map<String, String> variables, String extension) {
		
		String baseUrl = ConfigurationManager.getInstance().getString(Constants.KEY_WS_URL);
		String baseUrlPort = ConfigurationManager.getInstance().getString(Constants.KEY_WS_PORT);
		
		String base = null;
		if (baseUrlPort != null)
			base = baseUrl + ":" + baseUrlPort;
		else
			base = baseUrl;

		String servletName = null;
		final ServletContext context = ConfigurationManager.getInstance().getServletContext();
		if (context != null) {
			servletName = ConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
			if (servletName != null) {
				base += ("/" + servletName);
			}
		}
		
		// we have no URLs pointing to images in txt, only html
		if (extension.toLowerCase().equals("html")) {
			if (template.contains("{$ws_url}"))
				template = template.replaceAll("\\{\\$ws_url\\}", base);
		}
		
		Set<String> names = variables.keySet();
		
		for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
			
			final String name = iterator.next();
			final String variable = variables.get(name);

			if (variable != null && variable.length() != 0)
				template = template.replace("{$"+name+"}", variable);
		}		
		
		return template;
	}
	
	/**
	 * Mail. Only HTML templates are supported atm.
	 * 
	 * @param to email receiver addresses
	 * @param variables variables to parse in templates
	 * @param templateName template name
	 * @param session HTTP session
	 * @throws Exception
	 */
	public static void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception {

		final String mailformats[] = ConfigurationManager.getInstance().getSepValues("mail_formats");
		if (mailformats.length < 1)
			throw new IllegalArgumentException("At least one email format must be defined in configuration!");
		
		final Properties props = System.getProperties();

		boolean auth = ConfigurationManager.getInstance().getYesOrNo("mail_auth");
		boolean tlsEnable = ConfigurationManager.getInstance().getYesOrNo("mail_tls_enable");
		
		int port = -1;
		String portStr = DatabaseManager.getProperty("mail.port");
		if (portStr != null)
			port = Integer.valueOf(portStr).intValue();
		if (port < 0) {
			port = ConfigurationManager.getInstance().getInt("mail_port");
			if (port == -1) {
				LOG.warn("Using mail port 25.");
				port = 25;
			}
		}
		   
		String host = DatabaseManager.getProperty("mail.host");
		host = host == null ? ConfigurationManager.getInstance().getString("mail_host") : host; 

		props.put(Constants.MAIL_SMTP_HOST_KEY, host);
		props.put(Constants.MAIL_SMTP_PORT_KEY, "" + port);
		props.put(Constants.MAIL_SMTP_AUTH_KEY, Boolean.valueOf(auth).toString());

		if (tlsEnable) {
			props.put(Constants.MAIL_SMTP_TLS_ENABLE_KEY, Boolean.valueOf(tlsEnable).toString());
			// Use the following if you need SSL
			props.put("mail.smtp.socketFactory.port", port);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
		}
		jakarta.mail.Session mailSession = null;

		boolean pwEncoded = ConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);

		final String u = ConfigurationManager.getInstance().getString("mail_user");
		final String p = pwEncoded
				? ConfigurationManager.getInstance().getDecodedString("mail_password",
						SecureApplicationHolder.getInstance().getSecApp())
				: ConfigurationManager.getInstance().getString("mail_password");

		if (auth) {
			mailSession = jakarta.mail.Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(u, p);
				}
			});
		}
		else {
			mailSession = jakarta.mail.Session.getDefaultInstance(props);
		}

		final MimeMessage message = new MimeMessage(mailSession);
		
		String from = DatabaseManager.getProperty("mail.mailer");
		from = from == null ? ConfigurationManager.getInstance().getString("mail_from") : from; 
		
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
			
			String template = loadTemplate(templateName, session, mailformats[i]);		
			template = replaceAllVariables(template, variables, mailformats[i]);
			
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
