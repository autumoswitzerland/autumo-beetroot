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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.Utils;

/**
 * Abstract mailer class.
 */
public abstract class AbstractMailer implements Mailer {

	private final static Logger LOG = LoggerFactory.getLogger(AbstractMailer.class.getName());
	
	protected String mailformats[] = null;
	protected boolean auth = false;
	protected boolean tlsEnable = false;
	
	protected int port = -1;
	protected String portStr = null;
	protected String host = null;
	
	protected boolean pwEncoded = false;
	protected String user = null;
	protected String password = null;
	
	protected String from = null;
	
	
	protected Properties getProperties() throws Exception {

		final Properties props = System.getProperties();
		
		mailformats = BeetRootConfigurationManager.getInstance().getSepValues("mail_formats");
		if (mailformats.length < 1)
			throw new IllegalArgumentException("At least one email format must be defined in configuration!");

		auth = BeetRootConfigurationManager.getInstance().getYesOrNo("mail_auth");
		tlsEnable = BeetRootConfigurationManager.getInstance().getYesOrNo("mail_tls_enable");
		
		portStr = BeetRootDatabaseManager.getProperty("mail.port");
		if (portStr != null)
			port = Integer.valueOf(portStr).intValue();
		if (port < 0) {
			port = BeetRootConfigurationManager.getInstance().getInt("mail_port");
			if (port == -1) {
				LOG.warn("Using mail port 25.");
				port = 25;
			}
		}
		   
		host = BeetRootDatabaseManager.getProperty("mail.host");
		host = (host == null || host.length() == 0) ? BeetRootConfigurationManager.getInstance().getString("mail_host") : host; 

		pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
		user = BeetRootConfigurationManager.getInstance().getString("mail_user");
		password = pwEncoded
				? BeetRootConfigurationManager.getInstance().getDecodedString("mail_password",
						SecureApplicationHolder.getInstance().getSecApp())
				: BeetRootConfigurationManager.getInstance().getString("mail_password");

		from = BeetRootDatabaseManager.getProperty("mail.mailer");
		from = (from == null || from.length() == 0) ? BeetRootConfigurationManager.getInstance().getString("mail_from") : from; 
		
		
		props.put(Constants.MAIL_TRANSPORT_PROTOCOL, "smtp");
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
		
		return props;
	}
	
	protected String loadTemplate(String templateName, BeetRootHTTPSession session, String extension) throws Exception {
		
		Session userSession = null;
		if (session != null)
			userSession = session.getUserSession();
		
		
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		File f = null;	
		InputStream is = null;
		boolean streamIt = false;

		String res = null;
		
		
		if (session == null) {
			res = LanguageManager.getInstance().getResourceByLang("web/"+extension+"/:lang/email/" + templateName + "." + extension, LanguageManager.DEFAULT_LANG);
		} else {
			if (userSession == null)
				res = LanguageManager.getInstance().getResource("web/"+extension+"/:lang/email/" + templateName + "." + extension, Utils.normalizeUri(session.getUri()));
			else
				res = LanguageManager.getInstance().getResource("web/"+extension+"/:lang/email/" + templateName + "." + extension, userSession);
		}
		if (context != null) {
			f = new File(Utils.getRealPath(context) + res);
			if (!f.exists()) {
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + res);
				streamIt = true;
			}
		} else {
			f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + res);
		}
		
		
		if (!f.exists() || (streamIt && is == null)) {
			streamIt = false;
			LOG.warn("Resource '"+res+"' doesn't exist, looking up with default language '"+LanguageManager.DEFAULT_LANG+"'!");
			if (session == null) {
				res = LanguageManager.getInstance().getResourceByLang("web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension, LanguageManager.DEFAULT_LANG);
			} else {
				if (userSession == null)
					res = LanguageManager.getInstance().getResource("web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension, session.getUri());
				else
					res = LanguageManager.getInstance().getResource("web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension, userSession);
			}
			if (context != null) {
				f = new File(Utils.getRealPath(context) + res);
				if (!f.exists()) {
					is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + res);
					streamIt = true;
				}
			} else {
				f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + res);	
			}
			
			
			if (!f.exists() || (streamIt && is == null)) {
				streamIt = false;
				LOG.warn("Resource '"+res+"' doesn't exist, trying with NO language!");
				if (session == null) {
					res = LanguageManager.getInstance().getResourceByLang("web/"+extension+"/email/"+templateName+"."+extension, LanguageManager.DEFAULT_LANG);
				} else {
					res = LanguageManager.getInstance().getResourceWithoutLang("web/"+extension+"/email/"+templateName+"."+extension, session.getUri());
				}
				if (context != null) {
					f = new File(Utils.getRealPath(context) + res);
					if (!f.exists()) {
						is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + res);
						streamIt = true;
					}
				} else {
					f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + res);
				}
				
				if (!f.exists() || (streamIt && is == null)) {
					LOG.warn("Resource '" + res + "' doesn't exist, that's an error!");
					throw new FileNotFoundException("No email template for name '" + templateName + "' found at all!");
				}
			}
		}		
		
		
		BufferedReader br = null;
		if (context != null) {
			if (is != null)
				br = new BufferedReader(new InputStreamReader(is));
			else
				br = new BufferedReader(new FileReader(Utils.getRealPath(context) + res));
		} else {
			if (is != null)
				br = new BufferedReader(new InputStreamReader(is));
			else
				br = new BufferedReader(new FileReader(BeetRootConfigurationManager.getInstance().getRootPath() + res));
		}

		
		final StringBuffer sb = new StringBuffer();
	    String line;
	    while ((line = br.readLine()) != null)
	    	sb.append(line+"\n");
	    br.close();
		return sb.toString();
	}
	
	protected String replaceAllVariables(String template, Map<String, String> variables, String extension) {
		
		String baseUrl = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_URL);
		String baseUrlPort = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_PORT);
		
		String base = null;
		if (baseUrlPort != null)
			base = baseUrl + ":" + baseUrlPort;
		else
			base = baseUrl ;

		String servletName = null;
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		if (context != null) {
			servletName = BeetRootConfigurationManager.getInstance().getString("web_html_ref_pre_url_part");
			if (servletName != null) {
				base += ("/" + servletName);
			}
		}
		
		// we have no URLs pointing to images in txt, only html
		if (extension.toLowerCase().equals("html")) {
			if (template.contains("{$ws_url}"))
				template = template.replaceAll("\\{\\$ws_url\\}", base);
		}
		
		final Set<String> names = variables.keySet();
		
		for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
			
			final String name = iterator.next();
			final String variable = variables.get(name);

			if (variable != null && variable.length() != 0)
				template = template.replace("{$"+name+"}", variable);
		}		
		
		return template;
	}
	
	/**
	 * @see {@link Mailer#mail(String[], String, Map, String, BeetRootHTTPSession)}
	 */
	public abstract void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception;

}
