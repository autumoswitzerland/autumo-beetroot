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
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.utils.web.Web;

/**
 * Abstract mailer class.
 */
public abstract class AbstractMailer implements Mailer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractMailer.class.getName());
	
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
		
		portStr = BeetRootDatabaseManager.getInstance().getProperty("mail.port");
		if (portStr != null)
			port = Integer.valueOf(portStr).intValue();
		if (port < 0) {
			port = BeetRootConfigurationManager.getInstance().getInt("mail_port");
			if (port == -1) {
				LOG.warn("Using mail port 25.");
				port = 25;
			}
		}
		   
		host = BeetRootDatabaseManager.getInstance().getProperty("mail.host");
		host = (host == null || host.length() == 0) ? BeetRootConfigurationManager.getInstance().getString("mail_host") : host; 

		pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
		user = BeetRootConfigurationManager.getInstance().getString("mail_user");
		password = pwEncoded
				? BeetRootConfigurationManager.getInstance().getDecodedString("mail_password",
						SecureApplicationHolder.getInstance().getSecApp())
				: BeetRootConfigurationManager.getInstance().getString("mail_password");

		from = BeetRootDatabaseManager.getInstance().getProperty("mail.mailer");
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
		
		final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
		File f = null;	
		InputStream is = null;
		boolean streamIt = false;
		
		String lang = LanguageManager.getInstance().retrieveLanguage(session);
		String res = LanguageManager.getInstance().getResourceByLang("web/"+extension+"/:lang/email/" + templateName + "." + extension, lang);

		if (context != null) {
			f = new File(Web.getRealPath(context) + res);
			if (!f.exists()) {
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + res);
				streamIt = true;
			}
		} else {
			f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + res);
		}
		if (!f.exists() || (streamIt && is == null)) {
			streamIt = false;
			LOG.trace("Resource '"+res+"' doesn't exist, looking up with default language '"+LanguageManager.DEFAULT_LANG+"'!");
			res = "web/"+extension+"/"+LanguageManager.DEFAULT_LANG+"/email/" + templateName + "." + extension;
			if (context != null) {
				f = new File(Web.getRealPath(context) + res);
				if (!f.exists()) {
					is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + res);
					streamIt = true;
				}
			} else {
				f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + res);	
			}
			if (!f.exists() || (streamIt && is == null)) {
				streamIt = false;
				LOG.trace("Resource '"+res+"' doesn't exist, trying with NO language!");
				res = "web/"+extension+"/email/"+templateName+"."+extension;
				if (context != null) {
					f = new File(Web.getRealPath(context) + res);
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
				br = new BufferedReader(new FileReader(Web.getRealPath(context) + res));
		} else {
			if (is != null)
				br = new BufferedReader(new InputStreamReader(is));
			else
				br = new BufferedReader(new FileReader(BeetRootConfigurationManager.getInstance().getRootPath() + res));
		}

		
		final StringBuilder sb = new StringBuilder();
	    String line;
	    while ((line = br.readLine()) != null)
	    	sb.append(line+"\n");
	    br.close();
		return sb.toString();
	}

	protected String replaceAllLanguageVariables(String template, BeetRootHTTPSession session, String extension) {
		// Only when switched on!
		if (BeetRootConfigurationManager.getInstance().translateTemplates()) {
			int idx = -1;
			while ((idx = template.indexOf(BaseHandler.TAG_PREFIX_LANG)) != -1) {
				final int pos1 = idx + BaseHandler.TAG_PREFIX_LANG.length();
				final int pos2 = template.indexOf("}", idx + BaseHandler.TAG_PREFIX_LANG.length());
				int posC = template.indexOf(",", idx + BaseHandler.TAG_PREFIX_LANG.length());
				// if a comma is found outside the tag it refers not ro a replace variable!
				if (posC > pos2)
					posC = -1;
				String totrans = null; 
				String subValues = null; 
				String subValuesArr[] = null; 
				if (posC == -1) {
					totrans = template.substring(pos1, pos2); // no values to replace
				}
				else {
					totrans = template.substring(pos1, posC);
					subValues = template.substring(posC + 1, pos2);
					if (subValues.length() > 0) {
						subValuesArr = subValues.trim().split("\\s*,\\s*");
					}
				}
				String trans = "";
				if (totrans.length() > 0) {
					if (extension.toLowerCase().equals("html"))
						trans = LanguageManager.getInstance().translateTemplateFullEscape(totrans.trim(), session, subValuesArr);
					else
						trans = LanguageManager.getInstance().translateTemplate(totrans.trim(), session, subValuesArr, false);
				}
				template = template.substring(0, idx) + trans + template.substring(pos2 + 1);
			}
		}
		return template;
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
	 * See {@link Mailer#mail(String[], String, Map, String, BeetRootHTTPSession)}.
	 */
	public abstract void mail(String to[], String subject, Map<String, String> variables, String templateName, BeetRootHTTPSession session) throws Exception;

}
